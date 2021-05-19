package com.hedvig.underwriter.service

import com.hedvig.underwriter.model.InsuranceCompany
import com.neovisionaries.i18n.CountryCode
import org.springframework.stereotype.Service

@Service
class InsuranceCompanyServiceImpl : InsuranceCompanyService {
    override fun getInsuranceCompaniesByCountryCode(countryCode: CountryCode): Set<InsuranceCompany> =
        InsuranceCompany.insurersByCountryCode[countryCode] ?: emptySet()
}
