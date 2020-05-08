package com.hedvig.underwriter.graphql.type

import com.hedvig.graphql.commons.type.MonetaryAmountV2
import java.time.LocalDate
import java.util.UUID

data class BundledQuote(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val currentInsurer: CurrentInsurer?,
    val ssn: String?,
    val birthDate: LocalDate,
    val price: MonetaryAmountV2,
    val quoteDetails: QuoteDetails,
    val startDate: LocalDate?,
    val expiresAt: LocalDate,
    val email: String?,
    val dataCollectionId: UUID?
) {
    val typeOfContract: TypeOfContract
        get() = quoteDetails.typeOfContract
}
