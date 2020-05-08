package com.hedvig.underwriter.serviceIntegration.memberService

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.FinalizeOnBoardingRequest
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.InternalMember
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsMemberAlreadySignedResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsSsnAlreadySignedMemberResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.PersonStatusDto
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.StartNorwegianBankIdSignResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.StartSwedishBankIdSignResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterStartNorwegianBankIdSignSessionRequest
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterStartSwedishBankIdSignSessionRequest
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UpdateSsnRequest
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import com.hedvig.underwriter.web.dtos.UnderwriterQuoteSignRequest
import feign.FeignException
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException

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
            logger.error("Cannot check debt for the following personnummer {}", ssn, e)
        } catch (e: FeignException) {
            logger.error("Cannot check debt for the following personnummer {}", ssn, e)
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

    override fun finalizeOnboarding(quote: Quote, email: String, phoneNumber: String?) {
        logger.debug("Finalizing web on boarding by populating member-service")
        client.finalizeOnBoarding(quote.memberId!!, FinalizeOnBoardingRequest.fromQuote(quote, email, phoneNumber))
    }

    override fun startSwedishBankIdSignQuotes(
        memberId: Long,
        underwriterSessionReference: UUID,
        ssn: String,
        ipAddress: String,
        isSwitching: Boolean
    ): StartSwedishBankIdSignResponse {
        return client.startSwedishBankIdSign(
            memberId,
            UnderwriterStartSwedishBankIdSignSessionRequest(underwriterSessionReference, ssn, ipAddress, isSwitching)
        ).body!!
    }

    override fun startNorwegianBankIdSignQuotes(
        memberId: Long,
        underwriterSessionReference: UUID,
        ssn: String,
        successUrl: String,
        failUrl: String
    ): StartNorwegianBankIdSignResponse {
        return client.startNorwegianSing(
            memberId,
            UnderwriterStartNorwegianBankIdSignSessionRequest(
                underwriterSessionReference,
                ssn,
                successUrl,
                failUrl
            )
        ).body!!
    }

    override fun getMember(memberId: Long): InternalMember {
        return client.getMember(memberId).body!!
    }
}
