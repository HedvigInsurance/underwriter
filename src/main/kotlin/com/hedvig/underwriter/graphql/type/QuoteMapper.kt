package com.hedvig.underwriter.graphql.type

import com.hedvig.graphql.commons.type.MonetaryAmountV2
import com.hedvig.libs.translations.Translations
import com.hedvig.underwriter.graphql.type.depricated.CompleteQuoteDetails
import com.hedvig.underwriter.model.ApartmentProductSubType
import com.hedvig.underwriter.model.DanishAccidentData
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.DanishHomeContentsType
import com.hedvig.underwriter.model.DanishTravelData
import com.hedvig.underwriter.model.ExtraBuilding
import com.hedvig.underwriter.model.ExtraBuildingType
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianHomeContentsType
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.model.birthDate
import com.hedvig.underwriter.model.danishAccident
import com.hedvig.underwriter.model.danishHomeContents
import com.hedvig.underwriter.model.danishTravel
import com.hedvig.underwriter.model.email
import com.hedvig.underwriter.model.firstName
import com.hedvig.underwriter.model.lastName
import com.hedvig.underwriter.model.norwegianHomeContents
import com.hedvig.underwriter.model.norwegianTravel
import com.hedvig.underwriter.model.phoneNumber
import com.hedvig.underwriter.model.ssnMaybe
import com.hedvig.underwriter.model.swedishApartment
import com.hedvig.underwriter.model.swedishHouse
import com.hedvig.underwriter.model.validTo
import com.hedvig.underwriter.service.model.QuoteSchema
import com.hedvig.underwriter.util.toStockholmLocalDate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.Locale
import com.hedvig.underwriter.graphql.type.DanishHomeContentsType as ExternalDanishHomeContentsType
import com.hedvig.underwriter.graphql.type.NorwegianHomeContentsType as ExternalNorwegianHomeContentsType

@Component
class QuoteMapper(
    private val translations: Translations
) {
    fun mapToBundleQuote(
        quote: Quote,
        locale: Locale
    ): BundledQuote {
        return BundledQuote(
            id = quote.id,
            firstName = quote.firstName,
            lastName = quote.lastName,
            currentInsurer = quote.currentInsurer?.let { CurrentInsurer.create(it) },
            ssn = quote.ssnMaybe,
            birthDate = quote.birthDate,
            price = MonetaryAmountV2(
                quote.price!!.toPlainString(),
                quote.currency!!
            ),
            quoteDetails = mapToQuoteDetails(quote, locale),
            startDate = quote.startDate,
            expiresAt = quote.validTo.toStockholmLocalDate(),
            email = quote.email,
            dataCollectionId = quote.dataCollectionId,
            initiatedFrom = quote.initiatedFrom.name
        )
    }

    fun mapToQuoteResult(
        quote: Quote,
        insuranceCost: InsuranceCost,
        locale: Locale
    ): QuoteResult {
        return when {
            quote.isComplete -> {
                mapToCompleteQuoteResult(
                    quote,
                    insuranceCost,
                    locale
                )
            }
            else -> {
                mapToIncompleteQuoteResult(
                    quote = quote,
                    locale = locale
                )
            }
        }
    }

    fun mapToIncompleteQuoteDetails(
        quote: Quote,
        locale: Locale
    ): IncompleteQuoteDetails? =
        quote.swedishApartment?.let { apartment ->
            IncompleteQuoteDetails.IncompleteApartmentQuoteDetails(
                street = apartment.street,
                zipCode = apartment.zipCode,
                householdSize = apartment.householdSize,
                livingSpace = apartment.livingSpace,
                type = apartment.subType?.let { ApartmentType.valueOf(it.name) }
            )
        } ?: quote.swedishHouse?.let { house ->
            IncompleteQuoteDetails.IncompleteHouseQuoteDetails(
                street = house.street,
                zipCode = house.zipCode,
                householdSize = house.householdSize,
                livingSpace = house.livingSpace,
                ancillarySpace = house.ancillaryArea,
                extraBuildings = house.extraBuildings?.map { extraBuildingInput ->
                    extraBuilding(extraBuildingInput, locale)
                },
                numberOfBathrooms = house.numberOfBathrooms,
                yearOfConstruction = house.yearOfConstruction,
                isSubleted = house.isSubleted
            )
        }

    fun mapToCompleteQuoteResult(
        quote: Quote,
        insuranceCost: InsuranceCost,
        locale: Locale
    ): QuoteResult.CompleteQuote {
        return QuoteResult.CompleteQuote(
            id = quote.id,
            firstName = quote.firstName,
            lastName = quote.lastName,
            email = quote.email,
            phoneNumber = quote.phoneNumber,
            currentInsurer = quote.currentInsurer?.let { CurrentInsurer.create(it) },
            ssn = quote.ssnMaybe,
            birthDate = quote.birthDate,
            price = MonetaryAmountV2(
                quote.price!!.toPlainString(),
                quote.currency!!
            ),
            insuranceCost = insuranceCost,
            details = mapCompleteQuoteResult(
                quote,
                locale
            ),
            quoteDetails = mapToQuoteDetails(
                quote = quote,
                locale = locale
            ),
            expiresAt = quote.validTo.toStockholmLocalDate(),
            startDate = quote.startDate?.coerceAtLeast(LocalDate.now()),
            dataCollectionId = quote.dataCollectionId
        )
    }

    fun mapToQuoteSchemaData(
        quote: Quote
    ): QuoteSchema = when (quote.data) {
        is SwedishApartmentData -> QuoteSchema.SwedishApartment(
            lineOfBusiness = quote.data.subType!!,
            street = quote.data.street!!,
            zipCode = quote.data.zipCode!!,
            city = quote.data.city,
            livingSpace = quote.data.livingSpace!!,
            numberCoInsured = quote.data.householdSize!! - 1
        )
        is SwedishHouseData -> QuoteSchema.SwedishHouse(
            street = quote.data.street!!,
            zipCode = quote.data.zipCode!!,
            city = quote.data.city,
            livingSpace = quote.data.livingSpace!!,
            numberCoInsured = quote.data.householdSize!! - 1,
            ancillaryArea = quote.data.ancillaryArea!!,
            yearOfConstruction = quote.data.yearOfConstruction!!,
            numberOfBathrooms = quote.data.numberOfBathrooms!!,
            isSubleted = quote.data.isSubleted!!,
            extraBuildings = quote.data.extraBuildings!!.map { extraBuilding ->
                QuoteSchema.SwedishHouse.ExtraBuildingSchema(
                    type = extraBuilding.type,
                    area = extraBuilding.area,
                    hasWaterConnected = extraBuilding.hasWaterConnected
                )
            }
        )
        is NorwegianHomeContentsData -> QuoteSchema.NorwegianHomeContent(
            lineOfBusiness = quote.data.type,
            isYouth = quote.data.isYouth,
            street = quote.data.street,
            zipCode = quote.data.zipCode,
            city = quote.data.city,
            livingSpace = quote.data.livingSpace,
            numberCoInsured = quote.data.coInsured
        )
        is NorwegianTravelData -> QuoteSchema.NorwegianTravel(
            isYouth = quote.data.isYouth,
            numberCoInsured = quote.data.coInsured
        )
        is DanishHomeContentsData -> QuoteSchema.DanishHomeContent(
            street = quote.data.street,
            zipCode = quote.data.zipCode,
            bbrId = quote.data.bbrId,
            city = quote.data.city,
            livingSpace = quote.data.livingSpace,
            numberCoInsured = quote.data.coInsured,
            isStudent = quote.data.isStudent,
            lineOfBusiness = quote.data.type,
            apartment = quote.data.apartment,
            floor = quote.data.floor
        )
        is DanishAccidentData -> QuoteSchema.DanishAccident(
            street = quote.data.street,
            zipCode = quote.data.zipCode,
            bbrId = quote.data.bbrId,
            city = quote.data.city,
            apartment = quote.data.apartment,
            floor = quote.data.floor,
            numberCoInsured = quote.data.coInsured,
            isStudent = quote.data.isStudent
        )
        is DanishTravelData -> QuoteSchema.DanishTravel(
            street = quote.data.street,
            zipCode = quote.data.zipCode,
            bbrId = quote.data.bbrId,
            city = quote.data.city,
            apartment = quote.data.apartment,
            floor = quote.data.floor,
            numberCoInsured = quote.data.coInsured,
            isStudent = quote.data.isStudent
        )
    }

    private fun extraBuilding(
        extraBuilding: ExtraBuilding,
        locale: Locale
    ): ExtraBuildingValue = ExtraBuildingValue(
        type = com.hedvig.underwriter.graphql.type.ExtraBuildingType.valueOf(extraBuilding.type.name),
        area = extraBuilding.area,
        hasWaterConnected = extraBuilding.hasWaterConnected,
        displayName = extractDisplayName(extraBuilding.type, locale)
    )

    private fun mapToIncompleteQuoteResult(
        quote: Quote,
        locale: Locale
    ) = QuoteResult.IncompleteQuote(
        id = quote.id,
        firstName = quote.firstName,
        lastName = quote.lastName,
        email = quote.email,
        birthDate = quote.birthDate,
        currentInsurer = quote.currentInsurer?.let { CurrentInsurer.create(it) },
        details = mapToIncompleteQuoteDetails(
            quote,
            locale
        ),
        startDate = quote.startDate?.coerceAtLeast(LocalDate.now()),
        dataCollectionId = quote.dataCollectionId
    )

    private fun mapToQuoteDetails(
        quote: Quote,
        locale: Locale
    ): QuoteDetails =
        quote.swedishApartment?.let {
            QuoteDetails.SwedishApartmentQuoteDetails(
                street = it.street!!,
                zipCode = it.zipCode!!,
                householdSize = it.householdSize!!,
                livingSpace = it.livingSpace!!,
                type = when (it.subType) {
                    ApartmentProductSubType.BRF -> ApartmentType.BRF
                    ApartmentProductSubType.RENT -> ApartmentType.RENT
                    ApartmentProductSubType.STUDENT_BRF -> ApartmentType.STUDENT_BRF
                    ApartmentProductSubType.STUDENT_RENT -> ApartmentType.STUDENT_RENT
                    null -> throw IllegalArgumentException("Missing subType when mapping quote to SwedishApartmentQuoteDetails (quoteId=${quote.id})")
                }
            )
        } ?: quote.swedishHouse?.let {
            QuoteDetails.SwedishHouseQuoteDetails(
                street = it.street!!,
                zipCode = it.zipCode!!,
                householdSize = it.householdSize!!,
                livingSpace = it.livingSpace!!,
                ancillarySpace = it.ancillaryArea!!,
                extraBuildings = it.extraBuildings!!.map { extraBuildingInput ->
                    extraBuilding(extraBuildingInput, locale)
                },
                numberOfBathrooms = it.numberOfBathrooms!!,
                yearOfConstruction = it.yearOfConstruction!!,
                isSubleted = it.isSubleted!!
            )
        } ?: quote.norwegianHomeContents?.let {
            QuoteDetails.NorwegianHomeContentsDetails(
                street = it.street,
                zipCode = it.zipCode,
                coInsured = it.coInsured,
                livingSpace = it.livingSpace,
                isYouth = it.isYouth,
                type = when (it.type) {
                    NorwegianHomeContentsType.RENT -> ExternalNorwegianHomeContentsType.RENT
                    NorwegianHomeContentsType.OWN -> ExternalNorwegianHomeContentsType.OWN
                }
            )
        } ?: quote.norwegianTravel?.let {
            QuoteDetails.NorwegianTravelDetails(
                coInsured = it.coInsured,
                isYouth = it.isYouth
            )
        } ?: quote.danishHomeContents?.let {
            QuoteDetails.DanishHomeContentsDetails(
                street = it.street,
                apartment = it.apartment,
                floor = it.floor,
                zipCode = it.zipCode,
                city = it.city,
                bbrId = it.bbrId,
                coInsured = it.coInsured,
                livingSpace = it.livingSpace,
                isStudent = it.isStudent,
                type = when (it.type) {
                    DanishHomeContentsType.RENT -> ExternalDanishHomeContentsType.RENT
                    DanishHomeContentsType.OWN -> ExternalDanishHomeContentsType.OWN
                }
            )
        } ?: quote.danishAccident?.let {
            QuoteDetails.DanishAccidentDetails(
                street = it.street,
                zipCode = it.zipCode,
                apartment = it.apartment,
                floor = it.floor,
                bbrId = it.bbrId,
                city = it.city,
                coInsured = it.coInsured,
                isStudent = it.isStudent
            )
        } ?: quote.danishTravel?.let {
            QuoteDetails.DanishTravelDetails(
                street = it.street,
                zipCode = it.zipCode,
                apartment = it.apartment,
                floor = it.floor,
                bbrId = it.bbrId,
                city = it.city,
                coInsured = it.coInsured,
                isStudent = it.isStudent
            )
        }
        ?: throw IllegalStateException(
            "Trying to create QuoteDetails without `swedishApartment`, `swedishHouse`," +
                " `norwegianHomeContents`, `norwegianTravel`, `danishHomeContents`, `danishAccident` or `danishTravel` data"
        )

    private fun mapCompleteQuoteResult(
        quote: Quote,
        locale: Locale
    ): CompleteQuoteDetails =
        quote.swedishApartment?.let { apartment ->
            CompleteQuoteDetails.CompleteApartmentQuoteDetails(
                street = apartment.street!!,
                zipCode = apartment.zipCode!!,
                householdSize = apartment.householdSize!!,
                livingSpace = apartment.livingSpace!!,
                type = ApartmentType.valueOf(apartment.subType!!.name)
            )
        } ?: quote.swedishHouse?.let { house ->
            CompleteQuoteDetails.CompleteHouseQuoteDetails(
                street = house.street!!,
                zipCode = house.zipCode!!,
                householdSize = house.householdSize!!,
                livingSpace = house.livingSpace!!,
                ancillarySpace = house.ancillaryArea!!,
                extraBuildings = house.extraBuildings!!.map { extraBuildingInput ->
                    extraBuilding(extraBuildingInput, locale)
                },
                numberOfBathrooms = house.numberOfBathrooms!!,
                yearOfConstruction = house.yearOfConstruction!!,
                isSubleted = house.isSubleted!!
            )
        } ?: quote.norwegianHomeContents?.let {
            CompleteQuoteDetails.UnknownQuoteDetails()
        } ?: quote.norwegianTravel?.let {
            CompleteQuoteDetails.UnknownQuoteDetails()
        } ?: quote.danishHomeContents?.let {
            CompleteQuoteDetails.UnknownQuoteDetails()
        } ?: quote.danishAccident?.let {
            CompleteQuoteDetails.UnknownQuoteDetails()
        } ?: quote.danishTravel?.let {
            CompleteQuoteDetails.UnknownQuoteDetails()
        }
        ?: throw IllegalStateException(
            "Trying to create QuoteDetails without `swedishApartment`, `swedishHouse`," +
                " `norwegianHomeContents`, `norwegianTravel`, `danishHomeContents`, `danishAccident` or `danishTravel` data"
        )

    private fun extractDisplayName(ebt: ExtraBuildingType, locale: Locale): String =
        translations.get("EXTRA_BUILDING_DISPLAY_NAME_${ebt.name}", locale)
            ?: ebt.getDefaultDisplayName()
}
