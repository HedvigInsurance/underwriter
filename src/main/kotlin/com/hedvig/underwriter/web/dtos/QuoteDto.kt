package com.hedvig.underwriter.web.dtos

import com.hedvig.underwriter.model.Quote
import java.math.BigDecimal
import java.util.UUID

data class QuoteDto(
    val id: UUID,
    val price: BigDecimal?,
    val currency: String?,
    val contractId: UUID?,
    val agreementId: UUID?
) {
    companion object {
        fun from(quote: Quote) = QuoteDto(
            id = quote.id,
            price = quote.price,
            currency = quote.currency,
            contractId = quote.contractId,
            agreementId = quote.agreementId
        )
    }
}
