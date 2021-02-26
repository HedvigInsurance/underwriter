package com.hedvig.underwriter.web.dtos

import com.hedvig.underwriter.service.model.QuoteRequestData
import java.time.LocalDate

data class ExternalQuoteRequestDto(
    val memberId: String,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val ssn: String,
    val startDate: LocalDate,
    val swedishHouseData: QuoteRequestData.SwedishHouse?,
    val swedishApartmentData: QuoteRequestData.SwedishApartment?,
    val norwegianHomeContentsData: QuoteRequestData.NorwegianHomeContents?,
    val norwegianTravelData: QuoteRequestData.NorwegianTravel?,
    val danishHomeContentsData: QuoteRequestData.DanishHomeContents?,
    val danishAccidentData: QuoteRequestData.DanishAccident?,
    val danishTravelData: QuoteRequestData.DanishTravel?
)
