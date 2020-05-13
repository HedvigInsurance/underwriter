package com.hedvig.underwriter.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.hedvig.underwriter.model.ApartmentProductSubType
import com.hedvig.underwriter.model.NorwegianHomeContentsType
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ExtraBuildingRequestDto

sealed class QuoteRequestData {
    data class SwedishHouse(
        val street: String?,
        val zipCode: String?,
        val city: String?,
        val livingSpace: Int?,
        val householdSize: Int?,
        val ancillaryArea: Int?,
        val yearOfConstruction: Int?,
        val numberOfBathrooms: Int?,
        val extraBuildings: List<ExtraBuildingRequestDto>?,
        @field:JsonProperty("subleted")
        val isSubleted: Boolean?,
        val floor: Int? = 0
    ) : QuoteRequestData()

    data class SwedishApartment(
        val street: String?,
        val zipCode: String?,
        val city: String?,
        val livingSpace: Int?,
        val householdSize: Int?,
        val floor: Int?,
        val subType: ApartmentProductSubType?
    ) : QuoteRequestData()

    data class NorwegianHomeContents(
        val street: String?,
        val zipCode: String?,
        val city: String?,
        val coInsured: Int?,
        val livingSpace: Int?,
        @field:JsonProperty("youth")
        val isYouth: Boolean?,
        val subType: NorwegianHomeContentsType?
    ) : QuoteRequestData()

    data class NorwegianTravel(
        val coInsured: Int?,
        @field:JsonProperty("youth")
        val isYouth: Boolean?
    ) : QuoteRequestData()
}
