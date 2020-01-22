package uk.gov.justice.hmpps.offendersearch.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.offendersearch.BadRequestException;
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

        validateSearchForm(searchOptions);

        SearchRequest searchRequest = new SearchRequest("offender");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


        final var matchingAllFieldsQuery = buildMatchWithAllProvidedParameters(searchOptions);

        searchSourceBuilder.query(matchingAllFieldsQuery);
        searchRequest.source(searchSourceBuilder);
        SearchResponse response =
                hlClient.search(searchRequest);

        return getSearchResult(response);
    }

    protected BoolQueryBuilder buildMatchWithAllProvidedParameters(SearchDto searchOptions){

        var matchingAllFieldsQuery = QueryBuilders
                .boolQuery();

        if (StringUtils.isNotBlank(searchOptions.getSurname()) ){
            matchingAllFieldsQuery.must(QueryBuilders
                    .matchQuery("surname", searchOptions.getSurname()));
        }
        if (StringUtils.isNotBlank(searchOptions.getFirstName()) ){
            matchingAllFieldsQuery.must(QueryBuilders
                    .matchQuery("firstName", searchOptions.getFirstName()));
        }
        if (searchOptions.getDateOfBirth() !=  null ){
            matchingAllFieldsQuery.must(QueryBuilders
                    .matchQuery("dateOfBirth", searchOptions.getDateOfBirth()));
        }
        if (StringUtils.isNotBlank(searchOptions.getCrn())){
            matchingAllFieldsQuery.must(QueryBuilders
                    .matchQuery("otherIds.crn", searchOptions.getCrn()));
        }
        if (StringUtils.isNotBlank(searchOptions.getCroNumber())){
            matchingAllFieldsQuery.must(QueryBuilders
                    .matchQuery("otherIds.croNumber", searchOptions.getCroNumber()));
        }
        if (StringUtils.isNotBlank(searchOptions.getPncNumber())){
            matchingAllFieldsQuery.must(QueryBuilders
                    .matchQuery("otherIds.pncNumber", searchOptions.getPncNumber()));
        }
        if (StringUtils.isNotBlank(searchOptions.getNomsNumber())){
            matchingAllFieldsQuery.must(QueryBuilders
                    .matchQuery("otherIds.nomsNumber", searchOptions.getNomsNumber()));
        }

        return matchingAllFieldsQuery;
    }

    private void validateSearchForm(SearchDto searchOptions) {
        if (!searchOptions.isValid()){
            log.warn("Invalid search  - no criteria provided");
            throw new BadRequestException("Invalid search  - please provide at least 1 search parameter");
        }
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
