package uk.gov.justice.hmpps.offendersearch.health;


import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static lombok.AccessLevel.PROTECTED;


@AllArgsConstructor(access = PROTECTED)
public abstract class HealthCheck implements HealthIndicator {
    private final RestTemplate restTemplate;

    @Override
    public Health health() {
        try {
            // TODO elasticsearch healthcheck
            return Health.up().build();
        } catch (final RestClientException e) {
            return Health.down(e).build();
        }
    }
}
