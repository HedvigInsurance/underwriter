package com.hedvig.underwriter.service

import com.hedvig.underwriter.model.InsuranceCompany
import com.neovisionaries.i18n.CountryCode
import org.junit.Before
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class InsuranceCompanyServiceImplTest {

    lateinit var service: InsuranceCompanyService

    @Before
    fun setUp() {
        service = InsuranceCompanyServiceImpl()
    }

    @Test
    fun `validate insurer ids`() {
        val insurerIds = mapOf(
            CountryCode.SE to setOf(
                "if",
                "Folksam",
                "Trygg-Hansa",
                "Länsförsäkringar",
                "Länsförsäkringar Stockholm",
                "Moderna",
                "Gjensidige",
                "Vardia",
                "Tre Kronor",
                "ICA",
                "Dina Försäkringar",
                "Aktsam",
                "other"
            ),
            CountryCode.NO to setOf(
                "If NO",
                "Fremtind",
                "Gjensidige NO",
                "Tryg",
                "Eika",
                "Frende",
                "Storebrand",
                "Codan",
                "other"
            )
        )

        InsuranceCompany.insurersByCountry.forEach {
            assertEquals(insurerIds[it.key]!!.toList(), it.value.map { it.id })
        }
    }

    @Test
    fun `validate that other is not switchable`() {
        assertNotNull(InsuranceCompany.allInsurers["other"])
        assertEquals(false, InsuranceCompany.allInsurers["other"]!!.switchable)
    }
}
