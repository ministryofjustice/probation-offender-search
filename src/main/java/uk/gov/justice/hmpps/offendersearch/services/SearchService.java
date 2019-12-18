package uk.gov.justice.hmpps.offendersearch.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail;
import uk.gov.justice.hmpps.offendersearch.dto.SearchDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class SearchService {

    private RestHighLevelClient hlClient;
    private ObjectMapper mapper;

    @Autowired
    public SearchService(@Qualifier("client") final RestHighLevelClient hlClient, final ObjectMapper mapper) {
        this.hlClient = hlClient;
        this.mapper = mapper;
    }

    public List<OffenderDetail> performSearch(SearchDto searchOptions) throws IOException {

        SearchRequest searchRequest = new SearchRequest("offender");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        QueryBuilder queryBuilder = QueryBuilders
                .boolQuery()
                .must(QueryBuilders
                        .matchQuery("surname", searchOptions.getName()));

        searchSourceBuilder.query(queryBuilder);

        searchRequest.source(searchSourceBuilder);

        SearchResponse response =
                hlClient.search(searchRequest);

        return getSearchResult(response);
    }

    private List<OffenderDetail> getSearchResult(SearchResponse response) {

        SearchHit[] searchHit = response.getHits().getHits();

        var offenderDetailList = new ArrayList<OffenderDetail>();

        if (searchHit.length > 0) {

            Arrays.stream(searchHit)
                    .forEach(hit -> offenderDetailList
                            .add(parseOffenderDetail(hit.getSourceAsString())));
        }

        return offenderDetailList;
    }

    private OffenderDetail parseOffenderDetail(String src) {
        try {
            return mapper.readValue(src, OffenderDetail.class);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
