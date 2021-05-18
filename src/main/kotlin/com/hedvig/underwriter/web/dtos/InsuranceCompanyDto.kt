package com.hedvig.underwriter.web.dtos

import com.hedvig.underwriter.model.InsuranceCompany

data class InsuranceCompanyDto(
    val id: String,
    val displayName: String,
    val switchable: Boolean
)

fun InsuranceCompany.toDto(): InsuranceCompanyDto = InsuranceCompanyDto(id, displayName, switchable)
