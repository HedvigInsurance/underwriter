package com.hedvig.underwriter.service

import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse

interface RequotingService {

    fun blockDueToExistingAgreement(quote: Quote): Boolean
    fun useOldOrNewPrice(quote: Quote, newPrice: PriceQueryResponse): PriceQueryResponse
}
