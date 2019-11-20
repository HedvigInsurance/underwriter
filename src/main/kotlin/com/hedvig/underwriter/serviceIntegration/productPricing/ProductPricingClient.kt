package com.hedvig.underwriter.serviceIntegration.productPricing

import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.*
import feign.Headers
import javax.validation.Valid
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
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

    @PostMapping("/_/insurance/createRapioProduct")
    fun createProduct(
        @Valid @RequestBody req: RapioQuoteRequestDto,
        @RequestHeader(value = "hedvig.token") memberId: String

    ): ResponseEntity<RapioProductCreatedResponseDto>

    @PostMapping("/i/campaign/member/redeemCampaign")
    fun redeemCampaign(
        @Valid @RequestBody req: RedeemCampaignDto
    ): ResponseEntity<RedeemCampaignResponseDto>

    @PostMapping("/i/campaigns/validateRedeemCampaign")
    fun validateRedeemCampaign(
        @RequestBody req: ValidateCampaignDto
    ): ResponseEntity<ValidateCampaignResponseDto>
}
