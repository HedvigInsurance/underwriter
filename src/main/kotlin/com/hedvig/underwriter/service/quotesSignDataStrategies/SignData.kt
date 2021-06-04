package com.hedvig.underwriter.service.quotesSignDataStrategies

data class SignData(
    val memberId: String,
    val ipAddress: String?,
    val successUrl: String?,
    val failUrl: String?
)
