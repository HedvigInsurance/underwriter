package com.hedvig.underwriter.web.dtos

import com.hedvig.underwriter.model.Name
import com.hedvig.libs.logging.masking.Masked
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class SignQuotesRequestDto(
    val quoteIds: List<UUID>,
    val name: Name?,
    @Masked val ssn: String?,
    val startDate: LocalDate?,
    val insuranceCompany: String?,
    @Masked val email: String,
    val price: BigDecimal?, // Used for bundle verification
    val currency: String?,
    /**
     * If set, Underwriter will use this member when signing a quote rather than creating a new one.
     */
    val memberId: String?
)
