package com.hedvig.underwriter.service

import arrow.core.Either
import com.hedvig.underwriter.service.model.CompleteSignSessionData
import com.hedvig.underwriter.service.model.StartSignResponse
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import com.hedvig.underwriter.web.dtos.SignQuoteRequest
import com.hedvig.underwriter.web.dtos.SignRequest
import com.hedvig.underwriter.web.dtos.SignedQuoteResponseDto
import java.util.UUID

interface SignService {
    fun startSigningQuotes(
        quoteIds: List<UUID>,
        memberId: String,
        ipAddress: String?,
        successUrl: String?,
        failUrl: String?
    ): StartSignResponse

    fun completedSignSession(
        signSessionId: UUID,
        completeSignSessionData: CompleteSignSessionData
    )

    fun signQuote(
        completeQuoteId: UUID,
        body: SignQuoteRequest
    ): Either<ErrorResponseDto, SignedQuoteResponseDto>

    fun memberSigned(memberId: String, signedRequest: SignRequest)
}
