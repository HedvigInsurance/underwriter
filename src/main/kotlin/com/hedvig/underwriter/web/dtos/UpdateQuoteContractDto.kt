package com.hedvig.underwriter.web.dtos

import java.util.UUID

data class UpdateQuoteContractDto(
    val contractId: UUID,
    val agreementId: UUID
)
