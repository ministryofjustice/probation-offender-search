package uk.gov.justice.hmpps.offendersearch.cvlsearch

import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Validated
@RestController
@RequestMapping("licence-caseload")
class LicenceCaseloadSearchController(val licenceCaseloadService: LicenceCaseloadService) {

  @RequestMapping("by-team", method = [GET, POST])
  fun findLicenceCaseloadByTeam(@RequestBody @Valid request: LicenceCaseloadRequest) =
    licenceCaseloadService.findLicenceCaseload(request)
}
