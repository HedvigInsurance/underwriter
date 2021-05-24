package com.hedvig.underwriter.service

import com.hedvig.productPricingObjects.dtos.SelfChangeResult
import com.hedvig.underwriter.model.QuoteRepository
import com.hedvig.underwriter.model.QuoteState
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class SelfChangeService(
    private val quoteRepository: QuoteRepository,
    private val productPricingService: ProductPricingService
) {
    fun changeToQuotes(quoteIds: List<UUID>, contractIds: List<UUID>, memberId: String): SelfChangeResult {
        val quotes = quoteRepository.findQuotes(quoteIds)
        val result = productPricingService.selfChangeContracts(
            memberId = memberId,
            contractIds = contractIds,
            quotes = quotes
        )
        val changes = result.createdContracts + result.updatedContracts
        changes.forEach { change ->
            val quote = quotes.first { change.quoteId == it.id }
            quoteRepository.update(
                quote.copy(contractId = change.contractId, agreementId = change.agreementId, state = QuoteState.SIGNED)
            )
        }
        return result
    }
}
