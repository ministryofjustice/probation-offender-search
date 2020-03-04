package uk.gov.justice.hmpps.offendersearch.controllers;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.gov.justice.hmpps.offendersearch.BadRequestException;
import uk.gov.justice.hmpps.offendersearch.NotFoundException;
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail;
import uk.gov.justice.hmpps.offendersearch.dto.SearchDto;
import uk.gov.justice.hmpps.offendersearch.services.SearchService;

import java.io.IOException;
import java.util.List;

@Api(tags = {"offender-search"},
     authorizations = { @Authorization("ROLE_COMMUNITY") },
     description = "Provides offender search features for Delius elastic search")

@RestController
@RequestMapping(value = "search", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class OffenderSearchController {

    private final SearchService searchService;

    public OffenderSearchController(final SearchService searchService) {
      this.searchService = searchService;
    }

    @ApiOperation(
      value = "Search for an offender in Delius ElasticSearch",
      notes = "Specify the request criteria to match against",
      authorizations = { @Authorization("ROLE_COMMUNITY") },
      nickname="search")

    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = OffenderDetail.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid Request", response = BadRequestException.class),
            @ApiResponse(code = 404, message = "Not found", response = NotFoundException.class),
    })

    @PreAuthorize("hasRole('ROLE_COMMUNITY')")
    @GetMapping
    public List<OffenderDetail> searchOffenders(final @RequestBody SearchDto searchForm) throws IOException {
      log.info("Search called with {}", searchForm);
      return searchService.performSearch(searchForm);
    }
}
