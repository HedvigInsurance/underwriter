package com.hedvig.underwriter.service.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.hedvig.productPricingObjects.dtos.Agreement
import com.hedvig.underwriter.model.Partner
import com.hedvig.underwriter.model.ProductType
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.birthDate
import com.hedvig.underwriter.model.birthDateFromDanishSsn
import com.hedvig.underwriter.model.birthDateFromNorwegianSsn
import com.hedvig.underwriter.model.birthDateFromSwedishSsn
import com.hedvig.underwriter.model.email
import com.hedvig.underwriter.model.firstName
import com.hedvig.underwriter.model.lastName
import com.hedvig.underwriter.model.ssnMaybe
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.InternalMember
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.extensions.getOldProductType
import com.hedvig.underwriter.util.toStockholmInstant
import com.hedvig.underwriter.web.dtos.ExternalQuoteRequestDto
import com.hedvig.underwriter.web.dtos.QuoteRequestDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class QuoteRequest(
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phoneNumber: String?,
    val currentInsurer: String?,
    val birthDate: LocalDate?,
    val ssn: String?,
    val quotingPartner: Partner?,
    val productType: ProductType? = null,
    @field:JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @field:JsonSubTypes(
        JsonSubTypes.Type(value = QuoteRequestData.SwedishApartment::class, name = "apartment"),
        JsonSubTypes.Type(value = QuoteRequestData.SwedishHouse::class, name = "house"),
        JsonSubTypes.Type(value = QuoteRequestData.NorwegianHomeContents::class, name = "norwegianHomeContents"),
        JsonSubTypes.Type(value = QuoteRequestData.NorwegianTravel::class, name = "norwegianTravel"),
        JsonSubTypes.Type(value = QuoteRequestData.DanishHomeContents::class, name = "danishHomeContents"),
        JsonSubTypes.Type(value = QuoteRequestData.DanishAccident::class, name = "danishAccident"),
        JsonSubTypes.Type(value = QuoteRequestData.DanishTravel::class, name = "danishTravel")
    ) val incompleteQuoteData: QuoteRequestData?,
    val memberId: String?,
    val originatingProductId: UUID?,
    val startDate: Instant?,
    val dataCollectionId: UUID?
) {
    companion object {
        fun from(quoteRequestDto: QuoteRequestDto): QuoteRequest {
            if (
                quoteRequestDto.incompleteQuoteData == null &&
                quoteRequestDto.incompleteHouseQuoteData == null &&
                quoteRequestDto.incompleteApartmentQuoteData == null &&
                quoteRequestDto.norwegianHomeContentsData == null &&
                quoteRequestDto.norwegianTravelData == null &&
                quoteRequestDto.danishHomeContentsData == null &&
                quoteRequestDto.danishAccidentData == null &&
                quoteRequestDto.danishTravelData == null
            ) {
                throw RuntimeException("Cannot create quote data as incompleteQuoteData, incompleteHouseQuoteData and incompleteApartmentQuoteData, norwegianHomeContentsData, norwegianTravelData are all null")
            }

            return QuoteRequest(
                firstName = quoteRequestDto.firstName,
                lastName = quoteRequestDto.lastName,
                email = quoteRequestDto.email,
                currentInsurer = quoteRequestDto.currentInsurer,
                birthDate = quoteRequestDto.birthDate ?: when {
                    quoteRequestDto.incompleteQuoteData != null ||
                        quoteRequestDto.incompleteApartmentQuoteData != null ||
                        quoteRequestDto.incompleteHouseQuoteData != null -> quoteRequestDto.ssn?.birthDateFromSwedishSsn()
                    quoteRequestDto.norwegianHomeContentsData != null ||
                        quoteRequestDto.norwegianTravelData != null -> quoteRequestDto.ssn?.birthDateFromNorwegianSsn()
                    quoteRequestDto.danishHomeContentsData != null ||
                        quoteRequestDto.danishAccidentData != null ||
                        quoteRequestDto.danishTravelData != null -> quoteRequestDto.ssn?.birthDateFromDanishSsn()
                    else -> null
                },
                ssn = quoteRequestDto.ssn,
                quotingPartner = quoteRequestDto.quotingPartner,
                incompleteQuoteData = when {
                    quoteRequestDto.incompleteApartmentQuoteData != null -> quoteRequestDto.incompleteApartmentQuoteData
                    quoteRequestDto.incompleteHouseQuoteData != null -> quoteRequestDto.incompleteHouseQuoteData
                    quoteRequestDto.norwegianHomeContentsData != null -> quoteRequestDto.norwegianHomeContentsData
                    quoteRequestDto.norwegianTravelData != null -> quoteRequestDto.norwegianTravelData
                    quoteRequestDto.danishHomeContentsData != null -> quoteRequestDto.danishHomeContentsData
                    quoteRequestDto.danishAccidentData != null -> quoteRequestDto.danishAccidentData
                    quoteRequestDto.danishTravelData != null -> quoteRequestDto.danishTravelData
                    else -> quoteRequestDto.incompleteQuoteData
                },
                productType = quoteRequestDto.productType,
                memberId = quoteRequestDto.memberId,
                originatingProductId = quoteRequestDto.originatingProductId,
                startDate = quoteRequestDto.startDate,
                dataCollectionId = quoteRequestDto.dataCollectionId,
                phoneNumber = null
            )
        }

        fun from(quoteRequestDto: ExternalQuoteRequestDto): QuoteRequest {
            require(
                quoteRequestDto.swedishApartmentData != null ||
                quoteRequestDto.swedishHouseData != null ||
                quoteRequestDto.norwegianHomeContentsData != null ||
                quoteRequestDto.norwegianTravelData != null ||
                quoteRequestDto.danishHomeContentsData != null ||
                quoteRequestDto.danishAccidentData != null ||
                quoteRequestDto.danishTravelData != null
            ) {
                "Missing minimal quote related data"
            }

            return QuoteRequest(
                firstName = quoteRequestDto.firstName,
                lastName = quoteRequestDto.lastName,
                email = null,
                currentInsurer = null,
                birthDate = quoteRequestDto.birthDate,
                ssn = quoteRequestDto.ssn,
                quotingPartner = null,
                incompleteQuoteData = when {
                    quoteRequestDto.swedishApartmentData != null -> quoteRequestDto.swedishApartmentData
                    quoteRequestDto.swedishHouseData != null -> quoteRequestDto.swedishHouseData
                    quoteRequestDto.norwegianHomeContentsData != null -> quoteRequestDto.norwegianHomeContentsData
                    quoteRequestDto.norwegianTravelData != null -> quoteRequestDto.norwegianTravelData
                    quoteRequestDto.danishHomeContentsData != null -> quoteRequestDto.danishHomeContentsData
                    quoteRequestDto.danishAccidentData != null -> quoteRequestDto.danishAccidentData
                    quoteRequestDto.danishTravelData != null -> quoteRequestDto.danishTravelData
                    else -> throw IllegalStateException()
                },
                productType = when {
                    quoteRequestDto.swedishApartmentData != null -> ProductType.APARTMENT
                    quoteRequestDto.swedishHouseData != null -> ProductType.HOUSE
                    quoteRequestDto.norwegianHomeContentsData != null -> ProductType.HOME_CONTENT
                    quoteRequestDto.norwegianTravelData != null -> ProductType.TRAVEL
                    quoteRequestDto.danishHomeContentsData != null -> ProductType.HOME_CONTENT
                    quoteRequestDto.danishAccidentData != null -> ProductType.ACCIDENT
                    quoteRequestDto.danishTravelData != null -> ProductType.TRAVEL
                    else -> throw IllegalStateException()
                },
                memberId = quoteRequestDto.memberId,
                originatingProductId = null,
                startDate = quoteRequestDto.startDate.atStartOfDay().toStockholmInstant(),
                dataCollectionId = null,
                phoneNumber = null
            )
        }

        fun from(
            member: InternalMember,
            agreementData: Agreement,
            incompleteQuoteData: QuoteRequestData?
        ): QuoteRequest {
            return QuoteRequest(
                firstName = member.firstName,
                lastName = member.lastName,
                birthDate = member.birthDate,
                currentInsurer = null,
                email = member.email,
                quotingPartner = null,
                ssn = member.ssn,
                productType = agreementData.getOldProductType(),
                incompleteQuoteData = incompleteQuoteData,
                dataCollectionId = null,
                memberId = member.memberId.toString(),
                originatingProductId = agreementData.id,
                startDate = agreementData.fromDate?.atStartOfDay(ZoneId.of("Europe/Stockholm"))?.toInstant(),
                phoneNumber = member.phoneNumber
            )
        }

        fun from(memberId: String, schemaData: QuoteSchema): QuoteRequest {
            return QuoteRequest(
                firstName = null,
                lastName = null,
                birthDate = null,
                currentInsurer = null,
                email = null,
                quotingPartner = null,
                ssn = null,
                productType = when (schemaData) {
                    is QuoteSchema.SwedishApartment -> ProductType.APARTMENT
                    is QuoteSchema.SwedishHouse -> ProductType.HOUSE
                    is QuoteSchema.NorwegianHomeContent, is QuoteSchema.DanishHomeContent -> ProductType.HOME_CONTENT
                    is QuoteSchema.NorwegianTravel, is QuoteSchema.DanishTravel -> ProductType.TRAVEL
                    is QuoteSchema.DanishAccident -> ProductType.ACCIDENT
                },
                incompleteQuoteData = QuoteRequestData.from(schemaData),
                dataCollectionId = null,
                memberId = memberId,
                originatingProductId = null,
                startDate = null,
                phoneNumber = null
            )
        }

        fun from(quote: Quote, schemaData: QuoteSchema): QuoteRequest {
            return QuoteRequest(
                firstName = quote.firstName,
                lastName = quote.lastName,
                birthDate = quote.birthDate,
                currentInsurer = quote.currentInsurer,
                email = quote.email,
                quotingPartner = quote.attributedTo,
                ssn = quote.ssnMaybe,
                productType = when (schemaData) {
                    is QuoteSchema.SwedishApartment -> ProductType.APARTMENT
                    is QuoteSchema.SwedishHouse -> ProductType.HOUSE
                    is QuoteSchema.NorwegianHomeContent, is QuoteSchema.DanishHomeContent -> ProductType.HOME_CONTENT
                    is QuoteSchema.NorwegianTravel, is QuoteSchema.DanishTravel -> ProductType.TRAVEL
                    is QuoteSchema.DanishAccident -> ProductType.ACCIDENT
                },
                incompleteQuoteData = QuoteRequestData.from(schemaData),
                dataCollectionId = null,
                memberId = quote.memberId,
                originatingProductId = quote.originatingProductId,
                startDate = null,
                phoneNumber = null
            )
        }
    }
}
