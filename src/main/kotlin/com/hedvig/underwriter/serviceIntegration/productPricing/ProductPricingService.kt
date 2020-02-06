package com.hedvig.underwriter.serviceIntegration.productPricing

import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ApartmentQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.HouseQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ModifiedProductCreatedDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ModifyProductRequestDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.NorwegianHomeContentsQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.NorwegianTravelQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuotePriceResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RedeemCampaignDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedProductResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedQuoteRequest
import org.javamoney.moneta.Money
import org.springframework.http.ResponseEntity

interface ProductPricingService {
    fun priceFromProductPricingForApartmentQuote(apartmentQuotePriceDto: ApartmentQuotePriceDto): QuotePriceResponseDto

    fun priceFromProductPricingForHouseQuote(houseQuotePriceDto: HouseQuotePriceDto): QuotePriceResponseDto

    fun priceFromProductPricingForNorwegianHomeContentsQuote(norwegianHomeContentsQuotePriceDto: NorwegianHomeContentsQuotePriceDto): QuotePriceResponseDto

    fun priceFromProductPricingForNorwegianQuote(norwegianTravelQuotePriceDto: NorwegianTravelQuotePriceDto): QuotePriceResponseDto

    fun signedQuote(signedQuoteRequest: SignedQuoteRequest, memberId: String): SignedProductResponseDto

    fun createModifiedProductFromQuote(quoteRequestDto: ModifyProductRequestDto): ModifiedProductCreatedDto

    fun redeemCampaign(redeemCampaignDto: RedeemCampaignDto): ResponseEntity<Void>

    fun calculateInsuranceCost(price: Money, memberId: String): InsuranceCost
}
