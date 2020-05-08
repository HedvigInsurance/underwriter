package com.hedvig.underwriter.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.hedvig.underwriter.service.model.PersonPolicyHolder
import java.time.LocalDate
import java.util.UUID
import org.jdbi.v3.json.Json

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SwedishApartmentData::class, name = "apartment"),
    JsonSubTypes.Type(value = SwedishHouseData::class, name = "house"),
    JsonSubTypes.Type(value = NorwegianHomeContentsData::class, name = "norwegianHomeContentsData"),
    JsonSubTypes.Type(value = NorwegianTravelData::class, name = "norwegianTravelData")
)
sealed class QuoteData {
    abstract val isComplete: Boolean
    abstract val id: UUID

    fun productType(): ProductType {
        return when (this) {
            is SwedishHouseData -> ProductType.HOUSE
            is SwedishApartmentData -> ProductType.APARTMENT
            is NorwegianHomeContentsData -> ProductType.HOME_CONTENT
            is NorwegianTravelData -> ProductType.TRAVEL
        }
    }
}

data class SwedishHouseData(
    override val id: UUID,
    override val ssn: String? = null,
    override val birthDate: LocalDate? = null,
    override val firstName: String? = null,
    override val lastName: String? = null,
    override val email: String? = null,

    override val street: String? = null,
    override val zipCode: String? = null,
    override val city: String? = null,
    override var livingSpace: Int? = null,
    override var householdSize: Int? = null,
    val ancillaryArea: Int? = null,
    val yearOfConstruction: Int? = null,
    val numberOfBathrooms: Int? = null,
    @Json
    @get:Json
    val extraBuildings: List<ExtraBuilding>? = null,
    @get:JvmName("getIsSubleted")
    val isSubleted: Boolean? = null,
    val floor: Int? = 0,
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
}

data class SwedishApartmentData(
    override val id: UUID,
    override val ssn: String? = null,
    override val birthDate: LocalDate? = null,
    override val firstName: String? = null,
    override val lastName: String? = null,
    override val email: String? = null,

    override val street: String? = null,
    override val city: String? = null,
    override val zipCode: String? = null,
    override val householdSize: Int? = null,
    override val livingSpace: Int? = null,

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

    override fun updateName(firstName: String, lastName: String): SwedishApartmentData {
        return this.copy(firstName = firstName, lastName = lastName)
    }
}

data class NorwegianHomeContentsData(
    override val id: UUID,
    override val ssn: String? = null,
    override val birthDate: LocalDate,
    override val firstName: String,
    override val lastName: String,
    override val email: String?,

    override val street: String,
    override val city: String?,
    override val zipCode: String,
    override val livingSpace: Int,
    val coInsured: Int,
    @get:JvmName("getIsYouth")
    val isYouth: Boolean,
    val type: NorwegianHomeContentsType,
    @JsonIgnore
    val internalId: Int? = null
) : QuoteData(), AddressData, PersonPolicyHolder<NorwegianHomeContentsData> {

    override fun updateName(firstName: String, lastName: String): NorwegianHomeContentsData {
        return this.copy(firstName = firstName, lastName = lastName)
    }

    // TODO: Let's remove the concept of complete
    override val isComplete: Boolean
        get() = when (null) {
            firstName, lastName, street, zipCode, coInsured, livingSpace -> false
            else -> true
        }
}

data class NorwegianTravelData(
    override val id: UUID,
    override val ssn: String? = null,
    override val birthDate: LocalDate,
    override val firstName: String,
    override val lastName: String,
    override val email: String? = null,
    val coInsured: Int,
    @get:JvmName("getIsYouth")
    val isYouth: Boolean,
    @JsonIgnore
    val internalId: Int? = null
) : QuoteData(), PersonPolicyHolder<NorwegianTravelData> {

    override fun updateName(firstName: String, lastName: String): NorwegianTravelData {
        return this.copy(firstName = firstName, lastName = lastName)
    }

    // TODO: Let's remove the concept of complete
    override val isComplete: Boolean
        get() = when (null) {
            firstName, lastName, coInsured -> false
            else -> true
        }
}
