package com.hedvig.underwriter.serviceIntegration.priceEngine.dtos

import java.math.BigDecimal

data class OverriddenPrice(val price: BigDecimal, val overriddenBy: String) {

    companion object {

        fun from(price: BigDecimal?, overriddenBy: String?): OverriddenPrice? {
            return if (price != null)
                OverriddenPrice(
                    price,
                    overriddenBy!!
                )
            else null
        }
    }
}
