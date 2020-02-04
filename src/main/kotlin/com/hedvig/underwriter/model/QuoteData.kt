package com.hedvig.underwriter.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.hedvig.underwriter.service.model.PersonPolicyHolder
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ExtraBuildingRequestDto
import java.util.UUID
import org.jdbi.v3.json.Json

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ApartmentData::class, name = "apartment"),
    JsonSubTypes.Type(value = HouseData::class, name = "house")
)
sealed class QuoteData {
    abstract val isComplete: Boolean
    abstract val id: UUID

    fun productType(): ProductType {
        return when (this) {
            is HouseData -> ProductType.HOUSE
            is ApartmentData -> ProductType.APARTMENT
        }
    }
}

interface HomeInsurance {
    val street: String?
    val zipCode: String?
    val city: String?
    val livingSpace: Int?
    val householdSize: Int?
}

data class HouseData(
    override val id: UUID,
    override val ssn: String? = null,
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
) : QuoteData(), HomeInsurance, PersonPolicyHolder<HouseData> {
    @get:JsonIgnore
    override val isComplete: Boolean
        get() = when (null) {
            ssn, firstName, lastName, street, zipCode, householdSize, livingSpace -> false
            else -> true
        }

    override fun updateName(firstName: String, lastName: String) = this.copy(firstName = firstName, lastName = lastName)
}

data class ApartmentData(
    override val id: UUID,
    override val ssn: String? = null,
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
) : QuoteData(), HomeInsurance,
    PersonPolicyHolder<ApartmentData> {
    @get:JsonIgnore
    override val isComplete: Boolean
        get() = when (null) {
            ssn, firstName, lastName, street, zipCode, householdSize, livingSpace, subType -> false
            else -> true
        }

    @get:JsonProperty(value = "isStudent")
    val isStudent: Boolean
        get() = subType == ApartmentProductSubType.STUDENT_BRF || subType == ApartmentProductSubType.STUDENT_RENT

    override fun updateName(firstName: String, lastName: String): ApartmentData {
        return this.copy(firstName = firstName, lastName = lastName)
    }
}

data class ExtraBuilding(
    val type: ExtraBuildingType,
    val area: Int,
    val hasWaterConnected: Boolean,
    val displayName: String?
) {
    fun toDto(): ExtraBuildingRequestDto =
        ExtraBuildingRequestDto(
            id = null,
            type = type,
            area = area,
            hasWaterConnected = hasWaterConnected
        )

    companion object {
        fun from(extraBuildingDto: ExtraBuildingRequestDto): ExtraBuilding =
            ExtraBuilding(
                type = extraBuildingDto.type,
                area = extraBuildingDto.area,
                hasWaterConnected = extraBuildingDto.hasWaterConnected,
                displayName = null
            )
    }
}

enum class ExtraBuildingType {
    GARAGE,
    CARPORT,
    SHED,
    STOREHOUSE,
    FRIGGEBOD,
    ATTEFALL,
    OUTHOUSE,
    GUESTHOUSE,
    GAZEBO,
    GREENHOUSE,
    SAUNA,
    BARN,
    BOATHOUSE,
    OTHER
}
