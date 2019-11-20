package com.hedvig.underwriter.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import javax.money.MonetaryAmount

data class Campaign(
    val campaignId: String,
    val Incentive: Incentive
)

@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME)
sealed class Incentive
data class MonthlyCostDeduction(val amount: MonetaryAmount) : Incentive()
data class FreeMonths(val quantity: Int) : Incentive()
object NoDiscount : Incentive() { const val `_` : Boolean = true}

