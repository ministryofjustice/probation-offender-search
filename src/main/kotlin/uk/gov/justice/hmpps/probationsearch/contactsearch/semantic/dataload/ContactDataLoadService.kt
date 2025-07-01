package uk.gov.justice.hmpps.probationsearch.contactsearch.semantic.dataload

import com.microsoft.applicationinsights.TelemetryClient
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.opensearch._types.VersionType
import org.opensearch.client.opensearch.core.bulk.BulkOperation
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem
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
            .map { executor.runAsync { sendBulkRequestWithRetry(crn, it) } }
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
    val count = openSearchClient.search(
      { searchRequest ->
        searchRequest.index(INDEX_NAME)
          .routing(crn)
          .query { q -> q.matchesCrn(crn) }
          .trackTotalHits(TrackHits.of { it.count(1) })
          .size(0)
      },
      Any::class.java,
    ).hits().total()?.value() ?: 0
    return count > 0
  }

  private fun sendBulkRequestWithRetry(
    crn: String,
    operations: List<BulkOperation>,
    maxAttempts: Int = 3,
    delay: LongArray = longArrayOf(2, 4, 8),
    attempt: Int = 1,
  ) {
    val response = openSearchClient.bulk {
      it.index(INDEX_NAME)
        .routing(crn)
        .refresh(Refresh.True)
        .timeout { t -> t.time("5m") } // Increased timeout, in case a bulk request takes longer than the default 30 seconds
        .operations(operations)
    }

    val failedOperations = operations.filterIndexed { index, op -> shouldRetry(response.items()[index]) }
    if (failedOperations.isNotEmpty()) {
      if (attempt >= maxAttempts) {
        val errors = response.items().filter { shouldRetry(it) }.mapNotNull { it.error()?.reason() }
          .groupBy { it }.mapValues { it.value.size }
        throw DataLoadFailureException("Data load for $crn failed with ${errors.size} errors ($errors)")
      }

      // Retry after delay
      SECONDS.sleep(delay[minOf(attempt, delay.size) - 1])
      sendBulkRequestWithRetry(crn, failedOperations, attempt + 1)
    }
  }

  private fun shouldRetry(item: BulkResponseItem): Boolean {
    val error = item.error()
    return error != null && error.type() != "version_conflict_engine_exception"
  }

  private fun rollbackPartialLoad(crn: String) {
    openSearchClient.deleteByQuery {
      it.index(INDEX_NAME)
        .query { q -> q.matchesCrn(crn) }
        .routing(crn)
    }
  }
}