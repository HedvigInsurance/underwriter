package com.hedvig.underwriter.service.quoteStrategies

import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteData
import com.hedvig.underwriter.service.guidelines.BaseGuideline
import com.hedvig.underwriter.service.guidelines.TypedGuideline
import com.hedvig.underwriter.serviceIntegration.notificationService.dtos.QuoteCreatedEvent

abstract class QuoteStrategy {

    abstract fun createNotificationEvent(quote: Quote): QuoteCreatedEvent
    abstract fun getInsuranceCost(quote: Quote): InsuranceCost
    abstract fun getPersonalGuidelines(data: QuoteData): Set<BaseGuideline<QuoteData>>
    abstract fun getProductRules(data: QuoteData): Set<BaseGuideline<QuoteData>>

    inline fun <reified T : QuoteData> toTypedGuideline(it: BaseGuideline<T>) = TypedGuideline<QuoteData, T>(
        it,
        T::class
    )

    inline fun <reified T : QuoteData, reified Q : QuoteData> toTypedGuidelines(guidelines: Collection<BaseGuideline<Q>>) =
        guidelines.map { toTypedGuideline(it) }.toSet()
}
