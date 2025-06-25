package uk.gov.justice.hmpps.probationsearch.contactsearch.semantic.block

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
  private val telemetryClient: TelemetryClient
) {
  companion object {
    const val CONTACT_SEMANTIC_BLOCK = "contact-semantic-block-primary"
    const val CONTACT_SEMANTIC_BLOCK_TIMESTAMP = "yyyy-MM-dd'T'HH:mm:ss'Z'"
  }

  fun checkIfBlockedOrRollbackIfStale(crn: String, retries: Int = 1, blockContext: BlockContext.() -> Unit) {
    val ctx = BlockContext().apply(blockContext)
    val isBlocked = retry(retries, Duration.ofSeconds(5)) {
      // if block exists and was created more than 5 minutes ago, then rollback the partial load and delete the block
      getBlock(crn)?.timestamp?.let {
        if (it.isLongerAgoThan(Duration.ofMinutes(5))) {
          ctx.rollback()
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

  fun doWithBlock(crn: String, block: BlockContext.() -> Unit) {
    val ctx = BlockContext().apply(block)
    // Apply the block before the load action
    block(crn)
    try {
      // Run the load and when complete, delete the block
      ctx.action()
      unblock(crn)
    } catch (ex: Exception) {
      ctx.rollback()
      unblock(crn)
      // Log to Sentry and App insights
      Sentry.captureException(ex)
      telemetryClient.trackException(ex)
      throw ex
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

  private fun getBlock(crn: String): BlockDocument? =
    restTemplate[crn, BlockDocument::class.java, IndexCoordinates.of(CONTACT_SEMANTIC_BLOCK)]

  fun LocalDateTime.isLongerAgoThan(durationInPast: Duration): Boolean {
    return Duration.between(
      this,
      LocalDateTime.now(ZoneId.of("UTC")).atOffset(ZoneOffset.UTC),
    ).seconds > durationInPast.seconds
  }

  private fun retry(maxRetries: Int, timeout: Duration = Duration.ofSeconds(5), block: () -> Boolean): Boolean {
    repeat(maxRetries) { _ ->
      val res = block()
      if (!res) return false
      TimeUnit.MILLISECONDS.sleep(timeout.toMillis())
    }
    return true
  }
}