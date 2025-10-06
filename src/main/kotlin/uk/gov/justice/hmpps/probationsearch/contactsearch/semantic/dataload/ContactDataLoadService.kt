package uk.gov.justice.hmpps.probationsearch.contactsearch.semantic.dataload

import com.microsoft.applicationinsights.TelemetryClient
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.sentry.Sentry
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.ErrorCause
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.opensearch._types.VersionType
import org.opensearch.client.opensearch.core.bulk.BulkOperation
import org.opensearch.client.opensearch.core.search.TrackHits
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.DataLoadFailureException
import uk.gov.justice.hmpps.probationsearch.IndexNotReadyException
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.AsyncExtensions.runAsync
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.matchesCrn
import uk.gov.justice.hmpps.probationsearch.contactsearch.semantic.ContactSemanticSearchService.Companion.INDEX_NAME
import uk.gov.justice.hmpps.probationsearch.contactsearch.semantic.block.ContactBlockService
import uk.gov.justice.hmpps.probationsearch.services.DeliusService
import uk.gov.justice.hmpps.probationsearch.utils.Retry.retry
import java.io.StringReader
import java.util.concurrent.CompletableFuture.allOf
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.TimeoutException
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.measureTimedValue

@Service
class ContactDataLoadService(
  private val deliusService: DeliusService,
  private val openSearchClient: OpenSearchClient,
  private val telemetryClient: TelemetryClient,
  private val blockService: ContactBlockService,
  @param:Value($$"${dataload.ondemand.batch.size}")
  private val onDemandDataloadBatchSize: Int,
  @param:Qualifier("applicationTaskExecutor")
  private val executor: SimpleAsyncTaskExecutor,
) {
  fun loadDataOnDemand(crn: String) {
    // Do not proceed if the CRN is "blocked" (i.e. there is an ongoing data load job). If the block is stale, rollback any partially loaded data and proceed.
    blockService.checkIfBlockedOrRollbackIfStale(crn) { rollback { rollbackPartialLoad(crn) } }

    // If the CRN has not been indexed before, load all contacts on-demand before first search
    if (!crnExistsInIndex(crn)) {
      try {
        // Load the data asynchronously, so that it can be left to complete if the API call times out
        executor.runAsync {
          blockService.doWithBlock(crn) {
            action { loadData(crn) }
            rollback { rollbackPartialLoad(crn) }
          }
        }.get(30, SECONDS)
      } catch (_: TimeoutException) {
        throw IndexNotReadyException("Timed out waiting for contacts with CRN=$crn to be indexed for semantic search. The indexing process has not been interrupted.")
      }
    }
  }

  @WithSpan
  fun loadData(crn: String) {
    val (operations, duration) = measureTimedValue {
      val mapper = openSearchClient._transport().jsonpMapper()
      deliusService.getContacts(crn).map { contact ->
        BulkOperation.of { bulk ->
          bulk.index {
            it.id(contact.contactId.toString())
              .version(contact.version)
              .versionType(VersionType.External)
              .document(JsonData.from(mapper.jsonProvider().createParser(StringReader(contact.json)), mapper))
          }
        }
      }.also { operations ->
        // Split request into batches and run each in parallel
        allOf(
          *operations
            .chunked(onDemandDataloadBatchSize)
            .map {
              executor.runAsync {
                try {
                  sendBulkRequestWithRetry(crn, it)
                } catch (e: Exception) {
                  Sentry.captureException(e)
                  telemetryClient.trackException(e)
                }
              }
            }
            .toTypedArray(),
        ).join()
      }
    }
    telemetryClient.trackEvent(
      "OnDemandDataLoad",
      mapOf("crn" to crn),
      mapOf(
        "duration" to duration.toDouble(MILLISECONDS),
        "count" to operations.size.toDouble(),
      ),
    )
  }

  fun crnExistsInIndex(crn: String): Boolean {
    val count = retry {
      openSearchClient.search(
        { searchRequest ->
          searchRequest.index(INDEX_NAME)
            .query { q -> q.matchesCrn(crn) }
            .trackTotalHits(TrackHits.of { it.count(1) })
            .size(0)
        },
        Any::class.java,
      ).hits().total()?.value() ?: 0
    }
    return count > 0
  }

  private fun sendBulkRequestWithRetry(
    crn: String,
    operations: List<BulkOperation>,
    maxAttempts: Int = 3,
    delay: LongArray = longArrayOf(0, 1, 4),
    attempt: Int = 1,
  ) {
    val (response, duration) = measureTimedValue {
      openSearchClient.bulk {
        it.index(INDEX_NAME)
          .refresh(Refresh.True)
          .timeout { t -> t.time("5m") } // Increased timeout, in case a bulk request takes longer than the default 30 seconds
          .operations(operations)
      }
    }

    val failedOperations = operations.filterIndexed { index, _ -> shouldRetry(response.items()[index].error()) }
    if (failedOperations.isNotEmpty()) {
      val errors = response.items().mapNotNull { it.error() }
        .filter { it.type() != null } // type is annotated @Nonnull, but in practice can be null
      telemetryClient.trackEvent(
        "OnDemandDataLoadBatchAttemptFailure",
        mapOf(
          "crn" to crn,
          "attempt" to attempt.toString(),
          "errors" to errors.joinToString(", ", "[", "]") { it.toJsonString() },
        ),
        mapOf(
          "duration" to duration.toDouble(MILLISECONDS),
          "attemptedOperations" to operations.size.toDouble(),
          "failedOperations" to failedOperations.size.toDouble(),
        ),
      )
      if (attempt >= maxAttempts) {
        val errorReasonCounts = errors.filter { shouldRetry(it) }.mapNotNull { it.reason() }
          .groupBy { it }.mapValues { it.value.size }
        throw DataLoadFailureException("Data load for $crn failed with ${errorReasonCounts.size} errors ($errorReasonCounts)")
      }

      // Retry after delay
      SECONDS.sleep(delay[minOf(attempt, delay.size) - 1])
      sendBulkRequestWithRetry(crn, failedOperations, attempt + 1)
    }
  }

  private fun shouldRetry(error: ErrorCause?): Boolean {
    return error != null && error.type() != null // type is annotated @Nonnull, but in practice can be null
      && error.type() != "version_conflict_engine_exception"
  }

  private fun rollbackPartialLoad(crn: String) = retry {
    openSearchClient.deleteByQuery { it.index(INDEX_NAME).query { q -> q.matchesCrn(crn) } }
  }
}