package com.hedvig.underwriter.service.quoteStrategies

import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteData
import com.hedvig.underwriter.service.guidelines.BaseGuideline
import com.hedvig.underwriter.service.guidelines.NorwegianPersonGuidelines
import com.hedvig.underwriter.service.guidelines.NorwegianTravelGuidelines
import com.hedvig.underwriter.serviceIntegration.notificationService.dtos.QuoteCreatedEvent
import com.hedvig.underwriter.serviceIntegration.notificationService.quoteCreatedEvent
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService

class NorwegianTravelDataStrategy(productPricingService: ProductPricingService) : QuoteStrategy(productPricingService) {
    override fun createNotificationEvent(quote: Quote): QuoteCreatedEvent {
        require(quote.data is NorwegianTravelData)

        return quoteCreatedEvent(
            quote, null, null,
            if (quote.data.isYouth) {
                "YOUTH"
            } else {
                "REGULAR"
            }
        )
    }

    override fun getPersonalGuidelines(data: QuoteData): Set<BaseGuideline<QuoteData>> {
        return NorwegianPersonGuidelines.setOfRules
    }

    override fun getProductRules(data: QuoteData): Set<BaseGuideline<QuoteData>> {
        return NorwegianTravelGuidelines.setOfRules.map { toTypedGuideline(it) }.toSet()
    }
}
