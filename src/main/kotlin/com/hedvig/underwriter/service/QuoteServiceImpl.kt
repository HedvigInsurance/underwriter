package com.hedvig.underwriter.service

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import arrow.core.filterOrOther
import arrow.core.flatMap
import arrow.core.toOption
import com.fasterxml.jackson.databind.ObjectMapper
import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.AddressData
import com.hedvig.underwriter.model.DeletedQuote
import com.hedvig.underwriter.model.Market
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.QuoteRepository
import com.hedvig.underwriter.model.QuoteState
import com.hedvig.underwriter.model.email
import com.hedvig.underwriter.model.ssnMaybe
import com.hedvig.underwriter.model.validTo
import com.hedvig.underwriter.service.exceptions.NotFoundException
import com.hedvig.underwriter.service.exceptions.QuoteNotFoundException
import com.hedvig.underwriter.service.guidelines.BreachedGuidelineCode
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.service.quoteStrategies.QuoteStrategyService
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.notificationService.NotificationService
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuoteDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.extensions.toQuoteRequestData
import com.hedvig.underwriter.util.logger
import com.hedvig.underwriter.util.toMaskedJsonString
import com.hedvig.underwriter.web.dtos.AddAgreementFromQuoteRequest
import com.hedvig.underwriter.web.dtos.BreachedGuideline
import com.hedvig.underwriter.web.dtos.CompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.ErrorCodes
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.IllegalStateException
import java.time.Instant
import java.util.UUID

@Service
class QuoteServiceImpl(
    val underwriter: Underwriter,
    val memberService: MemberService,
    val productPricingService: ProductPricingService,
    val quoteRepository: QuoteRepository,
    val notificationService: NotificationService,
    val quoteStrategyService: QuoteStrategyService
) : QuoteService {

    @Autowired
    lateinit var objectMapper: ObjectMapper

    override fun updateQuote(
        quoteRequest: QuoteRequest,
        id: UUID,
        underwritingGuidelinesBypassedBy: String?
    ): Either<ErrorResponseDto, Quote> {

        return findQuoteOrError(id)
            .filterOrOther(
                { it.state == QuoteState.QUOTED || it.state == QuoteState.INCOMPLETE },
                {
                    ErrorResponseDto(
                        ErrorCodes.INVALID_STATE,
                        "quote [Id: ${it.id}] must be quoted to update but was really ${it.state} [Quote: $it]"
                    )
                }
            )
            .map { it.clearBreachedUnderwritingGuidelines() }
            .map { it.update(quoteRequest) }
            .flatMap { updatedQuote ->
                underwriter
                    .validateAndCompleteQuote(updatedQuote, underwritingGuidelinesBypassedBy)
                    .mapLeft { e ->
                        ErrorResponseDto(
                            ErrorCodes.MEMBER_BREACHES_UW_GUIDELINES,
                            "quote [Id: ${updatedQuote.id}] cannot be calculated, underwriting guidelines are breached [Quote: $updatedQuote]",
                            e.second.map { BreachedGuideline("Deprecated", it) }
                        )
                    }
            }.map { quoteRepository.update(it) }
    }

    private fun findQuoteOrError(id: UUID) =
        quoteRepository
            .find(id)
            .toOption()
            .toEither { ErrorResponseDto(ErrorCodes.NO_SUCH_QUOTE, "No such quote $id") }

    override fun removeCurrentInsurerFromQuote(id: UUID): Either<ErrorResponseDto, Quote> =
        findQuoteOrError(id)
            .map { quoteRepository.update(it.copy(currentInsurer = null)) }

    override fun removeStartDateFromQuote(id: UUID): Either<ErrorResponseDto, Quote> =
        findQuoteOrError(id)
            .map { quoteRepository.update(it.copy(startDate = null)) }

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
        quoteRequest: QuoteRequest,
        id: UUID?,
        initiatedFrom: QuoteInitiatedFrom,
        underwritingGuidelinesBypassedBy: String?,
        updateMemberService: Boolean
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto> {
        val quoteId = id ?: UUID.randomUUID()

        val breachedGuidelinesOrQuote = createAndSaveQuote(
            quoteRequest = quoteRequest,
            quoteId = quoteId,
            initiatedFrom = initiatedFrom,
            underwritingGuidelinesBypassedBy = underwritingGuidelinesBypassedBy
        )

        val quote = breachedGuidelinesOrQuote.getQuote()
        if (updateMemberService && quote.memberId != null) {
            memberService.finalizeOnboarding(quote, quote.email!!)
        }

        if (quote.memberId != null && quote.email != null && quote.state == QuoteState.QUOTED) {
            try {
                notificationService.sendQuoteCreatedEvent(quote)
            } catch (exception: Exception) {
                logger.error("Unable to send quote created event (quoteId=${quote.id})", exception)
            }
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
            quoteRequest = updatedQuoteRequest,
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

    override fun getMarketFromLatestQuote(memberId: String): Market {
        return getLatestQuoteForMemberId(memberId)!!.market
    }

    override fun deleteQuote(id: UUID) {

        logger.info("Delete quote $id")

        val quote = quoteRepository.find(id) ?: throw NotFoundException("No quote found")

        if (quote.agreementId != null) {
            logger.warn("Cannot delete quote since attached to agreement ${quote.agreementId}")
            throw IllegalStateException("Deleting a quote attached to agreement is not allowed")
        }

        val revs = quoteRepository.findQuoteRevisions(id)

        val deletedQuote = DeletedQuote(
            quoteId = quote.id,
            createdAt = quote.createdAt,
            deletedAt = Instant.now(),
            type = quote.data::class.simpleName ?: "-",
            memberId = quote.memberId,
            hashedSsn = quote.ssnMaybe?.hashCode().toString(),
            quote = quote.toMaskedJsonString(objectMapper).anonymiseStreetNumber(quote),
            revs = revs.toMaskedJsonString(objectMapper)
        )

        logger.info("Saving anonymized quote: $deletedQuote")
        quoteRepository.insert(deletedQuote)

        quoteRepository.delete(quote)
    }

    fun String.anonymiseStreetNumber(quote: Quote): String {
        if (quote.data !is AddressData) {
            return this
        }

        // Only keep words without numbers or non alphabetic letters
        val street = quote.data.street?.let {
            it
                .split(" ")
                .filter { it.chars().allMatch(Character::isLetter) }
                .joinToString(" ")
        }

        return this.replace("\"street\":null", "\"street\": \"$street\"")
    }

    override fun createQuoteFromAgreement(
        agreementId: UUID,
        memberId: String,
        underwritingGuidelinesBypassedBy: String?
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto> {
        val quoteId = UUID.randomUUID()

        val agreementData = productPricingService.getAgreement(agreementId)

        val member = memberService.getMember(memberId.toLong())

        val quoteRequestData = agreementData.toQuoteRequestData()

        val quoteData = QuoteRequest.from(
            member = member,
            agreementData = agreementData,
            incompleteQuoteData = quoteRequestData
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
        quoteRequest: QuoteRequest,
        quoteId: UUID,
        initiatedFrom: QuoteInitiatedFrom,
        underwritingGuidelinesBypassedBy: String?
    ): Either<Pair<Quote, List<BreachedGuidelineCode>>, Quote> {
        val breachedGuidelinesOrQuote =
            underwriter.createQuote(
                quoteRequest,
                quoteId,
                initiatedFrom,
                underwritingGuidelinesBypassedBy
            )
        val quote = breachedGuidelinesOrQuote.getQuote()

        quoteRepository.insert(quote)
        return breachedGuidelinesOrQuote
    }

    private fun Either<Pair<Quote, List<BreachedGuidelineCode>>, Quote>.getQuote(): Quote {
        return when (this) {
            is Either.Left -> a.first
            is Either.Right -> b
        }
    }

    private fun transformCompleteQuoteReturn(
        potentiallySavedQuote: Either<Pair<Quote, List<BreachedGuidelineCode>>, Quote>,
        quoteId: UUID
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto> {
        val quote = potentiallySavedQuote.getQuote()

        return if (!quote.breachedUnderwritingGuidelines.isNullOrEmpty()) {
            logger.info(
                "Underwriting guidelines breached for incomplete quote $quoteId: {}",
                quote.breachedUnderwritingGuidelines
            )
            Left(
                ErrorResponseDto(
                    ErrorCodes.MEMBER_BREACHES_UW_GUIDELINES,
                    "quote cannot be calculated, underwriting guidelines are breached [Quote: $quote",
                    quote.breachedUnderwritingGuidelines.map { BreachedGuideline("Deprecated", it) }
                )
            )
        } else {
            Right(
                CompleteQuoteResponseDto(
                    id = quote.id,
                    price = quote.price!!,
                    currency = quote.currency!!,
                    validTo = quote.validTo
                )
            )
        }
    }

    override fun calculateInsuranceCost(quote: Quote): InsuranceCost {
        check(quote.memberId != null) { "Can't calculate InsuranceCost on a quote without memberId [Quote: $quote]" }

        return quoteStrategyService.getInsuranceCost(quote)
    }

    override fun getQuotes(quoteIds: List<UUID>): List<Quote> {
        return quoteRepository.findQuotes(quoteIds)
    }

    override fun addAgreementFromQuote(
        request: AddAgreementFromQuoteRequest,
        token: String?
    ): Either<ErrorResponseDto, Quote> {
        val quote = getQuote(request.quoteId)
            ?: throw QuoteNotFoundException("Quote ${request.quoteId} not found when trying to add agreement")

        val quoteNotSignableErrorDto = assertQuoteIsNotSignedOrExpired(quote)
        if (quoteNotSignableErrorDto != null) {
            return Either.left(quoteNotSignableErrorDto)
        }

        val response = productPricingService.addAgreementFromQuote(
            quote = quote,
            request = request,
            token = token
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
