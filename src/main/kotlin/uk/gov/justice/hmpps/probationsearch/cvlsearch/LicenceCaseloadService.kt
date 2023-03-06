package uk.gov.justice.hmpps.probationsearch.cvlsearch

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.search.SearchHits
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.valueOf
import org.springframework.stereotype.Service

@Service
class LicenceCaseloadService(val elasticSearchClient: RestHighLevelClient, val objectMapper: ObjectMapper) {

  fun findLicenceCaseload(request: LicenceCaseloadRequest): Page<LicenceCaseloadPerson> {
    val sort: Sort = Sort.by(request.sortBy.map { Sort.Order(valueOf(it.direction.uppercase()), it.field) })
    val pageable = OffsetPageRequest(request.pageSize, request.offset, sort)
    val searchRequest = SearchRequest("person-search-primary")
    searchRequest.source(LicenceCaseloadQueryBuilder(request).sourceBuilder)
    val res = elasticSearchClient.search(searchRequest, RequestOptions.DEFAULT)
    return res.hits.toLicenceCaseload(pageable)
  }

  private fun PersonDetail.asLicenceCaseloadPerson(): LicenceCaseloadPerson {
    val staffForenames = offenderManagers[0].staff.forenames.split(" ")
    return LicenceCaseloadPerson(
      Name(surname, firstName, middleNames.joinToString { " " }),
      Identifiers(identifiers.crn, identifiers.croNumber, identifiers.nomsNumber, identifiers.pncNumber),
      Manager(
        offenderManagers[0].staff.code,
        Name(
          offenderManagers[0].staff.surname,
          staffForenames[0],
          if (staffForenames.size > 1) staffForenames[1] else null
        ),
        offenderManagers[0].team,
        offenderManagers[0].probationArea
      ),
      offenderManagers[0].fromDate
    )
  }

  private fun SearchHits.toLicenceCaseload(pageable: OffsetPageRequest): Page<LicenceCaseloadPerson> {
    val res = hits.map { hit ->
      objectMapper.readValue(hit.sourceAsString, PersonDetail::class.java).asLicenceCaseloadPerson()
    }
    return PageImpl(res, pageable, hits.size.toLong())
  }
}
