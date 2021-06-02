package com.hedvig.underwriter.serviceIntegration.productPricing.dtos

import com.hedvig.productPricingObjects.dtos.AgreementQuote

class SelfChangeRequest(
    val memberId: String,
    val quotes: List<AgreementQuote>
)
