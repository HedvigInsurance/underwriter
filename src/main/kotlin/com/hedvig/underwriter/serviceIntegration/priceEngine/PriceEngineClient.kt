package com.hedvig.underwriter.serviceIntegration.priceEngine

import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryRequest
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import feign.Headers
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Headers("Accept: application/json;charset=utf-8")
@FeignClient(
    name = "priceEngineClient",
    url = "\${hedvig.price-engine.url:price-engine}"
)
@ConditionalOnProperty("hedvig.integration.fakes", havingValue = "false", matchIfMissing = true)
interface PriceEngineClient {
    @PostMapping("/_/price/engine/query/price")
    fun queryPrice(
        @RequestBody request: PriceQueryRequest
    ): PriceQueryResponse
}
