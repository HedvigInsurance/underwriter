package com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.neovisionaries.i18n.CountryCode
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = AgreementQuote.SwedishApartmentQuote::class, name = "SwedishApartment"),
    JsonSubTypes.Type(value = AgreementQuote.SwedishHouseQuote::class, name = "SwedishHouse"),
    JsonSubTypes.Type(value = AgreementQuote.NorwegianHomeContentQuote::class, name = "NorwegianHomeContent"),
    JsonSubTypes.Type(value = AgreementQuote.NorwegianTravelQuote::class, name = "NorwegianTravel")
)
sealed class AgreementQuote(
    val quoteId: UUID,
    val fromDate: LocalDate?,
    val toDate: LocalDate?,
    val premium: BigDecimal,
    val currency: String
) {
    class SwedishApartmentQuote(
        quoteId: UUID,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        premium: BigDecimal,
        currency: String,
        val address: AddressDto,
        val coInsured: List<CoInsuredDto>,
        val squareMeters: Long,
        val lineOfBusiness: SwedishApartmentLineOfBusiness
    ) : AgreementQuote(quoteId, fromDate, toDate, premium, currency)

    class SwedishHouseQuote(
        quoteId: UUID,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        premium: BigDecimal,
        currency: String,
        val address: AddressDto,
        val coInsured: List<CoInsuredDto>,
        val squareMeters: Long,
        val ancillaryArea: Long,
        val yearOfConstruction: Int,
        val numberOfBathrooms: Int,
        val extraBuildings: List<ExtraBuildingDto>,
        val isSubleted: Boolean
    ) : AgreementQuote(quoteId, fromDate, toDate, premium, currency)

    class NorwegianHomeContentQuote(
        quoteId: UUID,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        premium: BigDecimal,
        currency: String,
        val address: AddressDto,
        val coInsured: List<CoInsuredDto>,
        val squareMeters: Long,
        val lineOfBusiness: NorwegianHomeContentLineOfBusiness
    ) : AgreementQuote(quoteId, fromDate, toDate, premium, currency)

    class NorwegianTravelQuote(
        quoteId: UUID,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        premium: BigDecimal,
        currency: String,
        val coInsured: List<CoInsuredDto>
    ) : AgreementQuote(quoteId, fromDate, toDate, premium, currency)

    companion object {
        fun from(quote: Quote) = when (quote.data) {
            is SwedishApartmentData -> SwedishApartmentQuote(
                quoteId = quote.id,
                fromDate = quote.startDate,
                toDate = null,
                premium = quote.price!!,
                currency = quote.currency,
                address = AddressDto(
                    street = quote.data.street!!,
                    postalCode = quote.data.zipCode!!,
                    city = quote.data.city,
                    country = CountryCode.SE
                ),
                coInsured = List(quote.data.householdSize!! - 1) { CoInsuredDto(null, null, null) },
                squareMeters = quote.data.livingSpace!!.toLong(),
                lineOfBusiness = SwedishApartmentLineOfBusiness.from(quote.data.subType!!)
            )
            is SwedishHouseData -> SwedishHouseQuote(
                quoteId = quote.id,
                fromDate = quote.startDate,
                toDate = null,
                premium = quote.price!!,
                currency = quote.currency,
                address = AddressDto(
                    street = quote.data.street!!,
                    postalCode = quote.data.zipCode!!,
                    city = quote.data.city,
                    country = CountryCode.SE
                ),
                coInsured = List(quote.data.householdSize!! - 1) { CoInsuredDto(null, null, null) },
                squareMeters = quote.data.livingSpace!!.toLong(),
                ancillaryArea = quote.data.ancillaryArea!!.toLong(),
                yearOfConstruction = quote.data.yearOfConstruction!!,
                numberOfBathrooms = quote.data.numberOfBathrooms!!,
                extraBuildings = quote.data.extraBuildings!!.map((ExtraBuildingDto)::from),
                isSubleted = quote.data.isSubleted!!
            )
            is NorwegianHomeContentsData -> NorwegianHomeContentQuote(
                quoteId = quote.id,
                fromDate = quote.startDate,
                toDate = null,
                premium = quote.price!!,
                currency = quote.currency,
                address = AddressDto(
                    street = quote.data.street,
                    postalCode = quote.data.zipCode,
                    city = quote.data.city,
                    country = CountryCode.NO
                ),
                coInsured = List(quote.data.coInsured) { CoInsuredDto(null, null, null) },
                squareMeters = quote.data.livingSpace.toLong(),
                lineOfBusiness = NorwegianHomeContentLineOfBusiness.from(quote.data.type, quote.data.isStudent)
            )
            is NorwegianTravelData -> NorwegianTravelQuote(
                quoteId = quote.id,
                fromDate = quote.startDate,
                toDate = null,
                premium = quote.price!!,
                currency = quote.currency,
                coInsured = List(quote.data.coInsured) { CoInsuredDto(null, null, null) }
            )
        }
    }
}
