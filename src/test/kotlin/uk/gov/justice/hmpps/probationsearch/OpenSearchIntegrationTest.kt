package uk.gov.justice.hmpps.probationsearch

import com.github.dockerjava.api.model.Ulimit
import org.opensearch.testcontainers.OpenSearchContainer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
abstract class OpenSearchIntegrationTest {
  private class TestOpenSearchContainer :
    OpenSearchContainer<TestOpenSearchContainer>("opensearchproject/opensearch:3.2.0") {
    override fun start() {
      // Keep the same container running throughout the tests
      if (!isRunning) super.start()
    }

    override fun stop() = Unit
  }

  companion object {
    @Container
    @JvmStatic
    private val openSearch = TestOpenSearchContainer().apply {
      withExposedPorts(9200, 9600)
      withEnv(
        mapOf(
          "node.name" to "opensearch",
          "cluster.name" to "probation-search-cluster",
          "discovery.type" to "single-node",
          "bootstrap.memory_lock" to "true",
          "DISABLE_INSTALL_DEMO_CONFIG" to "true",
          "DISABLE_SECURITY_PLUGIN" to "true",
          "OPENSEARCH_JAVA_OPTS" to "-Xms8g -Xmx8g",
          "plugins.ml_commons.only_run_on_ml_node" to "false",
          "plugins.ml_commons.model_access_control_enabled" to "false",
          "plugins.ml_commons.native_memory_threshold" to "99",
        ),
      )
      withCreateContainerCmdModifier {
        it.hostConfig?.withUlimits(listOf(Ulimit("memlock", -1L, -1L), Ulimit("nofile", 65536L, 65536L)))
      }
    }

    @JvmStatic
    @DynamicPropertySource
    fun openSearchProperties(registry: DynamicPropertyRegistry) {
      registry.add("opensearch.uris") {
        openSearch.start()
        openSearch.httpHostAddress
      }
    }
  }
}
