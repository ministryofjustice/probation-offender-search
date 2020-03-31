package uk.gov.justice.hmpps.offendersearch.controllers

import io.swagger.annotations.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.offendersearch.BadRequestException
import uk.gov.justice.hmpps.offendersearch.NotFoundException
import uk.gov.justice.hmpps.offendersearch.dto.MatchRequest
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.dto.OffenderMatches
import uk.gov.justice.hmpps.offendersearch.services.MatchService
import javax.validation.Valid

@Api(tags = ["offender-match"], authorizations = [Authorization("ROLE_COMMUNITY")], description = "Provides offender matching features for Delius elastic search")
@RestController
@RequestMapping(value = ["match"], produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_COMMUNITY')")
class OffenderMatchController(private val matchService: MatchService) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @ApiOperation(value = "Match for an offender in Delius ElasticSearch. It will return the best group of matching offenders based on the request", notes = "Specify the request criteria to match against", authorizations = [Authorization("ROLE_COMMUNITY")], nickname = "match")
  @ApiResponses(value = [ApiResponse(code = 200, message = "OK", response = OffenderMatches::class), ApiResponse(code = 400, message = "Invalid Request", response = BadRequestException::class), ApiResponse(code = 404, message = "Not found", response = NotFoundException::class)])
  @PostMapping
  fun matchOffenders(@Valid @RequestBody matchRequest: MatchRequest): OffenderMatches {
    log.info("Match called with {}", matchRequest)
    return matchService.match(matchRequest)
  }

}