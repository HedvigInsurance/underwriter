package com.hedvig.underwriter.service

import arrow.core.Either
import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuoteDto
import com.hedvig.underwriter.web.dtos.AddAgreementFromQuoteRequest
import com.hedvig.underwriter.web.dtos.CompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import java.time.LocalDate
import java.util.UUID

interface QuoteService {
    fun activateQuote(
        completeQuoteId: UUID,
        activationDate: LocalDate? = null,
        previousProductTerminationDate: LocalDate? = null
    ): Either<ErrorResponseDto, Quote>

    fun getQuote(completeQuoteId: UUID): Quote?
    fun getSingleQuoteForMemberId(memberId: String): QuoteDto?
    fun getLatestQuoteForMemberId(memberId: String): Quote?
    fun getQuotesForMemberId(memberId: String): List<QuoteDto>
    fun createQuote(
        incompleteQuoteData: QuoteRequest,
        id: UUID? = null,
        initiatedFrom: QuoteInitiatedFrom,
        underwritingGuidelinesBypassedBy: String?,
        updateMemberService: Boolean
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto>

    fun createQuoteFromAgreement(
        agreementId: UUID,
        memberId: String,
        underwritingGuidelinesBypassedBy: String?
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto>

    fun updateQuote(
        quoteRequest: QuoteRequest,
        id: UUID,
        underwritingGuidelinesBypassedBy: String? = null
    ): Either<ErrorResponseDto, Quote>

    fun removeCurrentInsurerFromQuote(
        id: UUID
    ): Either<ErrorResponseDto, Quote>

    fun removeStartDateFromQuote(
        id: UUID
    ): Either<ErrorResponseDto, Quote>

    fun getQuoteStateNotSignableErrorOrNull(quote: Quote): ErrorResponseDto?

    fun calculateInsuranceCost(quote: Quote): InsuranceCost

    fun getQuotes(quoteIds: List<UUID>): List<Quote>

    fun addAgreementFromQuote(request: AddAgreementFromQuoteRequest): Either<ErrorResponseDto, Quote>
}
