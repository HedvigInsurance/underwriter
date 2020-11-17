package com.hedvig.underwriter.graphql.type

import com.hedvig.underwriter.service.model.QuoteRequestData

class CreateDanishTravelInput(
    val street: String,
    val zipCode: String,
    val coInsured: Int,
    @get:JvmName("getIsStudent")
    val isStudent: Boolean
) {
    fun toQuoteRequestData() =
        QuoteRequestData.DanishTravel(
            street = this.street,
            zipCode = this.zipCode,
            coInsured = this.coInsured,
            isStudent = this.isStudent
        )
}