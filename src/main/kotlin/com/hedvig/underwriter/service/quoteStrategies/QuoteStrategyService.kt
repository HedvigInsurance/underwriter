package com.hedvig.underwriter.service.quoteStrategies

import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.DanishAccidentData
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.DanishTravelData
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteData
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.service.DebtChecker
import com.hedvig.underwriter.service.guidelines.BaseGuideline
import com.hedvig.underwriter.serviceIntegration.notificationService.dtos.QuoteCreatedEvent
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import org.springframework.stereotype.Service

@Service
class QuoteStrategyService(
    private val debtChecker: DebtChecker,
    private val productPricingService: ProductPricingService
) {
    fun getStrategy(quote: Quote): QuoteStrategy = getStrategy(quote.data)

    private fun getStrategy(quoteData: QuoteData): QuoteStrategy = when (quoteData) {
        is SwedishHouseData -> SwedishHouseDataStrategy(debtChecker, productPricingService)
        is SwedishApartmentData -> SwedishApartmentDataStrategy(debtChecker, productPricingService)
        is NorwegianHomeContentsData -> NorwegianHomeContentsDataStrategy(productPricingService)
        is NorwegianTravelData -> NorwegianTravelDataStrategy(productPricingService)
        is DanishHomeContentsData -> DanishHomeContentsDataStrategy(productPricingService)
        is DanishAccidentData -> DanishAccidentDataStrategy(productPricingService)
        is DanishTravelData -> DanishTravelDataStrategy(productPricingService)
    }

    fun getInsuranceCost(quote: Quote): InsuranceCost = getStrategy(quote.data).getInsuranceCost(quote)
    fun createQuoteCreatedEvent(quote: Quote): QuoteCreatedEvent =
        this.getStrategy(quote).createNotificationEvent(quote)

    fun getAllGuidelines(quote: Quote): Set<BaseGuideline<QuoteData>> {
        val strategy = this
            .getStrategy(quote)
        val personalGuidelines = strategy.getPersonalGuidelines(quote.data)
        val productRules = strategy.getProductRules(quote.data)

        return personalGuidelines.plus(productRules)
    }
}
