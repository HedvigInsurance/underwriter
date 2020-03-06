package com.hedvig.underwriter.service

import arrow.core.Either
import arrow.core.orNull
import com.hedvig.graphql.commons.type.MonetaryAmountV2
import com.hedvig.underwriter.graphql.type.InsuranceCost
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
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ModifyProductRequestDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuoteDto
import com.hedvig.underwriter.web.dtos.CompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.ErrorCodes
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import java.lang.RuntimeException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import org.javamoney.moneta.Money
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Service

@Service
class QuoteServiceImpl(
    val underwriter: Underwriter,
    val memberService: MemberService,
    val productPricingService: ProductPricingService,
    val quoteRepository: QuoteRepository
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
        val breachedGuidelinesOrQuote =
            underwriter.createQuote(incompleteQuoteData, quoteId, initiatedFrom, underwritingGuidelinesBypassedBy)
        val quote = when (breachedGuidelinesOrQuote) {
            is Either.Left -> breachedGuidelinesOrQuote.a.first
            is Either.Right -> breachedGuidelinesOrQuote.b
        }
        quoteRepository.insert(quote)

        if (updateMemberService && quote.memberId != null) {
            memberService.finalizeOnboarding(quote, quote.email ?: "")
        }

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
                    errorMessage = "No activation date given [QuoteId : $completeQuoteId]"
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

    override fun getQuoteStateNotSignableErrorOrNull(quote: Quote): ErrorResponseDto? {
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

    override fun calculateInsuranceCost(quote: Quote): InsuranceCost {
        val memberId = quote.memberId
            ?: throw RuntimeException("Can't calculate InsuranceCost on a quote without memberId [Quote: $quote]")

        // TODO once campaign service is up to speed lets remove this when
        return when (quote.data) {
            is SwedishHouseData,
            is SwedishApartmentData -> productPricingService.calculateInsuranceCost(
                Money.of(quote.price, "SEK"), memberId
            )
            is NorwegianHomeContentsData,
            is NorwegianTravelData -> InsuranceCost(
                monthlyGross = MonetaryAmountV2.of(quote.price!!, "NOK"),
                monthlyDiscount = MonetaryAmountV2.of(BigDecimal.ZERO, "NOK"),
                monthlyNet = MonetaryAmountV2.of(quote.price, "NOK"),
                freeUntil = null
            )
        }
    }

    override fun getQuotes(quoteIds: List<UUID>): List<Quote> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
