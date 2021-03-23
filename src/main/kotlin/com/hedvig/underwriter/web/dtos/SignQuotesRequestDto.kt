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
    @Masked val email: String,
    val price: BigDecimal? = null, // Used for bundle verification
    val currency: String? = null
) {
    companion object {
        fun from(quoteId: UUID, request: SignQuoteRequestDto): SignQuotesRequestDto = SignQuotesRequestDto(
            listOf(quoteId),
            request.name,
            request.ssn,
            request.startDate,
            request.email
        )
    }
}
