package com.hedvig.underwriter.service

import arrow.core.Either
import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.model.Market
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuoteDto
import com.hedvig.underwriter.web.dtos.AddAgreementFromQuoteRequest
import com.hedvig.underwriter.web.dtos.CompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import java.util.UUID

interface QuoteService {

    fun getQuote(completeQuoteId: UUID): Quote?
    fun getSingleQuoteForMemberId(memberId: String): QuoteDto?
    fun getLatestQuoteForMemberId(memberId: String): Quote?
    fun getQuotesForMemberId(memberId: String): List<QuoteDto>
    fun createQuote(
        quoteRequest: QuoteRequest,
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

    fun calculateInsuranceCost(quote: Quote): InsuranceCost

    fun getQuotes(quoteIds: List<UUID>): List<Quote>

    fun addAgreementFromQuote(request: AddAgreementFromQuoteRequest, token: String?): Either<ErrorResponseDto, Quote>

    fun createQuoteForNewContractFromHope(
        quoteRequest: QuoteRequest,
        underwritingGuidelinesBypassedBy: String?
    ): Either<ErrorResponseDto, CompleteQuoteResponseDto>

    fun expireQuote(id: UUID): Quote?
    fun deleteQuote(id: UUID)
    fun getQuoteByContractId(contractId: UUID): Quote?
    fun getMarketFromLatestQuote(memberId: String): Market
}
