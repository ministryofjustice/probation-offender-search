package uk.gov.justice.hmpps.probationsearch.controllers

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
import org.springdoc.core.annotations.ParameterObject
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
import uk.gov.justice.hmpps.probationsearch.config.SecurityUserContext
import uk.gov.justice.hmpps.probationsearch.dto.OffenderDetail
import uk.gov.justice.hmpps.probationsearch.dto.SearchDto
import uk.gov.justice.hmpps.probationsearch.dto.SearchPagedResults
import uk.gov.justice.hmpps.probationsearch.dto.SearchPhraseFilter
import uk.gov.justice.hmpps.probationsearch.dto.SearchPhraseResults
import uk.gov.justice.hmpps.probationsearch.security.getOffenderUserAccessFromScopes
import uk.gov.justice.hmpps.probationsearch.services.SearchService

@Tag(
  name = "offender-search",
  description = "Provides offender search features for Delius elastic search",
)
@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Validated
class OffenderSearchController(
  private val searchService: SearchService,
  private val securityUserContext: SecurityUserContext,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PreAuthorize("hasAnyRole('ROLE_COMMUNITY', 'ROLE_PROBATION__SEARCH_PERSON')")
  @RequestMapping("/search/people", method = [POST, GET])
  fun pagedSearch(
    @ParameterObject @PageableDefault
    pageable: Pageable,
    @RequestBody searchOptions: SearchDto,
  ): SearchPagedResults =
    searchService.performPagedSearch(pageable, searchOptions)

  @Operation(
    summary = "Search for an offender in Delius. Only offenders matching all request attributes will be returned",
    description = "Specify the request criteria to match against",
    operationId = "search",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid Request",
        content = [Content(examples = [])],
      ),
      ApiResponse(responseCode = "404", description = "Not found", content = [Content(examples = [])]),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_COMMUNITY', 'ROLE_PROBATION__SEARCH_PERSON')")
  @RequestMapping("/search", method = [POST, GET])
  fun searchOffenders(@RequestBody searchForm: SearchDto): List<OffenderDetail?>? = searchService.performSearch(searchForm)

  @Operation(
    summary = "Search for an offender in Delius using a search phrase. Only offenders matching all request attributes will be returned",
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
          - client must have ROLE_PROBATION__SEARCH_PERSON
          - by default any offenders that are restricted or have exclusion lists for staff will be redacted in the response (the crn and offender managers will still be visible) with these exceptions
            - there is a Delius user in context (username in JWT token along with an auth source of Delius) and they are allowed to view the offender, e.g. not on the exclusion list or is on the restricted list
            - the client has the scopes to allow it to ignore these lists; the two scopes to bypass this feature are "ignore_delius_exclusions_always" and "ignore_delius_inclusions_always"
         
      """,
    operationId = "searchByPhrase",
  )
  @PreAuthorize("hasAnyRole('ROLE_COMMUNITY', 'ROLE_PROBATION__SEARCH_PERSON')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(examples = [])]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an authorisation with role ROLE_PROBATION__SEARCH_PERSON", content = [Content(examples = [])]),
    ],
  )
  @PostMapping("/phrase")
  fun searchOffendersByPhrase(
    @Valid @RequestBody
    searchPhraseFilter: SearchPhraseFilter,
    @ParameterObject @PageableDefault
    pageable: Pageable,
  ): SearchPhraseResults = searchService.performSearch(
    searchPhraseFilter,
    pageable,
    getOffenderUserAccessFromScopes(securityUserContext),
  )

  @PreAuthorize("hasAnyRole('ROLE_COMMUNITY', 'ROLE_PROBATION__SEARCH_PERSON')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(examples = [])]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an authorisation with role ROLE_PROBATION__SEARCH_PERSON", content = [Content(examples = [])]),
      ApiResponse(responseCode = "500", description = "The list of CRNs provided exceeds the maximum of 512", content = [Content(examples = [])]),
    ],
  )
  @PostMapping("/crns")
  @Operation(
    description =
    """Match prisoners by a list of prisoner CRNs.
    
       This checks each CRN in the provided list against the offender's current CRN and also their previous CRN.
       Because of this and because the max limit of clauses that can be in the query is 1024; the maximum number of CRNs
       that can be sent to this endpoint is 512.
    """,
    summary = "Requires ROLE_PROBATION__SEARCH_PERSON role",
  )
  fun findByIds(
    @Parameter(required = true, name = "crnList") @RequestBody crnList: List<String>,
  ): List<OffenderDetail> {
    return searchService.findByListOfCRNs(crnList)
  }

  @PreAuthorize("hasAnyRole('ROLE_COMMUNITY', 'ROLE_PROBATION__SEARCH_PERSON')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(examples = [])]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an authorisation with role ROLE_PROBATION__SEARCH_PERSON", content = [Content(examples = [])]),
    ],
  )
  @PostMapping("/nomsNumbers")
  @Operation(description = "Match prisoners by a list of prisoner noms numbers", summary = "Requires ROLE_PROBATION__SEARCH_PERSON role")
  fun findByNomsNumbers(
    @Parameter(required = true, name = "nomsList") @RequestBody nomsList: List<String>,
  ): List<OffenderDetail> {
    return searchService.findByListOfNoms(nomsList)
  }

  @PreAuthorize("hasAnyRole('ROLE_COMMUNITY', 'ROLE_PROBATION__SEARCH_PERSON')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(examples = [])]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an authorisation with role ROLE_PROBATION__SEARCH_PERSON", content = [Content(examples = [])]),
    ],
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
    ),
  )
  @Operation(summary = "Match prisoners by a list of ldu codes", description = "Requires ROLE_PROBATION__SEARCH_PERSON role")
  fun findByLduCode(
    @Parameter(required = true, name = "lduList")
    @RequestBody
    @NotEmpty
    lduList: List<String>,
    @ParameterObject @PageableDefault
    pageable: Pageable,
  ): SearchPagedResults {
    return searchService.findByListOfLdu(pageable, lduList)
  }

  @PreAuthorize("hasAnyRole('ROLE_COMMUNITY', 'ROLE_PROBATION__SEARCH_PERSON')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(examples = [])]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an authorisation with role ROLE_PROBATION__SEARCH_PERSON", content = [Content(examples = [])]),
    ],
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
    ),
  )
  @Operation(description = "Match prisoners by a list of team codes", summary = "Requires ROLE_PROBATION__SEARCH_PERSON role")
  fun findByTeamCode(
    @Parameter(required = true, name = "teamCodeList")
    @ParameterObject
    @PageableDefault
    pageable: Pageable,
    @RequestBody @NotEmpty
    teamCodeList: List<String>,
  ): SearchPagedResults {
    return searchService.findByListOfTeamCodes(pageable, teamCodeList)
  }

  @PreAuthorize("hasAnyRole('ROLE_COMMUNITY', 'ROLE_PROBATION__SEARCH_PERSON')")
  @Operation(description = "Match prisoners by a ldu code", summary = "Requires ROLE_PROBATION__SEARCH_PERSON role")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid Request",
        content = [Content(examples = [])],
      ),
      ApiResponse(responseCode = "404", description = "Not found", content = [Content(examples = [])]),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(examples = [])]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an authorisation with role ROLE_PROBATION__SEARCH_PERSON", content = [Content(examples = [])]),
    ],
  )
  @GetMapping("/ldu-codes/{lduCode}")
  fun searchOffendersByLduCode(
    @PathVariable lduCode: String,
    @ParameterObject @PageableDefault
    pageable: Pageable,
  ): SearchPagedResults = searchService.findByLduCode(lduCode, pageable)

  @PreAuthorize("hasAnyRole('ROLE_COMMUNITY', 'ROLE_PROBATION__SEARCH_PERSON')")
  @Operation(summary = "Match prisoners by a team code", description = "Requires ROLE_PROBATION__SEARCH_PERSON role")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid Request",
        content = [Content(examples = [])],
      ),
      ApiResponse(responseCode = "404", description = "Not found", content = [Content(examples = [])]),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token", content = [Content(examples = [])]),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an authorisation with role ROLE_PROBATION__SEARCH_PERSON", content = [Content(examples = [])]),
    ],
  )
  @GetMapping("/team-codes/{teamCode}")
  fun searchOffendersByTeamCode(
    @PathVariable teamCode: String,
    @ParameterObject @PageableDefault
    pageable: Pageable,
  ): SearchPagedResults = searchService.findByTeamCode(teamCode, pageable)

  @GetMapping("/synthetic-monitor")
  fun syntheticMonitor() {
    val start = System.currentTimeMillis()
    val results = searchService.performSearch(SearchDto(surname = "Smith"))

    telemetryClient.trackEvent("synthetic-monitor", mapOf("results" to "${results.size}", "timeMs" to (System.currentTimeMillis() - start).toString()), null)
  }
}
