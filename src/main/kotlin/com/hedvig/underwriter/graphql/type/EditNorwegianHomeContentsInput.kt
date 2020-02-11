package com.hedvig.underwriter.graphql.type

import com.hedvig.underwriter.service.model.QuoteRequestData

data class EditNorwegianHomeContentsInput(
    val street: String?,
    val zipCode: String?,
    val coInsured: Int?,
    val livingSpace: Int?,
    val isStudent: Boolean?,
    val type: NorwegianHomeContentsType?
) {
    fun toQuoteRequestDataDto() =
        QuoteRequestData.NorwegianHomeContents(
            street = this.street,
            zipCode = this.zipCode,
            livingSpace = this.livingSpace,
            coInsured = this.coInsured,
            type = this.type?.let { com.hedvig.underwriter.model.NorwegianHomeContentsType.valueOf(it.name) },
            isStudent = this.isStudent,
            city = null
        )
}
