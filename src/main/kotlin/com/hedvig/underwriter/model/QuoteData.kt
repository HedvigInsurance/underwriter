package com.hedvig.underwriter.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.hedvig.underwriter.service.model.PersonPolicyHolder
import com.hedvig.libs.logging.masking.Masked
import org.jdbi.v3.json.Json
import java.time.LocalDate
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SwedishApartmentData::class, name = "apartment"),
    JsonSubTypes.Type(value = SwedishHouseData::class, name = "house"),
    JsonSubTypes.Type(value = NorwegianHomeContentsData::class, name = "norwegianHomeContentsData"),
    JsonSubTypes.Type(value = NorwegianTravelData::class, name = "norwegianTravelData"),
    JsonSubTypes.Type(value = DanishHomeContentsData::class, name = "danishHomeContentsData"),
    JsonSubTypes.Type(value = DanishAccidentData::class, name = "danishAccidentData"),
    JsonSubTypes.Type(value = DanishTravelData::class, name = "danishTravelData")
)
sealed class QuoteData {
    abstract val isComplete: Boolean
    abstract val id: UUID
}

data class SwedishHouseData(
    override val id: UUID,
    @Masked override val ssn: String? = null,
    override val birthDate: LocalDate? = null,
    @Masked override val firstName: String? = null,
    @Masked override val lastName: String? = null,
    @Masked override val email: String? = null,
    @Masked override val phoneNumber: String? = null,
    @Masked override val street: String? = null,
    override val zipCode: String? = null,
    override val city: String? = null,
    var livingSpace: Int? = null,
    override var householdSize: Int? = null,
    val ancillaryArea: Int? = null,
    val yearOfConstruction: Int? = null,
    val numberOfBathrooms: Int? = null,
    @Json
    @get:Json
    val extraBuildings: List<ExtraBuilding>? = null,
    @get:JvmName("getIsSubleted")
    val isSubleted: Boolean? = null,
    val floor: Int? = 0, // NOT USED
    @JsonIgnore
    val internalId: Int? = null
) : QuoteData(), HomeInsurance, PersonPolicyHolder<SwedishHouseData> {
    @get:JsonIgnore
    override val isComplete: Boolean
        get() = when (null) {
            ssn, firstName, lastName, street, zipCode, householdSize, livingSpace -> false
            else -> true
        }

    override fun updateName(firstName: String, lastName: String) = this.copy(firstName = firstName, lastName = lastName)
    override fun updateEmail(email: String) = this.copy(email = email)
    override fun updateSsn(ssn: String) = this.copy(ssn = ssn)
}

data class SwedishApartmentData(
    override val id: UUID,
    @Masked override val ssn: String? = null,
    override val birthDate: LocalDate? = null,
    @Masked override val firstName: String? = null,
    @Masked override val lastName: String? = null,
    @Masked override val email: String? = null,
    override val phoneNumber: String? = null,
    @Masked override val street: String? = null,
    override val city: String? = null,
    override val zipCode: String? = null,
    override val householdSize: Int? = null,
    val livingSpace: Int? = null,

    val subType: ApartmentProductSubType? = null,
    @JsonIgnore
    val internalId: Int? = null
) : QuoteData(), HomeInsurance, PersonPolicyHolder<SwedishApartmentData> {
    @get:JsonIgnore
    override val isComplete: Boolean
        get() = when (null) {
            ssn, firstName, lastName, street, zipCode, householdSize, livingSpace, subType -> false
            else -> true
        }

    @get:JsonProperty(value = "isStudent")
    val isStudent: Boolean
        get() = subType == ApartmentProductSubType.STUDENT_BRF || subType == ApartmentProductSubType.STUDENT_RENT

    override fun updateName(firstName: String, lastName: String) = this.copy(firstName = firstName, lastName = lastName)
    override fun updateEmail(email: String) = this.copy(email = email)
    override fun updateSsn(ssn: String) = this.copy(ssn = ssn)
}

data class NorwegianHomeContentsData(
    override val id: UUID,
    @Masked override val ssn: String? = null,
    override val birthDate: LocalDate,
    @Masked override val firstName: String?,
    @Masked override val lastName: String?,
    @Masked override val email: String?,
    @Masked override val phoneNumber: String? = null,
    @Masked override val street: String,
    override val city: String?,
    override val zipCode: String,
    val livingSpace: Int,
    val coInsured: Int,
    @get:JvmName("getIsYouth")
    val isYouth: Boolean,
    val type: NorwegianHomeContentsType,
    @JsonIgnore
    val internalId: Int? = null
) : QuoteData(), AddressData, PersonPolicyHolder<NorwegianHomeContentsData> {

    override fun updateName(firstName: String, lastName: String) = this.copy(firstName = firstName, lastName = lastName)
    override fun updateEmail(email: String) = this.copy(email = email)
    override fun updateSsn(ssn: String) =
        if (ssn.isValidNorwegianSsn())
            this.copy(ssn = ssn)
        else
            throw IllegalArgumentException("Invalid Norwegian SSN")

    // TODO: Let's remove the concept of complete
    override val isComplete: Boolean
        get() = when (null) {
            firstName, lastName, street, zipCode, coInsured, livingSpace -> false
            else -> true
        }
}

data class NorwegianTravelData(
    override val id: UUID,
    @Masked override val ssn: String? = null,
    override val birthDate: LocalDate,
    @Masked override val firstName: String?,
    @Masked override val lastName: String?,
    @Masked override val email: String? = null,
    @Masked override val phoneNumber: String? = null,
    val coInsured: Int,
    @get:JvmName("getIsYouth")
    val isYouth: Boolean,
    @JsonIgnore
    val internalId: Int? = null
) : QuoteData(), PersonPolicyHolder<NorwegianTravelData> {

    override fun updateName(firstName: String, lastName: String) = this.copy(firstName = firstName, lastName = lastName)
    override fun updateEmail(email: String) = this.copy(email = email)
    override fun updateSsn(ssn: String) =
        if (ssn.isValidNorwegianSsn())
            this.copy(ssn = ssn)
        else
            throw IllegalArgumentException("Invalid Norwegian SSN")

    // TODO: Let's remove the concept of complete
    override val isComplete: Boolean
        get() = when (null) {
            firstName, lastName, coInsured -> false
            else -> true
        }
}

data class DanishHomeContentsData(
    override val id: UUID,
    @Masked override val ssn: String?,
    override val birthDate: LocalDate,
    @Masked override val firstName: String?,
    @Masked override val lastName: String?,
    @Masked override val email: String?,
    @Masked override val phoneNumber: String? = null,
    @Masked override val street: String,
    override val zipCode: String,
    @Masked override val bbrId: String? = null,
    override val city: String?,
    override val apartment: String?,
    override val floor: String?,
    val livingSpace: Int,
    val coInsured: Int,
    @get:JvmName("getIsStudent")
    val isStudent: Boolean,
    val type: DanishHomeContentsType,
    @JsonIgnore
    val internalId: Int? = null
) : QuoteData(), DanishHomeContentAddressData, PersonPolicyHolder<DanishHomeContentsData> {

    override fun updateName(firstName: String, lastName: String) = this.copy(firstName = firstName, lastName = lastName)
    override fun updateEmail(email: String) = this.copy(email = email)
    override fun updateSsn(ssn: String) =
        if (ssn.isValidDanishSsn()) {
            this.copy(ssn = ssn)
        } else {
            throw IllegalArgumentException("Invalid SSN")
        }

    override val isComplete: Boolean
        get() = when (null) {
            firstName, lastName, coInsured -> false
            else -> true
        }
    }

data class DanishAccidentData(
    override val id: UUID,
    @Masked override val ssn: String?,
    override val birthDate: LocalDate,
    @Masked override val firstName: String?,
    @Masked override val lastName: String?,
    @Masked override val email: String?,
    @Masked override val phoneNumber: String? = null,
    override val street: String,
    override val zipCode: String,
    @Masked override val bbrId: String? = null,
    override val city: String?,
    override val apartment: String?,
    override val floor: String?,
    val coInsured: Int,
    @get:JvmName("getIsStudent")
    val isStudent: Boolean,
    @JsonIgnore
    val internalId: Int? = null
) : QuoteData(), DanishHomeContentAddressData, PersonPolicyHolder<DanishAccidentData> {

    override fun updateName(firstName: String, lastName: String) = this.copy(firstName = firstName, lastName = lastName)
    override fun updateEmail(email: String) = this.copy(email = email)
    override fun updateSsn(ssn: String) =
        if (ssn.isValidDanishSsn()) {
            this.copy(ssn = ssn)
        } else {
            throw IllegalArgumentException("Invalid SSN")
        }

    override val isComplete: Boolean
        get() = when (null) {
            firstName, lastName, coInsured -> false
            else -> true
        }
}

data class DanishTravelData(
    override val id: UUID,
    @Masked override val ssn: String?,
    override val birthDate: LocalDate,
    @Masked override val firstName: String?,
    @Masked override val lastName: String?,
    @Masked override val email: String?,
    @Masked override val phoneNumber: String? = null,
    @Masked override val street: String,
    override val zipCode: String,
    @Masked override val bbrId: String? = null,
    override val city: String?,
    override val apartment: String?,
    override val floor: String?,
    val coInsured: Int,
    @get:JvmName("getIsStudent")
    val isStudent: Boolean,
    @JsonIgnore
    val internalId: Int? = null
) : QuoteData(), DanishHomeContentAddressData, PersonPolicyHolder<DanishTravelData> {

    override fun updateName(firstName: String, lastName: String) = this.copy(firstName = firstName, lastName = lastName)
    override fun updateEmail(email: String) = this.copy(email = email)
    override fun updateSsn(ssn: String) =
        if (ssn.isValidDanishSsn()) {
            this.copy(ssn = ssn)
        } else {
            throw IllegalArgumentException("Invalid SSN")
        }

    override val isComplete: Boolean
        get() = when (null) {
            firstName, lastName, coInsured -> false
            else -> true
        }
}
