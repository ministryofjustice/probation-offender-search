package uk.gov.justice.hmpps.probationsearch.services

import io.flipt.client.FliptClient
import org.springframework.stereotype.Service

@Service
class FeatureFlags(private val client: FliptClient) {
  fun enabled(key: String) = try {
    client
      .evaluateBoolean(key, key, emptyMap<String, String>())
      .isEnabled
  } catch (e: Exception) {
    throw FeatureFlagException(key, e)
  }

  class FeatureFlagException(val key: String, e: Exception) : RuntimeException("Unable to retrieve '$key' flag", e)

  companion object {
    const val SEMANTIC_CONTACT_SEARCH = "semantic-contact-search"
  }
}