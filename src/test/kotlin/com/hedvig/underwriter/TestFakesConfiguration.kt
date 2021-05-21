package com.hedvig.underwriter

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
import com.hedvig.underwriter.web.dtos.UnderwriterQuoteSignRequest
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
}

class FakePriceEngineClient: PriceEngineClient {

    var response: PriceQueryResponse? = null

    override fun queryPrice(request: PriceQueryRequest): PriceQueryResponse {
        return response!!
    }
}

class FakeMemberServiceClient: MemberServiceClient {

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
