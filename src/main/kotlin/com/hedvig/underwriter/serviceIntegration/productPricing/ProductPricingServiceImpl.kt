package com.hedvig.underwriter.serviceIntegration.productPricing

import com.hedvig.productPricingObjects.dtos.Agreement
import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.AddAgreementRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateBundleInsuranceCostRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateInsuranceCostRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RedeemCampaignDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedQuoteRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractsRequest
import com.hedvig.underwriter.web.dtos.AddAgreementFromQuoteRequest
import com.hedvig.underwriter.web.dtos.SignRequest
import org.javamoney.moneta.Money
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@EnableFeignClients
class ProductPricingServiceImpl @Autowired constructor(
    val productPricingClient: ProductPricingClient
) : ProductPricingService {

    override fun signedQuote(
        signedQuoteRequest: SignedQuoteRequest,
        memberId: String
    ) = productPricingClient.signedQuote(signedQuoteRequest, memberId).body
        ?: throw RuntimeException("Create product returned with empty body")

    override fun addAgreementFromQuote(quote: Quote, request: AddAgreementFromQuoteRequest, token: String?) =
        productPricingClient.addAgreement(
            request = AddAgreementRequest.from(quote, request),
            token = token
        )

    override fun redeemCampaign(redeemCampaignDto: RedeemCampaignDto) =
        this.productPricingClient.redeemCampaign(redeemCampaignDto)

    override fun calculateInsuranceCost(price: Money, memberId: String): InsuranceCost =
        productPricingClient.calculateInsuranceCost(CalculateInsuranceCostRequest(price), memberId).body!!

    override fun calculateBundleInsuranceCost(
        request: CalculateBundleInsuranceCostRequest
    ): InsuranceCost =
        productPricingClient.calculateBundleInsuranceCost(request).body!!

    override fun calculateBundleInsuranceCostForMember(
        request: CalculateBundleInsuranceCostRequest,
        memberId: String
    ): InsuranceCost =
        productPricingClient.calculateBundleInsuranceCostForMember(request, memberId).body!!

    override fun createContractsFromQuotes(
        quotes: List<Quote>,
        signedRequest: SignRequest,
        token: String?
    ): List<CreateContractResponse> =
        productPricingClient.createContract(CreateContractsRequest.from(quotes, signedRequest), token)

    override fun hasContract(memberId: String): Boolean {
        val response = productPricingClient.hasContract(memberId, null)

        if (response.statusCode.isError) {
            throw RuntimeException("Failed check contracts for member $memberId: $response")
        }

        return response.body!!
    }

    override fun createContractsFromQuotesNoMandate(quotes: List<Quote>): List<CreateContractResponse> =
        productPricingClient.createContract(request = CreateContractsRequest.fromQuotesNoMandate(quotes), token = null)

    override fun getAgreement(agreementId: UUID): Agreement =
        productPricingClient.getAgreement(agreementId).body!!
}
