package uk.gov.justice.hmpps.probationsearch.contactsearch.semantic.block

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.LocalDateTime

data class BlockDocument(
  val crn: String,
  @field:Field(
    type = FieldType.Date,
    format = [],
    pattern = [ContactBlockService.Companion.CONTACT_SEMANTIC_BLOCK_TIMESTAMP],
  )
  @field:JsonFormat(
    shape = JsonFormat.Shape.STRING,
    pattern = ContactBlockService.Companion.CONTACT_SEMANTIC_BLOCK_TIMESTAMP,
    timezone = "UTC",
  )
  val timestamp: LocalDateTime,
)