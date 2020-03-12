package com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract

import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.firstName
import com.hedvig.underwriter.model.lastName
import com.hedvig.underwriter.model.ssn
import com.hedvig.underwriter.web.dtos.SignRequest

data class CreateContractsRequest(
    val memberId: String,
    val mandate: CreateMandateRequest?,
    val currentInsurer: String?,
    val signSource: QuoteInitiatedFrom,
    val quotes: List<AgreementQuote>
) {
    companion object {
        fun from(quotes: List<Quote>, signedRequest: SignRequest): CreateContractsRequest {
            val firstQuote = quotes.first()
            return CreateContractsRequest(
                memberId = firstQuote.memberId!!,
                mandate = CreateMandateRequest(
                    firstName = firstQuote.firstName,
                    lastName = firstQuote.lastName,
                    ssn = firstQuote.ssn,
                    referenceToken = signedRequest.referenceToken,
                    signature = signedRequest.signature,
                    oscpResponse = signedRequest.oscpResponse
                ),
                currentInsurer = firstQuote.currentInsurer,
                signSource = firstQuote.initiatedFrom,
                quotes = quotes.map { quote -> AgreementQuote.from(quote) }
            )
        }

        fun fromQuotesNoMandate(quotes: List<Quote>): CreateContractsRequest {
            val firstQuote = quotes.first()
            return CreateContractsRequest(
                memberId = firstQuote.memberId!!,
                mandate = null,
                currentInsurer = firstQuote.currentInsurer,
                signSource = firstQuote.initiatedFrom,
                quotes = quotes.map { quote -> AgreementQuote.from(quote) }
            )
        }
    }
}
