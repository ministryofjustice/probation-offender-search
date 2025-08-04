package uk.gov.justice.hmpps.probationsearch.contactsearch.model

enum class SortType(val aliases: List<String>, val searchField: String) {
  DATE(listOf("date", "CONTACT_DATE"), "date.date"),
  LAST_UPDATED_DATETIME(listOf("lastUpdated"), "lastUpdatedDateTime"),
  SCORE(listOf("relevance", "RELEVANCE"), "_score"),
  START_DATE_TIME(listOf("startDateTime"), "startDateTime"),
  ;

  companion object {
    fun from(searchField: String): SortType? = entries.firstOrNull { it.searchField == searchField }
  }
}