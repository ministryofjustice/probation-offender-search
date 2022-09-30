package uk.gov.justice.hmpps.offendersearch.addresssearch

import io.swagger.annotations.Api
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Authorization
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.offendersearch.BadRequestException

@Api(
  tags = ["address-search"],
  authorizations = [Authorization("ROLE_COMMUNITY")],
  description = "Provides address search features for Delius elastic search"
)
@RestController
@RequestMapping("/search/addresses")
@Validated
class AddressSearchController(
  private val addressSearchService: AddressSearchService
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @ApiResponses(
    value = [
      ApiResponse(
        code = 400,
        message = "Invalid Request",
        response = BadRequestException::class
      )
    ]
  )
  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @RequestMapping(method = [RequestMethod.GET, RequestMethod.POST])
  fun searchOffenders(
    @RequestBody addressSearchRequest: AddressSearchRequest,
    @RequestParam(required = false, defaultValue = "20") pageSize: Int,
    @RequestParam(required = false, defaultValue = "0") offset: Int,
  ): List<AddressSearchResponse?> {
    log.info("Search called with {}", addressSearchRequest)
    return addressSearchService.performSearch(addressSearchRequest, pageSize, offset)
  }
}
