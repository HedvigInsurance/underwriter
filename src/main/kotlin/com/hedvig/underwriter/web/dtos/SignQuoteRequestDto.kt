package com.hedvig.underwriter.web.dtos

import com.hedvig.underwriter.model.Name
import com.hedvig.libs.logging.masking.Masked
import java.time.LocalDate

data class SignQuoteRequestDto(
    @Masked val name: Name?,
    @Masked val ssn: String?,
    val startDate: LocalDate?,
    val insuranceCompany: String? = null,
    @Masked val email: String
)
