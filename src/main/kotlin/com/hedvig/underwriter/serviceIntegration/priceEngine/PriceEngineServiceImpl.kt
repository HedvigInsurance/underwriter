package com.hedvig.underwriter.serviceIntegration.priceEngine

import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryRequest
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import org.springframework.stereotype.Service

@Service
class PriceEngineServiceImpl(
    private val priceEngineClient: PriceEngineClient
) : PriceEngineService {
    override fun queryNorwegianHomeContentPrice(query: PriceQueryRequest.NorwegianHomeContent): PriceQueryResponse {
        return priceEngineClient.queryPrice(query)
    }

    override fun queryNorwegianTravelPrice(query: PriceQueryRequest.NorwegianTravel): PriceQueryResponse {
        return priceEngineClient.queryPrice(query)
    }
}
