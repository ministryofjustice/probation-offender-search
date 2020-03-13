package uk.gov.justice.hmpps.offendersearch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class HealthInfo implements HealthIndicator {
    // private final RestTemplate restTemplate;

    @Autowired(required=false)
    private BuildProperties buildProperties;

    @Override
    public Health health() {

        log.info("****** Called the custom healthCheck ******");

        try {
            // TODO: Ping the remote Delius elasticSearch service when this is catered for
            return Health.up().withDetail("version", getVersion()).build();
        } catch (final RestClientException e) {
            return Health.down().withDetail("problem", e.getMessage()).build();
        }
    }

    private String getVersion() {
        return buildProperties == null ? "version not available" : buildProperties.getVersion();
    }
}