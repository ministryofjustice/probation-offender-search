package uk.gov.justice.hmpps.offendersearch.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.justice.hmpps.offendersearch.dto.SearchDto;

import static org.junit.Assert.*;

public class SearchServiceTest {

    
    private SearchService searchService;
    @Mock
    private RestHighLevelClient mockEsClient;

    @Before
    public void setup() {
        searchService = new SearchService(mockEsClient, new ObjectMapper());
    }

    @Test
    public void buildMatchWithAllProvidedParameters_surnameOnly() {
      //  final var queryWithSurnithSurname.toString()                  ).isEqualTo("fdf");
    }
}