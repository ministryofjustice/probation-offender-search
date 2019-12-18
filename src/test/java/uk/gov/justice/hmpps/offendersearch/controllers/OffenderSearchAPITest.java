package uk.gov.justice.hmpps.offendersearch.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev,localstack")
@RunWith(SpringJUnit4ClassRunner.class)
public class OffenderSearchAPITest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().port(4571).jettyStopTimeout(10000L));

    @LocalServerPort
    int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${test.token.good}")
    private String validOauthToken;

    @Before
    public void setup() {
        RestAssured.port = port;
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
                new ObjectMapperConfig().jackson2ObjectMapperFactory((aClass, s) -> objectMapper));
    }

    @Test
    public void offenderSearch() throws IOException {

            stubFor(get(anyUrl()).willReturn(
                    okForContentType("application/json", response("src/test/resources/elasticsearchdata/singleMatch.json"))));


        final var results = given()
                .auth()
                .oauth2(validOauthToken)
                .contentType(APPLICATION_JSON_VALUE)
                .body("{\"name\":\"smith\"}")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(OffenderDetail[].class);

         assertThat(results).hasSize(1);
         assertThat(results).extracting("firstName").containsOnly("John");
    }

    private String response(String file) throws IOException {
        return Files.readString(Paths.get(file));
    }
}