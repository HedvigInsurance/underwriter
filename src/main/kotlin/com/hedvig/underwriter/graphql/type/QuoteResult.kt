package com.hedvig.underwriter.graphql.type

import com.hedvig.graphql.commons.type.MonetaryAmountV2
import com.hedvig.underwriter.graphql.type.depricated.CompleteQuoteDetails
import java.time.LocalDate
import java.util.UUID

sealed class QuoteResult {

    data class CompleteQuote(
        val id: UUID,
        val firstName: String,
        val lastName: String,
        val currentInsurer: CurrentInsurer?,
        val ssn: String?,
        val birthDate: LocalDate,
        val price: MonetaryAmountV2,
        val insuranceCost: InsuranceCost,
        @Deprecated("use quoteDetails")
        val details: CompleteQuoteDetails,
        val quoteDetails: QuoteDetails,
        val startDate: LocalDate?,
        val expiresAt: LocalDate,
        val email: String?,
        val dataCollectionId: UUID?
    ) : QuoteResult(), CreateQuoteResult {
        val typeOfContract: TypeOfContract
            get() = quoteDetails.typeOfContract
    }

    @Deprecated("Incomplete is deprecated")
    data class IncompleteQuote(
        val id: UUID,
        val firstName: String?,
        val lastName: String?,
        val birthDate: LocalDate?,
        val currentInsurer: CurrentInsurer?,
        val details: IncompleteQuoteDetails?,
        val startDate: LocalDate?,
        val email: String?,
        val dataCollectionId: UUID?
    ) : QuoteResult()
}
