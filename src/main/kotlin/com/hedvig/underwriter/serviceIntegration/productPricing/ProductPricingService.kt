package com.hedvig.underwriter.serviceIntegration.productPricing

import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.Quote
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
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.web.dtos.SignRequest
import org.javamoney.moneta.Money
import org.springframework.http.ResponseEntity

interface ProductPricingService {
    fun priceFromProductPricingForApartmentQuote(apartmentQuotePriceDto: ApartmentQuotePriceDto): QuotePriceResponseDto

    fun priceFromProductPricingForHouseQuote(houseQuotePriceDto: HouseQuotePriceDto): QuotePriceResponseDto

    fun priceFromProductPricingForNorwegianHomeContentsQuote(norwegianHomeContentsQuotePriceDto: NorwegianHomeContentsQuotePriceDto): QuotePriceResponseDto

    fun priceFromProductPricingForNorwegianTravelQuote(norwegianTravelQuotePriceDto: NorwegianTravelQuotePriceDto): QuotePriceResponseDto

    fun signedQuote(signedQuoteRequest: SignedQuoteRequest, memberId: String): SignedProductResponseDto

    fun createModifiedProductFromQuote(quoteRequestDto: ModifyProductRequestDto): ModifiedProductCreatedDto

    fun redeemCampaign(redeemCampaignDto: RedeemCampaignDto): ResponseEntity<Void>

    fun calculateInsuranceCost(price: Money, memberId: String): InsuranceCost

    fun createContract(quotes: List<Quote>, signedRequest: SignRequest): List<CreateContractResponse>
}
