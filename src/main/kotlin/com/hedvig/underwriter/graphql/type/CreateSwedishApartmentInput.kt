package com.hedvig.underwriter.graphql.type

import com.hedvig.underwriter.service.model.QuoteRequestData
import com.hedvig.libs.logging.masking.Masked

data class CreateSwedishApartmentInput(
    @Masked val street: String,
    val zipCode: String,
    val householdSize: Int,
    val livingSpace: Int,
    val type: ApartmentType
) {
    fun toQuoteRequestData() =
        QuoteRequestData.SwedishApartment(
            street = this.street,
            zipCode = this.zipCode,
            livingSpace = this.livingSpace,
            householdSize = this.householdSize,
            subType = this.type.toSubType(),
            city = null,
            floor = null
        )
}
