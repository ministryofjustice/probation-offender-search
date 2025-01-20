package uk.gov.justice.hmpps.probationsearch.contactsearch

import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/search/contacts")
class ContactSearchController(val contactSearchService: ContactSearchService) {
  @PreAuthorize("hasAnyRole('ROLE_PROBATION_CONTACT_SEARCH', 'ROLE_PROBATION_INTEGRATION_ADMIN')")
  @RequestMapping(method = [RequestMethod.GET, RequestMethod.POST])
  fun searchContact(
    @RequestBody request: ContactSearchRequest,
    @ParameterObject @PageableDefault pageable: Pageable,
    @RequestParam(defaultValue = "false") semantic: Boolean = false,
  ) = if (semantic) {
    contactSearchService.semanticSearch(request, pageable)
  } else {
    contactSearchService.keywordSearch(request, pageable)
  }
}
