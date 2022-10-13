package uk.gov.justice.hmpps.offendersearch.controllers

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.annotations.Api
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Authorization
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.offendersearch.BadRequestException
import uk.gov.justice.hmpps.offendersearch.NotFoundException
import uk.gov.justice.hmpps.offendersearch.config.SecurityUserContext
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.dto.SearchDto
import uk.gov.justice.hmpps.offendersearch.dto.SearchPagedResults
import uk.gov.justice.hmpps.offendersearch.dto.SearchPhraseFilter
import uk.gov.justice.hmpps.offendersearch.dto.SearchPhraseResults
import uk.gov.justice.hmpps.offendersearch.security.getOffenderUserAccessFromScopes
import uk.gov.justice.hmpps.offendersearch.services.SearchService
import javax.validation.Valid
import javax.validation.constraints.NotEmpty

@Api(
  tags = ["offender-search"],
  authorizations = [Authorization("ROLE_COMMUNITY")],
  description = "Provides offender search features for Delius elastic search"
)
@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Validated
class OffenderSearchController(
  private val searchService: SearchService,
  private val securityUserContext: SecurityUserContext,
  private val telemetryClient: TelemetryClient
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @ApiOperation(
    value = "Search for an offender in Delius ElasticSearch. Only offenders matching all request attributes will be returned",
    notes = "Specify the request criteria to match against",
    authorizations = [Authorization("ROLE_COMMUNITY")],
    nickname = "search"
  )
  @ApiResponses(
    value = [
      ApiResponse(
        code = 200,
        message = "OK",
        response = OffenderDetail::class,
        responseContainer = "List"
      ), ApiResponse(
        code = 400,
        message = "Invalid Request",
        response = BadRequestException::class
      ), ApiResponse(code = 404, message = "Not found", response = NotFoundException::class)
    ]
  )
  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @RequestMapping("/search", method = [POST, GET])
  fun searchOffenders(@RequestBody searchForm: SearchDto): List<OffenderDetail?>? {
    log.info("Search called with {}", searchForm)
    return searchService.performSearch(searchForm)
  }

  @ApiOperation(
    value = "Search for an offender in Delius ElasticSearch using a search phrase. Only offenders matching all request attributes will be returned",
    notes =
    """The phrase can contain one or more of the following:
        
        - first name
        - middle name
        - surname
        - first line of address
        - county of address
        - town of address
        - postal code of address
        - date of birth (in any common format)
        - CRN
        - NOMS number
        - PNC number
        - CRO number
        - National Insurance number
        - Recorded gender
        - Telephone numbers
        
        Both primary and alias names will be searched.
        
        When using this API be aware of a few anomalies between searching using all terms or not (e.g AND versus OR query) :
         - A phrase that contain just single letter will result in all records being matched since essentially single letters for the AND query are discarded
         - first name when matched is artificially boosted in the result sort order for OR queries, due to how first names are also searched as if it is a prefix as well a complete match
         
         Roles and scopes:
          - client must have ROLE_COMMUNITY
          - by default any offenders that are restricted or have exclusion lists for staff will be redacted in the response (the crn and offender managers will still be visible) with these exceptions
            - there is a Delius user in context (username in JWT token along with an auth source of Delius) and they are allowed to view the offender, e.g. not on the exclusion list or is on the restricted list
            - the client has the scopes to allow it to ignore these lists; the two scopes to bypass this feature are "ignore_delius_exclusions_always" and "ignore_delius_inclusions_always"
         
      """,
    nickname = "searchByPhrase"
  )
  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @ApiResponses(
    value = [
      ApiResponse(code = 401, message = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(code = 403, message = "Forbidden, requires an authorisation with role ROLE_COMMUNITY")
    ]
  )
  @PostMapping("/phrase")
  @ApiImplicitParams(
    ApiImplicitParam(
      name = "page",
      dataType = "int",
      paramType = "query",
      value = "Results page you want to retrieve (0..N)",
      example = "0",
      defaultValue = "0"
    ),
    ApiImplicitParam(
      name = "size",
      dataType = "int",
      paramType = "query",
      value = "Number of records per page.",
      example = "10",
      defaultValue = "10"
    )
  )
  fun searchOffendersByPhrase(
    @Valid @RequestBody searchPhraseFilter: SearchPhraseFilter,
    @PageableDefault pageable: Pageable
  ): SearchPhraseResults {
    log.info("Search called with {}", searchPhraseFilter)
    return searchService.performSearch(
      searchPhraseFilter,
      pageable,
      getOffenderUserAccessFromScopes(securityUserContext)
    )
  }

  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @ApiResponses(
    value = [
      ApiResponse(code = 401, message = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(code = 403, message = "Forbidden, requires an authorisation with role ROLE_COMMUNITY")
    ]
  )
  @PostMapping("/crns")
  @ApiOperation(value = "Match prisoners by a list of prisoner crns", notes = "Requires ROLE_COMMUNITY role")
  fun findByIds(
    @ApiParam(required = true, name = "crnList") @RequestBody crnList: List<String>
  ): List<OffenderDetail> {
    return searchService.findByListOfCRNs(crnList)
  }

  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @ApiResponses(
    value = [
      ApiResponse(code = 401, message = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(code = 403, message = "Forbidden, requires an authorisation with role ROLE_COMMUNITY")
    ]
  )
  @PostMapping("/nomsNumbers")
  @ApiOperation(value = "Match prisoners by a list of prisoner noms numbers", notes = "Requires ROLE_COMMUNITY role")
  fun findByNomsNumbers(
    @ApiParam(required = true, name = "nomsList") @RequestBody nomsList: List<String>
  ): List<OffenderDetail> {
    return searchService.findByListOfNoms(nomsList)
  }

  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @ApiResponses(
    value = [
      ApiResponse(code = 401, message = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(code = 403, message = "Forbidden, requires an authorisation with role ROLE_COMMUNITY")
    ]
  )
  @PostMapping("/ldu-codes")
  @ApiImplicitParams(
    ApiImplicitParam(
      name = "page",
      dataType = "int",
      paramType = "query",
      value = "Results page you want to retrieve (0..N)",
      example = "0",
      defaultValue = "0"
    ),
    ApiImplicitParam(
      name = "size",
      dataType = "int",
      paramType = "query",
      value = "Number of records per page.",
      example = "10",
      defaultValue = "10"
    )
  )
  @ApiOperation(value = "Match prisoners by a list of ldu codes", notes = "Requires ROLE_COMMUNITY role")
  fun findByLduCode(
    @ApiParam(required = true, name = "lduList")
    @RequestBody @NotEmpty lduList: List<String>,
    @PageableDefault pageable: Pageable
  ): SearchPagedResults {
    return searchService.findByListOfLdu(pageable, lduList)
  }

  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @ApiResponses(
    value = [
      ApiResponse(code = 401, message = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(code = 403, message = "Forbidden, requires an authorisation with role ROLE_COMMUNITY")
    ]
  )
  @PostMapping("/team-codes")
  @ApiImplicitParams(
    ApiImplicitParam(
      name = "page",
      dataType = "int",
      paramType = "query",
      value = "Results page you want to retrieve (0..N)",
      example = "0",
      defaultValue = "0"
    ),
    ApiImplicitParam(
      name = "size",
      dataType = "int",
      paramType = "query",
      value = "Number of records per page.",
      example = "10",
      defaultValue = "10"
    )
  )
  @ApiOperation(value = "Match prisoners by a list of team codes", notes = "Requires ROLE_COMMUNITY role")
  fun findByTeamCode(
    @ApiParam(required = true, name = "teamCodeList")
    @PageableDefault pageable: Pageable,
    @RequestBody @NotEmpty teamCodeList: List<String>
  ): SearchPagedResults {
    return searchService.findByListOfTeamCodes(pageable, teamCodeList)
  }

  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @ApiOperation(value = "Match prisoners by a ldu code", notes = "Requires ROLE_COMMUNITY role")
  @ApiResponses(
    value = [
      ApiResponse(
        code = 200,
        message = "OK",
        response = SearchPagedResults::class
      ), ApiResponse(
        code = 400,
        message = "Invalid Request",
        response = BadRequestException::class
      ), ApiResponse(code = 404, message = "Not found", response = NotFoundException::class),
      ApiResponse(code = 401, message = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(code = 403, message = "Forbidden, requires an authorisation with role ROLE_COMMUNITY")
    ]
  )
  @GetMapping("/ldu-codes/{lduCode}")
  fun searchOffendersByLduCode(
    @PathVariable lduCode: String,
    @PageableDefault pageable: Pageable,
  ): SearchPagedResults {
    log.info("Searching for offenders by ldu code: {}", lduCode)
    return searchService.findByLduCode(lduCode, pageable)
  }

  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @ApiOperation(value = "Match prisoners by a team code", notes = "Requires ROLE_COMMUNITY role")
  @ApiResponses(
    value = [
      ApiResponse(
        code = 200,
        message = "OK",
        response = SearchPagedResults::class
      ), ApiResponse(
        code = 400,
        message = "Invalid Request",
        response = BadRequestException::class
      ), ApiResponse(code = 404, message = "Not found", response = NotFoundException::class),
      ApiResponse(code = 401, message = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(code = 403, message = "Forbidden, requires an authorisation with role ROLE_COMMUNITY")
    ]
  )
  @GetMapping("/team-codes/{teamCode}")
  fun searchOffendersByTeamCode(
    @PathVariable teamCode: String,
    @PageableDefault pageable: Pageable,
  ): SearchPagedResults {
    log.info("Searching for offenders by team code: {}", teamCode)
    return searchService.findByTeamCode(teamCode, pageable)
  }

  @GetMapping("/synthetic-monitor")
  fun syntheticMonitor() {
    val start = System.currentTimeMillis()
    val results = searchService.performSearch(SearchDto(surname = "Smith"))

    telemetryClient.trackEvent("synthetic-monitor", mapOf("results" to "${results.size}", "timeMs" to (System.currentTimeMillis() - start).toString()), null)
  }
}
