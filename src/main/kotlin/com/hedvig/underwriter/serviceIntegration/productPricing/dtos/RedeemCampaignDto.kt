package com.hedvig.underwriter.serviceIntegration.productPricing.dtos

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import javax.money.MonetaryAmount

data class RedeemCampaignDto(
    val memberId: String,
    val code: String,
    val activationDate: LocalDate
)

data class ValidateCampaignDto(
    val code: String
)

data class RedeemCampaignResponseDto(
    val campaignId : String,
    val incentive: Incentive
)

data class ValidateCampaignResponseDto(
    val campaignId : String,
    val incentive: Incentive
)

@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME)
sealed class Incentive {
    data class MonthlyCostDeduction(val amount:MonetaryAmount) : Incentive()
    data class FreeMonths(val quantity: Int) : Incentive()
    object NoDiscount : Incentive() { const val `_` : Boolean = true}
}
