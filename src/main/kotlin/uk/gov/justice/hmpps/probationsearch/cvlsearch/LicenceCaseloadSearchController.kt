package uk.gov.justice.hmpps.probationsearch.cvlsearch

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("licence-caseload")
class LicenceCaseloadSearchController(val licenceCaseloadService: LicenceCaseloadService) {

  @PreAuthorize("hasRole('ROLE_CVL_SEARCH')")
  @RequestMapping("by-team", method = [GET, POST])
  fun findLicenceCaseloadByTeam(@RequestBody @Valid request: LicenceCaseloadRequest) =
    licenceCaseloadService.findLicenceCaseload(request)
}
