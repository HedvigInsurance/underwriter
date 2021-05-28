package com.hedvig.underwriter.service.quotesSignDataStrategies

import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteRepository
import com.hedvig.underwriter.model.QuoteState
import com.hedvig.underwriter.service.model.SignMethod
import com.hedvig.underwriter.service.model.StartSignResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import org.springframework.stereotype.Service

@Service
class SelfChangeCommittingStrategy(
    private val quoteRepository: QuoteRepository,
    private val productPricingService: ProductPricingService
) : SignStrategy {

    // This is a non-signing strategy shoehorned into the concept of signing, in order to avoid
    // a complete rewrite of how a quote becomes a contract.
    //
    // Instead, we finalise the strategy right away
    override fun startSign(quotes: List<Quote>, signData: SignData): StartSignResponse {
        val result = productPricingService.selfChangeContracts(
            memberId = signData.memberId,
            quotes = quotes
        )
        val changes = result.createdContracts + result.updatedContracts
        changes.forEach { change ->
            val quote = quotes.first { change.quoteId == it.id }
            quoteRepository.update(
                quote.copy(contractId = change.contractId, agreementId = change.agreementId, state = QuoteState.SIGNED)
            )
        }
        return StartSignResponse.AlreadyCompleted
    }

    override fun getSignMethod(quotes: List<Quote>) = SignMethod.APPROVE_ONLY
}
