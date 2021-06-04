package com.hedvig.underwriter.web.dtos

import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import java.math.BigDecimal
import java.util.UUID

data class QuoteDto(
    val id: UUID,
    val price: BigDecimal?,
    val currency: String?,
    val initiatedFrom: QuoteInitiatedFrom,
    val contractId: UUID?,
    val agreementId: UUID?
) {
    companion object {
        fun from(quote: Quote) = QuoteDto(
            id = quote.id,
            price = quote.price,
            currency = quote.currency,
            initiatedFrom = quote.initiatedFrom,
            contractId = quote.contractId,
            agreementId = quote.agreementId
        )
    }
}
