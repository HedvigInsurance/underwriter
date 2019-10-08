package com.hedvig.underwriter.web.Dtos

import com.hedvig.underwriter.model.DateWithZone
import com.hedvig.underwriter.model.Name

data class SignQuoteRequest (
        val name: Name?,
        val startDateWithZone: DateWithZone?,
        val email: String
)
