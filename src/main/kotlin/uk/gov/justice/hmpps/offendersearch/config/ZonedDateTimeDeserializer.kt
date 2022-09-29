package uk.gov.justice.hmpps.offendersearch.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

@JsonComponent
class ZonedDateTimeDeserializer : JsonDeserializer<ZonedDateTime>() {
    companion object {
        val formatter: DateTimeFormatter = DateTimeFormatterBuilder().parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .parseLenient()
            .optionalStart()
            .appendOffsetId()
            .parseStrict()
            .optionalStart()
            .appendLiteral('[')
            .parseCaseSensitive()
            .appendZoneRegionId()
            .appendLiteral(']')
            .optionalEnd()
            .optionalEnd()
            .toFormatter()
    }

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(parser: JsonParser, context: DeserializationContext?): ZonedDateTime {
        val datetime = formatter.parseBest(parser.text, ZonedDateTime::from, LocalDateTime::from)
        return if (datetime is ZonedDateTime) datetime.withZoneSameInstant(EuropeLondon)
        else (datetime as LocalDateTime).atZone(EuropeLondon)
    }
}
