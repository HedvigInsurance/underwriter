package com.hedvig.underwriter.serviceIntegration.productPricing.dtos

import java.util.UUID

data class SelfChangeResult(
    val updatedContracts: List<ContractChange>,
    val createdContracts: List<ContractChange>,
    val terminatedContractIds: List<UUID>
) {
    data class ContractChange(
        val quoteId: UUID,
        val agreementId: UUID,
        val contractId: UUID
    )
}
