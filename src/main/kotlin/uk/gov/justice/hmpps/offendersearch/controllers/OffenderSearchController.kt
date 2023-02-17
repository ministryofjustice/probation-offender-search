package uk.gov.justice.hmpps.offendersearch.controllers

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
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
import uk.gov.justice.hmpps.offendersearch.config.SecurityUserContext
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.dto.SearchDto
import uk.gov.justice.hmpps.offendersearch.dto.SearchPagedResults
import uk.gov.justice.hmpps.offendersearch.dto.SearchPhraseFilter
import uk.gov.justice.hmpps.offendersearch.dto.SearchPhraseResults
import uk.gov.justice.hmpps.offendersearch.security.getOffenderUserAccessFromScopes
import uk.gov.justice.hmpps.offendersearch.services.SearchService

@Tag(
  name = "offender-search",
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

  @Operation(
    summary = "Search for an offender in Delius ElasticSearch. Only offenders matching all request attributes will be returned",
    description = "Specify the request criteria to match against",
    operationId = "search"
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ), ApiResponse(
        responseCode = "400",
        description = "Invalid Request",
        content = [Content(examples = [])]
      ), ApiResponse(responseCode = "404", description = "Not found", content = [Content(examples = [])])
    ]
  )
  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @RequestMapping("/search", method = [POST, GET])
  fun searchOffenders(@RequestBody searchForm: SearchDto): List<OffenderDetail?>? {
    log.info("Search called with {}", searchForm)
    return searchService.performSearch(searchForm)
  }

  @Operation(
    summary = "Search for an offender in Delius ElasticSearch using a search phrase. Only offenders matching all request attributes will be returned",
    description =
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
    operationId = "searchByPhrase"
  )
  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(examples = [])]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an authorisation with role ROLE_COMMUNITY", content = [Content(examples = [])])
    ]
  )
  @PostMapping("/phrase")
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
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(examples = [])]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an authorisation with role ROLE_COMMUNITY", content = [Content(examples = [])])
    ]
  )
  @PostMapping("/crns")
  @Operation(description = "Match prisoners by a list of prisoner crns", summary = "Requires ROLE_COMMUNITY role")
  fun findByIds(
    @Parameter(required = true, name = "crnList") @RequestBody crnList: List<String>
  ): List<OffenderDetail> {
    return searchService.findByListOfCRNs(crnList)
  }

  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(examples = [])]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an authorisation with role ROLE_COMMUNITY", content = [Content(examples = [])])
    ]
  )
  @PostMapping("/nomsNumbers")
  @Operation(description = "Match prisoners by a list of prisoner noms numbers", summary = "Requires ROLE_COMMUNITY role")
  fun findByNomsNumbers(
    @Parameter(required = true, name = "nomsList") @RequestBody nomsList: List<String>
  ): List<OffenderDetail> {
    return searchService.findByListOfNoms(nomsList)
  }

  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(examples = [])]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an authorisation with role ROLE_COMMUNITY", content = [Content(examples = [])])
    ]
  )
  @PostMapping("/ldu-codes")
  @Parameters(
    Parameter(
      name = "page",
      description = "Results page you want to retrieve (0..N)",
      example = "0",
    ),
    Parameter(
      name = "size",
      description = "Number of records per page.",
      example = "10",
    )
  )
  @Operation(summary = "Match prisoners by a list of ldu codes", description = "Requires ROLE_COMMUNITY role")
  fun findByLduCode(
    @Parameter(required = true, name = "lduList")
    @RequestBody @NotEmpty lduList: List<String>,
    @PageableDefault pageable: Pageable
  ): SearchPagedResults {
    return searchService.findByListOfLdu(pageable, lduList)
  }

  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(examples = [])]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an authorisation with role ROLE_COMMUNITY", content = [Content(examples = [])])
    ]
  )
  @PostMapping("/team-codes")
  @Parameters(
    Parameter(
      name = "page",
      description = "Results page you want to retrieve (0..N)",
      example = "0",
    ),
    Parameter(
      name = "size",
      description = "Number of records per page.",
      example = "10",
    )
  )
  @Operation(description = "Match prisoners by a list of team codes", summary = "Requires ROLE_COMMUNITY role")
  fun findByTeamCode(
    @Parameter(required = true, name = "teamCodeList")
    @PageableDefault pageable: Pageable,
    @RequestBody @NotEmpty teamCodeList: List<String>
  ): SearchPagedResults {
    return searchService.findByListOfTeamCodes(pageable, teamCodeList)
  }

  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @Operation(description = "Match prisoners by a ldu code", summary = "Requires ROLE_COMMUNITY role")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ), ApiResponse(
        responseCode = "400",
        description = "Invalid Request",
        content = [Content(examples = [])]
      ), ApiResponse(responseCode = "404", description = "Not found", content = [Content(examples = [])]),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(examples = [])]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an authorisation with role ROLE_COMMUNITY", content = [Content(examples = [])])
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
  @Operation(summary = "Match prisoners by a team code", description = "Requires ROLE_COMMUNITY role")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ), ApiResponse(
        responseCode = "400",
        description = "Invalid Request",
        content = [Content(examples = [])]
      ), ApiResponse(responseCode = "404", description = "Not found", content = [Content(examples = [])]),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(examples = [])]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an authorisation with role ROLE_COMMUNITY", content = [Content(examples = [])])
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
