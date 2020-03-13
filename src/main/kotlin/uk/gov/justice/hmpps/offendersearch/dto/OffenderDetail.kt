package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModelProperty
import java.time.LocalDate

data class OffenderDetail(
    val previousSurname: String? = null,
    @ApiModelProperty(required = true) val offenderId: Long? = null,
    val title: String? = null,
    val firstName: String? = null,
    val middleNames: List<String>? = null,
    val surname: String? = null,
    val dateOfBirth: LocalDate? = null,
    val gender: String? = null,
    val otherIds: IDs? = null,
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
    val exclusionMessage: String? = null
)