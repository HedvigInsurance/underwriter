package com.hedvig.underwriter.service

import com.hedvig.underwriter.model.InsuranceCompany
import com.neovisionaries.i18n.CountryCode
import org.springframework.stereotype.Service

@Service
class InsuranceCompanyServiceImpl : InsuranceCompanyService {
    override fun getInsuranceCompanies(countryCode: CountryCode): Set<InsuranceCompany> =
        InsuranceCompany.insurersByCountry[countryCode] ?: emptySet()
}
