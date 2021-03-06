package com.hedvig.underwriter.testhelp.databuilder

import com.hedvig.underwriter.service.model.QuoteRequestData

data class DanishAccidentQuoteRequestDataBuilder(
    val street: String = "",
    val zipCode: String = "",
    val bbrId: String? = "1234",
    val city: String? = "city",
    val apartment: String? = "3",
    val floor: String? = "2",
    val coInsured: Int = 1,
    val isStudent: Boolean = false
) : DataBuilder<QuoteRequestData.DanishAccident> {
    override fun build() = QuoteRequestData.DanishAccident(
        street = street,
        zipCode = zipCode,
        apartment = apartment,
        floor = floor,
        city = city,
        bbrId = bbrId,
        coInsured = coInsured,
        isStudent = isStudent
    )
}
