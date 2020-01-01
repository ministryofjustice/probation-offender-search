package uk.gov.justice.hmpps.offendersearch.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

@Slf4j
@Component
public class LocalStackHelper {

    @Qualifier("client")
    @Autowired
    RestHighLevelClient esClient;


    public void loadData() throws IOException {

        destroyIndex();
        destroyPipeline();
        buildPipeline();
        buildIndex();

        loadOffender("1", loadFile("src/test/resources/elasticsearchdata/john-smith.json"));
        loadOffender("2", loadFile("src/test/resources/elasticsearchdata/jane-smith.json"));
        loadOffender("3", loadFile("src/test/resources/elasticsearchdata/sam-jones-deleted.json"));
        loadOffender("4", loadFile("src/test/resources/elasticsearchdata/antonio-gramsci-n01.json"));
        loadOffender("5", loadFile("src/test/resources/elasticsearchdata/antonio-gramsci-n02.json"));
        loadOffender("6", loadFile("src/test/resources/elasticsearchdata/antonio-gramsci-n03.json"));
        loadOffender("7", loadFile("src/test/resources/elasticsearchdata/anne-gramsci-n02.json"));
        loadOffender("8", loadFile("src/test/resources/elasticsearchdata/antonio-gramsci-c20.json"));
    }

    private void loadOffender(String key, String offender) throws IOException {
        log.debug("Loading offender: {}", offender);

        esClient.getLowLevelClient().performRequest("put", "/offender/document/" + key + "?pipeline=pnc-pipeline", new HashMap<>(), new StringEntity(offender), new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"));
    }

    public void destroyIndex() throws IOException {
        log.debug("Dropping offender index");
        try {
            esClient.getLowLevelClient().performRequest("delete", "/offender", new BasicHeader("any", "any"));
        } catch (ResponseException e) {
            log.debug("destroyIndex returned ", e);
        }
    }

    public void destroyPipeline() throws IOException {
        try {
            log.debug("destroy pipeline");
            esClient.getLowLevelClient().performRequest("delete", "/_ingest/pipeline/pnc-pipeline", new BasicHeader("any", "any"));
        } catch (ResponseException e) {
            log.debug("destroyPipeline returned ", e);
        }
    }

    public void buildIndex() throws IOException {
        log.debug("Build index");

        esClient.getLowLevelClient().performRequest("put", "/offender", new HashMap<>(), new StringEntity(loadFile("src/test/resources/elasticsearchdata/create-index.json")), new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"));
    }

    public void buildPipeline() throws IOException {
        log.debug("Build pipeline");
        esClient.getLowLevelClient().performRequest("put", "/_ingest/pipeline/pnc-pipeline", new HashMap<>(), new StringEntity(loadFile("src/test/resources/elasticsearchdata/create-pipeline.json")), new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"));
    }

    private String loadFile(String file) throws IOException {
        return Files.readString(Paths.get(file));
    }
}
