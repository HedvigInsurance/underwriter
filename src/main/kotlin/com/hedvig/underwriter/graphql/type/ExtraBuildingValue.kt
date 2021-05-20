package com.hedvig.underwriter.graphql.type

interface ExtraBuildingCore {
    val type: ExtraBuildingType
    val area: Int
    val displayName: String
    val hasWaterConnected: Boolean
}

data class ExtraBuildingValue(
    override val type: ExtraBuildingType,
    override val area: Int,
    override val displayName: String,
    override val hasWaterConnected: Boolean
) : ExtraBuildingCore
