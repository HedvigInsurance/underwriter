package com.hedvig.underwriter.model

import com.neovisionaries.i18n.CountryCode

data class InsuranceCompany(
    val id: String,
    val displayName: String,
    val switchable: Boolean
) {
    companion object {

        private val other = InsuranceCompany("other", "Other", false)

        val insurersByCountryCode = mapOf(
            CountryCode.SE to setOf(
                InsuranceCompany("if", "If", false),
                InsuranceCompany("Folksam", "Folksam", true),
                InsuranceCompany("Trygg-Hansa", "Trygg-Hansa", true),
                InsuranceCompany("Länsförsäkringar", "Länsförsäkringar", false),
                InsuranceCompany("Länsförsäkringar Stockholm", "Länsförsäkringar Stockholm", true),
                InsuranceCompany("Moderna", "Moderna", false),
                InsuranceCompany("Gjensidige", "Gjensidige", false),
                InsuranceCompany("Vardia", "Vardia", false),
                InsuranceCompany("Tre Kronor", "Tre Kronor", true),
                InsuranceCompany("ICA", "Ica", true),
                InsuranceCompany("Dina Försäkringar", "Dina Försäkringar", false),
                InsuranceCompany("Aktsam", "Aktsam", true),
                other
            ),
            CountryCode.NO to setOf(
                InsuranceCompany("If NO", "If", true),
                InsuranceCompany("Fremtind", "Fremtind", true),
                InsuranceCompany("Gjensidige NO", "Gjensidige", true),
                InsuranceCompany("Tryg", "Tryg", true),
                InsuranceCompany("Eika", "Eika", true),
                InsuranceCompany("Frende", "Frende", true),
                InsuranceCompany("Storebrand", "Storebrand", true),
                InsuranceCompany("Codan", "Codan", true),
                other
            )
        )

        val allInsurers = insurersByCountryCode.values.flatten().toSet().associateBy { it.id }
    }
}
