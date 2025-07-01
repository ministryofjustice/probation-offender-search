package uk.gov.justice.hmpps.probationsearch.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.Period

@JsonIgnoreProperties(ignoreUnknown = true)
data class OffenderDetail(
  val previousSurname: String? = null,
  val offenderId: Long,
  val title: String? = null,
  val firstName: String? = null,
  val middleNames: List<String>? = null,
  val surname: String? = null,
  val dateOfBirth: LocalDate? = null,
  val gender: String? = null,
  val otherIds: IDs,
  val contactDetails: ContactDetails? = null,
  val offenderProfile: OffenderProfile? = null,
  val offenderAliases: List<OffenderAlias>? = null,
  val offenderManagers: List<OffenderManager>? = null,
  val softDeleted: Boolean? = null,
  val currentDisposal: String? = null,
  val partitionArea: String? = null,
  val currentRestriction: Boolean? = null,
  val restrictionMessage: String? = null,
  val currentExclusion: Boolean? = null,
  val exclusionMessage: String? = null,
  @param:Schema(
    description = "map of fields which matched a search term (Only return for phrase searching)",
    example = "{\"surname\": [\"Smith\"], \"offenderAliases.surname\": [\"SMITH\"]}",
  )
  val highlight: Map<String, List<String>>? = null,
  val accessDenied: Boolean? = null,
  val currentTier: String? = null,
  val activeProbationManagedSentence: Boolean? = null,
  val mappa: MappaDetails? = null,
  val probationStatus: ProbationStatus? = null,
) {
  val age: Int? get() = dateOfBirth?.let { Period.between(it, LocalDate.now()).years }
}
