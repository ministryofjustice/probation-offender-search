package uk.gov.justice.hmpps.probationsearch.contactsearch.activitysearch

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicLong

object ActivityIdGenerator {
  private val id = AtomicLong(1)
  fun getAndIncrement() = id.getAndIncrement()
}

object ActivityGenerator {

  private fun generate(
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
    crn: String = "T654321",
    id: Long = ActivityIdGenerator.getAndIncrement(),
    complied: String? = null,
  ) = ActivitySearchResult(
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
    0.5,
    complied,
  )

  val contacts = listOf(
    generate(
      startTime = LocalTime.now(),
      date = LocalDate.now().plusDays(2),
      lastUpdatedDateTime = LocalDateTime.now().minusSeconds(10),
      outcomeDescription = "outcome1",
    ),

    generate(
      crn = "X123456",
      startTime = LocalTime.now(),
      date = LocalDate.now().plusDays(2),
      lastUpdatedDateTime = LocalDateTime.now().minusSeconds(10),
      outcomeDescription = "outcome1",
    ),

    generate(
      startTime = LocalTime.now(),
      date = LocalDate.now().plusDays(1),
      lastUpdatedDateTime = LocalDateTime.now().minusSeconds(20),
      complied = "complied",
      notes = "I complied",
    ),

    generate(
      crn = "X123456",
      startTime = LocalTime.now(),
      date = LocalDate.now(),
      lastUpdatedDateTime = LocalDateTime.now().minusSeconds(20),
      complied = "complied",
      notes = "I complied",
    ),

    generate(
      startTime = LocalTime.now(),
      date = LocalDate.now().plusDays(0),
      lastUpdatedDateTime = LocalDateTime.now().minusSeconds(40),
      notes = "I have no outcome",
    ),
    generate(
      startTime = LocalTime.now().plusMinutes(10),
      date = LocalDate.now().plusDays(0),
      lastUpdatedDateTime = LocalDateTime.now().plusHours(1),
      notes = "I have no outcome date in the future",
    ),
    generate(
      startTime = LocalTime.now(),
      date = LocalDate.now().plusDays(1),
      lastUpdatedDateTime = LocalDateTime.now().minusSeconds(40),
      notes = "I have no outcome date in the future",
    ),
    generate(
      startTime = LocalTime.now(),
      date = LocalDate.now().plusDays(0),
      lastUpdatedDateTime = LocalDateTime.now().minusSeconds(30),
      outcomeDescription = "outcome2",
    ),
    generate(
      typeCode = "TYPE_CODE1",
      typeDescription = "Special Description to be found",
      date = LocalDate.now().plusDays(-1),
      startTime = LocalTime.now(),
      outcomeDescription = "outcome3",
      complied = "ftc",
      notes = "I failed to comply",
    ),

    generate(
      crn = "X123456",
      typeCode = "TYPE_CODE1",
      typeDescription = "Special Description to be found",
      date = LocalDate.now().plusDays(-1),
      startTime = LocalTime.now(),
      outcomeDescription = "outcome3",
      complied = "ftc",
      notes = "I failed to comply",
    ),

    generate(
      typeCode = "TYPE_CODE2",
      typeDescription = "Another Special Description to be found",
      date = LocalDate.now().plusDays(-2),
      startTime = LocalTime.now(),
      outcomeDescription = "outcome4",
      notes = "I failed to comply",
    ),

    generate(
      crn = "X123456",
      typeCode = "TYPE_CODE2ANOTHER",
      typeDescription = "Another Special Description to be found",
      date = LocalDate.now().plusDays(-2),
      startTime = LocalTime.now(),
      outcomeDescription = "outcome4",
      notes = "I failed to comply",
    ),

    generate(
      typeCode = "TYPE_CODE3",
      typeDescription = "3 days ago",
      date = LocalDate.now().plusDays(-3),
      startTime = LocalTime.now(),
      outcomeDescription = "outcome5",
      notes = "I failed to comply",
    ),

    )
}
