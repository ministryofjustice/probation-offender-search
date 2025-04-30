package uk.gov.justice.hmpps.probationsearch.services

import io.flipt.api.FliptClient
import io.flipt.api.evaluation.models.EvaluationRequest
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.config.FliptConfig

@Service
class FeatureFlags(private val client: FliptClient) {
  fun enabled(key: String) = try {
    client.evaluation().evaluateBoolean(
      EvaluationRequest.builder()
        .namespaceKey(FliptConfig.NAMESPACE)
        .flagKey(key)
        .build(),
    ).isEnabled
  } catch (e: Exception) {
    throw FeatureFlagException(key, e)
  }

  class FeatureFlagException(val key: String, e: Exception) : RuntimeException("Unable to retrieve '$key' flag", e)

  companion object {
    const val SEMANTIC_CONTACT_SEARCH = "semantic-contact-search"
  }
}