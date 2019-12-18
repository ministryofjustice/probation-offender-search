package uk.gov.justice.hmpps.offendersearch.config;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfiguration {

    public static final String SERVICE_NAME = "es";
    @Value("${elasticsearch.port}")
    private int port;

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.scheme}")
    private String scheme;

    @Value("${elasticsearch.aws.signrequests}")
    private boolean shouldSignRequests;

    @Value("${aws.region:eu-west-2}")
    private String awsRegion;

    @Bean
    public RestHighLevelClient client(){

        if (shouldSignRequests) {
            var signer = new AWS4Signer();
            signer.setServiceName(SERVICE_NAME);
            signer.setRegionName(awsRegion);

            var clientBuilder = RestClient.builder(new HttpHost(host, port, scheme)).setHttpClientConfigCallback(
                    callback -> callback.addInterceptorLast(
                            new AWSRequestSigningApacheInterceptor(SERVICE_NAME, signer, new DefaultAWSCredentialsProviderChain())));

            return new RestHighLevelClient(clientBuilder);
        }
        return new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme)));
    }

}
