package uk.gov.justice.hmpps.probationsearch.utils

object TermSplitter {
  fun split(term: String): List<String> {
    return term.trim().takeIf { it.isNotEmpty() }?.split("\\s+".toRegex()).orEmpty()
  }
}