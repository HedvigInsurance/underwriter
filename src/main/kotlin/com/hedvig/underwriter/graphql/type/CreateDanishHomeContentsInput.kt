package com.hedvig.underwriter.graphql.type

import com.hedvig.underwriter.service.model.QuoteRequestData
import com.hedvig.underwriter.util.logging.Masked
import com.hedvig.underwriter.model.DanishHomeContentsType as InternalDanishHomeContentsType

data class CreateDanishHomeContentsInput(
    @Masked val street: String,
    val zipCode: String,
    val coInsured: Int,
    val livingSpace: Int,
    @get:JvmName("getIsStudent")
    val isStudent: Boolean,
    val type: DanishHomeContentsType
) {
    fun toQuoteRequestData() =
        QuoteRequestData.DanishHomeContents(
            street = this.street,
            zipCode = this.zipCode,
            livingSpace = this.livingSpace,
            coInsured = this.coInsured,
            isStudent = this.isStudent,
            subType = when (this.type) {
                DanishHomeContentsType.OWN -> InternalDanishHomeContentsType.OWN
                DanishHomeContentsType.RENT -> InternalDanishHomeContentsType.RENT
            }
        )
}
