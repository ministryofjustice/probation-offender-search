package uk.gov.justice.hmpps.probationsearch.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

@Configuration
class ExecutorConfig {
  @Bean
  fun virtualThreadExecutor() = Executors.newVirtualThreadPerTaskExecutor()
}
