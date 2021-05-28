package com.hedvig.underwriter.graphql.type

import com.hedvig.graphql.commons.type.MonetaryAmountV2
import com.hedvig.libs.logging.masking.Masked
import java.time.LocalDate
import java.util.UUID

data class BundledQuote(
    val id: UUID,
    @Masked val firstName: String,
    @Masked val lastName: String,
    val currentInsurer: CurrentInsurer?,
    @Masked val ssn: String?,
    val birthDate: LocalDate,
    val price: MonetaryAmountV2,
    val quoteDetails: QuoteDetails,
    val startDate: LocalDate?,
    val expiresAt: LocalDate,
    @Masked val email: String?,
    val dataCollectionId: UUID?,
    val initiatedFrom: String
) {
    val typeOfContract: ContractAgreementType
        get() = quoteDetails.typeOfContract
}
