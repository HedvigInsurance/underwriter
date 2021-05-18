package com.hedvig.underwriter.service

import com.hedvig.underwriter.model.InsuranceCompany
import com.neovisionaries.i18n.CountryCode

interface InsuranceCompanyService {
    fun getInsuranceCompanies(countryCode: CountryCode): Set<InsuranceCompany>
}
