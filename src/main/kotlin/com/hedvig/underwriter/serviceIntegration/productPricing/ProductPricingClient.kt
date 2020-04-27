package com.hedvig.underwriter.serviceIntegration.productPricing

import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.AddAgreementRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.Agreement
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ApartmentQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateBundleInsuranceCostRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateInsuranceCostRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.HouseQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ModifiedProductCreatedDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ModifyProductRequestDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuotePriceResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RedeemCampaignDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedProductResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedQuoteRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.AddAgreementResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractsRequest
import feign.Headers
import java.util.UUID
import javax.validation.Valid
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

@Headers("Accept: application/json;charset=utf-8")
@FeignClient(
    name = "productPricingClient",
    url = "\${hedvig.product-pricing.url:product-pricing}"
)
interface ProductPricingClient {

    @PostMapping("/_/insurance/getHomeQuotePrice")
    fun priceFromProductPricingForHomeQuote(
        @Valid @RequestBody req: ApartmentQuotePriceDto
    ): ResponseEntity<QuotePriceResponseDto>

    @PostMapping("/_/insurance/getHouseQuotePrice")
    fun priceFromProductPricingForHouseQuote(
        @Valid @RequestBody req: HouseQuotePriceDto
    ): ResponseEntity<QuotePriceResponseDto>

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

    @PostMapping("/_/insurance/quotes/createModifiedProduct")
    fun createModifiedProductFromQuote(
        @Valid @RequestBody quoteRequestDto: ModifyProductRequestDto,
        @RequestHeader("hedvig.token") memberId: String
    ): ModifiedProductCreatedDto

    @PostMapping("/i/campaign/member/redeemCampaign")
    fun redeemCampaign(
        @Valid @RequestBody req: RedeemCampaignDto
    ): ResponseEntity<Void>

    @PostMapping("/_/contracts/create")
    fun createContract(
        @Valid @RequestBody request: CreateContractsRequest
    ): List<CreateContractResponse>

    @PostMapping("/_/agreements/add")
    fun addAgreement(
        @Valid @RequestBody request: AddAgreementRequest
    ): AddAgreementResponse

    @PostMapping("/_/underwriter/{memberId}/calculate/bundleInsuranceCost")
    fun calculateBundleInsuranceCost(
        @Valid @RequestBody request: CalculateBundleInsuranceCostRequest,
        @PathVariable memberId: String
    ): ResponseEntity<InsuranceCost>

    @GetMapping("/_/agreements/{agreementId}")
    fun getAgreementFromAgreementId(
        @PathVariable agreementId: UUID
    ): ResponseEntity<Agreement>
}
