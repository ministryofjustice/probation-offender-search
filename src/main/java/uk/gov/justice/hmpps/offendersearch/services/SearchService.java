package uk.gov.justice.hmpps.offendersearch.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail;
import uk.gov.justice.hmpps.offendersearch.dto.SearchDto;

import java.util.List;

@Service
@Slf4j
public class SearchService {

    public SearchService() {
    }

    public List<OffenderDetail> performSearch(SearchDto searchOptions){
        return List.of();
    }

}
