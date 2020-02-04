package com.hedvig.underwriter.web.dtos

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.hedvig.underwriter.model.Partner
import com.hedvig.underwriter.model.ProductType
import com.hedvig.underwriter.service.model.QuoteRequestData
import com.hedvig.underwriter.service.model.QuoteRequestData.Apartment
import com.hedvig.underwriter.service.model.QuoteRequestData.House
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
    val productType: ProductType?,
    @field:JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @field:JsonSubTypes(
        JsonSubTypes.Type(value = Apartment::class, name = "apartment"),
        JsonSubTypes.Type(value = House::class, name = "house")
    ) val incompleteQuoteData: QuoteRequestData?,
    val incompleteHouseQuoteData: House?,
    val incompleteApartmentQuoteData: Apartment?,
    val memberId: String? = null,
    val originatingProductId: UUID? = null,
    val startDate: Instant? = null,
    val dataCollectionId: UUID? = null,
    val underwritingGuidelinesBypassedBy: String? = null
)
