package com.hedvig.underwriter.serviceIntegration.productPricing

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import com.fasterxml.jackson.databind.ObjectMapper
import com.hedvig.underwriter.model.*
import com.hedvig.underwriter.service.QuoteServiceImpl
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.*
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.Incentive as DtoIncentive
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.stereotype.Service

@Service
@EnableFeignClients
class ProductPricingServiceImpl @Autowired constructor(
    val productPricingClient: ProductPricingClient,
    val objectMapper : ObjectMapper
) : ProductPricingService {
    val logger = LoggerFactory.getLogger(QuoteServiceImpl::class.java)!!

    override fun priceFromProductPricingForHouseQuote(houseQuotePriceDto: HouseQuotePriceDto): QuotePriceResponseDto {
        val price = this.productPricingClient.priceFromProductPricingForHouseQuote(houseQuotePriceDto).body!!.price
        return QuotePriceResponseDto(price)
    }

    override fun priceFromProductPricingForApartmentQuote(apartmentQuotePriceDto: ApartmentQuotePriceDto): QuotePriceResponseDto {
        val price = this.productPricingClient.priceFromProductPricingForHomeQuote(apartmentQuotePriceDto).body!!.price
        return QuotePriceResponseDto(price)
    }

    override fun createProduct(
        rapioQuoteRequest: RapioQuoteRequestDto,
        memberId: String
    ): RapioProductCreatedResponseDto {
        val rapioProductCreatedResponseDto = this.productPricingClient.createProduct(rapioQuoteRequest, memberId)
        val signedQuote = rapioProductCreatedResponseDto.body
        return signedQuote!!
    }

    override fun redeemCampaign(redeemCampaignDto: RedeemCampaignDto) =
        this.productPricingClient.redeemCampaign(redeemCampaignDto)

    override fun validateRedeemableCampaign(redeemCampaignDto: ValidateCampaignDto): Either<String, Campaign> =
        try {
            val body = this.productPricingClient.validateRedeemCampaign(redeemCampaignDto)
            Right(Campaign( body.body!!.campaignId, antiCorruptIncentive(body.body!!.incentive)))
        } catch (e:FeignException) {
            if(e.status() == 400) {
                val errorResponse = objectMapper.readTree(e.content())
                Left(errorResponse["message"].asText())
            } else {
                logger.error("Failed to redeem ${redeemCampaignDto.code}  with response ${e.contentUTF8()}")
                throw e
            }
        }

    private fun antiCorruptIncentive(incentive: DtoIncentive): Incentive = when (incentive) {
        is DtoIncentive.MonthlyCostDeduction -> MonthlyCostDeduction(incentive.amount)
        is DtoIncentive.FreeMonths -> FreeMonths(incentive.quantity)
        is DtoIncentive.NoDiscount -> NoDiscount
    }
}
