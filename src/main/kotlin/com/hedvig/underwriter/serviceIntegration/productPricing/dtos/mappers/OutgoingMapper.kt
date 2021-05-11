package com.hedvig.underwriter.serviceIntegration.productPricing.dtos.mappers

import com.hedvig.productPricingObjects.dtos.Address
import com.hedvig.productPricingObjects.dtos.AgreementQuote
import com.hedvig.productPricingObjects.dtos.CoInsured
import com.hedvig.productPricingObjects.dtos.ExtraBuildingDto
import com.hedvig.productPricingObjects.enums.DanishAccidentLineOfBusiness
import com.hedvig.productPricingObjects.enums.DanishHomeContentLineOfBusiness
import com.hedvig.productPricingObjects.enums.DanishTravelLineOfBusiness
import com.hedvig.productPricingObjects.enums.ExtraBuildingType
import com.hedvig.productPricingObjects.enums.NorwegianHomeContentLineOfBusiness
import com.hedvig.productPricingObjects.enums.NorwegianTravelLineOfBusiness
import com.hedvig.productPricingObjects.enums.SwedishApartmentLineOfBusiness
import com.hedvig.underwriter.model.ApartmentProductSubType
import com.hedvig.underwriter.model.DanishAccidentData
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.DanishHomeContentsType
import com.hedvig.underwriter.model.DanishTravelData
import com.hedvig.underwriter.model.ExtraBuilding
import com.hedvig.underwriter.model.LineItem
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianHomeContentsType
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteData
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.neovisionaries.i18n.CountryCode
import java.time.LocalDate
import com.hedvig.productPricingObjects.dtos.LineItem as DtoLineItem

class OutgoingMapper {
    companion object {
        fun toQuote(quote: Quote, fromDate: LocalDate? = null, toDate: LocalDate? = null) = when (quote.data) {
            is SwedishApartmentData -> AgreementQuote.SwedishApartmentQuote(
                quoteId = quote.id,
                fromDate = fromDate ?: quote.startDate,
                toDate = toDate,
                premium = quote.price!!,
                currency = quote.currency!!,
                currentInsurer = quote.currentInsurer,
                address = this.toAddress(quote.data),
                coInsured = List(quote.data.householdSize!! - 1) { CoInsured(null, null, null) },
                squareMeters = quote.data.livingSpace!!.toLong(),
                lineOfBusiness = this.toLineOfBusiness(quote.data.subType!!),
                lineItems = toLineItems(quote.lineItems)
            )
            is SwedishHouseData -> AgreementQuote.SwedishHouseQuote(
                quoteId = quote.id,
                fromDate = fromDate ?: quote.startDate,
                toDate = toDate,
                premium = quote.price!!,
                currency = quote.currency!!,
                currentInsurer = quote.currentInsurer,
                address = this.toAddress(quote.data),
                coInsured = List(quote.data.householdSize!! - 1) { CoInsured(null, null, null) },
                squareMeters = quote.data.livingSpace!!.toLong(),
                ancillaryArea = quote.data.ancillaryArea!!.toLong(),
                yearOfConstruction = quote.data.yearOfConstruction!!,
                numberOfBathrooms = quote.data.numberOfBathrooms!!,
                extraBuildings = quote.data.extraBuildings!!.map { extraBuilding ->
                    this.toExtraBuildingDto(
                        extraBuilding
                    )
                },
                isSubleted = quote.data.isSubleted!!,
                lineItems = toLineItems(quote.lineItems)
            )
            is NorwegianHomeContentsData -> AgreementQuote.NorwegianHomeContentQuote(
                quoteId = quote.id,
                fromDate = fromDate ?: quote.startDate,
                toDate = toDate,
                premium = quote.price!!,
                currency = quote.currency!!,
                currentInsurer = quote.currentInsurer,
                address = this.toAddress(quote.data),
                coInsured = List(quote.data.coInsured) { CoInsured(null, null, null) },
                squareMeters = quote.data.livingSpace.toLong(),
                lineOfBusiness = this.toLineOfBusiness(quote.data.type, quote.data.isYouth),
                lineItems = toLineItems(quote.lineItems)
            )
            is NorwegianTravelData -> AgreementQuote.NorwegianTravelQuote(
                quoteId = quote.id,
                fromDate = fromDate ?: quote.startDate,
                toDate = toDate,
                premium = quote.price!!,
                currency = quote.currency!!,
                currentInsurer = quote.currentInsurer,
                coInsured = List(quote.data.coInsured) { CoInsured(null, null, null) },
                lineOfBusiness = this.toLineOfBusiness(quote.data.isYouth),
                lineItems = toLineItems(quote.lineItems)
            )
            is DanishHomeContentsData -> AgreementQuote.DanishHomeContentQuote(
                quoteId = quote.id,
                fromDate = fromDate ?: quote.startDate,
                toDate = toDate,
                premium = quote.price!!,
                currency = quote.currency!!,
                currentInsurer = quote.currentInsurer,
                address = this.toAddress(quote.data),
                squareMeters = quote.data.livingSpace.toLong(),
                coInsured = List(quote.data.coInsured) { CoInsured(null, null, null) },
                lineOfBusiness = this.toLineOfBusiness(quote.data.type, quote.data.isStudent),
                lineItems = toLineItems(quote.lineItems)
            )
            is DanishAccidentData -> AgreementQuote.DanishAccidentQuote(
                quoteId = quote.id,
                fromDate = fromDate ?: quote.startDate,
                toDate = toDate,
                premium = quote.price!!,
                currency = quote.currency!!,
                currentInsurer = quote.currentInsurer,
                address = this.toAddress(quote.data),
                coInsured = List(quote.data.coInsured) { CoInsured(null, null, null) },
                lineOfBusiness = if (quote.data.isStudent) DanishAccidentLineOfBusiness.STUDENT else DanishAccidentLineOfBusiness.REGULAR,
                lineItems = toLineItems(quote.lineItems)
            )
            is DanishTravelData -> AgreementQuote.DanishTravelQuote(
                quoteId = quote.id,
                fromDate = fromDate ?: quote.startDate,
                toDate = toDate,
                premium = quote.price!!,
                currency = quote.currency!!,
                currentInsurer = quote.currentInsurer,
                address = this.toAddress(quote.data),
                coInsured = List(quote.data.coInsured) { CoInsured(null, null, null) },
                lineOfBusiness = if (quote.data.isStudent) DanishTravelLineOfBusiness.STUDENT else DanishTravelLineOfBusiness.REGULAR,
                lineItems = toLineItems(quote.lineItems)
            )
        }

        private fun toAddress(data: QuoteData) = when (data) {
            is SwedishApartmentData -> Address(
                street = data.street!!,
                postalCode = data.zipCode!!,
                city = data.city,
                coLine = null,
                country = CountryCode.SE,
                floor = null,
                apartment = null
            )
            is SwedishHouseData -> Address(
                street = data.street!!,
                postalCode = data.zipCode!!,
                city = data.city,
                coLine = null,
                country = CountryCode.SE,
                floor = null,
                apartment = null
            )
            is NorwegianHomeContentsData -> Address(
                street = data.street,
                postalCode = data.zipCode,
                city = data.city,
                coLine = null,
                country = CountryCode.NO,
                floor = null,
                apartment = null
            )
            is DanishHomeContentsData -> Address(
                street = data.street,
                postalCode = data.zipCode,
                city = data.city,
                coLine = null,
                country = CountryCode.DK,
                floor = data.floor,
                apartment = data.apartment
            )
            is DanishAccidentData -> Address(
                street = data.street,
                postalCode = data.zipCode,
                city = data.city,
                coLine = null,
                country = CountryCode.DK,
                floor = data.floor,
                apartment = data.apartment
            )
            is DanishTravelData -> Address(
                street = data.street,
                postalCode = data.zipCode,
                city = data.city,
                coLine = null,
                country = CountryCode.DK,
                floor = data.floor,
                apartment = data.apartment
            )
            is NorwegianTravelData -> throw RuntimeException("Cannot create AddressDto from NorwegianTravelData (data=$data)")
        }

        private fun toExtraBuildingDto(extraBuilding: ExtraBuilding) = ExtraBuildingDto(
            id = null,
            type = ExtraBuildingType.valueOf(extraBuilding.type.name),
            area = extraBuilding.area,
            hasWaterConnected = extraBuilding.hasWaterConnected,
            displayName = extraBuilding.displayName
        )

        fun toLineOfBusiness(type: ApartmentProductSubType) = when (type) {
            ApartmentProductSubType.BRF -> SwedishApartmentLineOfBusiness.BRF
            ApartmentProductSubType.RENT -> SwedishApartmentLineOfBusiness.RENT
            ApartmentProductSubType.STUDENT_BRF -> SwedishApartmentLineOfBusiness.STUDENT_BRF
            ApartmentProductSubType.STUDENT_RENT -> SwedishApartmentLineOfBusiness.STUDENT_RENT
        }

        fun toLineOfBusiness(type: NorwegianHomeContentsType, isYouth: Boolean) = when (type) {
            NorwegianHomeContentsType.RENT -> if (isYouth) NorwegianHomeContentLineOfBusiness.YOUTH_RENT else NorwegianHomeContentLineOfBusiness.RENT
            NorwegianHomeContentsType.OWN -> if (isYouth) NorwegianHomeContentLineOfBusiness.YOUTH_OWN else NorwegianHomeContentLineOfBusiness.OWN
        }

        fun toLineOfBusiness(isYouth: Boolean) = when (isYouth) {
            true -> NorwegianTravelLineOfBusiness.YOUTH
            false -> NorwegianTravelLineOfBusiness.REGULAR
        }

        private fun toLineOfBusiness(type: DanishHomeContentsType, isStudent: Boolean) = when (type) {
            DanishHomeContentsType.RENT -> if (isStudent) DanishHomeContentLineOfBusiness.STUDENT_RENT else DanishHomeContentLineOfBusiness.RENT
            DanishHomeContentsType.OWN -> if (isStudent) DanishHomeContentLineOfBusiness.STUDENT_OWN else DanishHomeContentLineOfBusiness.OWN
        }

        private fun toLineItems(lineItems: List<LineItem>): List<DtoLineItem> {
            return lineItems.map { com.hedvig.productPricingObjects.dtos.LineItem(it.type, it.subType, it.amount) }
                .toList()
        }
    }
}
