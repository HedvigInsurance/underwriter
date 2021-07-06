package com.hedvig.underwriter.web.dtos

import com.hedvig.libs.logging.masking.Masked
import java.math.BigDecimal

data class OverridePriceRequestDto(
    val price: BigDecimal,
    @Masked val overriddenBy: String
)
