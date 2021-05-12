package com.hedvig.underwriter.service

import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.LineItem
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import java.math.BigDecimal
import java.util.UUID
import javax.money.MonetaryAmount

interface RequotingService {

    fun blockDueToExistingAgreement(quote: Quote): Boolean
    fun useOldOrNewPrice(quote: Quote, newPrice: Price): Price
}

data class Price(
    val price: MonetaryAmount,
    val lineItems: List<LineItem>,
    val priceFromId: UUID? = null
) {
    val amount by lazy { price.number.numberValueExact(BigDecimal::class.java) }
    val currency by lazy { price.currency.currencyCode }

    companion object {
        fun from(response: PriceQueryResponse): Price {
            return Price(response.price, response.lineItems ?: emptyList())
        }
    }
}
