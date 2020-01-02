package uk.gov.justice.hmpps.offendersearch.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail;
import uk.gov.justice.hmpps.offendersearch.util.LocalStackHelper;

import java.io.IOException;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev,localstack")
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(listeners = {
        DependencyInjectionTestExecutionListener.class,
        OffenderSearchAPIIntegrationTest.class})
@ContextConfiguration
public class OffenderSearchAPIIntegrationTest extends AbstractTestExecutionListener {

    @Value("${test.token.good}")
    private String validOauthToken;

    @Override
    public void beforeTestClass(TestContext testContext) throws IOException {
        ObjectMapper objectMapper = testContext.getApplicationContext().getBean(ObjectMapper.class);
        RestHighLevelClient esClient = testContext.getApplicationContext().getBean(RestHighLevelClient.class);
        new LocalStackHelper(esClient).loadData();
        RestAssured.port = Integer.parseInt(Objects.requireNonNull(testContext.getApplicationContext().getEnvironment().getProperty("local.server.port")));;
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
                new ObjectMapperConfig().jackson2ObjectMapperFactory((aClass, s) -> objectMapper));
    }


    @Test
    public void surnameSearch() {

        final var results = given()
                .auth()
                .oauth2(validOauthToken)
                .contentType(APPLICATION_JSON_VALUE)
                .body("{\"surname\":\"smith\"}")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(OffenderDetail[].class);

        assertThat(results).hasSize(2);
        assertThat(results).extracting("firstName").containsExactlyInAnyOrder("John", "Jane");
    }

    @Test
    public void allParameters() {

        final var results = given()
                .auth()
                .oauth2(validOauthToken)
                .contentType(APPLICATION_JSON_VALUE)
                .body("{\"surname\": \"smith\",\"firstName\": \"John\",\"crn\": \"X00001\",\"croNumber\": \"SF80/655108T\", \"nomsNumber\": \"G8020GG\",\"pncNumber\": \"2018/0123456X\"}\n")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(OffenderDetail[].class);

        assertThat(results).hasSize(1);
        assertThat(results).extracting("firstName").containsExactly("John");
    }
    @Test
    public void noSearchParameters_badRequest() {

        given()
                .auth()
                .oauth2(validOauthToken)
                .contentType(APPLICATION_JSON_VALUE)
                .body("{}")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("developerMessage", containsString("Invalid search  - please provide at least 1 search parameter"));
    }

    @Test
    public void noResults() {

        final var results = given()
                .auth()
                .oauth2(validOauthToken)
                .contentType(APPLICATION_JSON_VALUE)
                .body("{\"surname\":\"potter\"}")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .extract()
                .as(OffenderDetail[].class);
        assertThat(results).hasSize(0);
    }

}