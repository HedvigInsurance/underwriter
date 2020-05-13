package com.hedvig.underwriter.service

import arrow.core.Either
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
import com.hedvig.underwriter.service.guidelines.NorwegianPersonGuidelines
import com.hedvig.underwriter.service.guidelines.NorwegianTravelGuidelines
import com.hedvig.underwriter.service.guidelines.PersonalDebt
import com.hedvig.underwriter.service.guidelines.SwedishApartmentGuidelines
import com.hedvig.underwriter.service.guidelines.SwedishHouseGuidelines
import com.hedvig.underwriter.service.guidelines.SwedishPersonalGuidelines
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.service.model.QuoteRequestData
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineService
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryRequest
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
    private val debtChecker: DebtChecker,
    private val productPricingService: ProductPricingService,
    private val priceEngineService: PriceEngineService
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
                        birthDate = quoteRequest.birthDate,
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
                        birthDate = quoteRequest.birthDate,
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
                        ssn = quoteRequest.ssn,
                        birthDate = quoteRequest.birthDate!!,
                        firstName = quoteRequest.firstName!!,
                        lastName = quoteRequest.lastName!!,
                        email = quoteRequest.email,
                        type = quoteData.subType!!,
                        street = quoteData.street!!,
                        zipCode = quoteData.zipCode!!,
                        city = quoteData.city,
                        isYouth = quoteData.isYouth!!,
                        coInsured = quoteData.coInsured!!,
                        livingSpace = quoteData.livingSpace!!
                    )
                is QuoteRequestData.NorwegianTravel ->
                    NorwegianTravelData(
                        id = UUID.randomUUID(),
                        ssn = quoteRequest.ssn,
                        birthDate = quoteRequest.birthDate!!,
                        firstName = quoteRequest.firstName!!,
                        lastName = quoteRequest.lastName!!,
                        email = quoteRequest.email,
                        coInsured = quoteData.coInsured!!,
                        isYouth = quoteData.isYouth!!
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
                ApartmentQuotePriceDto.from(quote)
            ).price
            is SwedishHouseData -> productPricingService.priceFromProductPricingForHouseQuote(
                HouseQuotePriceDto.from(quote)
            ).price
            is NorwegianHomeContentsData -> priceEngineService.queryNorwegianHomeContentPrice(
                PriceQueryRequest.NorwegianHomeContent.from(quote.id, quote.memberId, quote.data)
            ).priceBigDecimal
            is NorwegianTravelData -> priceEngineService.queryNorwegianTravelPrice(
                PriceQueryRequest.NorwegianTravel.from(quote.id, quote.memberId, quote.data)
            ).priceBigDecimal
        }
    }

    fun validateGuidelines(data: QuoteData): List<String> {
        val errors = mutableListOf<String>()

        errors.addAll(validatePersonalGuidelines(data))

        errors.addAll(validateProductGuidelines(data))
        return errors
    }

    private fun validatePersonalGuidelines(data: QuoteData): List<String> =
        when (data) {
            is SwedishApartmentData,
            is SwedishHouseData ->
                runRules(
                    data, SwedishPersonalGuidelines(
                        debtChecker
                    ).setOfRules
                )
            is NorwegianHomeContentsData,
            is NorwegianTravelData -> runRules(
                data, NorwegianPersonGuidelines.setOfRules
            )
        }

    private fun validateProductGuidelines(data: QuoteData): List<String> =
        when (data) {
            is SwedishHouseData ->
                runRules(
                    data,
                    SwedishHouseGuidelines.setOfRules
                )
            is SwedishApartmentData ->
                runRules(
                    data,
                    SwedishApartmentGuidelines.setOfRules
                )
            is NorwegianHomeContentsData ->
                runRules(
                    data,
                    NorwegianHomeContentsGuidelines.setOfRules
                )
            is NorwegianTravelData ->
                runRules(
                    data,
                    NorwegianTravelGuidelines.setOfRules
                )
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
