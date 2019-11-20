package com.hedvig.underwriter.service

import arrow.core.Either
import com.hedvig.underwriter.model.Campaign
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuoteDto
import com.hedvig.underwriter.web.dtos.CompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import com.hedvig.underwriter.web.dtos.IncompleteQuoteDto
import com.hedvig.underwriter.web.dtos.IncompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.SignQuoteRequest
import com.hedvig.underwriter.web.dtos.SignedQuoteResponseDto
import java.util.UUID

interface QuoteService {
    fun completeQuote(incompleteQuoteId: UUID): Either<ErrorResponseDto, CompleteQuoteResponseDto>
    fun signQuote(completeQuoteId: UUID, body: SignQuoteRequest): Either<ErrorResponseDto, SignedQuoteResponseDto>

    fun getQuote(completeQuoteId: UUID): Quote?
    fun getQuoteFromMemberId(memberId: String): QuoteDto?
    fun createApartmentQuote(incompleteQuoteDto: IncompleteQuoteDto): IncompleteQuoteResponseDto
    fun updateQuote(incompleteQuoteDto: IncompleteQuoteDto, id: UUID): Either<ErrorResponseDto, Quote>
    fun redeemCode(completeQuoteId: UUID, code: String): Either<ErrorResponseDto, Campaign>
}
