package com.hedvig.underwriter.serviceIntegration.productPricing.dtos

import com.hedvig.underwriter.model.ExtraBuildingType
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.ExtraBuildingDto
import java.util.UUID

data class ExtraBuildingRequestDto(
    val id: UUID?,
    val type: ExtraBuildingType,
    val area: Int,
    val hasWaterConnected: Boolean
) {
    companion object {
        fun from(extraBuildingDto: ExtraBuildingDto) = ExtraBuildingRequestDto(
            id = null,
            type = extraBuildingDto.type,
            area = extraBuildingDto.area,
            hasWaterConnected = extraBuildingDto.hasWaterConnected
        )
    }
}
