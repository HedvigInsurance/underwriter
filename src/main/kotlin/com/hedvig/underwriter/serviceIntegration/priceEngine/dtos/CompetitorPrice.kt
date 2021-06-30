package com.hedvig.underwriter.serviceIntegration.priceEngine.dtos

import com.hedvig.underwriter.serviceIntegration.lookupService.dtos.CompetitorPricing
import java.math.BigDecimal

data class CompetitorPrice(val price: BigDecimal, val numberInsured: Int?) {

    companion object {

        fun from(competitorPricing: CompetitorPricing?): CompetitorPrice? {
            return if (competitorPricing != null)
                CompetitorPrice(
                    competitorPricing.monthlyNetPremium.number.numberValueExact(BigDecimal::class.java),
                    competitorPricing.numberInsured)
            else null
        }
    }
}
