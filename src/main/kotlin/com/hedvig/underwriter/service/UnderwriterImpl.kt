package com.hedvig.underwriter.service

import arrow.core.Either
import com.hedvig.underwriter.model.DanishAccidentData
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.DanishTravelData
import com.hedvig.underwriter.model.LineItem
import com.hedvig.underwriter.model.Market
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
import com.hedvig.underwriter.service.guidelines.BreachedGuidelineCode
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes.OK
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.service.quoteStrategies.QuoteStrategyService
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineService
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryRequest
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.util.MetricsCounter
import com.hedvig.underwriter.util.logger
import com.hedvig.underwriter.util.toStockholmLocalDate
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class UnderwriterImpl(
    private val priceEngineService: PriceEngineService,
    private val quoteStrategyService: QuoteStrategyService,
    private val requotingService: RequotingService,
    private val blockedByTypeCounter: BlockedByTypeCounter,
    private val breachedGuidelinesCounter: BreachedGuidelinesCounter
) : Underwriter {

    @Value("\${features.block-requoting:false}")
    private var blockRequoting: Boolean = false

    override fun createQuote(
        quoteRequest: QuoteRequest,
        id: UUID,
        initiatedFrom: QuoteInitiatedFrom,
        underwritingGuidelinesBypassedBy: String?
    ): Either<Pair<Quote, List<BreachedGuidelineCode>>, Quote> {
        val now = Instant.now()

        val quote = Quote(
            id = id,
            createdAt = now,
            updatedAt = now,
            productType = quoteRequest.productType!!,
            initiatedFrom = initiatedFrom,
            attributedTo = quoteRequest.quotingPartner ?: Partner.HEDVIG,
            data = createQuoteData(quoteRequest),
            state = QuoteState.INCOMPLETE,
            memberId = quoteRequest.memberId,
            breachedUnderwritingGuidelines = null,
            originatingProductId = quoteRequest.originatingProductId,
            currentInsurer = quoteRequest.currentInsurer,
            startDate = quoteRequest.startDate?.toStockholmLocalDate(),
            dataCollectionId = quoteRequest.dataCollectionId,
            underwritingGuidelinesBypassedBy = underwritingGuidelinesBypassedBy
        )

        // Do customer have an active agreement for this already?
        if (requotingService.blockDueToExistingAgreement(quote)) {
            blockedByTypeCounter.increment(quote)
            if (blockRequoting) {
                logger.info("Cannot create quote, customer already have an active agreement")
                throw IllegalStateException("Creation of quote is blocked")
            }
        }

        return validateAndCompleteQuote(quote, underwritingGuidelinesBypassedBy)
    }

    private fun createQuoteData(quoteRequest: QuoteRequest): QuoteData =
        quoteRequest.incompleteQuoteData?.createQuoteData(quoteRequest)
            ?: throw IllegalArgumentException("Must provide either house or apartment data")

    override fun validateAndCompleteQuote(
        quote: Quote,
        underwritingGuidelinesBypassedBy: String?
    ): Either<Pair<Quote, List<BreachedGuidelineCode>>, Quote> {
        val breachedUnderwritingGuidelines = mutableListOf<BreachedGuidelineCode>()
        if (underwritingGuidelinesBypassedBy == null) {
            breachedUnderwritingGuidelines.addAll(
                validateGuidelines(quote)
            )
        }
        return if (breachedUnderwritingGuidelines.isEmpty()) {
            Either.right(complete(quote))
        } else {
            Either.left(
                quote.copy(
                    breachedUnderwritingGuidelines = breachedUnderwritingGuidelines.map { it }
                ) to breachedUnderwritingGuidelines
            )
        }
    }

    private fun complete(quote: Quote): Quote {
        val priceResponse = getPriceRetrievedFromProductPricing(quote)

        // Check if we should reuse old price if customer have done this query before
        val price = requotingService.useOldOrNewPrice(quote, Price.from(priceResponse))

        return quote.copy(
            price = price.amount,
            currency = price.currency,
            priceFrom = price.priceFromId,
            lineItems = price.lineItems.map {
                LineItem(
                    type = it.type,
                    subType = it.subType,
                    amount = it.amount
                )
            }.toList(),
            state = QuoteState.QUOTED
        )
    }

    private fun getPriceRetrievedFromProductPricing(quote: Quote): PriceQueryResponse {
        return when (quote.data) {
            is SwedishApartmentData -> priceEngineService.querySwedishApartmentPrice(
                PriceQueryRequest.SwedishApartment.from(quote.id, quote.memberId, quote.data, quote.dataCollectionId)
            )
            is SwedishHouseData -> priceEngineService.querySwedishHousePrice(
                PriceQueryRequest.SwedishHouse.from(quote.id, quote.memberId, quote.data, quote.dataCollectionId)
            )
            is NorwegianHomeContentsData -> priceEngineService.queryNorwegianHomeContentPrice(
                PriceQueryRequest.NorwegianHomeContent.from(quote.id, quote.memberId, quote.data)
            )
            is NorwegianTravelData -> priceEngineService.queryNorwegianTravelPrice(
                PriceQueryRequest.NorwegianTravel.from(quote.id, quote.memberId, quote.data)
            )
            is DanishHomeContentsData -> {
//                do we need the dataCollectionId or is this just for Sweden?
                priceEngineService.queryDanishHomeContentPrice(
                    PriceQueryRequest.DanishHomeContent.from(quote.id, quote.memberId, quote.data)
                )
            }
            is DanishAccidentData -> {
                priceEngineService.queryDanishAccidentPrice(
                    PriceQueryRequest.DanishAccident.from(quote.id, quote.memberId, quote.data)
                )
            }
            is DanishTravelData -> {
                priceEngineService.queryDanishTravelPrice(
                    PriceQueryRequest.DanishTravel.from(quote.id, quote.memberId, quote.data)
                )
            }
        }
    }

    fun validateGuidelines(data: Quote): List<BreachedGuidelineCode> {
        val errors = mutableListOf<BreachedGuidelineCode>()

        val guidelines = quoteStrategyService.getAllGuidelines(data)
        errors.addAll(runRules(data.data, guidelines))
        errors.forEach {
            breachedGuidelinesCounter.increment(data.market, it)
        }
        return errors
    }

    fun <T : QuoteData> runRules(
        data: T,
        listOfRules: Set<BaseGuideline<T>>
    ): MutableList<BreachedGuidelineCode> {
        val guidelineErrors = mutableListOf<BreachedGuidelineCode>()

        for (rule in listOfRules) {
            val error = rule.validate(data)
            if (error != OK) {
                guidelineErrors.add(error)
                if (rule.skipAfter) {
                    break
                }
            }
        }
        return guidelineErrors
    }

    @Component
    class BlockedByTypeCounter(override val registry: MeterRegistry) :
        MetricsCounter(registry, "requoting.blocked_by_type") {
        fun increment(quote: Quote) {
            super.increment("type", quote.data::class.simpleName, "initiated_from", quote.initiatedFrom.name)
        }
    }

    @Component
    class BreachedGuidelinesCounter(override val registry: MeterRegistry) :
        MetricsCounter(registry, "breached.underwriting.guidelines") {
        fun increment(market: Market, breachedGuidelineCode: String) {
            super.increment("market", market.name, "breachedGuideline", breachedGuidelineCode)
        }
    }
}
