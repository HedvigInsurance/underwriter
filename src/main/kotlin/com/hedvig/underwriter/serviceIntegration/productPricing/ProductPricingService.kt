package com.hedvig.underwriter.serviceIntegration.productPricing

import com.hedvig.productPricingObjects.dtos.Agreement
import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateBundleInsuranceCostRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RedeemCampaignDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedProductResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedQuoteRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.AddAgreementResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.web.dtos.AddAgreementFromQuoteRequest
import com.hedvig.underwriter.web.dtos.SignRequest
import org.javamoney.moneta.Money
import org.springframework.http.ResponseEntity
import java.util.UUID

interface ProductPricingService {

    fun signedQuote(signedQuoteRequest: SignedQuoteRequest, memberId: String): SignedProductResponseDto

    fun addAgreementFromQuote(quote: Quote, request: AddAgreementFromQuoteRequest, token: String?): AddAgreementResponse

    fun redeemCampaign(redeemCampaignDto: RedeemCampaignDto): ResponseEntity<Void>

    fun calculateInsuranceCost(price: Money, memberId: String): InsuranceCost

    fun calculateBundleInsuranceCost(request: CalculateBundleInsuranceCostRequest): InsuranceCost

    fun calculateBundleInsuranceCostForMember(request: CalculateBundleInsuranceCostRequest, memberId: String): InsuranceCost

    fun createContractsFromQuotes(
        quotes: List<Quote>,
        signedRequest: SignRequest,
        token: String?
    ): List<CreateContractResponse>

    fun createContractsFromQuotesNoMandate(quotes: List<Quote>): List<CreateContractResponse>

    fun getAgreement(agreementId: UUID): Agreement
}
