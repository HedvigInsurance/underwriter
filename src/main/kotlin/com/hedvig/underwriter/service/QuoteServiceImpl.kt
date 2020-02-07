package com.hedvig.underwriter.service

import arrow.core.Either
import arrow.core.Right
import arrow.core.orNull
import arrow.core.right
import com.hedvig.underwriter.extensions.validTo
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.QuoteRepository
import com.hedvig.underwriter.model.QuoteState
import com.hedvig.underwriter.service.exceptions.QuoteCompletionFailedException
import com.hedvig.underwriter.service.exceptions.QuoteNotFoundException
import com.hedvig.underwriter.service.model.PersonPolicyHolder
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.serviceIntegration.customerio.CustomerIO
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UpdateSsnRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ModifyProductRequestDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuoteDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RedeemCampaignDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.SignedQuoteRequest
import com.hedvig.underwriter.web.dtos.CompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.ErrorCodes
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import com.hedvig.underwriter.web.dtos.SignQuoteRequest
import com.hedvig.underwriter.web.dtos.SignRequest
import com.hedvig.underwriter.web.dtos.SignedQuoteResponseDto
import com.hedvig.underwriter.web.dtos.UnderwriterQuoteSignRequest
import feign.FeignException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import org.javamoney.moneta.Money
import org.slf4j.LoggerFactory.getLogger
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class QuoteServiceImpl(
    val underwriter: Underwriter,
    val memberService: MemberService,
    val productPricingService: ProductPricingService,
    val quoteRepository: QuoteRepository,
    val customerIOClient: CustomerIO?,
    val env: Environment
) : QuoteService {

    val logger = getLogger(QuoteServiceImpl::class.java)!!

    override fun updateQuote(
        quoteRequest: QuoteRequest,
        id: UUID,
        underwritingGuidelinesBypassedBy: String?
    ): Either<ErrorResponseDto, Quote> {
        val quote = quoteRepository
            .find(id)
            ?: return Either.Left(
                ErrorResponseDto(ErrorCodes.NO_SUCH_QUOTE, "No such quote $id")
            )

        if (quote.state != QuoteState.QUOTED) {
            if (quote.state == QuoteState.INCOMPLETE) {
                logger.error("updating quote that is in incomplete state. quote: $quote")
            }
            return Either.Left(
                ErrorResponseDto(
                    ErrorCodes.INVALID_STATE,
                    "quote must be quoted to update but was really ${quote.state}"
                )
            )
        }

        return try {
            quoteRepository.modify(quote.id) { quoteToUpdate ->
                val updatedQuote = quoteToUpdate!!.update(quoteRequest)
                underwriter.updateQuote(updatedQuote, underwritingGuidelinesBypassedBy)
                    .bimap(
                        { errors ->
                            throw QuoteCompletionFailedException(
                                "Unable to complete quote: ${errors.second}",
                                errors.second
                            )
                        },
                        { quote -> quote }
                    ).orNull()
            }!!.let { updatedQuote -> Either.right(updatedQuote) }
        } catch (e: QuoteCompletionFailedException) {
            e.breachedUnderwritingGuidelines?.let { breachedUnderwritingGuidelines ->
                Either.left(
                    ErrorResponseDto(
                        ErrorCodes.MEMBER_BREACHES_UW_GUIDELINES,
                        "quote cannot be calculated, underwriting guidelines are breached",
                        breachedUnderwritingGuidelines
                    )
                )
            } ?: Either.left(
                ErrorResponseDto(
                    ErrorCodes.UNKNOWN_ERROR_CODE,
                    "Unable to complete quote"
                )
            )
        }
    }

    override fun removeCurrentInsurerFromQuote(id: UUID): Either<ErrorResponseDto, Quote> {
        val updatedQuote = quoteRepository.modify(id) { quoteToUpdate ->
            quoteToUpdate!!.copy(currentInsurer = null)
        }

        return Either.right(updatedQuote!!)
    }

    override fun removeStartDateFromQuote(id: UUID): Either<ErrorResponseDto, Quote> {
        val updatedQuote = quoteRepository.modify(id) { quoteToUpdate ->
            quoteToUpdate!!.copy(startDate = null)
        }

        return Either.right(updatedQuote!!)
    }

    override fun getQuote(completeQuoteId: UUID): Quote? {
        return quoteRepository.find(completeQuoteId)
    }

    override fun getSingleQuoteForMemberId(memberId: String): QuoteDto? {
        val quote = quoteRepository.findOneByMemberId(memberId)
        return quote?.let((QuoteDto)::fromQuote)
    }

    override fun getLatestQuoteForMemberId(memberId: String): Quote? =
        quoteRepository.findLatestOneByMemberId(memberId)

    override fun getQuotesForMemberId(memberId: String): List<QuoteDto> =
        quoteRepository.findByMemberId(memberId)
            .map((QuoteDto)::fromQuote)

    override fun createQuote(
        incompleteQuoteData: QuoteRequest,
        id: UUID?,
        initiatedFrom: QuoteInitiatedFrom,
        underwritingGuidelinesBypassedBy: String?
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto> {
        val quoteId = id ?: UUID.randomUUID()
        val breachedGuidelinesOrQuote =
            underwriter.createQuote(incompleteQuoteData, quoteId, initiatedFrom, underwritingGuidelinesBypassedBy)
        val quote = when (breachedGuidelinesOrQuote) {
            is Either.Left -> breachedGuidelinesOrQuote.a.first
            is Either.Right -> breachedGuidelinesOrQuote.b
        }
        quoteRepository.insert(quote)

        return transformCompleteQuoteReturn(breachedGuidelinesOrQuote, quoteId)
    }

    private fun transformCompleteQuoteReturn(
        potentiallySavedQuote: Either<Pair<Quote, List<String>>, Quote>,
        quoteId: UUID
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto> {
        return potentiallySavedQuote.bimap(
            { breachedUnderwritingGuidelines ->
                logger.error(
                    "Underwriting guidelines breached for incomplete quote $quoteId: {}",
                    breachedUnderwritingGuidelines
                )
                ErrorResponseDto(
                    ErrorCodes.MEMBER_BREACHES_UW_GUIDELINES,
                    "quote cannot be calculated, underwriting guidelines are breached",
                    breachedUnderwritingGuidelines.second
                )
            },
            { completeQuote ->
                CompleteQuoteResponseDto(
                    id = completeQuote.id,
                    price = completeQuote.price!!,
                    validTo = completeQuote.validTo
                )
            }
        )
    }

    override fun signQuote(
        completeQuoteId: UUID,
        body: SignQuoteRequest
    ): Either<ErrorResponseDto, SignedQuoteResponseDto> {
        val quote = getQuote(completeQuoteId)
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
            val quoteNotSignableErrorDto = getQuoteStateNotSignableErrorOrNull(quote)
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
        signedInMemberService: Boolean,
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

        val signedProductId = productPricingService.signedQuote(
            SignedQuoteRequest(
                price = Money.of(quote.price, "SEK"),
                quote = quote,
                referenceToken = signedRequest.referenceToken,
                signature = signedRequest.signature,
                oscpResponse = signedRequest.oscpResponse
            ),
            quote.memberId
        ).id

        val quoteWithProductId = quoteRepository.update(quote.copy(signedProductId = signedProductId))
        checkNotNull(quoteWithProductId.memberId) { "Quote must have a member id! Quote id: ${quote.id}" }

        quoteWithProductId.attributedTo.campaignCode?.let { campaignCode ->
            try {
                productPricingService.redeemCampaign(
                    RedeemCampaignDto(
                        quoteWithProductId.memberId,
                        campaignCode,
                        LocalDate.now(ZoneId.of("Europe/Stockholm"))
                    )
                )
            } catch (e: FeignException) {
                logger.error("Failed to redeem $campaignCode for partner ${quoteWithProductId.attributedTo} with response ${e.message}")
            }
        }

        if (!signedInMemberService && quoteWithProductId.data is PersonPolicyHolder<*>) {
            memberService.signQuote(
                quoteWithProductId.memberId.toLong(),
                UnderwriterQuoteSignRequest(quoteWithProductId.data.ssn!!)
            )
        }

        val signedAt = Instant.now()
        val signedQuote = quoteWithProductId.copy(state = QuoteState.SIGNED)

        quoteRepository.update(signedQuote, signedAt)

        val activeProfiles = env.activeProfiles.intersect(listOf("staging", "production"))
        if (activeProfiles.isNotEmpty() && customerIOClient == null) {
            logger.error("customerIOClient is null even thou $activeProfiles is set")
        }
        customerIOClient?.postSignUpdate(quoteWithProductId)

        return SignedQuoteResponseDto(signedProductId, signedAt)
    }

    override fun activateQuote(
        completeQuoteId: UUID,
        activationDate: LocalDate?,
        previousProductTerminationDate: LocalDate?
    ): Either<ErrorResponseDto, Quote> {
        val quote = getQuote(completeQuoteId)
            ?: throw QuoteNotFoundException("Quote $completeQuoteId not found when trying to sign")

        val quoteNotSignableErrorDto = getQuoteStateNotSignableErrorOrNull(quote)
        if (quoteNotSignableErrorDto != null) {
            Either.left(quoteNotSignableErrorDto)
        }

        val finalActivationDate = activationDate ?: quote.startDate
        val finalTerminationDate = previousProductTerminationDate ?: finalActivationDate

        if (finalActivationDate == null) {
            return Either.left(
                ErrorResponseDto(
                    errorCode = ErrorCodes.UNKNOWN_ERROR_CODE,
                    errorMessage = "No activation date given"
                )
            )
        }

        val result = productPricingService.createModifiedProductFromQuote(
            ModifyProductRequestDto.from(
                quote,
                activationDate = finalActivationDate,
                previousInsuranceTerminationDate = finalTerminationDate!!
            )
        )

        val updatedQuote = quoteRepository.update(
            quote.copy(
                signedProductId = result.id,
                state = QuoteState.SIGNED
            )
        )

        return Either.right(updatedQuote)
    }

    private fun getQuoteStateNotSignableErrorOrNull(quote: Quote): ErrorResponseDto? {
        if (quote.state == QuoteState.EXPIRED) {
            return ErrorResponseDto(
                ErrorCodes.MEMBER_QUOTE_HAS_EXPIRED,
                "cannot sign quote it has expired"
            )
        }

        if (quote.state == QuoteState.SIGNED) {
            return ErrorResponseDto(
                ErrorCodes.MEMBER_HAS_EXISTING_INSURANCE,
                "quote is already signed"
            )
        }

        return null
    }
}
