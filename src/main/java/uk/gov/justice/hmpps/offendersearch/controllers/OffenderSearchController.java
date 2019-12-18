package uk.gov.justice.hmpps.offendersearch.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail;
import uk.gov.justice.hmpps.offendersearch.dto.SearchDto;
import uk.gov.justice.hmpps.offendersearch.services.SearchService;

import java.io.IOException;
import java.util.List;

@Api(tags = {"offender-search"})

@RestController
@RequestMapping(
        value="search",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@PreAuthorize("hasRole('ROLE_COMMUNITY')")
public class OffenderSearchController {

    private final SearchService searchService;

    public OffenderSearchController(final SearchService searchService) {
        this.searchService = searchService;
    }

    /* --------------------------------------------------------------------------------*/

    @ApiOperation(
            value = "todo",
            notes = "todo",
            nickname="search")

    @GetMapping
    public List<OffenderDetail> searchOffenders(final @RequestBody SearchDto searchForm) throws IOException {

        log.debug("Search called");

        return searchService.performSearch(searchForm);
    }

}
