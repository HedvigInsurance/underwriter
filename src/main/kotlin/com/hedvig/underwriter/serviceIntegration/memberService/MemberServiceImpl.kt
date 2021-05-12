package com.hedvig.underwriter.serviceIntegration.memberService

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hedvig.underwriter.model.Market
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.service.exceptions.NotFoundException
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.FinalizeOnBoardingRequest
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.InternalMember
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsMemberAlreadySignedResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsSsnAlreadySignedMemberResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.NationalIdentification
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.PersonStatusDto
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterStartSignSessionRequest
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterStartSignSessionResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UpdateSsnRequest
import com.hedvig.underwriter.util.logger
import com.hedvig.underwriter.util.maskSsn
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import com.hedvig.underwriter.web.dtos.UnderwriterQuoteSignRequest
import feign.FeignException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import java.util.UUID

@Service
@EnableFeignClients
class MemberServiceImpl @Autowired constructor(
    val client: MemberServiceClient,
    val objectMapper: ObjectMapper
) : MemberService {

    override fun isSsnAlreadySignedMemberEntity(ssn: String): IsSsnAlreadySignedMemberResponse {
        return this.client.checkIsSsnAlreadySignedMemberEntity(ssn)
    }

    override fun isMemberIdAlreadySignedMemberEntity(memberId: Long): IsMemberAlreadySignedResponse =
        client.checkIsMemberAlreadySignedMemberEntity(memberId)

    override fun updateMemberSsn(memberId: Long, request: UpdateSsnRequest) {
        this.client.updateMemberSsn(memberId, request)
    }

    override fun signQuote(
        memberId: Long,
        request: UnderwriterQuoteSignRequest
    ): Either<ErrorResponseDto, UnderwriterQuoteSignResponse> {
        try {
            val response = this.client.signQuote(memberId, request)
            return Either.right(response.body!!)
        } catch (ex: FeignException) {
            if (ex.status() == 422) {
                val error = objectMapper.readValue<ErrorResponseDto>(ex.contentUTF8())
                return Either.left(error)
            }
            throw RuntimeException("Un handled FeignException", ex)
        }
    }

    override fun checkPersonDebt(ssn: String) {
        try {
            this.client.checkPersonDebt(ssn)
        } catch (e: RestClientResponseException) {
            logger.error("Cannot check debt for the following personnummer {}", ssn.maskSsn(Market.SWEDEN), e)
        } catch (e: FeignException) {
            logger.error("Cannot check debt for the following personnummer {}", ssn.maskSsn(Market.SWEDEN), e)
        }
    }

    override fun getPersonStatus(ssn: String): PersonStatusDto {
        val response = this.client.personStatus(ssn)
        return if (response.body != null) response.body!! else throw NullPointerException("person status should not be null")
    }

    override fun createMember(): String {
        val memberId = this.client.createMember().body
        return memberId!!.memberId
    }

    override fun finalizeOnboarding(quote: Quote, email: String) {
        logger.debug("Finalizing web on boarding by populating member-service")
        client.finalizeOnBoarding(quote.memberId!!, FinalizeOnBoardingRequest.fromQuote(quote, email))
    }

    override fun startSwedishBankIdSign(
        memberId: Long,
        underwriterSessionReference: UUID,
        nationalIdentification: NationalIdentification,
        ipAddress: String,
        isSwitching: Boolean
    ): UnderwriterStartSignSessionResponse.SwedishBankId {
        val request = UnderwriterStartSignSessionRequest.SwedishBankId(
            underwriterSessionReference,
            nationalIdentification,
            ipAddress,
            isSwitching
        )
        val response = client.startSign(memberId, request).body
        require(response is UnderwriterStartSignSessionResponse.SwedishBankId)
        return response
    }

    override fun startSimpleSign(
        memberId: Long,
        underwriterSessionReference: UUID,
        nationalIdentification: NationalIdentification
    ): UnderwriterStartSignSessionResponse.SimpleSign {

        val request = UnderwriterStartSignSessionRequest.SimpleSign(
            underwriterSessionReference,
            nationalIdentification
        )
        val response = client.startSign(memberId, request).body
        require(response is UnderwriterStartSignSessionResponse.SimpleSign)
        return response
    }

    override fun getMember(memberId: Long): InternalMember {
        return client.getMember(memberId).body!!
    }

    override fun deleteMember(memberId: String) {

        val response = client.deleteMember(memberId)

        if (response.statusCodeValue == 404) {
            throw NotFoundException("Failed to delete member $memberId in Member Service, member not found")
        }

        if (response.statusCode.isError) {
            throw java.lang.RuntimeException("Failed to delete member $memberId in Member Service: $response")
        }
    }
}
