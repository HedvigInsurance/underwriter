package com.hedvig.underwriter.serviceIntegration.productPricing

import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.Agreement
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ApartmentQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateBundleInsuranceCostRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.HouseQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ModifiedProductCreatedDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ModifyProductRequestDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuotePriceResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RedeemCampaignDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedProductResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedQuoteRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.AddAgreementResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.web.dtos.AddAgreementFromQuoteRequest
import com.hedvig.underwriter.web.dtos.SignRequest
import java.util.UUID
import org.javamoney.moneta.Money
import org.springframework.http.ResponseEntity

interface ProductPricingService {
    fun priceFromProductPricingForApartmentQuote(apartmentQuotePriceDto: ApartmentQuotePriceDto): QuotePriceResponseDto

    fun priceFromProductPricingForHouseQuote(houseQuotePriceDto: HouseQuotePriceDto): QuotePriceResponseDto

    fun signedQuote(signedQuoteRequest: SignedQuoteRequest, memberId: String): SignedProductResponseDto

    fun createModifiedProductFromQuote(quoteRequestDto: ModifyProductRequestDto): ModifiedProductCreatedDto

    fun addAgreementFromQuote(quote: Quote, request: AddAgreementFromQuoteRequest): AddAgreementResponse

    fun redeemCampaign(redeemCampaignDto: RedeemCampaignDto): ResponseEntity<Void>

    fun calculateInsuranceCost(price: Money, memberId: String): InsuranceCost

    fun calculateBundleInsuranceCost(request: CalculateBundleInsuranceCostRequest, memberId: String): InsuranceCost

    fun createContractsFromQuotes(quotes: List<Quote>, signedRequest: SignRequest): List<CreateContractResponse>

    fun createContractsFromQuotesNoMandate(quotes: List<Quote>): List<CreateContractResponse>

    fun getAgreement(agreementId: UUID): Agreement
}
