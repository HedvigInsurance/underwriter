package com.hedvig.underwriter.serviceIntegration.productPricing

import arrow.core.Either
import com.hedvig.underwriter.model.Campaign
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.*
import org.springframework.http.ResponseEntity

interface ProductPricingService {
    fun priceFromProductPricingForApartmentQuote(apartmentQuotePriceDto: ApartmentQuotePriceDto): QuotePriceResponseDto

    fun priceFromProductPricingForHouseQuote(houseQuotePriceDto: HouseQuotePriceDto): QuotePriceResponseDto

    fun createProduct(rapioQuoteRequest: RapioQuoteRequestDto, memberId: String): RapioProductCreatedResponseDto

    fun redeemCampaign(redeemCampaignDto: RedeemCampaignDto): ResponseEntity<RedeemCampaignResponseDto>
    fun validateRedeemableCampaign(redeemCampaignDto: ValidateCampaignDto): Either<String, Campaign>
}
