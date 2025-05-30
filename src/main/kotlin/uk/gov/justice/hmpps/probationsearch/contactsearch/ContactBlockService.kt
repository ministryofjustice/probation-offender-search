package uk.gov.justice.hmpps.probationsearch.contactsearch

import com.microsoft.applicationinsights.TelemetryClient
import io.sentry.Sentry
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.IndexQuery
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.IndexNotReadyException
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

@Service
class ContactBlockService(
  private val restTemplate: OpenSearchRestTemplate,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    const val CONTACT_SEMANTIC_BLOCK = "contact-semantic-block"
    const val CONTACT_SEMANTIC_BLOCK_TIMESTAMP = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'"
  }

  fun checkIfBlockedOrRollbackIfStale(crn: String, rollback: () -> (Unit)) {
    val isBlocked = retry(1) {
      getBlock(crn)?.timestamp?.let {
        if (it.isLongerAgoThan(Duration.ofMinutes(5))) {
          rollback()
          unblock(crn)
          return@let false
        }
        return@let true
      } ?: false
    }
    if (isBlocked) {
      throw IndexNotReadyException("Index for $crn is still blocked")
    }
  }

  fun retryWithBlockAndRollback(crn: String, maxRetries: Int = 2, action: () -> (Unit), rollback: () -> (Unit)) {
    block(crn)
    (0..maxRetries).forEach { count ->
      try {
        action()
        unblock(crn)
        return
      } catch (ex: Exception) {
        if (count == maxRetries) {
          rollback()
          unblock(crn)
          Sentry.captureException(ex)
          telemetryClient.trackException(ex)
          throw ex
        }
      }
    }
  }

  private fun block(crn: String) {
    val indexQuery = IndexQuery()
    indexQuery.id = crn
    indexQuery.`object` = BlockJson(crn)
    restTemplate.index(indexQuery, IndexCoordinates.of(CONTACT_SEMANTIC_BLOCK))
  }

  private fun unblock(crn: String) {
    restTemplate.delete(crn, IndexCoordinates.of(CONTACT_SEMANTIC_BLOCK))
  }

  private fun getBlock(crn: String): ContactBlockResult? =
    restTemplate[crn, ContactBlockResult::class.java, IndexCoordinates.of(CONTACT_SEMANTIC_BLOCK)]

  fun LocalDateTime.isLongerAgoThan(durationInPast: Duration): Boolean {
    return Duration.between(
      this,
      LocalDateTime.now(ZoneId.of("UTC")).atOffset(ZoneOffset.UTC),
    ).seconds > durationInPast.seconds
  }

  private fun retry(count: Int, timeout: Duration = Duration.ofSeconds(5), block: () -> Boolean): Boolean {
    (0..count).forEach { _ ->
      val res = block()
      if (!res) return false
      TimeUnit.MILLISECONDS.sleep(timeout.toMillis())
    }
    return true
  }
}