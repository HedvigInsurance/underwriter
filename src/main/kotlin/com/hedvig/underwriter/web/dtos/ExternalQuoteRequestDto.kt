package com.hedvig.underwriter.web.dtos

import com.hedvig.underwriter.service.model.QuoteRequestData
import com.hedvig.libs.logging.masking.Masked
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import java.time.LocalDate

data class ExternalQuoteRequestDto(
    val memberId: String,
    @Masked val firstName: String,
    @Masked val lastName: String,
    val birthDate: LocalDate,
    @Masked val ssn: String,
    val startDate: LocalDate,
    val initiatedFrom: QuoteInitiatedFrom,
    val swedishHouseData: QuoteRequestData.SwedishHouse?,
    val swedishApartmentData: QuoteRequestData.SwedishApartment?,
    val norwegianHomeContentsData: QuoteRequestData.NorwegianHomeContents?,
    val norwegianTravelData: QuoteRequestData.NorwegianTravel?,
    val danishHomeContentsData: QuoteRequestData.DanishHomeContents?,
    val danishAccidentData: QuoteRequestData.DanishAccident?,
    val danishTravelData: QuoteRequestData.DanishTravel?
)
