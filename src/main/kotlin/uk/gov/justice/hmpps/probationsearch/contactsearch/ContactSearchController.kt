package uk.gov.justice.hmpps.probationsearch.contactsearch

import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/search/contacts")
class ContactSearchController(val contactSearchService: ContactSearchService) {
  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @RequestMapping(method = [RequestMethod.GET, RequestMethod.POST])
  fun searchContact(
    @RequestBody request: ContactSearchRequest,
    @PageableDefault pageable: Pageable
  ): ContactSearchResponse {
    return contactSearchService.performSearch(request, pageable)
  }
}
