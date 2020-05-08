package com.hedvig.underwriter.serviceIntegration.productPricing.dtos

import com.neovisionaries.i18n.CountryCode

data class Address(
    val street: String,
    val coLine: String? = null,
    val postalCode: String,
    val city: String? = null,
    val country: CountryCode
)
