package com.hedvig.underwriter.serviceIntegration.priceEngine

import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryRequest
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import org.javamoney.moneta.Money
import java.util.*

class PriceEngineServiceStub: PriceEngineService {
    override fun queryNorwegianHomeContentPrice(query: PriceQueryRequest.NorwegianHomeContent): PriceQueryResponse {
        return PriceQueryResponse(queryId = UUID.randomUUID(), price = Money.of(109, "NOK"))
    }

    override fun queryNorwegianTravelPrice(query: PriceQueryRequest.NorwegianTravel): PriceQueryResponse {
        return PriceQueryResponse(queryId = UUID.randomUUID(), price = Money.of(109, "NOK"))
    }
}
