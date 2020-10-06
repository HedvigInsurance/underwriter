package com.hedvig.underwriter.service

import arrow.core.Either
import arrow.core.orNull
import com.hedvig.graphql.commons.type.MonetaryAmountV2
import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.Danish_PLACEHOLDER_Data
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.QuoteRepository
import com.hedvig.underwriter.model.QuoteState
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.model.email
import com.hedvig.underwriter.model.validTo
import com.hedvig.underwriter.service.exceptions.QuoteCompletionFailedException
import com.hedvig.underwriter.service.exceptions.QuoteNotFoundException
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.notificationService.NotificationService
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuoteDto
import com.hedvig.underwriter.web.dtos.AddAgreementFromQuoteRequest
import com.hedvig.underwriter.web.dtos.CompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.ErrorCodes
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import org.javamoney.moneta.Money
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class QuoteServiceImpl(
    val underwriter: Underwriter,
    val memberService: MemberService,
    val productPricingService: ProductPricingService,
    val quoteRepository: QuoteRepository,
    val notificationService: NotificationService
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

        if (quote.state != QuoteState.QUOTED && quote.state != QuoteState.INCOMPLETE) {
            return Either.Left(
                ErrorResponseDto(
                    ErrorCodes.INVALID_STATE,
                    "quote [Id: ${quote.id}] must be quoted to update but was really ${quote.state} [Quote: $quote]"
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
                        "quote [Id: ${quote.id}] cannot be calculated, underwriting guidelines are breached [Quote: $quote]",
                        breachedUnderwritingGuidelines
                    )
                )
            } ?: Either.left(
                ErrorResponseDto(
                    ErrorCodes.UNKNOWN_ERROR_CODE,
                    "Unable to complete quote [Quote: $quote]"
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
        underwritingGuidelinesBypassedBy: String?,
        updateMemberService: Boolean
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto> {
        val quoteId = id ?: UUID.randomUUID()

        val breachedGuidelinesOrQuote = createAndSaveQuote(
            quoteData = incompleteQuoteData,
            quoteId = quoteId,
            initiatedFrom = initiatedFrom,
            underwritingGuidelinesBypassedBy = underwritingGuidelinesBypassedBy
        )

        val quote = breachedGuidelinesOrQuote.getQuote()
        if (updateMemberService && quote.memberId != null) {
            memberService.finalizeOnboarding(quote, quote.email ?: "")
        }

        if (quote.memberId != null && quote.email != null) {
            notificationService.sendQuoteCreatedEvent(quote)
        }

        return transformCompleteQuoteReturn(breachedGuidelinesOrQuote, quoteId)
    }

    override fun createQuoteForNewContractFromHope(
        quoteRequest: QuoteRequest,
        underwritingGuidelinesBypassedBy: String?
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto> {
        val quoteId = UUID.randomUUID()

        val updatedQuoteRequest = updateQuoteRequestWithMember(quoteRequest)

        val breachedGuidelinesOrQuote = createAndSaveQuote(
            quoteData = updatedQuoteRequest,
            quoteId = quoteId,
            initiatedFrom = QuoteInitiatedFrom.HOPE,
            underwritingGuidelinesBypassedBy = underwritingGuidelinesBypassedBy
        )

        return transformCompleteQuoteReturn(breachedGuidelinesOrQuote, quoteId)
    }

    override fun expireQuote(id: UUID): Quote? {
        return quoteRepository.expireQuote(id)
    }

    override fun getQuoteByContractId(contractId: UUID): Quote? {
        return quoteRepository.findByContractId(contractId)
    }

    override fun createQuoteFromAgreement(
        agreementId: UUID,
        memberId: String,
        underwritingGuidelinesBypassedBy: String?
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto> {
        val quoteId = UUID.randomUUID()

        val agreementData = productPricingService.getAgreement(agreementId)

        val member = memberService.getMember(memberId.toLong())

        val incompleteQuoteData = agreementData.toQuoteRequestData()

        val quoteData = QuoteRequest.from(
            member = member,
            agreementData = agreementData,
            incompleteQuoteData = incompleteQuoteData
        )

        val breachedGuidelinesOrQuote = createAndSaveQuote(
            quoteData,
            quoteId,
            QuoteInitiatedFrom.HOPE,
            underwritingGuidelinesBypassedBy
        )

        return transformCompleteQuoteReturn(breachedGuidelinesOrQuote, quoteId)
    }

    private fun updateQuoteRequestWithMember(input: QuoteRequest): QuoteRequest {
        val member = memberService.getMember(input.memberId!!.toLong())
        return input.copy(
            firstName = member.firstName,
            lastName = member.lastName,
            email = member.email,
            birthDate = member.birthDate,
            ssn = member.ssn
        )
    }

    private fun createAndSaveQuote(
        quoteData: QuoteRequest,
        quoteId: UUID,
        initiatedFrom: QuoteInitiatedFrom,
        underwritingGuidelinesBypassedBy: String?
    ): Either<Pair<Quote, List<String>>, Quote> {
        val breachedGuidelinesOrQuote =
            underwriter.createQuote(quoteData, quoteId, initiatedFrom, underwritingGuidelinesBypassedBy)
        val quote = breachedGuidelinesOrQuote.getQuote()

        quoteRepository.insert(quote)
        return breachedGuidelinesOrQuote
    }

    private fun Either<Pair<Quote, List<String>>, Quote>.getQuote(): Quote {
        return when (this) {
            is Either.Left -> a.first
            is Either.Right -> b
        }
    }

    private fun transformCompleteQuoteReturn(
        potentiallySavedQuote: Either<Pair<Quote, List<String>>, Quote>,
        quoteId: UUID
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto> {
        return potentiallySavedQuote.bimap(
            { breachedUnderwritingGuidelines ->
                logger.info(
                    "Underwriting guidelines breached for incomplete quote $quoteId: {}",
                    breachedUnderwritingGuidelines
                )
                ErrorResponseDto(
                    ErrorCodes.MEMBER_BREACHES_UW_GUIDELINES,
                    "quote cannot be calculated, underwriting guidelines are breached [Quote: ${breachedUnderwritingGuidelines.first}",
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

    override fun calculateInsuranceCost(quote: Quote): InsuranceCost {
        val memberId = quote.memberId
            ?: throw RuntimeException("Can't calculate InsuranceCost on a quote without memberId [Quote: $quote]")

        return when (quote.data) {
            is SwedishHouseData,
            is SwedishApartmentData -> productPricingService.calculateInsuranceCost(
                Money.of(quote.price, "SEK"), memberId
            )
            is NorwegianHomeContentsData,
            is NorwegianTravelData -> productPricingService.calculateInsuranceCost(
                Money.of(quote.price, "NOK"), memberId
            )
            is Danish_PLACEHOLDER_Data -> {
                //TODO: fix when replacing _PLACEHOLDER_
                //TODO: Implement actual request
                InsuranceCost(
                        MonetaryAmountV2("9999", "DKK"),
                        MonetaryAmountV2("0", "DKK"),
                        MonetaryAmountV2("9999", "DKK"),
                    null
                )
            }
        }
    }

    override fun getQuotes(quoteIds: List<UUID>): List<Quote> {
        return quoteRepository.findQuotes(quoteIds)
    }

    override fun addAgreementFromQuote(request: AddAgreementFromQuoteRequest): Either<ErrorResponseDto, Quote> {
        val quote = getQuote(request.quoteId)
            ?: throw QuoteNotFoundException("Quote ${request.quoteId} not found when trying to add agreement")

        val quoteNotSignableErrorDto = assertQuoteIsNotSignedOrExpired(quote)
        if (quoteNotSignableErrorDto != null) {
            Either.left(quoteNotSignableErrorDto)
        }

        val response = productPricingService.addAgreementFromQuote(
            quote = quote,
            request = request
        )

        val updatedQuote = quoteRepository.update(
            quote.copy(
                agreementId = response.agreementId,
                contractId = response.contractId,
                state = QuoteState.SIGNED
            )
        )

        return Either.right(updatedQuote)
    }
}

fun assertQuoteIsNotSignedOrExpired(quote: Quote): ErrorResponseDto? {
    if (quote.state == QuoteState.EXPIRED) {
        return ErrorResponseDto(
            ErrorCodes.MEMBER_QUOTE_HAS_EXPIRED,
            "cannot sign quote it has expired [Quote: $quote]"
        )
    }

    if (quote.state == QuoteState.SIGNED) {
        return ErrorResponseDto(
            ErrorCodes.MEMBER_HAS_EXISTING_INSURANCE,
            "quote is already signed [Quote: $quote]"
        )
    }
    return null
}
