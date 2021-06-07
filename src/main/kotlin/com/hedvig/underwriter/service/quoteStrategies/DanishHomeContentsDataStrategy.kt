package com.hedvig.underwriter.service.quoteStrategies

import com.hedvig.graphql.commons.type.MonetaryAmountV2
import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteData
import com.hedvig.underwriter.service.guidelines.BaseGuideline
import com.hedvig.underwriter.service.guidelines.DanishHomeContentsGuidelines
import com.hedvig.underwriter.service.guidelines.DanishPersonGuidelines
import com.hedvig.underwriter.serviceIntegration.notificationService.dtos.QuoteCreatedEvent
import com.hedvig.underwriter.serviceIntegration.notificationService.quoteCreatedEvent
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService

class DanishHomeContentsDataStrategy(productPricingService: ProductPricingService) : QuoteStrategy(productPricingService) {
    override fun createNotificationEvent(quote: Quote): QuoteCreatedEvent {
        require(quote.data is DanishHomeContentsData)

        return quoteCreatedEvent(
            quote,
            quote.data.street,
            quote.data.zipCode,
            if (quote.data.isStudent) {
                "STUDENT"
            } else {
                "REGULAR"
            }
        )
    }

    override fun getPersonalGuidelines(data: QuoteData): Set<BaseGuideline<QuoteData>> {
        return DanishPersonGuidelines.setOfRules
    }

    override fun getProductRules(data: QuoteData): Set<BaseGuideline<QuoteData>> {
        return DanishHomeContentsGuidelines.setOfRules.map { toTypedGuideline(it) }.toSet()
    }
}
