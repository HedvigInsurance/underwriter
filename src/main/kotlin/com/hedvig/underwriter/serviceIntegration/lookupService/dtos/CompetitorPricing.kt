package com.hedvig.underwriter.serviceIntegration.lookupService.dtos

import javax.money.MonetaryAmount

data class CompetitorPricing(
    val monthlyNetPremium: MonetaryAmount,
    val monthlyGrossPremium: MonetaryAmount,
    val monthlyDiscount: MonetaryAmount,
    val insuranceObjectAddress: String?,
    val postalCode: String?,
    val livingArea: Int?,
    val numberInsured: Int?
)
