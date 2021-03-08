package com.hedvig.underwriter.graphql.type.depricated

import com.hedvig.underwriter.graphql.type.ApartmentType
import com.hedvig.underwriter.graphql.type.ExtraBuilding
import com.hedvig.underwriter.util.Pii

@Deprecated("Use QuoteDetails")
sealed class CompleteQuoteDetails {
    data class CompleteApartmentQuoteDetails(
        @Pii val street: String,
        val zipCode: String,
        val householdSize: Int,
        val livingSpace: Int,
        val type: ApartmentType
    ) : CompleteQuoteDetails()

    data class CompleteHouseQuoteDetails(
        @Pii val street: String,
        val zipCode: String,
        val householdSize: Int,
        val livingSpace: Int,
        val ancillarySpace: Int,
        val extraBuildings: List<ExtraBuilding.ExtraBuildingCore>,
        val numberOfBathrooms: Int,
        val yearOfConstruction: Int,
        val isSubleted: Boolean
    ) : CompleteQuoteDetails()

    data class UnknownQuoteDetails(
        val unknown: String = "This is deprecated use quoteDetails instead!"
    ) : CompleteQuoteDetails()
}
