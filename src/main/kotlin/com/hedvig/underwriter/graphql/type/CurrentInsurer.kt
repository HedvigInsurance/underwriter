package com.hedvig.underwriter.graphql.type

import com.hedvig.underwriter.model.InsuranceCompany

data class CurrentInsurer(
    val id: String,
    val displayName: String,
    val switchable: Boolean
) {
    companion object {
        fun from(insuranceCompany: InsuranceCompany) = CurrentInsurer(
            id = insuranceCompany.id,
            displayName = insuranceCompany.displayName,
            switchable = insuranceCompany.switchable
        )

        fun create(id: String) = InsuranceCompany.allInsurers[id]?.let { from(it) }
            ?: throw IllegalArgumentException("Unknown id when creating CurrentInsurer (id=$id)")
    }
}
