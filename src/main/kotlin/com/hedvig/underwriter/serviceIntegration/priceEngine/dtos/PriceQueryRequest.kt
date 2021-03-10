package com.hedvig.underwriter.serviceIntegration.priceEngine.dtos

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.hedvig.productPricingObjects.enums.DanishHomeContentLineOfBusiness
import com.hedvig.productPricingObjects.enums.NorwegianHomeContentLineOfBusiness
import com.hedvig.productPricingObjects.enums.NorwegianTravelLineOfBusiness
import com.hedvig.productPricingObjects.enums.SwedishApartmentLineOfBusiness
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.DanishHomeContentsType
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.model.birthDateFromSwedishSsn
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.mappers.OutgoingMapper
import java.time.LocalDate
import java.time.Year
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = PriceQueryRequest.NorwegianHomeContent::class, name = "NorwegianHomeContent"),
    JsonSubTypes.Type(value = PriceQueryRequest.NorwegianTravel::class, name = "NorwegianTravel"),
    JsonSubTypes.Type(value = PriceQueryRequest.SwedishApartment::class, name = "SwedishApartment"),
    JsonSubTypes.Type(value = PriceQueryRequest.SwedishHouse::class, name = "SwedishHouse"),
    JsonSubTypes.Type(value = PriceQueryRequest.DanishHomeContent::class, name = "DanishHomeContent")
)
sealed class PriceQueryRequest {
    abstract val holderMemberId: String?
    abstract val quoteId: UUID?
    abstract val holderBirthDate: LocalDate
    abstract val numberCoInsured: Int

    data class NorwegianHomeContent(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        val lineOfBusiness: NorwegianHomeContentLineOfBusiness,
        val postalCode: String,
        val squareMeters: Int
    ) : PriceQueryRequest() {
        companion object {
            fun from(quoteId: UUID, memberId: String?, data: NorwegianHomeContentsData) = NorwegianHomeContent(
                holderMemberId = memberId,
                quoteId = quoteId,
                holderBirthDate = data.birthDate,
                numberCoInsured = data.coInsured,
                lineOfBusiness = OutgoingMapper.toLineOfBusiness(data.type, data.isYouth),
                postalCode = data.zipCode,
                squareMeters = data.livingSpace
            )
        }
    }

    data class NorwegianTravel(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        val lineOfBusiness: NorwegianTravelLineOfBusiness
    ) : PriceQueryRequest() {
        companion object {
            fun from(quoteId: UUID, memberId: String?, data: NorwegianTravelData) = NorwegianTravel(
                holderMemberId = memberId,
                quoteId = quoteId,
                holderBirthDate = data.birthDate,
                numberCoInsured = data.coInsured,
                lineOfBusiness = OutgoingMapper.toLineOfBusiness(data.isYouth)
            )
        }
    }

    data class SwedishApartment(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        val lineOfBusiness: SwedishApartmentLineOfBusiness,
        val squareMeters: Int,
        val postalCode: String,
        val dataCollectionId: UUID?
    ) : PriceQueryRequest() {
        companion object {
            fun from(quoteId: UUID, memberId: String?, data: SwedishApartmentData, dataCollectionId: UUID?) =
                SwedishApartment(
                    holderMemberId = memberId,
                    quoteId = quoteId,
                    holderBirthDate = data.birthDate ?: data.ssn!!.birthDateFromSwedishSsn(),
                    numberCoInsured = data.householdSize!! - 1,
                    lineOfBusiness = OutgoingMapper.toLineOfBusiness(data.subType!!),
                    squareMeters = data.livingSpace!!,
                    postalCode = data.zipCode!!,
                    dataCollectionId = dataCollectionId
                )
        }
    }

    data class SwedishHouse(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        val squareMeters: Int,
        val postalCode: String,
        val ancillaryArea: Int,
        val yearOfConstruction: Year,
        val numberOfBathrooms: Int,
        val extraBuildings: List<ExtraBuildingRequestDto>,
        val isSubleted: Boolean,
        val dataCollectionId: UUID?
    ) : PriceQueryRequest() {
        companion object {
            fun from(quoteId: UUID, memberId: String?, data: SwedishHouseData, dataCollectionId: UUID?) = SwedishHouse(
                holderMemberId = memberId,
                quoteId = quoteId,
                holderBirthDate = data.birthDate ?: data.ssn!!.birthDateFromSwedishSsn(),
                numberCoInsured = data.householdSize!! - 1,
                squareMeters = data.livingSpace!!,
                postalCode = data.zipCode!!,
                ancillaryArea = data.ancillaryArea!!,
                yearOfConstruction = Year.of(data.yearOfConstruction!!),
                numberOfBathrooms = data.numberOfBathrooms!!,
                extraBuildings = data.extraBuildings!!.map { extraBuilding ->
                    ExtraBuildingRequestDto(
                        id = null,
                        hasWaterConnected = extraBuilding.hasWaterConnected,
                        area = extraBuilding.area,
                        type = extraBuilding.type
                    )
                },
                isSubleted = data.isSubleted!!,
                dataCollectionId = dataCollectionId
            )
        }
    }

    data class DanishHomeContent(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        val lineOfBusiness: DanishHomeContentLineOfBusiness,
        val bbrId: String?
    ) : PriceQueryRequest() {
        companion object {
            fun from(quoteId: UUID, memberId: String?, data: DanishHomeContentsData) = DanishHomeContent(
                holderMemberId = memberId,
                quoteId = quoteId,
                holderBirthDate = data.birthDate,
                numberCoInsured = data.coInsured,
                lineOfBusiness = when {
                    data.type == DanishHomeContentsType.RENT && data.isStudent -> DanishHomeContentLineOfBusiness.STUDENT_RENT
                    data.type == DanishHomeContentsType.OWN && data.isStudent -> DanishHomeContentLineOfBusiness.STUDENT_OWN
                    data.type == DanishHomeContentsType.RENT && !data.isStudent -> DanishHomeContentLineOfBusiness.RENT
                    data.type == DanishHomeContentsType.OWN && !data.isStudent -> DanishHomeContentLineOfBusiness.OWN
                    else -> throw Exception("Unknown type")
                },
                bbrId = data.bbrId
            )
        }
    }
}
