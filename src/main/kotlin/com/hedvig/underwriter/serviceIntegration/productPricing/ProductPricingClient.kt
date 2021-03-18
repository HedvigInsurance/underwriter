package com.hedvig.underwriter.serviceIntegration.productPricing

import com.hedvig.productPricingObjects.dtos.Agreement
import com.hedvig.productPricingObjects.dtos.SelfChangeRequest
import com.hedvig.productPricingObjects.dtos.SelfChangeResult
import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.AddAgreementRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateBundleInsuranceCostRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateInsuranceCostRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RedeemCampaignDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedProductResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedQuoteRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.AddAgreementResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractsRequest
import feign.Headers
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import java.util.UUID
import javax.validation.Valid

@Headers("Accept: application/json;charset=utf-8")
@FeignClient(
    name = "productPricingClient",
    url = "\${hedvig.product-pricing.url:product-pricing}"
)
interface ProductPricingClient {

    @PostMapping("/_/underwriter/{memberId}/signed/quote")
    fun signedQuote(
        @Valid @RequestBody req: SignedQuoteRequest,
        @PathVariable memberId: String
    ): ResponseEntity<SignedProductResponseDto>

    @PostMapping("/_/underwriter/{memberId}/calculate/insurance/cost")
    fun calculateInsuranceCost(
        @Valid @RequestBody req: CalculateInsuranceCostRequest,
        @PathVariable memberId: String
    ): ResponseEntity<InsuranceCost>

    @PostMapping("/i/campaign/member/redeemCampaign")
    fun redeemCampaign(
        @Valid @RequestBody req: RedeemCampaignDto
    ): ResponseEntity<Void>

    @PostMapping("/_/contracts/create")
    fun createContract(
        @Valid @RequestBody request: CreateContractsRequest,
        @RequestHeader("Authorization") token: String?
    ): List<CreateContractResponse>

    @PostMapping("/_/agreements/add")
    fun addAgreement(
        @Valid @RequestBody request: AddAgreementRequest,
        @RequestHeader("Authorization") token: String?
    ): AddAgreementResponse

    @PostMapping("/_/underwriter/{memberId}/calculate/bundleInsuranceCost")
    fun calculateBundleInsuranceCostForMember(
        @Valid @RequestBody request: CalculateBundleInsuranceCostRequest,
        @PathVariable memberId: String
    ): ResponseEntity<InsuranceCost>

    @PostMapping("/_/underwriter/calculate/bundleInsuranceCost")
    fun calculateBundleInsuranceCost(
        @Valid @RequestBody request: CalculateBundleInsuranceCostRequest
    ): ResponseEntity<InsuranceCost>

    @GetMapping("/_/agreements/{agreementId}")
    fun getAgreement(
        @PathVariable agreementId: UUID
    ): ResponseEntity<Agreement>

    @PostMapping("/contracts/selfChange")
    fun selfChangeContracts(
        @RequestHeader("Hedvig.token") memberId: String,
        @RequestBody body: SelfChangeRequest
    ): SelfChangeResult
}
