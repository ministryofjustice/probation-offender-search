package uk.gov.justice.hmpps.probationsearch.contactsearch

import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchResult
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
    0.5,
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
    generate(
      crn = "Z123456",
      notes = """
        To Sherlock Holmes she is always _the_ woman. I have seldom heard him
        mention her under any other name. In his eyes she eclipses and
        predominates the whole of her sex. It was not that he felt any emotion
        akin to love for Irene Adler. All emotions, and that one particularly,
        were abhorrent to his cold, precise but admirably balanced mind. He
        was, I take it, the most perfect reasoning and observing machine that
        the world has seen, but as a lover he would have placed himself in a
        false position. He never spoke of the softer passions, save with a gibe
        and a sneer. They were admirable things for the observer—excellent for
        drawing the veil from men’s motives and actions. But for the trained
        reasoner to admit such intrusions into his own delicate and finely
        adjusted temperament was to introduce a distracting factor which might
        throw a doubt upon all his mental results. Grit in a sensitive
        instrument, or a crack in one of his own high-power lenses, would not
        be more disturbing than a strong emotion in a nature such as his. And
        yet there was but one woman to him, and that woman was the late Irene
        Adler, of dubious and questionable memory.
        
        I had seen little of Holmes lately. My marriage had drifted us away
        from each other. My own complete happiness, and the home-centred
        interests which rise up around the man who first finds himself master
        of his own establishment, were sufficient to absorb all my attention,
        while Holmes, who loathed every form of society with his whole Bohemian
        soul, remained in our lodgings in Baker Street, buried among his old
        books, and alternating from week to week between cocaine and ambition,
        the drowsiness of the drug, and the fierce energy of his own keen
        nature. He was still, as ever, deeply attracted by the study of crime,
        and occupied his immense faculties and extraordinary powers of
        observation in following out those clues, and clearing up those
        mysteries which had been abandoned as hopeless by the official police.
        From time to time I heard some vague account of his doings: of his
        summons to Odessa in the case of the Trepoff murder, of his clearing up
        of the singular tragedy of the Atkinson brothers at Trincomalee, and
        finally of the mission which he had accomplished so delicately and
        successfully for the reigning family of Holland. Beyond these signs of
        his activity, however, which I merely shared with all the readers of
        the daily press, I knew little of my former friend and companion.
      """.trimIndent(),
    ),
  )
}
