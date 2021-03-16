package com.hedvig.underwriter.service

import arrow.core.Either
import com.hedvig.underwriter.model.ApartmentProductSubType
import com.hedvig.underwriter.model.Market
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.service.guidelines.AgeRestrictionGuideline
import com.hedvig.underwriter.service.guidelines.NorwegianHomeContentsLivingSpaceNotMoreThan250Sqm
import com.hedvig.underwriter.service.guidelines.NorwegianHomeContentscoInsuredNotMoreThan5
import com.hedvig.underwriter.service.guidelines.NorwegianSsnNotMatchesBirthDate
import com.hedvig.underwriter.service.guidelines.NorwegianTravelCoInsuredNotMoreThan5
import com.hedvig.underwriter.service.guidelines.NorwegianYouthHomeContentsAgeNotMoreThan30Years
import com.hedvig.underwriter.service.guidelines.NorwegianYouthHomeContentsCoInsuredNotMoreThan0
import com.hedvig.underwriter.service.guidelines.NorwegianYouthHomeContentsLivingSpaceNotMoreThan50Sqm
import com.hedvig.underwriter.service.guidelines.NorwegianYouthTravelAgeNotMoreThan30Years
import com.hedvig.underwriter.service.guidelines.NorwegianYouthTravelCoInsuredNotMoreThan0
import com.hedvig.underwriter.service.guidelines.PersonalDebt
import com.hedvig.underwriter.service.guidelines.SocialSecurityNumberFormat
import com.hedvig.underwriter.service.guidelines.SwedishApartmentHouseHoldSizeAtLeast1
import com.hedvig.underwriter.service.guidelines.SwedishApartmentHouseHoldSizeNotMoreThan6
import com.hedvig.underwriter.service.guidelines.SwedishApartmentLivingSpaceAtLeast1Sqm
import com.hedvig.underwriter.service.guidelines.SwedishApartmentLivingSpaceNotMoreThan250Sqm
import com.hedvig.underwriter.service.guidelines.SwedishHouseExtraBuildingsSizeAtLeast1Sqm
import com.hedvig.underwriter.service.guidelines.SwedishHouseExtraBuildingsSizeNotOverThan75Sqm
import com.hedvig.underwriter.service.guidelines.SwedishHouseHouseholdSizeAtLeast1
import com.hedvig.underwriter.service.guidelines.SwedishHouseHouseholdSizeNotMoreThan6
import com.hedvig.underwriter.service.guidelines.SwedishHouseLivingSpaceAtLeast1Sqm
import com.hedvig.underwriter.service.guidelines.SwedishHouseLivingSpaceNotMoreThan250Sqm
import com.hedvig.underwriter.service.guidelines.SwedishHouseNumberOfBathrooms
import com.hedvig.underwriter.service.guidelines.SwedishHouseNumberOfExtraBuildingsWithAreaOverSixSqm
import com.hedvig.underwriter.service.guidelines.SwedishHouseYearOfConstruction
import com.hedvig.underwriter.service.guidelines.SwedishStudentApartmentAgeNotMoreThan30Years
import com.hedvig.underwriter.service.guidelines.SwedishStudentApartmentHouseholdSizeNotMoreThan2
import com.hedvig.underwriter.service.guidelines.SwedishStudentApartmentLivingSpaceNotMoreThan50Sqm
import com.hedvig.underwriter.service.quoteStrategies.QuoteStrategyService
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineService
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.testhelp.databuilder.DanishHomeContentsQuoteRequestBuilder
import com.hedvig.underwriter.testhelp.databuilder.NorwegianHomeContentsQuoteRequestBuilder
import com.hedvig.underwriter.testhelp.databuilder.NorwegianHomeContentsQuoteRequestDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.NorwegianTravelQuoteRequestBuilder
import com.hedvig.underwriter.testhelp.databuilder.NorwegianTravelQuoteRequestDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.SwedishApartmentQuoteRequestBuilder
import com.hedvig.underwriter.testhelp.databuilder.SwedishApartmentQuoteRequestDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.SwedishHouseQuoteRequestBuilder
import com.hedvig.underwriter.testhelp.databuilder.SwedishHouseQuoteRequestDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.SwedishHouseQuoteRequestDataExtraBuildingsBuilder
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.javamoney.moneta.Money
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@RunWith(SpringRunner::class)
class UnderwriterImplTest {

    @MockkBean
    lateinit var debtChecker: DebtChecker

    @MockkBean
    lateinit var priceEngineService: PriceEngineService

    @MockkBean(relaxed = true)
    lateinit var metrics: Metrics

    @Before
    fun setup() {
        every {
            priceEngineService.querySwedishApartmentPrice(
                any()
            )
        } returns
            PriceQueryResponse(
                UUID.randomUUID(),
                Money.of(BigDecimal.ONE, "SEK")
            )

        every {
            priceEngineService.querySwedishHousePrice(
                any()
            )
        } returns
            PriceQueryResponse(
                UUID.randomUUID(),
                Money.of(BigDecimal.ONE, "SEK")
            )
    }

    @Test
    fun successfullyCreatesSwedishApartmentQuote() {

        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = SwedishApartmentQuoteRequestBuilder().build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun successfullyCreatesSwedishStudentApartmentQuote() {
        val cut = UnderwriterImpl(
            priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics
        )
        val quoteRequest = SwedishApartmentQuoteRequestBuilder(
            ssn = "200112031356",
            data = SwedishApartmentQuoteRequestDataBuilder(
                subType = ApartmentProductSubType.STUDENT_BRF,
                livingSpace = 50,
                householdSize = 2
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun successfullyCreatesSwedishHouseQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = SwedishHouseQuoteRequestBuilder().build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun successfullyCreatesNorwegianHomeContentsQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = NorwegianHomeContentsQuoteRequestBuilder().build()
        val quoteId = UUID.randomUUID()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every { priceEngineService.queryNorwegianHomeContentPrice(any()) } returns PriceQueryResponse(
            quoteId,
            Money.of(BigDecimal.ONE, "NOK")
        )

        val result = cut.createQuote(quoteRequest, quoteId, QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun successfullyCreatesNorwegianTravelQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = NorwegianTravelQuoteRequestBuilder().build()
        val quoteId = UUID.randomUUID()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every { priceEngineService.queryNorwegianTravelPrice(any()) } returns PriceQueryResponse(
            quoteId,
            Money.of(BigDecimal.ONE, "NOK")
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun underwritingGuidelineHitInvalidSwedishSsn() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = SwedishApartmentQuoteRequestBuilder(ssn = "invalid", birthDate = LocalDate.of(1912, 12, 12)).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(listOf(SocialSecurityNumberFormat.breachedGuideline))
        verify(exactly = 1) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitPersonalDebtCheckSwedishQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = SwedishApartmentQuoteRequestBuilder().build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf("RED")

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(listOf(PersonalDebt(debtChecker).breachedGuideline))
        verify(exactly = 1) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitAgeOnCreatesSwedishApartmentQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = SwedishApartmentQuoteRequestBuilder(ssn = "202001010000").build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(listOf(AgeRestrictionGuideline.breachedGuideline))
        verify(exactly = 1) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllLowerApartmentRulesOnCreatesSwedishApartmentQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = SwedishApartmentQuoteRequestBuilder(
            data = SwedishApartmentQuoteRequestDataBuilder(
                householdSize = 0,
                livingSpace = 0
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                SwedishApartmentHouseHoldSizeAtLeast1.breachedGuideline,
                SwedishApartmentLivingSpaceAtLeast1Sqm.breachedGuideline
            )
        )
        verify(exactly = 2) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllUpperApartmentRulesOnCreatesSwedishApartmentQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = SwedishApartmentQuoteRequestBuilder(
            data = SwedishApartmentQuoteRequestDataBuilder(
                householdSize = 7,
                livingSpace = 251
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                SwedishApartmentHouseHoldSizeNotMoreThan6.breachedGuideline,
                SwedishApartmentLivingSpaceNotMoreThan250Sqm.breachedGuideline
            )
        )
        verify(exactly = 2) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllLowerStudentApartmentRulesOnCreatesSwedishStudentApartmentQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = SwedishApartmentQuoteRequestBuilder(
            ssn = "200112031356",
            data = SwedishApartmentQuoteRequestDataBuilder(
                subType = ApartmentProductSubType.STUDENT_BRF,
                livingSpace = 0,
                householdSize = 0
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                SwedishApartmentHouseHoldSizeAtLeast1.breachedGuideline,
                SwedishApartmentLivingSpaceAtLeast1Sqm.breachedGuideline
            )
        )
        verify(exactly = 2) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllUpperStudentApartmentRulesOnCreatesSwedishStudentApartmentQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = SwedishApartmentQuoteRequestBuilder(
            ssn = "198812031356",
            data = SwedishApartmentQuoteRequestDataBuilder(
                subType = ApartmentProductSubType.STUDENT_BRF,
                livingSpace = 51,
                householdSize = 3
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                SwedishStudentApartmentHouseholdSizeNotMoreThan2.breachedGuideline,
                SwedishStudentApartmentLivingSpaceNotMoreThan50Sqm.breachedGuideline,
                SwedishStudentApartmentAgeNotMoreThan30Years.breachedGuideline
            )
        )
        verify(exactly = 3) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllLowerHouseRulesOnCreatesSwedishHouseQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = SwedishHouseQuoteRequestBuilder(
            data = SwedishHouseQuoteRequestDataBuilder(
                householdSize = 0, livingSpace = 0, yearOfConstruction = 1924,
                extraBuildings = listOf(
                    SwedishHouseQuoteRequestDataExtraBuildingsBuilder(area = 0).build()
                )
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                SwedishHouseHouseholdSizeAtLeast1.breachedGuideline,
                SwedishHouseLivingSpaceAtLeast1Sqm.breachedGuideline,
                SwedishHouseYearOfConstruction.breachedGuideline,
                SwedishHouseExtraBuildingsSizeAtLeast1Sqm.breachedGuideline
            )
        )
        verify(exactly = 4) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllUpperHouseRulesOnCreatesSwedishHouseQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = SwedishHouseQuoteRequestBuilder(
            data = SwedishHouseQuoteRequestDataBuilder(
                householdSize = 7, livingSpace = 251, numberOfBathrooms = 3,
                extraBuildings = listOf(
                    SwedishHouseQuoteRequestDataExtraBuildingsBuilder(area = 7).build(),
                    SwedishHouseQuoteRequestDataExtraBuildingsBuilder(area = 7).build(),
                    SwedishHouseQuoteRequestDataExtraBuildingsBuilder(area = 7).build(),
                    SwedishHouseQuoteRequestDataExtraBuildingsBuilder(area = 7).build(),
                    SwedishHouseQuoteRequestDataExtraBuildingsBuilder(area = 76).build()
                )
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                SwedishHouseHouseholdSizeNotMoreThan6.breachedGuideline,
                SwedishHouseLivingSpaceNotMoreThan250Sqm.breachedGuideline,
                SwedishHouseNumberOfBathrooms.breachedGuideline,
                SwedishHouseNumberOfExtraBuildingsWithAreaOverSixSqm.breachedGuideline,
                SwedishHouseExtraBuildingsSizeNotOverThan75Sqm.breachedGuideline
            )
        )
        verify(exactly = 5) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun successfullyCreateNorwegianHomeContentsQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = NorwegianHomeContentsQuoteRequestBuilder().build()

        every { priceEngineService.queryNorwegianHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "NOK")
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun underwritingGuidelineHitWhenNorwegianSsnNotMatch() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = NorwegianHomeContentsQuoteRequestBuilder(
            ssn = "24057408215"
        ).build()

        every { priceEngineService.queryNorwegianHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "NOK")
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                NorwegianSsnNotMatchesBirthDate.breachedGuideline
            )
        )
        verify(exactly = 1) { metrics.increment(Market.NORWAY, any()) }
    }

    @Test
    fun successfullyCreateNorwegianHomeContentsQuoteWhenSsnIsNull() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = NorwegianHomeContentsQuoteRequestBuilder(
            ssn = null
        ).build()

        every { priceEngineService.queryNorwegianHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "NOK")
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun successfullyCreateNorwegianHomeTravelQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = NorwegianTravelQuoteRequestBuilder().build()

        every { priceEngineService.queryNorwegianTravelPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "NOK")
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun underwritingGuidelineHitAllUpperApartmentRulesOnCreatesNorwegianHomeContentsQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = NorwegianHomeContentsQuoteRequestBuilder(
            data = NorwegianHomeContentsQuoteRequestDataBuilder(
                coInsured = 6,
                livingSpace = 251
            )
        ).build()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                NorwegianHomeContentscoInsuredNotMoreThan5.breachedGuideline,
                NorwegianHomeContentsLivingSpaceNotMoreThan250Sqm.breachedGuideline
            )
        )
        verify(exactly = 2) { metrics.increment(Market.NORWAY, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllUpperApartmentRulesOnCreatesNorwegianHomeContentsYouthQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = NorwegianHomeContentsQuoteRequestBuilder(
            ssn = "28026400734",
            birthDate = LocalDate.of(1964, 2, 28),
            data = NorwegianHomeContentsQuoteRequestDataBuilder(
                coInsured = 3,
                livingSpace = 51,
                isYouth = true
            )
        ).build()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                NorwegianYouthHomeContentsLivingSpaceNotMoreThan50Sqm.breachedGuideline,
                NorwegianYouthHomeContentsAgeNotMoreThan30Years.breachedGuideline,
                NorwegianYouthHomeContentsCoInsuredNotMoreThan0.breachedGuideline
            )
        )
        verify(exactly = 3) { metrics.increment(Market.NORWAY, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllUpperApartmentRulesOnCreatesNorwegianTravelQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = NorwegianTravelQuoteRequestBuilder(
            data = NorwegianTravelQuoteRequestDataBuilder(
                coInsured = 6
            )
        ).build()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                NorwegianTravelCoInsuredNotMoreThan5.breachedGuideline
            )
        )
        verify(exactly = 1) { metrics.increment(Market.NORWAY, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllUpperApartmentRulesOnCreatesNorwegianTravelYouthQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = NorwegianTravelQuoteRequestBuilder(
            birthDate = LocalDate.now().minusYears(31).minusDays(1),
            data = NorwegianTravelQuoteRequestDataBuilder(
                coInsured = 1,
                isYouth = true
            )
        ).build()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                NorwegianSsnNotMatchesBirthDate.breachedGuideline,
                NorwegianYouthTravelAgeNotMoreThan30Years.breachedGuideline,
                NorwegianYouthTravelCoInsuredNotMoreThan0.breachedGuideline
            )
        )
        verify(exactly = 3) { metrics.increment(Market.NORWAY, any()) }
    }

    @Test
    fun successfullyCreatesDanishHomeContentsQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = DanishHomeContentsQuoteRequestBuilder().build()
        val quoteId = UUID.randomUUID()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        /* TODO: This should be verified once price engine is in place
        every { priceEngineService.queryDanishHomeContentPrice(any()) } returns PriceQueryResponse(
            quoteId,
            Money.of(BigDecimal.ONE, "NOK")
        )*/

        val result = cut.createQuote(quoteRequest, quoteId, QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun `on breached guideline verify increment counter`() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), metrics)
        val quoteRequest = NorwegianTravelQuoteRequestBuilder(
            birthDate = LocalDate.now().minusYears(31).minusDays(1),
            data = NorwegianTravelQuoteRequestDataBuilder(
                coInsured = 1
            )
        ).build()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).hasSize(1)
        verify(exactly = 1) { metrics.increment(Market.NORWAY, NorwegianSsnNotMatchesBirthDate.breachedGuideline) }
    }
}
