package uk.gov.justice.hmpps.offendersearch.addresssearch

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(
  name = "address-search",
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
        responseCode = "400",
        description = "Invalid Request",
        content = [Content()]
      ),
      ApiResponse(
        responseCode = "200",
        description = "OK",
        content = [
          Content(
            mediaType = "application/json",
            examples = [
              ExampleObject(
                value = """
         {
          "personAddresses": [
              {
                  "person": {
                      "id": 123,
                      "crn": "X123123",
                      "dob": "1990-08-16",
                      "gender": "Male"
                  },
                  "address": {
                      "id": 123,
                      "buildingName": "Burnham House",
                      "addressNumber": "1",
                      "streetName": "Church Road",
                      "district": "Clarendon Park",
                      "town": "Leicester",
                      "county": "Leicestershire",
                      "postcode": "LM2 1BF",
                      "startDate": "2020-08-03",
                      "notes": "notes text",
                      "createdDateTime": "2020-08-11T11:00:01+01:00",
                      "lastUpdatedDateTime": "2022-06-09T11:47:51+01:00",
                      "status": {
                          "code": "M",
                          "description": "Main"
                      },
                      "type": {
                          "code": "A02",
                          "description": "Approved Premises"
                      },
                      "noFixedAbode": false
                  },
                  "matchScore": 100
              }
            ]
           }"""
              )
            ]
          )
        ]
      )
    ]
  )
  @PreAuthorize("hasRole('ROLE_COMMUNITY')")
  @RequestMapping(method = [RequestMethod.GET, RequestMethod.POST])
  fun searchOffenders(
    @RequestBody addressSearchRequest: AddressSearchRequest,
    @RequestParam(required = false, defaultValue = "100") maxResults: Int
  ): AddressSearchResponses {
    log.info("Search called with {}", addressSearchRequest)
    return addressSearchService.performSearch(addressSearchRequest, maxResults)
  }
}
