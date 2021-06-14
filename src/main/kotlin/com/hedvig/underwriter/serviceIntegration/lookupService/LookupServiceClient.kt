package com.hedvig.underwriter.serviceIntegration.lookupService

import com.hedvig.underwriter.serviceIntegration.lookupService.dtos.CompetitorPricing
import feign.Headers
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID
import javax.validation.Valid

@Headers("Accept: application/json;charset=utf-8")
@FeignClient(
    name = "lookupServiceClient",
    url = "\${hedvig.lookup-service.url:lookup-service}"
)
@Component
interface LookupServiceClient {
    @GetMapping("/_/insurely/{clientReferenceId}/monthlyPremiumData")
    fun getMatchingCompetitorPrice(
        @PathVariable @Valid clientReferenceId: UUID,
        @RequestParam @Valid marketCurrency: String?,
        @RequestParam @Valid insuranceType: String?

    ): ResponseEntity<CompetitorPricing>
}
