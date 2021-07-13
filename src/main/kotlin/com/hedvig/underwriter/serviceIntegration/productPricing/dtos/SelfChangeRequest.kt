package com.hedvig.underwriter.serviceIntegration.productPricing.dtos

import com.hedvig.productPricingObjects.dtos.AgreementQuote
import com.hedvig.underwriter.model.QuoteInitiatedFrom

class SelfChangeRequest(
    val memberId: String,
    val signSource: QuoteInitiatedFrom,
    val quotes: List<AgreementQuote>
)
