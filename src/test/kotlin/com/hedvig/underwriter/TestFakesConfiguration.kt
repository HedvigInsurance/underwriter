package com.hedvig.underwriter

import com.hedvig.productPricingObjects.dtos.Agreement
import com.hedvig.productPricingObjects.enums.ContractStateFilter
import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.serviceIntegration.memberService.MemberServiceClient
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.FinalizeOnBoardingRequest
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.HelloHedvigResponseDto
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.InternalMember
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsMemberAlreadySignedResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsSsnAlreadySignedMemberResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.PersonStatusDto
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterStartSignSessionRequest
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterStartSignSessionResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UpdateSsnRequest
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineClient
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryRequest
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingClient
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.AddAgreementRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateBundleInsuranceCostRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.CalculateInsuranceCostRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RedeemCampaignDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SelfChangeRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SelfChangeResult
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedProductResponseDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedQuoteRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.AddAgreementResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractsRequest
import com.hedvig.underwriter.web.dtos.UnderwriterQuoteSignRequest
import java.util.UUID
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@Configuration
class TestFakesConfiguration {
    @get:Bean
    val priceEngineClient: FakePriceEngineClient = FakePriceEngineClient()

    @get:Bean
    val memberServiceClient: FakeMemberServiceClient = FakeMemberServiceClient()

    @get:Bean
    val productPricingClient: FakeProductPricingClient = FakeProductPricingClient()
}

class FakePriceEngineClient : PriceEngineClient {

    var response: PriceQueryResponse? = null

    override fun queryPrice(request: PriceQueryRequest): PriceQueryResponse {
        return response!!
    }
}

class FakeMemberServiceClient : MemberServiceClient {

    var personStatus: PersonStatusDto? = null

    override fun createMember(): ResponseEntity<HelloHedvigResponseDto> {
        TODO("Not yet implemented")
    }

    override fun personStatus(ssn: String): ResponseEntity<PersonStatusDto> {
        return personStatus?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }

    override fun checkPersonDebt(ssn: String): ResponseEntity<Void> {
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    override fun signQuote(
        memberId: Long,
        underwriterQuoteSignRequest: UnderwriterQuoteSignRequest
    ): ResponseEntity<UnderwriterQuoteSignResponse> {
        TODO("Not yet implemented")
    }

    override fun updateMemberSsn(memberId: Long, request: UpdateSsnRequest) {
        TODO("Not yet implemented")
    }

    override fun checkIsSsnAlreadySignedMemberEntity(ssn: String): IsSsnAlreadySignedMemberResponse {
        TODO("Not yet implemented")
    }

    override fun checkIsMemberAlreadySignedMemberEntity(memberId: Long): IsMemberAlreadySignedResponse {
        TODO("Not yet implemented")
    }

    override fun finalizeOnBoarding(memberId: String, req: FinalizeOnBoardingRequest): ResponseEntity<*> {
        TODO("Not yet implemented")
    }

    override fun startSign(
        memberId: Long,
        request: UnderwriterStartSignSessionRequest
    ): ResponseEntity<UnderwriterStartSignSessionResponse> {
        TODO("Not yet implemented")
    }

    override fun getMember(memberId: Long): ResponseEntity<InternalMember> {
        TODO("Not yet implemented")
    }

    override fun deleteMember(memberId: String): ResponseEntity<*> {
        TODO("Not yet implemented")
    }
}

class FakeProductPricingClient : ProductPricingClient {

    var selfChangeResult: SelfChangeResult? = null

    override fun signedQuote(req: SignedQuoteRequest, memberId: String): ResponseEntity<SignedProductResponseDto> {
        TODO("Not yet implemented")
    }

    override fun calculateInsuranceCost(
        req: CalculateInsuranceCostRequest,
        memberId: String
    ): ResponseEntity<InsuranceCost> {
        TODO("Not yet implemented")
    }

    override fun redeemCampaign(req: RedeemCampaignDto): ResponseEntity<Void> {
        TODO("Not yet implemented")
    }

    override fun createContract(request: CreateContractsRequest, token: String?): List<CreateContractResponse> {
        TODO("Not yet implemented")
    }

    override fun hasContract(memberId: String, stateFilter: ContractStateFilter?): ResponseEntity<Boolean> {
        TODO("Not yet implemented")
    }

    override fun addAgreement(request: AddAgreementRequest, token: String?): AddAgreementResponse {
        TODO("Not yet implemented")
    }

    override fun calculateBundleInsuranceCostForMember(
        request: CalculateBundleInsuranceCostRequest,
        memberId: String
    ): ResponseEntity<InsuranceCost> {
        TODO("Not yet implemented")
    }

    override fun calculateBundleInsuranceCost(request: CalculateBundleInsuranceCostRequest): ResponseEntity<InsuranceCost> {
        TODO("Not yet implemented")
    }

    override fun getAgreement(agreementId: UUID): ResponseEntity<Agreement> {
        TODO("Not yet implemented")
    }

    override fun selfChangeContracts(request: SelfChangeRequest): SelfChangeResult {
        return selfChangeResult!!
    }
}
