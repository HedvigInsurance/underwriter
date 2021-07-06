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
import com.hedvig.underwriter.serviceIntegration.lookupService.dtos.CompetitorPricing
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.mappers.OutgoingMapper
import java.math.BigDecimal
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
    abstract val partner: Partner
    abstract val competitorPrice: CompetitorPrice?
    abstract val overriddenPrice: OverriddenPrice?

    data class NorwegianHomeContent(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        override val partner: Partner,
        override val competitorPrice: CompetitorPrice? = null,
        override val overriddenPrice: OverriddenPrice? = null,
        val lineOfBusiness: NorwegianHomeContentLineOfBusiness,
        val postalCode: String,
        val squareMeters: Int
    ) : PriceQueryRequest() {
        companion object {
            fun from(
                quoteId: UUID,
                memberId: String?,
                partner: Partner,
                data: NorwegianHomeContentsData,
                competitorPricing: CompetitorPricing?,
                overriddenPrice: BigDecimal?,
                priceOverriddenBy: String?
            ) =
                NorwegianHomeContent(
                    holderMemberId = memberId,
                    quoteId = quoteId,
                    holderBirthDate = data.birthDate,
                    numberCoInsured = data.coInsured,
                    partner = partner,
                    lineOfBusiness = OutgoingMapper.toLineOfBusiness(data.type, data.isYouth),
                    postalCode = data.zipCode,
                    squareMeters = data.livingSpace,
                    competitorPrice = CompetitorPrice.from(competitorPricing),
                    overriddenPrice = OverriddenPrice.from(overriddenPrice, priceOverriddenBy)
                )
        }
    }

    data class NorwegianTravel(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        override val partner: Partner,
        override val competitorPrice: CompetitorPrice? = null,
        override val overriddenPrice: OverriddenPrice? = null,
        val lineOfBusiness: NorwegianTravelLineOfBusiness
    ) : PriceQueryRequest() {
        companion object {
            fun from(
                quoteId: UUID,
                memberId: String?,
                partner: Partner,
                data: NorwegianTravelData,
                competitorPricing: CompetitorPricing?,
                overriddenPrice: BigDecimal?,
                priceOverriddenBy: String?
            ) = NorwegianTravel(
                holderMemberId = memberId,
                quoteId = quoteId,
                holderBirthDate = data.birthDate,
                numberCoInsured = data.coInsured,
                partner = partner,
                lineOfBusiness = OutgoingMapper.toLineOfBusiness(data.isYouth),
                competitorPrice = CompetitorPrice.from(competitorPricing),
                overriddenPrice = OverriddenPrice.from(overriddenPrice, priceOverriddenBy)
            )
        }
    }

    data class SwedishApartment(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        override val partner: Partner,
        override val competitorPrice: CompetitorPrice? = null,
        override val overriddenPrice: OverriddenPrice? = null,
        val lineOfBusiness: SwedishApartmentLineOfBusiness,
        val squareMeters: Int,
        val postalCode: String
    ) : PriceQueryRequest() {
        companion object {
            fun from(
                quoteId: UUID,
                memberId: String?,
                data: SwedishApartmentData,
                partner: Partner,
                competitorPricing: CompetitorPricing?,
                overriddenPrice: BigDecimal?,
                priceOverriddenBy: String?
            ) = SwedishApartment(
                holderMemberId = memberId,
                quoteId = quoteId,
                holderBirthDate = data.birthDate ?: data.ssn!!.birthDateFromSwedishSsn(),
                numberCoInsured = data.householdSize!! - 1,
                partner = partner,
                lineOfBusiness = OutgoingMapper.toLineOfBusiness(data.subType!!),
                squareMeters = data.livingSpace!!,
                postalCode = data.zipCode!!,
                competitorPrice = CompetitorPrice.from(competitorPricing),
                overriddenPrice = OverriddenPrice.from(overriddenPrice, priceOverriddenBy)
            )
        }
    }

    data class SwedishHouse(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        override val partner: Partner,
        override val competitorPrice: CompetitorPrice? = null,
        override val overriddenPrice: OverriddenPrice? = null,
        val squareMeters: Int,
        val postalCode: String,
        val ancillaryArea: Int,
        val yearOfConstruction: Year,
        val numberOfBathrooms: Int,
        val extraBuildings: List<ExtraBuildingRequestDto>,
        val isSubleted: Boolean
    ) : PriceQueryRequest() {
        companion object {
            fun from(
                quoteId: UUID,
                memberId: String?,
                partner: Partner,
                data: SwedishHouseData,
                competitorPricing: CompetitorPricing?,
                overriddenPrice: BigDecimal?,
                priceOverriddenBy: String?
            ) = SwedishHouse(
                holderMemberId = memberId,
                quoteId = quoteId,
                holderBirthDate = data.birthDate ?: data.ssn!!.birthDateFromSwedishSsn(),
                numberCoInsured = data.householdSize!! - 1,
                partner = partner,
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
                competitorPrice = CompetitorPrice.from(competitorPricing),
                overriddenPrice = OverriddenPrice.from(overriddenPrice, priceOverriddenBy)
            )
        }
    }

    data class DanishHomeContent(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        override val partner: Partner,
        override val competitorPrice: CompetitorPrice? = null,
        override val overriddenPrice: OverriddenPrice? = null,
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
            fun from(
                quoteId: UUID,
                memberId: String?,
                partner: Partner,
                data: DanishHomeContentsData,
                competitorPricing: CompetitorPricing?,
                overriddenPrice: BigDecimal?,
                priceOverriddenBy: String?
            ) =
                DanishHomeContent(
                    holderMemberId = memberId,
                    quoteId = quoteId,
                    holderBirthDate = data.birthDate,
                    numberCoInsured = data.coInsured,
                    partner = partner,
                    bbrId = data.bbrId,
                    postalCode = data.zipCode,
                    street = data.street,
                    apartment = data.apartment,
                    floor = data.floor,
                    city = data.city,
                    student = data.isStudent,
                    housingType = data.type,
                    squareMeters = data.livingSpace,
                    competitorPrice = CompetitorPrice.from(competitorPricing),
                    overriddenPrice = OverriddenPrice.from(overriddenPrice, priceOverriddenBy)
                )
        }
    }

    data class DanishAccident(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        override val partner: Partner,
        override val competitorPrice: CompetitorPrice? = null,
        override val overriddenPrice: OverriddenPrice? = null,
        val bbrId: String?,
        val postalCode: String,
        val street: String,
        val apartment: String?,
        val floor: String?,
        val city: String?,
        val student: Boolean
    ) : PriceQueryRequest() {
        companion object {
            fun from(
                quoteId: UUID,
                memberId: String?,
                partner: Partner,
                data: DanishAccidentData,
                competitorPricing: CompetitorPricing?,
                overriddenPrice: BigDecimal?,
                priceOverriddenBy: String?
            ) = DanishAccident(
                holderMemberId = memberId,
                quoteId = quoteId,
                holderBirthDate = data.birthDate,
                numberCoInsured = data.coInsured,
                partner = partner,
                bbrId = data.bbrId,
                postalCode = data.zipCode,
                street = data.street,
                apartment = data.apartment,
                floor = data.floor,
                city = data.city,
                student = data.isStudent,
                competitorPrice = CompetitorPrice.from(competitorPricing),
                overriddenPrice = OverriddenPrice.from(overriddenPrice, priceOverriddenBy)
            )
        }
    }

    data class DanishTravel(
        override val holderMemberId: String?,
        override val quoteId: UUID?,
        override val holderBirthDate: LocalDate,
        override val numberCoInsured: Int,
        override val partner: Partner,
        override val competitorPrice: CompetitorPrice? = null,
        override val overriddenPrice: OverriddenPrice? = null,
        val bbrId: String?,
        val postalCode: String,
        val street: String,
        val apartment: String?,
        val floor: String?,
        val city: String?,
        val student: Boolean
    ) : PriceQueryRequest() {
        companion object {
            fun from(
                quoteId: UUID,
                memberId: String?,
                partner: Partner,
                data: DanishTravelData,
                competitorPricing: CompetitorPricing?,
                overriddenPrice: BigDecimal?,
                priceOverriddenBy: String?
            ) = DanishTravel(
                holderMemberId = memberId,
                quoteId = quoteId,
                holderBirthDate = data.birthDate,
                numberCoInsured = data.coInsured,
                partner = partner,
                bbrId = data.bbrId,
                postalCode = data.zipCode,
                street = data.street,
                apartment = data.apartment,
                floor = data.floor,
                city = data.city,
                student = data.isStudent,
                competitorPrice = CompetitorPrice.from(competitorPricing),
                overriddenPrice = OverriddenPrice.from(overriddenPrice, priceOverriddenBy)
            )
        }
    }
}
