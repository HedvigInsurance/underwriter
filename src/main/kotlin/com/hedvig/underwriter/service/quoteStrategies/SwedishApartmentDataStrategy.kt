package com.hedvig.underwriter.service.quoteStrategies

import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteData
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.service.DebtChecker
import com.hedvig.underwriter.service.guidelines.BaseGuideline
import com.hedvig.underwriter.service.guidelines.SwedishApartmentGuidelines
import com.hedvig.underwriter.service.guidelines.SwedishPersonalGuidelines
import com.hedvig.underwriter.serviceIntegration.notificationService.dtos.QuoteCreatedEvent
import com.hedvig.underwriter.serviceIntegration.notificationService.quoteCreatedEvent
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService

class SwedishApartmentDataStrategy(
    private val debtChecker: DebtChecker,
    productPricingService: ProductPricingService
) : QuoteStrategy(productPricingService) {
    override fun createNotificationEvent(quote: Quote): QuoteCreatedEvent {
        require(quote.data is SwedishApartmentData)
        return quoteCreatedEvent(quote, quote.data.street, quote.data.zipCode, quote.data.subType!!.toString())
    }

    override fun getPersonalGuidelines(data: QuoteData): Set<BaseGuideline<QuoteData>> {
        return SwedishPersonalGuidelines(
            debtChecker
        ).setOfRules
    }

    override fun getProductRules(data: QuoteData): Set<BaseGuideline<QuoteData>> {
        return SwedishApartmentGuidelines.setOfRules.map { toTypedGuideline(it) }.toSet()
    }
}
