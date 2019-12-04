package com.hedvig.underwriter.serviceIntegration.productPricing

import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ApartmentQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.HouseQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ModifiedProductCreatedDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ModifyProductRequestDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuotePriceResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RapioProductCreatedResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RapioQuoteRequestDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RedeemCampaignDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedProductResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedQuoteRequest
import feign.Headers
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import javax.validation.Valid

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

    @PostMapping("/_/underwriter/{memberId}/signed/quote")
    fun signedQuote(
        @Valid @RequestBody req: SignedQuoteRequest,
        @PathVariable memberId: String
    ): ResponseEntity<SignedProductResponseDto>

    @PostMapping("/_/insurance/quotes/createModifiedProduct")
    fun createModifiedProductFromQuote(
        @Valid @RequestBody quoteRequestDto: ModifyProductRequestDto,
        @RequestHeader("hedvig.token") memberId: String
    ): ModifiedProductCreatedDto

    @PostMapping("/i/campaign/member/redeemCampaign")
    fun redeemCampaign(
        @Valid @RequestBody req: RedeemCampaignDto
    ): ResponseEntity<Void>
}
