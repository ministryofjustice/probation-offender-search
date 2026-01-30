package uk.gov.justice.hmpps.probationsearch.contactsearch.semantic.block

import com.microsoft.applicationinsights.TelemetryClient
import io.sentry.Sentry
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.IndexQuery
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.IndexNotReadyException
import uk.gov.justice.hmpps.probationsearch.utils.Retry.retry
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit


@Service
class ContactBlockService(
  private val restTemplate: OpenSearchRestTemplate,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    const val CONTACT_SEMANTIC_BLOCK = "contact-semantic-block-primary"
  }

  fun checkIfBlockedOrRollbackIfStale(crn: String, retries: Int = 1, rollback: () -> Unit) {
    val isBlocked = retryUntilBlockIsCleared(retries, Duration.ofSeconds(5)) {
      // if block exists and was created more than 5 minutes ago, then rollback the partial load and delete the block
      getBlock(crn)?.timestamp?.let {
        if (it.isLongerAgoThan(Duration.ofMinutes(5))) {
          rollback()
          unblock(crn)
          return@let false
        }
        return@let true
      } ?: false
    }
    // if the block still exists after 1 retry 5 seconds later, then throw an IndexNotReadyException
    if (isBlocked) {
      throw IndexNotReadyException("Index for $crn is still blocked")
    }
  }

  fun <T> doWithBlock(crn: String, action: () -> T, rollback: () -> Unit): T {
    // Apply the block before the load action
    block(crn)
    try {
      // Run the load and when complete, delete the block
      return action()
    } catch (ex: Exception) {
      rollback()
      // Log to Sentry and App insights
      Sentry.captureException(ex)
      telemetryClient.trackException(ex)
      throw ex
    } finally {
      unblock(crn)
    }
  }

  private fun block(crn: String) {
    val indexQuery = IndexQuery()
    indexQuery.setId(crn)
    indexQuery.setObject(BlockJson(crn))
    retry { restTemplate.index(indexQuery, IndexCoordinates.of(CONTACT_SEMANTIC_BLOCK)) }
  }

  private fun unblock(crn: String) = retry {
    restTemplate.delete(crn, IndexCoordinates.of(CONTACT_SEMANTIC_BLOCK))
  }

  private fun getBlock(crn: String): BlockDocument? = retry {
    restTemplate[crn, BlockDocument::class.java, IndexCoordinates.of(CONTACT_SEMANTIC_BLOCK)]
  }

  fun String.isLongerAgoThan(durationInPast: Duration): Boolean {
    return Duration.between(
      ZonedDateTime.parse(this),
      ZonedDateTime.now(),
    ).seconds > durationInPast.seconds
  }

  private fun retryUntilBlockIsCleared(
    maxRetries: Int,
    timeout: Duration = Duration.ofSeconds(5),
    block: () -> Boolean,
  ): Boolean {
    repeat(maxRetries) { _ ->
      val res = block()
      if (!res) return false
      TimeUnit.MILLISECONDS.sleep(timeout.toMillis())
    }
    return true
  }
}