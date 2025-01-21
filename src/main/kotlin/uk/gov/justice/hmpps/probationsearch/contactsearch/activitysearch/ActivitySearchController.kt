package uk.gov.justice.hmpps.probationsearch.contactsearch.activitysearch

import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/search")
class ActivitySearchController(val activitySearchService: ActivitySearchService) {

  @PreAuthorize("hasRole('ROLE_PROBATION_CONTACT_SEARCH')")
  @RequestMapping("/activity", method = [RequestMethod.POST])
  fun searchActivity(
    @RequestBody request: ActivitySearchRequest,
    @ParameterObject @PageableDefault pageable: Pageable,
  ) = activitySearchService.activitySearch(request, pageable)
}
