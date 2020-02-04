package com.hedvig.underwriter.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ExtraBuildingRequestDto
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.jdbi.v3.json.Json

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SwedishApartmentData::class, name = "apartment"),
    JsonSubTypes.Type(value = SwedishHouseData::class, name = "house")
)
sealed class QuoteData {
    abstract val isComplete: Boolean
    abstract val id: UUID

    fun productType(): ProductType {
        return when (this) {
            is SwedishHouseData -> ProductType.HOUSE
            is SwedishApartmentData -> ProductType.APARTMENT
            is NorwegianHomeContentsData -> ProductType.UNKNOWN
            is NorwegianTravelData -> ProductType.UNKNOWN
        }
    }

    abstract fun passUwGuidelines(): List<String>
}

interface PersonPolicyHolder<T : QuoteData> {
    val ssn: String?
    val firstName: String?
    val lastName: String?
    val email: String?

    fun updateName(firstName: String, lastName: String): T

    fun age(): Long {
        val dateToday = LocalDate.now()

        return this.ssn!!.birthDateFromSsn().until(dateToday, ChronoUnit.YEARS)
    }

    fun ssnIsValid(): Boolean {
        val trimmedInput = ssn!!.trim().replace("-", "").replace(" ", "")

        if (trimmedInput.length != 12) {
            // reasonQuoteCannotBeCompleted += "ssn not valid"
            return false
        }

        try {
            LocalDate.parse(
                trimmedInput.substring(0, 4) + "-" + trimmedInput.substring(
                    4,
                    6
                ) + "-" + trimmedInput.substring(6, 8)
            )
        } catch (exception: Exception) {
            // reasonQuoteCannotBeCompleted += "ssn not valid"
            return false
        }
        return true
    }
}

interface HomeInsurance {
    val street: String?
    val zipCode: String?
    val city: String?
    val livingSpace: Int?
    val householdSize: Int?
}

//TODO: Let's split this file up. But right now we are refactoring some other parts of this so let's do it after
data class SwedishHouseData(
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
) : QuoteData(), HomeInsurance, PersonPolicyHolder<SwedishHouseData> {
    @get:JsonIgnore
    override val isComplete: Boolean
        get() = when (null) {
            ssn, firstName, lastName, street, zipCode, householdSize, livingSpace -> false
            else -> true
        }

    override fun updateName(firstName: String, lastName: String) = this.copy(firstName = firstName, lastName = lastName)

    override fun passUwGuidelines(): List<String> {
        val errors = mutableListOf<String>()

        if (this.householdSize!! < 1) {
            errors += "breaches underwriting guideline household size, must be at least 1"
        }
        if (this.livingSpace!! < 1) {
            errors += "breaches underwriting guidline living space, must be at least 1 sqm"
        }

        if (householdSize!! > 6) {
            errors += "breaches underwriting guideline household size, must not be more than 6"
        }

        if (livingSpace!! > 250) {
            errors += "breaches underwriting guideline living space, must not be more than 250 sqm"
        }

        if (yearOfConstruction!! < 1925) {
            errors += "breaches underwriting guideline year of construction, must not be older than 1925"
        }

        if (numberOfBathrooms!! > 2) {
            errors += "breaches underwriting guideline number of bathrooms, must not be more than 2"
        }

        if (extraBuildings!!.filter { building -> building.area > 6 }.size > 4) {
            errors += "breaches underwriting guideline extra building areas, number of extra buildings with an area over 6 sqm must not be more than 4"
        }

        if (extraBuildings.any { building -> building.area > 75 }) {
            errors += "breaches underwriting guideline extra building areas, extra buildings may not be over 75 sqm"
        }

        if (extraBuildings.any { building -> building.area < 1 }) {
            errors += "breaches underwriting guideline extra building areas, extra buildings must have an area of at least 1"
        }

        return errors
    }
}

data class SwedishApartmentData(
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

    override fun passUwGuidelines(): List<String> {
        val errors = mutableListOf<String>()

        if (this.householdSize!! < 1) {
            errors.add("breaches underwriting guideline household size, must be at least 1")
        }
        if (this.livingSpace!! < 1) {
            errors.add("breaches underwriting guidline living space, must be at least 1")
        }

        when (this.subType) {
            ApartmentProductSubType.STUDENT_RENT, ApartmentProductSubType.STUDENT_BRF -> {
                if (this.householdSize!! > 2) errors.add("breaches underwriting guideline household size must be less than 2")
                if (this.livingSpace!! > 50) errors.add("breaches underwriting guideline living space must be less than or equal to 50sqm")
                if (this.ssn!!.birthDateFromSsn().until(
                        LocalDate.now(),
                        ChronoUnit.YEARS
                    ) > 30
                ) errors.add("breaches underwriting guidelines member must be 30 years old or younger")
            }
            else -> {
                if (this.householdSize!! > 6) errors.add("breaches underwriting guideline household size must be less than or equal to 6")
                if (this.livingSpace!! > 250) errors.add("breaches underwriting guideline living space must be less than or equal to 250sqm")
            }
        }

        return errors
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

data class NorwegianHomeContentsData(
    override val id: UUID,
    override val ssn: String,
    override val firstName: String,
    override val lastName: String,
    override val email: String,

    override val street: String,
    override val city: String,
    override val zipCode: String,
    override val householdSize: Int,
    override val livingSpace: Int,
    val isSudent: Boolean,
    val type: NorwegianHomeContentsType
):  QuoteData(), HomeInsurance, PersonPolicyHolder<NorwegianHomeContentsData> {

    override fun updateName(firstName: String, lastName: String): NorwegianHomeContentsData {
        return this.copy(firstName = firstName, lastName = lastName)
    }

    //TODO: Let's remove the consept of complete
    override val isComplete: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    //TODO: should not even be here
    override fun passUwGuidelines(): List<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

enum class NorwegianHomeContentsType{
    RENT,
    OWN
}

data class NorwegianTravelData(
    override val id: UUID,
    override val ssn: String? = null,
    override val firstName: String? = null,
    override val lastName: String? = null,
    override val email: String? = null,
    val coinsured: Int
): QuoteData(), PersonPolicyHolder<NorwegianTravelData> {

    override fun updateName(firstName: String, lastName: String): NorwegianTravelData {
        return this.copy(firstName = firstName, lastName = lastName)
    }

    //TODO: Let's remove the consept of complete
    override val isComplete: kotlin.Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    //TODO: should not even be here
    override fun passUwGuidelines(): List<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

