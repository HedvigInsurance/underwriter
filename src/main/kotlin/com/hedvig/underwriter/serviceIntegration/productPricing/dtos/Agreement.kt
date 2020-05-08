package com.hedvig.underwriter.serviceIntegration.productPricing.dtos

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.hedvig.underwriter.model.ApartmentProductSubType
import com.hedvig.underwriter.model.NorwegianHomeContentsType
import com.hedvig.underwriter.model.ProductType
import com.hedvig.underwriter.service.model.QuoteRequestData
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.ExtraBuildingDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.NorwegianHomeContentLineOfBusiness
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.NorwegianTravelLineOfBusiness
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.SwedishApartmentLineOfBusiness
import java.time.LocalDate
import java.util.UUID
import javax.money.MonetaryAmount

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Agreement.SwedishApartment::class, name = "SwedishApartment"),
    JsonSubTypes.Type(value = Agreement.SwedishHouse::class, name = "SwedishHouse"),
    JsonSubTypes.Type(value = Agreement.NorwegianHomeContent::class, name = "NorwegianHomeContent"),
    JsonSubTypes.Type(value = Agreement.NorwegianTravel::class, name = "NorwegianTravel")
)
sealed class Agreement {
    abstract val id: UUID
    abstract val fromDate: LocalDate?
    abstract val toDate: LocalDate?
    abstract val basePremium: MonetaryAmount
    abstract val certificateUrl: String?
    abstract val status: AgreementStatus

    abstract fun getOldProductType(): ProductType

    abstract fun toQuoteRequestData(): QuoteRequestData

    data class SwedishApartment(
        override val id: UUID,
        override val fromDate: LocalDate?,
        override val toDate: LocalDate?,
        override val basePremium: MonetaryAmount,
        override val certificateUrl: String?,
        override val status: AgreementStatus,
        val lineOfBusiness: SwedishApartmentLineOfBusiness,
        val address: Address,
        val numberCoInsured: Int,
        val squareMeters: Long
    ) : Agreement() {
        override fun toQuoteRequestData() =
            QuoteRequestData.SwedishApartment(
                street = this.address.street,
                zipCode = this.address.postalCode,
                livingSpace = this.squareMeters.toInt(),
                householdSize = this.numberCoInsured,
                subType = ApartmentProductSubType.valueOf(this.lineOfBusiness.name),
                city = null,
                floor = null
            )
        override fun getOldProductType() = ProductType.APARTMENT
    }

    data class SwedishHouse(
        override val id: UUID,
        override val fromDate: LocalDate?,
        override val toDate: LocalDate?,
        override val basePremium: MonetaryAmount,
        override val certificateUrl: String?,
        override val status: AgreementStatus,
        val address: Address,
        val numberCoInsured: Int,
        val squareMeters: Long,
        val ancillaryArea: Long,
        val yearOfConstruction: Int,
        val numberOfBathrooms: Int,
        val extraBuildings: List<ExtraBuildingDto>,
        val isSubleted: Boolean
    ) : Agreement() {
        override fun toQuoteRequestData() =
            QuoteRequestData.SwedishHouse(
                street = this.address.street,
                zipCode = this.address.postalCode,
                city = this.address.city,
                livingSpace = this.squareMeters.toInt(),
                householdSize = this.numberCoInsured,
                ancillaryArea = this.ancillaryArea.toInt(),
                yearOfConstruction = this.yearOfConstruction,
                numberOfBathrooms = this.numberOfBathrooms,
                extraBuildings = this.extraBuildings.map {
                        extraBuildingDto -> ExtraBuildingRequestDto.from(extraBuildingDto) },
                isSubleted = this.isSubleted,
                floor = 0
            )
        override fun getOldProductType() = ProductType.HOUSE
    }

    data class NorwegianHomeContent(
        override val id: UUID,
        override val fromDate: LocalDate?,
        override val toDate: LocalDate?,
        override val basePremium: MonetaryAmount,
        override val certificateUrl: String?,
        override val status: AgreementStatus,
        val lineOfBusiness: NorwegianHomeContentLineOfBusiness,
        val address: Address,
        val numberCoInsured: Int,
        val squareMeters: Long
    ) : Agreement() {
        override fun toQuoteRequestData() = QuoteRequestData.NorwegianHomeContents(
            street = this.address.street,
            zipCode = this.address.postalCode,
            livingSpace = this.squareMeters.toInt(),
            city = null,
            coInsured = this.numberCoInsured,
            type = NorwegianHomeContentsType.valueOf(this.lineOfBusiness.name),
            isYouth = when (this.lineOfBusiness) {
                NorwegianHomeContentLineOfBusiness.OWN,
                NorwegianHomeContentLineOfBusiness.RENT -> {
                    false
                }
                NorwegianHomeContentLineOfBusiness.YOUTH_OWN,
                NorwegianHomeContentLineOfBusiness.YOUTH_RENT -> {
                    true
                }
            }
        )
        override fun getOldProductType() = ProductType.HOME_CONTENT
    }

    data class NorwegianTravel(

        override val id: UUID,
        override val fromDate: LocalDate?,
        override val toDate: LocalDate?,
        override val basePremium: MonetaryAmount,
        override val certificateUrl: String?,
        override val status: AgreementStatus,
        val lineOfBusiness: NorwegianTravelLineOfBusiness,
        val numberCoInsured: Int
    ) : Agreement() {
        override fun toQuoteRequestData() = QuoteRequestData.NorwegianTravel(
            coInsured = this.numberCoInsured,
            isYouth = when (this.lineOfBusiness) {
                NorwegianTravelLineOfBusiness.REGULAR -> false
                NorwegianTravelLineOfBusiness.YOUTH -> true
            }
        )
        override fun getOldProductType() = ProductType.TRAVEL
    }
}
