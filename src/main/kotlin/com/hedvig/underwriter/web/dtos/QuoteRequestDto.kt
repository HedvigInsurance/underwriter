package com.hedvig.underwriter.web.dtos

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.hedvig.underwriter.model.Partner
import com.hedvig.underwriter.model.ProductType
import com.hedvig.underwriter.service.model.QuoteRequestData
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class QuoteRequestDto(
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val currentInsurer: String?,
    val birthDate: LocalDate?,
    val ssn: String?,
    val quotingPartner: Partner?,
    val productType: ProductType? = ProductType.UNKNOWN,
    @field:JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @field:JsonSubTypes(
        JsonSubTypes.Type(value = QuoteRequestData.SwedishApartment::class, name = "apartment"),
        JsonSubTypes.Type(value = QuoteRequestData.SwedishHouse::class, name = "house"),
        JsonSubTypes.Type(value = QuoteRequestData.NorwegianHomeContents::class, name = "norwegianHomeContents"),
        JsonSubTypes.Type(value = QuoteRequestData.NorwegianTravel::class, name = "norwegianTravel")
    )
    val incompleteQuoteData: QuoteRequestData?,
    val incompleteHouseQuoteData: QuoteRequestData.SwedishHouse?,
    val incompleteApartmentQuoteData: QuoteRequestData.SwedishApartment?,
    val norwegianHomeContentsData: QuoteRequestData.NorwegianHomeContents?,
    val norwegianTravelData: QuoteRequestData.NorwegianTravel?,
    val memberId: String? = null,
    val originatingProductId: UUID? = null,
    val startDate: Instant? = null,
    @Deprecated("use dataCollectionIds")
    val dataCollectionId: UUID? = null,
    val dataCollectionIds: List<UUID?> = emptyList(),
    val underwritingGuidelinesBypassedBy: String? = null
)
