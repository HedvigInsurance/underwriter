package com.hedvig.underwriter.serviceIntegration.priceEngine.dtos

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.hedvig.productPricingObjects.enums.NorwegianHomeContentLineOfBusiness
import com.hedvig.productPricingObjects.enums.NorwegianTravelLineOfBusiness
import com.hedvig.productPricingObjects.enums.SwedishApartmentLineOfBusiness
import com.hedvig.underwriter.model.DanishAccidentData
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.DanishHomeContentsType
import com.hedvig.underwriter.model.DanishTravelData
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.Partner
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
    JsonSubTypes.Type(value = PriceQueryRequest.DanishHomeContent::class, name = "DanishHomeContent"),
    JsonSubTypes.Type(value = PriceQueryRequest.DanishAccident::class, name = "DanishAccident"),
    JsonSubTypes.Type(value = PriceQueryRequest.DanishTravel::class, name = "DanishTravel")

)
sealed class PriceQueryRequest {
    abstract val holderMemberId: String?
    abstract val quoteId: UUID?
    abstract val holderBirthDate: LocalDate
    abstract val numberCoInsured: Int
    abstract val partner: String

    data class NorwegianHomeContent(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        override val partner: String,
        val lineOfBusiness: NorwegianHomeContentLineOfBusiness,
        val postalCode: String,
        val squareMeters: Int
    ) : PriceQueryRequest() {
        companion object {
            fun from(quoteId: UUID, memberId: String?, partner: Partner, data: NorwegianHomeContentsData) =
                NorwegianHomeContent(
                    holderMemberId = memberId,
                    quoteId = quoteId,
                    holderBirthDate = data.birthDate,
                    numberCoInsured = data.coInsured,
                    partner = partner.partnerName,
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
        override val partner: String,
        val lineOfBusiness: NorwegianTravelLineOfBusiness
    ) : PriceQueryRequest() {
        companion object {
            fun from(quoteId: UUID, memberId: String?, partner: Partner, data: NorwegianTravelData) = NorwegianTravel(
                holderMemberId = memberId,
                quoteId = quoteId,
                holderBirthDate = data.birthDate,
                numberCoInsured = data.coInsured,
                partner = partner.partnerName,
                lineOfBusiness = OutgoingMapper.toLineOfBusiness(data.isYouth)
            )
        }
    }

    data class SwedishApartment(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        override val partner: String,
        val lineOfBusiness: SwedishApartmentLineOfBusiness,
        val squareMeters: Int,
        val postalCode: String,
        val dataCollectionId: UUID?
    ) : PriceQueryRequest() {
        companion object {
            fun from(
                quoteId: UUID,
                memberId: String?,
                data: SwedishApartmentData,
                dataCollectionId: UUID?,
                partner: Partner
            ) = SwedishApartment(
                holderMemberId = memberId,
                quoteId = quoteId,
                holderBirthDate = data.birthDate ?: data.ssn!!.birthDateFromSwedishSsn(),
                numberCoInsured = data.householdSize!! - 1,
                partner = partner.partnerName,
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
        override val partner: String,
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
            fun from(
                quoteId: UUID,
                memberId: String?,
                partner: Partner,
                data: SwedishHouseData,
                dataCollectionId: UUID?
            ) = SwedishHouse(
                holderMemberId = memberId,
                quoteId = quoteId,
                holderBirthDate = data.birthDate ?: data.ssn!!.birthDateFromSwedishSsn(),
                numberCoInsured = data.householdSize!! - 1,
                partner = partner.partnerName,
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
        override val partner: String,
        val squareMeters: Int,
        val bbrId: String?,
        val postalCode: String,
        val street: String,
        val apartment: String?,
        val floor: String?,
        val city: String?,
        val student: Boolean,
        val housingType: DanishHomeContentsType
    ) : PriceQueryRequest() {
        companion object {
            fun from(quoteId: UUID, memberId: String?, partner: Partner, data: DanishHomeContentsData) =
                DanishHomeContent(
                    holderMemberId = memberId,
                    quoteId = quoteId,
                    holderBirthDate = data.birthDate,
                    numberCoInsured = data.coInsured,
                    partner = partner.partnerName,
                    bbrId = data.bbrId,
                    postalCode = data.zipCode,
                    street = data.street,
                    apartment = data.apartment,
                    floor = data.floor,
                    city = data.city,
                    student = data.isStudent,
                    housingType = data.type,
                    squareMeters = data.livingSpace
                )
        }
    }

    data class DanishAccident(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        override val partner: String,
        val bbrId: String?,
        val postalCode: String,
        val street: String,
        val apartment: String?,
        val floor: String?,
        val city: String?,
        val student: Boolean
    ) : PriceQueryRequest() {
        companion object {
            fun from(quoteId: UUID, memberId: String?, partner: Partner, data: DanishAccidentData) = DanishAccident(
                holderMemberId = memberId,
                quoteId = quoteId,
                holderBirthDate = data.birthDate,
                numberCoInsured = data.coInsured,
                partner = partner.partnerName,
                bbrId = data.bbrId,
                postalCode = data.zipCode,
                street = data.street,
                apartment = data.apartment,
                floor = data.floor,
                city = data.city,
                student = data.isStudent
            )
        }
    }

    data class DanishTravel(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        override val partner: String,
        val bbrId: String?,
        val postalCode: String,
        val street: String,
        val apartment: String?,
        val floor: String?,
        val city: String?,
        val student: Boolean
    ) : PriceQueryRequest() {
        companion object {
            fun from(quoteId: UUID, memberId: String?, partner: Partner, data: DanishTravelData) = DanishTravel(
                holderMemberId = memberId,
                quoteId = quoteId,
                holderBirthDate = data.birthDate,
                numberCoInsured = data.coInsured,
                partner = partner.partnerName,
                bbrId = data.bbrId,
                postalCode = data.zipCode,
                street = data.street,
                apartment = data.apartment,
                floor = data.floor,
                city = data.city,
                student = data.isStudent
            )
        }
    }
}
