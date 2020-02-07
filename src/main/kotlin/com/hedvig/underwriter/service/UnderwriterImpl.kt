package com.hedvig.underwriter.service

import arrow.core.Either
import com.hedvig.underwriter.model.ApartmentProductSubType
import com.hedvig.underwriter.model.ExtraBuilding
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.Partner
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteData
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.QuoteState
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.service.guidelines.BaseGuideline
import com.hedvig.underwriter.service.guidelines.NorwegianHomeContentsGuidelines
import com.hedvig.underwriter.service.guidelines.NorwegianTravelGuidelines
import com.hedvig.underwriter.service.guidelines.PersonalDebt
import com.hedvig.underwriter.service.guidelines.SwedishApartmentGuidelines
import com.hedvig.underwriter.service.guidelines.SwedishHouseGuidelines
import com.hedvig.underwriter.service.guidelines.SwedishPersonalGuidelines
import com.hedvig.underwriter.service.guidelines.SwedishStudentApartmentGuidelines
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.service.model.QuoteRequestData
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ApartmentQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.HouseQuotePriceDto
import com.hedvig.underwriter.util.toStockholmLocalDate
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UnderwriterImpl(
    val debtChecker: DebtChecker,
    val productPricingService: ProductPricingService
) : Underwriter {

    override fun createQuote(
        quoteRequest: QuoteRequest,
        id: UUID,
        initiatedFrom: QuoteInitiatedFrom,
        underwritingGuidelinesBypassedBy: String?
    ): Either<Pair<Quote, List<String>>, Quote> {
        val now = Instant.now()

        val quote = Quote(
            id = id,
            createdAt = now,
            productType = quoteRequest.productType!!,
            initiatedFrom = initiatedFrom,
            attributedTo = quoteRequest.quotingPartner
                ?: Partner.HEDVIG,
            data = when (val quoteData = quoteRequest.incompleteQuoteData) {
                is QuoteRequestData.SwedishApartment ->
                    SwedishApartmentData(
                        id = UUID.randomUUID(),
                        ssn = quoteRequest.ssn,
                        firstName = quoteRequest.firstName,
                        lastName = quoteRequest.lastName,
                        email = quoteRequest.email,
                        subType = quoteData.subType,
                        street = quoteData.street,
                        zipCode = quoteData.zipCode,
                        city = quoteData.city,
                        householdSize = quoteData.householdSize,
                        livingSpace = quoteData.livingSpace
                    )
                is QuoteRequestData.SwedishHouse ->
                    SwedishHouseData(
                        id = UUID.randomUUID(),
                        ssn = quoteRequest.ssn,
                        firstName = quoteRequest.firstName,
                        lastName = quoteRequest.lastName,
                        email = quoteRequest.email,
                        street = quoteData.street,
                        zipCode = quoteData.zipCode,
                        city = quoteData.city,
                        householdSize = quoteData.householdSize,
                        livingSpace = quoteData.livingSpace,
                        numberOfBathrooms = quoteData.numberOfBathrooms,
                        isSubleted = quoteData.isSubleted,
                        extraBuildings = quoteData.extraBuildings?.map((ExtraBuilding)::from),
                        ancillaryArea = quoteData.ancillaryArea,
                        yearOfConstruction = quoteData.yearOfConstruction
                    )
                is QuoteRequestData.NorwegianHomeContents ->
                    NorwegianHomeContentsData(
                        id = UUID.randomUUID(),
                        ssn = quoteRequest.ssn!!,
                        firstName = quoteRequest.firstName!!,
                        lastName = quoteRequest.lastName!!,
                        email = quoteRequest.email,
                        type = quoteData.type!!,
                        street = quoteData.street!!,
                        zipCode = quoteData.zipCode!!,
                        city = quoteData.city,
                        isStudent = quoteData.isStudent!!,
                        coinsured = quoteData.coinsured!!,
                        livingSpace = quoteData.livingSpace!!
                    )
                is QuoteRequestData.NorwegianTravel ->
                    NorwegianTravelData(
                        id = UUID.randomUUID(),
                        ssn = quoteRequest.ssn!!,
                        firstName = quoteRequest.firstName!!,
                        lastName = quoteRequest.lastName!!,
                        email = quoteRequest.email,
                        coinsured = quoteData.coinsured!!
                    )
                null -> throw IllegalArgumentException("Must provide either house or apartment data")
            },
            state = QuoteState.INCOMPLETE,
            memberId = quoteRequest.memberId,
            breachedUnderwritingGuidelines = null,
            originatingProductId = quoteRequest.originatingProductId,
            currentInsurer = quoteRequest.currentInsurer,
            startDate = quoteRequest.startDate?.toStockholmLocalDate(),
            dataCollectionId = quoteRequest.dataCollectionId,
            underwritingGuidelinesBypassedBy = underwritingGuidelinesBypassedBy
        )

        return validateAndCompleteQuote(quote, underwritingGuidelinesBypassedBy)
    }

    override fun updateQuote(
        quote: Quote,
        underwritingGuidelinesBypassedBy: String?
    ): Either<Pair<Quote, List<String>>, Quote> {
        return validateAndCompleteQuote(quote, underwritingGuidelinesBypassedBy)
    }

    private fun validateAndCompleteQuote(
        quote: Quote,
        underwritingGuidelinesBypassedBy: String?
    ): Either<Pair<Quote, List<String>>, Quote> {
        val breachedUnderwritingGuidelines = mutableListOf<String>()
        if (underwritingGuidelinesBypassedBy == null) {
            breachedUnderwritingGuidelines.addAll(
                validateGuidelines(quote.data)
            )
        }

        return if (breachedUnderwritingGuidelines.isEmpty()) {
            Either.right(complete(quote))
        } else {
            logBreachedUnderwritingGuidelines(quote, breachedUnderwritingGuidelines)
            Either.left(quote to breachedUnderwritingGuidelines)
        }
    }

    private fun logBreachedUnderwritingGuidelines(quote: Quote, breachedUnderwritingGuidelines: List<String>) {
        when (quote.initiatedFrom) {
            QuoteInitiatedFrom.WEBONBOARDING,
            QuoteInitiatedFrom.APP,
            QuoteInitiatedFrom.IOS,
            QuoteInitiatedFrom.ANDROID -> {
                if (breachedUnderwritingGuidelines != listOf(PersonalDebt.ERROR_MESSAGE)) {
                    logger.error("Breached underwriting guidelines from a controlled flow. Quote: $quote Breached underwriting guidelines: $breachedUnderwritingGuidelines")
                }
            }
            QuoteInitiatedFrom.HOPE,
            QuoteInitiatedFrom.RAPIO -> {
                // no-op
            }
        }
    }

    private fun complete(quote: Quote): Quote {
        val price = getPriceRetrievedFromProductPricing(quote)
        return quote.copy(
            price = price,
            state = QuoteState.QUOTED
        )
    }

    private fun getPriceRetrievedFromProductPricing(quote: Quote): BigDecimal {
        return when (quote.data) {
            is SwedishApartmentData -> productPricingService.priceFromProductPricingForApartmentQuote(
                ApartmentQuotePriceDto.from(
                    quote
                )
            ).price
            is SwedishHouseData -> productPricingService.priceFromProductPricingForHouseQuote(
                HouseQuotePriceDto.from(
                    quote
                )
            ).price
            // TODO: This needs to be fixed should be done by the underwriter
            is NorwegianHomeContentsData -> BigDecimal.ZERO
            is NorwegianTravelData -> BigDecimal.ZERO
        }
    }

    fun validateGuidelines(data: QuoteData): List<String> {
        val errors = mutableListOf<String>()

        errors.addAll(
            runRules(
                data, SwedishPersonalGuidelines(
                    debtChecker
                ).setOfRules
            )
        )

        when (data) {
            is SwedishHouseData -> errors.addAll(
                runRules(
                    data,
                    SwedishHouseGuidelines.setOfRules
                )
            )
            is SwedishApartmentData -> when (data.subType) {
                ApartmentProductSubType.STUDENT_BRF, ApartmentProductSubType.STUDENT_RENT ->
                    errors.addAll(
                        runRules(
                            data,
                            SwedishStudentApartmentGuidelines.setOfRules
                        )
                    )
                else -> errors.addAll(
                    runRules(
                        data,
                        SwedishApartmentGuidelines.setOfRules
                    )
                )
            }
            is NorwegianHomeContentsData -> errors.addAll(
                runRules(
                    data,
                    NorwegianHomeContentsGuidelines.setOfRules
                )
            )
            is NorwegianTravelData -> errors.addAll(
                runRules(
                    data,
                    NorwegianTravelGuidelines.setOfRules
                )
            )
        }
        return errors
    }

    fun <T> runRules(
        data: T,
        listOfRules: Set<BaseGuideline<T>>
    ): MutableList<String> {
        val guidelineErrors = mutableListOf<String>()

        listOfRules.forEach {
            val error = it.invokeValidate(data)
            if (error != null) {
                guidelineErrors.add(error)
            }
            if (it.skipAfter) {
                return@forEach
            }
        }
        return guidelineErrors
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UnderwriterImpl::class.java)
    }
}
