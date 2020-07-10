package com.hedvig.underwriter.serviceIntegration.productPricing

import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.AddAgreementRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.Agreement
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ApartmentQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateBundleInsuranceCostRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateInsuranceCostRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.HouseQuotePriceDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuotePriceResponseDto
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

    override fun priceFromProductPricingForHouseQuote(houseQuotePriceDto: HouseQuotePriceDto): QuotePriceResponseDto {
        val price = this.productPricingClient.priceFromProductPricingForHouseQuote(houseQuotePriceDto).body!!.price
        return QuotePriceResponseDto(price)
    }

    override fun priceFromProductPricingForApartmentQuote(apartmentQuotePriceDto: ApartmentQuotePriceDto): QuotePriceResponseDto {
        val price = this.productPricingClient.priceFromProductPricingForHomeQuote(apartmentQuotePriceDto).body!!.price
        return QuotePriceResponseDto(price)
    }

    override fun signedQuote(
        signedQuoteRequest: SignedQuoteRequest,
        memberId: String
    ) = productPricingClient.signedQuote(signedQuoteRequest, memberId).body
        ?: throw RuntimeException("Create product returned with empty body")

    override fun addAgreementFromQuote(quote: Quote, request: AddAgreementFromQuoteRequest) =
        productPricingClient.addAgreement(AddAgreementRequest.from(quote, request))

    override fun redeemCampaign(redeemCampaignDto: RedeemCampaignDto) =
        this.productPricingClient.redeemCampaign(redeemCampaignDto)

    override fun calculateInsuranceCost(price: Money, memberId: String): InsuranceCost =
        productPricingClient.calculateInsuranceCost(CalculateInsuranceCostRequest(price), memberId).body!!

    override fun calculateBundleInsuranceCost(
        request: CalculateBundleInsuranceCostRequest,
        memberId: String
    ): InsuranceCost =
        productPricingClient.calculateBundleInsuranceCost(request, memberId).body!!

    override fun createContractsFromQuotes(
        quotes: List<Quote>,
        signedRequest: SignRequest,
        token: String?
    ): List<CreateContractResponse> =
        productPricingClient.createContract(CreateContractsRequest.from(quotes, signedRequest), token)

    override fun createContractsFromQuotesNoMandate(quotes: List<Quote>): List<CreateContractResponse> =
        productPricingClient.createContract(request = CreateContractsRequest.fromQuotesNoMandate(quotes), token = null)

    override fun getAgreement(agreementId: UUID): Agreement =
        productPricingClient.getAgreement(agreementId).body!!

    override fun memberHasContract(memberId: String): Boolean = productPricingClient.memberHasContract(memberId)
}
