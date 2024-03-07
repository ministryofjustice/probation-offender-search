package uk.gov.justice.hmpps.probationsearch.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.probationsearch.dto.MatchRequest
import uk.gov.justice.hmpps.probationsearch.dto.OffenderMatches
import uk.gov.justice.hmpps.probationsearch.services.MatchService

@Tag(name = "offender-match", description = "Provides offender matching features for Delius elastic search")
@RestController
@RequestMapping(value = ["match"], produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_COMMUNITY', 'ROLE_PROBATION__SEARCH_PERSON')")
class OffenderMatchController(private val matchService: MatchService) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Operation(
    summary = "Match for an offender in Delius. It will return the best group of matching offenders based on the request",
    description = "Specify the request criteria to match against",
    operationId = "match",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(
        responseCode = "400",
        description = "Invalid Request",
      ),
      ApiResponse(responseCode = "404", description = "Not found"),
    ],
  )
  @PostMapping
  fun matchOffenders(
    @Valid @RequestBody
    matchRequest: MatchRequest,
  ): OffenderMatches = matchService.match(matchRequest)
}
