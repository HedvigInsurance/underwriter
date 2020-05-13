package com.hedvig.underwriter.graphql.type

import com.hedvig.underwriter.service.model.QuoteRequestData

data class CreateNorwegianHomeContentsInput(
    val street: String,
    val zipCode: String,
    val coInsured: Int,
    val livingSpace: Int,
    @get:JvmName("getIsYouth")
    val isYouth: Boolean,
    val type: NorwegianHomeContentsType
) {
    fun toQuoteRequestData() =
        QuoteRequestData.NorwegianHomeContents(
            street = this.street,
            zipCode = this.zipCode,
            livingSpace = this.livingSpace,
            coInsured = this.coInsured,
            subType = com.hedvig.underwriter.model.NorwegianHomeContentsType.valueOf(this.type.name),
            isYouth = this.isYouth,
            city = null
        )
}
