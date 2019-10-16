package com.hedvig.underwriter.serviceIntegration.productPricing

import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.HomeQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.HouseQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuotePriceResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RapioProductCreatedResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RapioQuoteRequestDto
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
        url = "\${hedvig.product-pricing.url:product-pricing}")
interface ProductPricingClient {

    @PostMapping("/_/insurance/getHomeQuotePrice")
    fun priceFromProductPricingForHomeQuote(
        @Valid @RequestBody req: HomeQuotePriceDto
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
}
