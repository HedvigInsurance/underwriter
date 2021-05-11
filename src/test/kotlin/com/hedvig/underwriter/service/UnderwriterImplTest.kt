package com.hedvig.underwriter.service

import arrow.core.Either
import com.hedvig.underwriter.model.ApartmentProductSubType
import com.hedvig.underwriter.model.Market
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.service.guidelines.BreachedGuidelinesCodes
import com.hedvig.underwriter.service.quoteStrategies.QuoteStrategyService
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineService
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.testhelp.databuilder.DanishAccidentQuoteRequestBuilder
import com.hedvig.underwriter.testhelp.databuilder.DanishAccidentQuoteRequestDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.DanishHomeContentsQuoteRequestBuilder
import com.hedvig.underwriter.testhelp.databuilder.DanishHomeContentsQuoteRequestDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.DanishTravelQuoteRequestBuilder
import com.hedvig.underwriter.testhelp.databuilder.DanishTravelQuoteRequestDataBuilder
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
    lateinit var metrics: UnderwriterImpl.BreachedGuidelinesCounter

    val requotingService = RequotingServiceImpl(mockk(relaxed = true), mockk())

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

        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = SwedishApartmentQuoteRequestBuilder().build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun successfullyCreatesSwedishStudentApartmentQuote() {
        val cut = UnderwriterImpl(
            priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics
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
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = SwedishHouseQuoteRequestBuilder().build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun successfullyCreatesNorwegianHomeContentsQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = SwedishApartmentQuoteRequestBuilder(ssn = "invalid", birthDate = LocalDate.of(1912, 12, 12)).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(listOf(BreachedGuidelinesCodes.INVALID_SSN_LENGTH))
        verify(exactly = 1) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitPersonalDebtCheckSwedishQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = SwedishApartmentQuoteRequestBuilder().build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf("RED")

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(listOf(BreachedGuidelinesCodes.DEBT_CHECK))
        verify(exactly = 1) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitAgeOnCreatesSwedishApartmentQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = SwedishApartmentQuoteRequestBuilder(ssn = "202001010000").build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(listOf(BreachedGuidelinesCodes.UNDERAGE))
        verify(exactly = 1) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllLowerApartmentRulesOnCreatesSwedishApartmentQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
                BreachedGuidelinesCodes.TOO_SMALL_NUMBER_OF_HOUSE_HOLD_SIZE,
                BreachedGuidelinesCodes.TOO_SMALL_LIVING_SPACE
            )
        )
        verify(exactly = 2) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllUpperApartmentRulesOnCreatesSwedishApartmentQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
                BreachedGuidelinesCodes.TOO_HIGH_NUMBER_OF_HOUSE_HOLD_SIZE,
                BreachedGuidelinesCodes.TOO_MUCH_LIVING_SPACE
            )
        )
        verify(exactly = 2) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllLowerStudentApartmentRulesOnCreatesSwedishStudentApartmentQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
                BreachedGuidelinesCodes.TOO_SMALL_NUMBER_OF_HOUSE_HOLD_SIZE,
                BreachedGuidelinesCodes.TOO_SMALL_LIVING_SPACE
            )
        )
        verify(exactly = 2) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllUpperStudentApartmentRulesOnCreatesSwedishStudentApartmentQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
                BreachedGuidelinesCodes.STUDENT_TOO_BIG_HOUSE_HOLD_SIZE,
                BreachedGuidelinesCodes.STUDENT_TOO_MUCH_LIVING_SPACE,
                BreachedGuidelinesCodes.STUDENT_OVERAGE
            )
        )
        verify(exactly = 3) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllLowerHouseRulesOnCreatesSwedishHouseQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
                BreachedGuidelinesCodes.TOO_SMALL_NUMBER_OF_HOUSE_HOLD_SIZE,
                BreachedGuidelinesCodes.TOO_SMALL_LIVING_SPACE,
                BreachedGuidelinesCodes.TOO_EARLY_YEAR_OF_CONSTRUCTION,
                BreachedGuidelinesCodes.TOO_SMALL_EXTRA_BUILDING_SIZE
            )
        )
        verify(exactly = 4) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllUpperHouseRulesOnCreatesSwedishHouseQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
                BreachedGuidelinesCodes.TOO_HIGH_NUMBER_OF_HOUSE_HOLD_SIZE,
                BreachedGuidelinesCodes.TOO_MUCH_LIVING_SPACE,
                BreachedGuidelinesCodes.TOO_MANY_BATHROOMS,
                BreachedGuidelinesCodes.TOO_MANY_EXTRA_BUILDINGS,
                BreachedGuidelinesCodes.TOO_BIG_EXTRA_BUILDING_SIZE
            )
        )
        verify(exactly = 5) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun underwritingGuidelineHitTOO_MUCH_LIVING_SPACEOnCreatesSwedishHouseQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = SwedishHouseQuoteRequestBuilder(
            data = SwedishHouseQuoteRequestDataBuilder(
                householdSize = 1,
                numberOfBathrooms = 1,
                ancillaryArea = 100,
                livingSpace = 201,
                extraBuildings = listOf(
                    SwedishHouseQuoteRequestDataExtraBuildingsBuilder(area = 7).build()
                )
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.TOO_MUCH_LIVING_SPACE
            )
        )
        verify(exactly = 1) { metrics.increment(Market.SWEDEN, any()) }
    }

    @Test
    fun successfullyCreateNorwegianHomeContentsQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
                BreachedGuidelinesCodes.SSN_DOES_NOT_MATCH_BIRTH_DATE
            )
        )
        verify(exactly = 1) { metrics.increment(Market.NORWAY, any()) }
    }

    @Test
    fun successfullyCreateNorwegianHomeContentsQuoteWhenSsnIsNull() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
                BreachedGuidelinesCodes.TOO_HIGH_NUMBER_OF_CO_INSURED,
                BreachedGuidelinesCodes.TOO_MUCH_LIVING_SPACE
            )
        )
        verify(exactly = 2) { metrics.increment(Market.NORWAY, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllUpperApartmentRulesOnCreatesNorwegianHomeContentsYouthQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
                BreachedGuidelinesCodes.YOUTH_TOO_HIGH_NUMBER_OF_CO_INSURED,
                BreachedGuidelinesCodes.YOUTH_TOO_MUCH_LIVING_SPACE,
                BreachedGuidelinesCodes.YOUTH_OVERAGE
            )
        )
        verify(exactly = 3) { metrics.increment(Market.NORWAY, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllUpperApartmentRulesOnCreatesNorwegianTravelQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = NorwegianTravelQuoteRequestBuilder(
            data = NorwegianTravelQuoteRequestDataBuilder(
                coInsured = 6
            )
        ).build()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.TOO_HIGH_NUMBER_OF_CO_INSURED
            )
        )
        verify(exactly = 1) { metrics.increment(Market.NORWAY, any()) }
    }

    @Test
    fun underwritingGuidelineHitAllUpperApartmentRulesOnCreatesNorwegianTravelYouthQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
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
                BreachedGuidelinesCodes.SSN_DOES_NOT_MATCH_BIRTH_DATE,
                BreachedGuidelinesCodes.YOUTH_TOO_HIGH_NUMBER_OF_CO_INSURED,
                BreachedGuidelinesCodes.YOUTH_OVERAGE
            )
        )
        verify(exactly = 3) { metrics.increment(Market.NORWAY, any()) }
    }

    @Test
    fun `on breached guideline verify increment counter`() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = NorwegianTravelQuoteRequestBuilder(
            birthDate = LocalDate.now().minusYears(31).minusDays(1),
            data = NorwegianTravelQuoteRequestDataBuilder(
                coInsured = 1
            )
        ).build()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).hasSize(1)
        verify(exactly = 1) { metrics.increment(Market.NORWAY, BreachedGuidelinesCodes.SSN_DOES_NOT_MATCH_BIRTH_DATE) }
    }

    @Test
    fun successfullyCreatesDanishHomeContentsQuote() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = DanishHomeContentsQuoteRequestBuilder().build()
        val quoteId = UUID.randomUUID()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        every { priceEngineService.queryDanishHomeContentPrice(any()) } returns PriceQueryResponse(
            quoteId,
            Money.of(BigDecimal.ONE, "NOK")
        )

        val result = cut.createQuote(quoteRequest, quoteId, QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun underwritingGuidelineHitWhenDanishSsnNotMatch() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = DanishHomeContentsQuoteRequestBuilder(
            ssn = "0411357627",
            birthDate = LocalDate.of(1964, 11, 4)
        ).build()

        /* TODO: This should be verified once price engine is in place
        every { priceEngineService.queryDanishHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "DKK")
        )*/

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.SSN_DOES_NOT_MATCH_BIRTH_DATE
            )
        )
        verify(exactly = 1) { metrics.increment(Market.DENMARK, any()) }
    }

    @Test
    fun underwritingGuidelineHitWhenDanishUnderage() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = DanishHomeContentsQuoteRequestBuilder(
            ssn = "1110137970",
            birthDate = LocalDate.of(2013, 1, 11)
        ).build()

        /* TODO: This should be verified once price engine is in place
        every { priceEngineService.queryDanishHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "DKK")
        )*/

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.UNDERAGE
            )
        )
        verify(exactly = 1) { metrics.increment(Market.DENMARK, any()) }
    }

    @Test
    fun underwritingGuidelineHitWhenDanishInvalidSsn() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = DanishHomeContentsQuoteRequestBuilder(
            ssn = "04113576261234",
            birthDate = LocalDate.of(1935, 11, 4)
        ).build()

        /* TODO: This should be verified once price engine is in place
        every { priceEngineService.queryDanishHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "DKK")
        )*/

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.INVALID_SSN
            )
        )
        verify(exactly = 1) { metrics.increment(Market.DENMARK, any()) }
    }

    @Test
    fun underwritingGuidelineHitWhenDanishHomeContentCoInsuredToLow() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = DanishHomeContentsQuoteRequestBuilder(
            data = DanishHomeContentsQuoteRequestDataBuilder(
                coInsured = -1
            )
        ).build()

        /* TODO: This should be verified once price engine is in place
        every { priceEngineService.queryDanishHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "DKK")
        )*/

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.NEGATIVE_NUMBER_OF_CO_INSURED
            )
        )
        verify(exactly = 1) { metrics.increment(Market.DENMARK, any()) }
    }

    @Test
    fun underwritingGuidelineHitWhenDanishHomeContentStudentCoInsuredAndAgeTooHigh() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = DanishHomeContentsQuoteRequestBuilder(
            data = DanishHomeContentsQuoteRequestDataBuilder(
                isStudent = true,
                coInsured = 2
            )
        ).build()

        /* TODO: This should be verified once price engine is in place
        every { priceEngineService.queryDanishHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "DKK")
        )*/

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.TOO_HIGH_NUMBER_OF_CO_INSURED,
                BreachedGuidelinesCodes.STUDENT_OVERAGE
            )
        )
        verify(exactly = 2) { metrics.increment(Market.DENMARK, any()) }
    }

    @Test
    fun underwritingGuidelineHitWhenDanishHomeContentCoInsuredRegularToHigh() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = DanishHomeContentsQuoteRequestBuilder(
            data = DanishHomeContentsQuoteRequestDataBuilder(
                coInsured = 7
            )
        ).build()

        /* TODO: This should be verified once price engine is in place
        every { priceEngineService.queryDanishHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "DKK")
        )*/

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.TOO_HIGH_NUMBER_OF_CO_INSURED
            )
        )
        verify(exactly = 1) { metrics.increment(Market.DENMARK, any()) }
    }

    @Test
    fun underwritingGuidelineHitWhenDanishHomeContentLivingSpaceTooLow() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = DanishHomeContentsQuoteRequestBuilder(
            data = DanishHomeContentsQuoteRequestDataBuilder(
                livingSpace = 4
            )
        ).build()

        /* TODO: This should be verified once price engine is in place
        every { priceEngineService.queryDanishHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "DKK")
        )*/

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.TOO_SMALL_LIVING_SPACE
            )
        )
        verify(exactly = 1) { metrics.increment(Market.DENMARK, any()) }
    }

    @Test
    fun underwritingGuidelineHitWhenDanishHomeContentStudentLivingSpaceAndAgeTooHigh() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = DanishHomeContentsQuoteRequestBuilder(
            data = DanishHomeContentsQuoteRequestDataBuilder(
                isStudent = true,
                livingSpace = 101
            )
        ).build()

        /* TODO: This should be verified once price engine is in place
        every { priceEngineService.queryDanishHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "DKK")
        )*/

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.STUDENT_TOO_MUCH_LIVING_SPACE,
                BreachedGuidelinesCodes.STUDENT_OVERAGE
            )
        )
        verify(exactly = 2) { metrics.increment(Market.DENMARK, any()) }
    }

    @Test
    fun underwritingGuidelineHitWhenDanishHomeContentLivingSpaceRegularTooHigh() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = DanishHomeContentsQuoteRequestBuilder(
            data = DanishHomeContentsQuoteRequestDataBuilder(
                livingSpace = 251
            )
        ).build()

        /* TODO: This should be verified once price engine is in place
        every { priceEngineService.queryDanishHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "DKK")
        )*/

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.TOO_MUCH_LIVING_SPACE
            )
        )
        verify(exactly = 1) { metrics.increment(Market.DENMARK, any()) }
    }

    @Test
    fun underwritingGuidelineHitWhenDanishAccidentStudentCoInsuredAndAgeToHigh() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = DanishAccidentQuoteRequestBuilder(
            data = DanishAccidentQuoteRequestDataBuilder(
                isStudent = true,
                coInsured = 2
            )
        ).build()

        /* TODO: This should be verified once price engine is in place
        every { priceEngineService.queryDanishAccidentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "DKK")
        )*/

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.TOO_HIGH_NUMBER_OF_CO_INSURED,
                BreachedGuidelinesCodes.STUDENT_OVERAGE
            )
        )
        verify(exactly = 2) { metrics.increment(Market.DENMARK, any()) }
    }

    @Test
    fun underwritingGuidelineHitWhenDanishRegularAccidentCoInsuredTooHigh() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = DanishAccidentQuoteRequestBuilder(
            data = DanishAccidentQuoteRequestDataBuilder(
                coInsured = 7
            )
        ).build()

        /* TODO: This should be verified once price engine is in place
        every { priceEngineService.queryDanishAccidentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "DKK")
        )*/

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.TOO_HIGH_NUMBER_OF_CO_INSURED
            )
        )
        verify(exactly = 1) { metrics.increment(Market.DENMARK, any()) }
    }

    @Test
    fun underwritingGuidelineHitWhenDanishTravelStudentCoInsuredAndAgeToHigh() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = DanishTravelQuoteRequestBuilder(
            data = DanishTravelQuoteRequestDataBuilder(
                isStudent = true,
                coInsured = 2
            )
        ).build()

        /* TODO: This should be verified once price engine is in place
        every { priceEngineService.queryDanishAccidentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "DKK")
        )*/

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.TOO_HIGH_NUMBER_OF_CO_INSURED,
                BreachedGuidelinesCodes.STUDENT_OVERAGE
            )
        )
        verify(exactly = 2) { metrics.increment(Market.DENMARK, any()) }
    }

    @Test
    fun underwritingGuidelineHitWhenDanishRegularTravelCoInsuredTooHigh() {
        val cut = UnderwriterImpl(priceEngineService, QuoteStrategyService(debtChecker, mockk()), requotingService, mockk(), metrics)
        val quoteRequest = DanishTravelQuoteRequestBuilder(
            data = DanishTravelQuoteRequestDataBuilder(
                coInsured = 7
            )
        ).build()

        /* TODO: This should be verified once price engine is in place
        every { priceEngineService.queryDanishAccidentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "DKK")
        )*/

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                BreachedGuidelinesCodes.TOO_HIGH_NUMBER_OF_CO_INSURED
            )
        )
        verify(exactly = 1) { metrics.increment(Market.DENMARK, any()) }
    }
}
