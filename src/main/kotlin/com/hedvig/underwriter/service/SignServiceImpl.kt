package com.hedvig.underwriter.service

import arrow.core.Either
import arrow.core.Right
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.QuoteRepository
import com.hedvig.underwriter.model.QuoteState
import com.hedvig.underwriter.model.SignSessionRepository
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.model.ssn
import com.hedvig.underwriter.service.exceptions.QuoteNotFoundException
import com.hedvig.underwriter.service.model.CompleteSignSessionData
import com.hedvig.underwriter.service.model.PersonPolicyHolder
import com.hedvig.underwriter.service.model.StartSignResponse
import com.hedvig.underwriter.serviceIntegration.customerio.CustomerIO
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UpdateSsnRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RedeemCampaignDto
import com.hedvig.underwriter.web.dtos.ErrorCodes
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import com.hedvig.underwriter.web.dtos.SignQuoteRequest
import com.hedvig.underwriter.web.dtos.SignRequest
import com.hedvig.underwriter.web.dtos.SignedQuoteResponseDto
import com.hedvig.underwriter.web.dtos.UnderwriterQuoteSignRequest
import feign.FeignException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class SignServiceImpl(
    val quoteService: QuoteService,
    // FIXME: SignService should only use quoteService in my opinion, but I'm not up for doing that refactor now.
    val quoteRepository: QuoteRepository,
    val memberService: MemberService,
    val productPricingService: ProductPricingService,
    val signSessionRepository: SignSessionRepository,
    val customerIO: CustomerIO,
    val env: Environment
) : SignService {

    override fun startSigningQuotes(
        quoteIds: List<UUID>,
        memberId: String,
        ipAddress: String?,
        successUrl: String?,
        failUrl: String?
    ): StartSignResponse {

        if (memberService.isMemberIdAlreadySignedMemberEntity(memberId.toLong()).memberAlreadySigned) {
            return StartSignResponse.FailedToStartSign(MEMBER_HAS_ALREADY_SIGNED_ERROR_MESSAGE)
        }

        val quotes = quoteService.getQuotes(quoteIds)
        quotes.forEach { quote ->
            quote.memberId?.let { quoteMemberId ->
                if (memberId != quoteMemberId) {
                    logger.info("Member [id: $memberId] tried to sign quote with member id: $quoteMemberId. [Quotes: $quotes]")
                    return StartSignResponse.FailedToStartSign(VARIOUS_MEMBER_ID_ERROR_MESSAGE)
                }
            } ?: run {
                logger.info("Member [id: $memberId] tried to sign quote without member id. [Quotes: $quotes]")
                return StartSignResponse.FailedToStartSign(SIGNING_QUOTE_WITH_OUT_MEMBER_ID_ERROR_MESSAGE)
            }

            val quoteNotSignableErrorDto = quoteService.getQuoteStateNotSignableErrorOrNull(quote)
            if (quoteNotSignableErrorDto != null) {
                return StartSignResponse.FailedToStartSign(quoteNotSignableErrorDto.errorMessage)
            }
        }

        when (val data = getSignDataFromQuotes(quotes)) {
            is QuotesSignData.SwedishBankId -> {
                val signSessionId = signSessionRepository.insert(quoteIds)

                val ip = ipAddress ?: run {
                    logger.error("Trying to sign swedish quotes without an ip address [Quotes: $quotes]")
                    "127.0.0.1"
                }

                val response = memberService.startSwedishBankIdSignQuotes(
                    data.memberId.toLong(),
                    signSessionId,
                    data.ssn,
                    ip,
                    data.isSwitching
                )

                return response.autoStartToken?.let { autoStartToken ->
                    StartSignResponse.SwedishBankIdSession(signSessionId, autoStartToken)
                } ?: StartSignResponse.FailedToStartSign(errorMessage = response.internalErrorMessage!!)
            }
            is QuotesSignData.NorwegianBankId -> {
                if (successUrl == null || failUrl == null) {
                    return StartSignResponse.FailedToStartSign(TARGET_URL_NOT_PROVIDED_ERROR_MESSAGE)
                }

                val signSessionId = signSessionRepository.insert(quoteIds)

                val response =
                    memberService.startNorwegianBankIdSignQuotes(
                        data.memberId.toLong(),
                        signSessionId,
                        data.ssn,
                        successUrl,
                        failUrl
                    )

                return response.redirectUrl?.let { redirectUrl ->
                    StartSignResponse.NorwegianBankIdSession(signSessionId, redirectUrl)
                } ?: response.internalErrorMessage?.let {
                    StartSignResponse.FailedToStartSign(it)
                } ?: StartSignResponse.FailedToStartSign(response.errorMessages!!.joinToString(", "))
            }
            is QuotesSignData.CanNotBeBundled ->
                return StartSignResponse.FailedToStartSign("Quotes can not be bundled")
        }
    }

    override fun completedSignSession(signSessionId: UUID, completeSignSessionData: CompleteSignSessionData) {
        val quotes = quoteService.getQuotes(signSessionRepository.find(signSessionId))

        val createContractResponse = when (completeSignSessionData) {
            is CompleteSignSessionData.SwedishBankIdDataComplete ->
                productPricingService.createContractsFromQuotes(
                    quotes, SignRequest(
                        referenceToken = completeSignSessionData.referenceToken,
                        signature = completeSignSessionData.signature,
                        oscpResponse = completeSignSessionData.oscpResponse
                    )
                )
            is CompleteSignSessionData.NoMandate ->
                productPricingService.createContractsFromQuotesNoMandate(quotes)
        }
        quotes.forEach { quote ->
            val signedContractId = createContractResponse.first { quote.id == it.quoteId }.contractId
            finishingUpSignedQuote(quote, signedContractId, false)
        }
    }

    private fun finishingUpSignedQuote(
        quote: Quote,
        signedContractId: UUID,
        signStartedInMemberService: Boolean
    ): SignedQuoteResponseDto {
        val quoteWithProductId = quoteRepository.update(
            quote.copy(signedProductId = signedContractId)
        )
        checkNotNull(quoteWithProductId.memberId) { "Quote must have a member id! Quote id: ${quote.id}" }

        quoteWithProductId.attributedTo.campaignCode?.let { campaignCode ->
            try {
                productPricingService.redeemCampaign(
                    RedeemCampaignDto(
                        quoteWithProductId.memberId,
                        campaignCode,
                        LocalDate.now(quote.getTimeZoneId())
                    )
                )
            } catch (e: FeignException) {
                logger.error("Failed to redeem $campaignCode for partner ${quoteWithProductId.attributedTo} with response ${e.message}")
            }
        }

        if (!signStartedInMemberService && quoteWithProductId.data is PersonPolicyHolder<*>) {
            memberService.signQuote(
                quoteWithProductId.memberId.toLong(),
                UnderwriterQuoteSignRequest(quoteWithProductId.data.ssn!!)
            )
        }

        val signedAt = Instant.now()
        val signedQuote = quoteWithProductId.copy(state = QuoteState.SIGNED)

        quoteRepository.update(signedQuote, signedAt)

        val activeProfiles = env.activeProfiles.intersect(listOf("staging", "production"))
        try {
            if (activeProfiles.isNotEmpty()) {
                logger.error("customerIOClient is null even thou $activeProfiles is set")
            }
            customerIO.postSignUpdate(quoteWithProductId)
        } catch (ex: Exception) {
            logger.error(
                "Something went wrong while posting a signing update to customerIO " +
                    "[ActiveProfile: $activeProfiles] [SignQuote: $signedQuote]"
            )
        }

        return SignedQuoteResponseDto(signedContractId, signedAt)
    }

    override fun signQuote(
        completeQuoteId: UUID,
        body: SignQuoteRequest
    ): Either<ErrorResponseDto, SignedQuoteResponseDto> {
        val quote = quoteRepository.find(completeQuoteId)
            ?: throw QuoteNotFoundException("Quote $completeQuoteId not found when trying to sign")

        if (quote.signedProductId != null) {
            throw RuntimeException("There is a signed product id ${quote.signedProductId} already")
        }

        val updatedName = if (body.name != null && quote.data is PersonPolicyHolder<*>) {
            quote.copy(data = quote.data.updateName(firstName = body.name.firstName, lastName = body.name.lastName))
        } else {
            quote
        }

        val updatedStartTime = when {
            body.startDate != null -> {
                updatedName.copy(
                    startDate = body.startDate
                )
            }
            else -> updatedName.copy(startDate = null)
        }

        val quoteWithMember = if (quote.memberId == null) {
            val quoteNotSignableErrorDto = quoteService.getQuoteStateNotSignableErrorOrNull(quote)
            if (quoteNotSignableErrorDto != null) {
                return Either.left(quoteNotSignableErrorDto)
            }

            val memberAlreadySigned = when (quote.data) {
                is PersonPolicyHolder<*> -> memberService.isSsnAlreadySignedMemberEntity(quote.data.ssn!!)
                else -> throw RuntimeException("Unsupported quote data class")
            }

            if (memberAlreadySigned.ssnAlreadySignedMember) {
                return Either.Left(
                    ErrorResponseDto(
                        ErrorCodes.MEMBER_HAS_EXISTING_INSURANCE,
                        "quote is already signed"
                    )
                )
            }

            val memberId = memberService.createMember()

            memberService.updateMemberSsn(memberId.toLong(), UpdateSsnRequest(ssn = quote.data.ssn!!))

            quoteRepository.update(updatedStartTime.copy(memberId = memberId))
        } else {
            quote
        }

        return Right(
            signQuoteWithMemberId(
                quoteWithMember,
                false,
                SignRequest("", "", ""),
                body.email
            )
        )
    }

    override fun memberSigned(memberId: String, signedRequest: SignRequest) {
        quoteRepository.findLatestOneByMemberId(memberId)?.let { quote ->
            signQuoteWithMemberId(quote, true, signedRequest, null)
        } ?: throw IllegalStateException("Tried to perform member sign with no quote!")
    }

    private fun signQuoteWithMemberId(
        quote: Quote,
        signStartedInMemberService: Boolean,
        signedRequest: SignRequest,
        email: String?
    ): SignedQuoteResponseDto {
        checkNotNull(quote.memberId) { "Quote must have a member id! Quote id: ${quote.id}" }
        checkNotNull(quote.price) { "Quote must have a price to sign! Quote id: ${quote.id}" }

        if (quote.initiatedFrom == QuoteInitiatedFrom.RAPIO) {
            email?.let {
                memberService.finalizeOnboarding(quote, it)
            }
                ?: throw IllegalArgumentException("Must have an email when signing from rapio!")
        }

        val createdAgreementId =
            productPricingService.createContractsFromQuotes(listOf(quote), signedRequest).first().agreementId

        return finishingUpSignedQuote(quote, createdAgreementId, signStartedInMemberService)
    }

    private fun getSignDataFromQuotes(quotes: List<Quote>): QuotesSignData {
        return when (quotes.size) {
            1 ->
                when (quotes[0].data) {
                    is SwedishApartmentData,
                    is SwedishHouseData -> QuotesSignData.SwedishBankId(
                        quotes[0].memberId!!,
                        quotes[0].ssn,
                        quotes[0].currentInsurer != null
                    )
                    is NorwegianHomeContentsData,
                    is NorwegianTravelData -> QuotesSignData.NorwegianBankId(quotes[0].memberId!!, quotes[0].ssn)
                }
            2 -> if (
                quotes.any { quote -> quote.data is NorwegianHomeContentsData } &&
                quotes.any { quote -> quote.data is NorwegianTravelData }
            ) {
                var ssn: String? = null
                quotes.forEach { quote ->
                    if (quote.data is PersonPolicyHolder<*>) {
                        quote.data.ssn?.let {
                            ssn = it
                            return@forEach
                        }
                    } else {
                        throw RuntimeException("Quote data should not be able to be of type ${quote.data::class}")
                    }
                }
                QuotesSignData.NorwegianBankId(quotes[0].memberId!!, ssn!!)
            } else {
                QuotesSignData.CanNotBeBundled
            }
            else -> QuotesSignData.CanNotBeBundled
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this.javaClass)!!
        const val SIGNING_QUOTE_WITH_OUT_MEMBER_ID_ERROR_MESSAGE = "quotes must have member id to be able to sign"
        const val VARIOUS_MEMBER_ID_ERROR_MESSAGE = "creation and signing must be made by the same member"
        const val TARGET_URL_NOT_PROVIDED_ERROR_MESSAGE = "Bad request: Must provide `successUrl` and `failUrl` when starting norwegian sign"
        const val MEMBER_HAS_ALREADY_SIGNED_ERROR_MESSAGE = "Member has already signed"
    }
}
