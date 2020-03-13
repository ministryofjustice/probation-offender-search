package uk.gov.justice.hmpps.offendersearch.controllers

import io.swagger.annotations.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.offendersearch.BadRequestException
import uk.gov.justice.hmpps.offendersearch.NotFoundException
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.dto.SearchDto
import uk.gov.justice.hmpps.offendersearch.services.SearchService

@Api(tags = ["offender-search"], authorizations = [Authorization("ROLE_COMMUNITY")], description = "Provides offender search features for Delius elastic search")
@RestController
@RequestMapping(value = ["search"], produces = [MediaType.APPLICATION_JSON_VALUE])
class OffenderSearchController(private val searchService: SearchService) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @ApiOperation(value = "Search for an offender in Delius ElasticSearch", notes = "Specify the request criteria to match against", authorizations = [Authorization("ROLE_COMMUNITY")], nickname = "search")
  @ApiResponses(value = [ApiResponse(code = 200, message = "OK", response = OffenderDetail::class, responseContainer = "List"), ApiResponse(code = 400, message = "Invalid Request", response = BadRequestException::class), ApiResponse(code = 404, message = "Not found", response = NotFoundException::class)])
  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @GetMapping
  fun searchOffenders(@RequestBody searchForm: SearchDto): List<OffenderDetail?>? {
    log.info("Search called with {}", searchForm)
    return searchService.performSearch(searchForm)
  }

}