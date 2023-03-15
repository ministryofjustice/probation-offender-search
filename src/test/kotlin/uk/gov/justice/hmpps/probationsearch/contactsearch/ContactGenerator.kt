package uk.gov.justice.hmpps.probationsearch.contactsearch

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicLong

object IdGenerator {
  private val id = AtomicLong(1)
  fun getAndIncrement() = id.getAndIncrement()
}

object ContactGenerator {

  fun generate(
    typeCode: String = "CODE",
    typeDescription: String = "Description of Type",
    outcomeCode: String? = null,
    outcomeDescription: String? = null,
    description: String? = null,
    notes: String? = null,
    date: LocalDate = LocalDate.now(),
    startTime: LocalTime? = null,
    endTime: LocalTime? = null,
    lastUpdatedDateTime: LocalDateTime = LocalDateTime.now(),
    highlights: Map<String, List<String>> = mapOf(),
    crn: String = "T123456",
    id: Long = IdGenerator.getAndIncrement(),
  ) = ContactSearchResult(
    crn,
    id,
    typeCode,
    typeDescription,
    outcomeCode,
    outcomeDescription,
    description,
    notes,
    date,
    startTime,
    endTime,
    lastUpdatedDateTime,
    highlights,
  )

  val contacts = listOf(
    generate(
      startTime = LocalTime.of(9, 0),
      lastUpdatedDateTime = LocalDateTime.now().minusSeconds(10),
    ),
    generate(
      startTime = LocalTime.of(9, 30),
      lastUpdatedDateTime = LocalDateTime.now().minusSeconds(20),
    ),
    generate(
      startTime = LocalTime.of(10, 0),
      lastUpdatedDateTime = LocalDateTime.now().minusSeconds(30),
    ),
    generate(
      startTime = LocalTime.of(10, 30),
      lastUpdatedDateTime = LocalDateTime.now().minusSeconds(40),
    ),
    generate(
      typeCode = "FIND_ME",
      typeDescription = "Special Description to be found",
      startTime = LocalTime.of(11, 0),
    ),
    generate(
      crn = "N123456",
      date = LocalDate.now().minusDays(4),
      startTime = LocalTime.of(9, 0),
    ),
    generate(
      crn = "N123456",
      date = LocalDate.now().minusDays(3),
      startTime = LocalTime.of(9, 30),
    ),
    generate(
      crn = "N123456",
      date = LocalDate.now().minusDays(2),
      startTime = LocalTime.of(10, 0),
    ),
    generate(
      crn = "N123456",
      date = LocalDate.now().minusDays(1),
      startTime = LocalTime.of(10, 30),
    ),
    generate(
      crn = "N123456",
      typeCode = "FIND_ME",
      typeDescription = "Special Description to be found",
      startTime = LocalTime.of(11, 0),
    ),
    generate(
      crn = "Z123456",
      typeCode = "UNIQUE",
      typeDescription = "Unique Description",
      date = LocalDate.of(2023, 1, 1),
      notes = "New Year Contact",
    ),
    generate(
      crn = "H123456",
      typeCode = "HIGH",
      typeDescription = "Matches should be highlighted",
      outcomeCode = "HIGH",
      outcomeDescription = "Matches were highlighted",
    ),
  )
}
