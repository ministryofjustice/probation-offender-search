package uk.gov.justice.hmpps.offendersearch.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfiguration {

    @Value("${elasticsearch.port}")
    private int port;

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.scheme}")
    private String scheme;

    @Bean
    public RestHighLevelClient client(){

        return new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme)));
    }

}
