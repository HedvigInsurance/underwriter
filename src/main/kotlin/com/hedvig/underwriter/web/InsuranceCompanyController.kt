package com.hedvig.underwriter.web

import com.hedvig.libs.logging.calls.LogCall
import com.hedvig.underwriter.service.InsuranceCompanyService
import com.hedvig.underwriter.web.dtos.InsuranceCompanyDto
import com.hedvig.underwriter.web.dtos.toDto
import com.neovisionaries.i18n.CountryCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/insurance-companies")
class InsuranceCompanyController(
    val insuranceCompanyService: InsuranceCompanyService
) {

    @GetMapping
    @LogCall
    fun getInsuranceCompanies(@RequestParam countryCode: CountryCode): ResponseEntity<List<InsuranceCompanyDto>> {
        return ResponseEntity.ok(insuranceCompanyService.getInsuranceCompaniesByCountryCode(countryCode).map { it.toDto() })
    }

}
