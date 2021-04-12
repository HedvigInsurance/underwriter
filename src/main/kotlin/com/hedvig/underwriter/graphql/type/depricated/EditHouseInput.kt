package com.hedvig.underwriter.graphql.type.depricated

import com.hedvig.underwriter.extensions.toExtraBuilding
import com.hedvig.underwriter.graphql.type.ExtraBuildingInput
import com.hedvig.underwriter.service.model.QuoteRequestData
import com.hedvig.libs.logging.masking.Masked

@Deprecated("Use EditSwedishHouseInput")
data class EditHouseInput(
    @Masked val street: String?,
    val zipCode: String?,
    val householdSize: Int?,
    val livingSpace: Int?,
    val ancillarySpace: Int?,
    val yearOfConstruction: Int?,
    val numberOfBathrooms: Int?,
    val isSubleted: Boolean?,
    val extraBuildings: List<ExtraBuildingInput>?
) {
    fun toQuoteRequestDataDto() =
        QuoteRequestData.SwedishHouse(
            street = this.street,
            zipCode = this.zipCode,
            livingSpace = this.livingSpace,
            householdSize = this.householdSize,
            ancillaryArea = this.ancillarySpace,
            yearOfConstruction = this.yearOfConstruction,
            isSubleted = this.isSubleted,
            extraBuildings = this.extraBuildings?.toExtraBuilding(),
            numberOfBathrooms = this.numberOfBathrooms,
            city = null
        )
}
