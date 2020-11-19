package com.hedvig.underwriter.serviceIntegration.priceEngine.dtos

import com.hedvig.underwriter.model.ExtraBuildingType
import java.util.UUID

data class ExtraBuildingRequestDto(
    val id: UUID?,
    val type: ExtraBuildingType,
    val area: Int,
    val hasWaterConnected: Boolean
)
