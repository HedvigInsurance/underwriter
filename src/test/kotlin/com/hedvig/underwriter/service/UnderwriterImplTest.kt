package com.hedvig.underwriter.service

import arrow.core.Either
import com.hedvig.underwriter.model.ApartmentProductSubType
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.service.guidelines.AgeRestrictionGuideline
import com.hedvig.underwriter.service.guidelines.NorwegianHomeContentsLivingSpaceNotMoreThan250Sqm
import com.hedvig.underwriter.service.guidelines.NorwegianHomeContentscoInsuredNotMoreThan5
import com.hedvig.underwriter.service.guidelines.NorwegianSsnNotMatchesBirthDate
import com.hedvig.underwriter.service.guidelines.NorwegianTravelCoInsuredNotMoreThan5
import com.hedvig.underwriter.service.guidelines.NorwegianYouthHomeContentsAgeNotMoreThan30Years
import com.hedvig.underwriter.service.guidelines.NorwegianYouthHomeContentsCoInsuredNotMoreThan2
import com.hedvig.underwriter.service.guidelines.NorwegianYouthHomeContentsLivingSpaceNotMoreThan50Sqm
import com.hedvig.underwriter.service.guidelines.NorwegianYouthTravelAgeNotMoreThan30Years
import com.hedvig.underwriter.service.guidelines.NorwegianYouthTravelCoInsuredNotMoreThan0
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
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineService
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuotePriceResponseDto
import com.hedvig.underwriter.testhelp.databuilder.a
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.javamoney.moneta.Money
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class UnderwriterImplTest {

    @Test
    fun successfullyCreatesSwedishApartmentQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.SwedishApartmentQuoteRequestBuilder().build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every { productPricingService.priceFromProductPricingForApartmentQuote(any()) } returns QuotePriceResponseDto(
            BigDecimal.ONE
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun successfullyCreatesSwedishStudentApartmentQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.SwedishApartmentQuoteRequestBuilder(
            ssn = "200112031356",
            data = a.SwedishApartmentQuoteRequestDataBuilder(
                subType = ApartmentProductSubType.STUDENT_BRF,
                livingSpace = 50,
                householdSize = 2
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every { productPricingService.priceFromProductPricingForApartmentQuote(any()) } returns QuotePriceResponseDto(
            BigDecimal.ONE
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun successfullyCreatesSwedishHouseQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.SwedishHouseQuoteRequestBuilder().build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every { productPricingService.priceFromProductPricingForHouseQuote(any()) } returns QuotePriceResponseDto(
            BigDecimal.ONE
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun successfullyCreatesNorwegianHomeContentsQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.NorwegianHomeContentsQuoteRequestBuilder().build()
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
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.NorwegianTravelQuoteRequestBuilder().build()
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
    fun underwritingGuidelineHitAgeOnCreatesSwedishApartmentQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.SwedishApartmentQuoteRequestBuilder(ssn = "202001010000").build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every { productPricingService.priceFromProductPricingForApartmentQuote(any()) } returns QuotePriceResponseDto(
            BigDecimal.ONE
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(listOf(AgeRestrictionGuideline.errorMessage))
    }

    @Test
    fun underwritingGuidelineHitAllLowerApartmentRulesOnCreatesSwedishApartmentQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.SwedishApartmentQuoteRequestBuilder(
            data = a.SwedishApartmentQuoteRequestDataBuilder(
                householdSize = 0,
                livingSpace = 0
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every { productPricingService.priceFromProductPricingForApartmentQuote(any()) } returns QuotePriceResponseDto(
            BigDecimal.ONE
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                SwedishApartmentHouseHoldSizeAtLeast1.errorMessage,
                SwedishApartmentLivingSpaceAtLeast1Sqm.errorMessage
            )
        )
    }

    @Test
    fun underwritingGuidelineHitAllUpperApartmentRulesOnCreatesSwedishApartmentQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.SwedishApartmentQuoteRequestBuilder(
            data = a.SwedishApartmentQuoteRequestDataBuilder(
                householdSize = 7,
                livingSpace = 251
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every { productPricingService.priceFromProductPricingForApartmentQuote(any()) } returns QuotePriceResponseDto(
            BigDecimal.ONE
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                SwedishApartmentHouseHoldSizeNotMoreThan6.errorMessage,
                SwedishApartmentLivingSpaceNotMoreThan250Sqm.errorMessage
            )
        )
    }

    @Test
    fun underwritingGuidelineHitAllLowerStudentApartmentRulesOnCreatesSwedishStudentApartmentQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.SwedishApartmentQuoteRequestBuilder(
            ssn = "200112031356",
            data = a.SwedishApartmentQuoteRequestDataBuilder(
                subType = ApartmentProductSubType.STUDENT_BRF,
                livingSpace = 0,
                householdSize = 0
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every { productPricingService.priceFromProductPricingForApartmentQuote(any()) } returns QuotePriceResponseDto(
            BigDecimal.ONE
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                SwedishApartmentHouseHoldSizeAtLeast1.errorMessage,
                SwedishApartmentLivingSpaceAtLeast1Sqm.errorMessage
            )
        )
    }

    @Test
    fun underwritingGuidelineHitAllUpperStudentApartmentRulesOnCreatesSwedishStudentApartmentQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.SwedishApartmentQuoteRequestBuilder(
            ssn = "198812031356",
            data = a.SwedishApartmentQuoteRequestDataBuilder(
                subType = ApartmentProductSubType.STUDENT_BRF,
                livingSpace = 51,
                householdSize = 3
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every { productPricingService.priceFromProductPricingForApartmentQuote(any()) } returns QuotePriceResponseDto(
            BigDecimal.ONE
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                SwedishStudentApartmentHouseholdSizeNotMoreThan2.errorMessage,
                SwedishStudentApartmentLivingSpaceNotMoreThan50Sqm.errorMessage,
                SwedishStudentApartmentAgeNotMoreThan30Years.errorMessage
            )
        )
    }

    @Test
    fun underwritingGuidelineHitAllLowerHouseRulesOnCreatesSwedishHouseQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.SwedishHouseQuoteRequestBuilder(
            data = a.SwedishHouseQuoteRequestDataBuilder(
                householdSize = 0, livingSpace = 0, yearOfConstruction = 1924, extraBuildings = listOf(
                    a.SwedishHouseQuoteRequestDataExtraBuildingsBuilder(area = 0).build()
                )
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every { productPricingService.priceFromProductPricingForHouseQuote(any()) } returns QuotePriceResponseDto(
            BigDecimal.ONE
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                SwedishHouseHouseholdSizeAtLeast1.errorMessage,
                SwedishHouseLivingSpaceAtLeast1Sqm.errorMessage,
                SwedishHouseYearOfConstruction.errorMessage,
                SwedishHouseExtraBuildingsSizeAtLeast1Sqm.errorMessage
            )
        )
    }

    @Test
    fun underwritingGuidelineHitAllUpperHouseRulesOnCreatesSwedishHouseQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.SwedishHouseQuoteRequestBuilder(
            data = a.SwedishHouseQuoteRequestDataBuilder(
                householdSize = 7, livingSpace = 251, numberOfBathrooms = 3, extraBuildings = listOf(
                    a.SwedishHouseQuoteRequestDataExtraBuildingsBuilder(area = 7).build(),
                    a.SwedishHouseQuoteRequestDataExtraBuildingsBuilder(area = 7).build(),
                    a.SwedishHouseQuoteRequestDataExtraBuildingsBuilder(area = 7).build(),
                    a.SwedishHouseQuoteRequestDataExtraBuildingsBuilder(area = 7).build(),
                    a.SwedishHouseQuoteRequestDataExtraBuildingsBuilder(area = 76).build()
                )
            )
        ).build()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every { productPricingService.priceFromProductPricingForHouseQuote(any()) } returns QuotePriceResponseDto(
            BigDecimal.ONE
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                SwedishHouseHouseholdSizeNotMoreThan6.errorMessage,
                SwedishHouseLivingSpaceNotMoreThan250Sqm.errorMessage,
                SwedishHouseNumberOfBathrooms.errorMessage,
                SwedishHouseNumberOfExtraBuildingsWithAreaOverSixSqm.errorMessage,
                SwedishHouseExtraBuildingsSizeNotOverThan75Sqm.errorMessage
            )
        )
    }

    @Test
    fun successfullyCreateNorwegianHomeContentsQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.NorwegianHomeContentsQuoteRequestBuilder().build()

        every { priceEngineService.queryNorwegianHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "NOK")
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun underwritingGuidelineHitWhenNorwegianSsnNotMatch() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.NorwegianHomeContentsQuoteRequestBuilder(
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
                NorwegianSsnNotMatchesBirthDate.errorMessage
            )
        )
    }

    @Test
    fun successfullyCreateNorwegianHomeContentsQuoteWhenSsnIsNull() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.NorwegianHomeContentsQuoteRequestBuilder(
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
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.NorwegianTravelQuoteRequestBuilder().build()

        every { priceEngineService.queryNorwegianTravelPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(0, "NOK")
        )

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Right)
    }

    @Test
    fun underwritingGuidelineHitAllUpperApartmentRulesOnCreatesNorwegianHomeContentsQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.NorwegianHomeContentsQuoteRequestBuilder(
            data = a.NorwegianHomeContentsQuoteRequestDataBuilder(
                coInsured = 6,
                livingSpace = 251
            )
        ).build()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                NorwegianHomeContentscoInsuredNotMoreThan5.errorMessage,
                NorwegianHomeContentsLivingSpaceNotMoreThan250Sqm.errorMessage
            )
        )
    }

    @Test
    fun underwritingGuidelineHitAllUpperApartmentRulesOnCreatesNorwegianHomeContentsYouthQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.NorwegianHomeContentsQuoteRequestBuilder(
            ssn = "28026400734",
            birthDate = LocalDate.of(1964, 2, 28),
            data = a.NorwegianHomeContentsQuoteRequestDataBuilder(
                coInsured = 3,
                livingSpace = 51,
                isYouth = true
            )
        ).build()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                NorwegianYouthHomeContentsLivingSpaceNotMoreThan50Sqm.errorMessage,
                NorwegianYouthHomeContentsAgeNotMoreThan30Years.errorMessage,
                NorwegianYouthHomeContentsCoInsuredNotMoreThan2.errorMessage
            )
        )
    }

    @Test
    fun underwritingGuidelineHitAllUpperApartmentRulesOnCreatesNorwegianTravelQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.NorwegianTravelQuoteRequestBuilder(
            data = a.NorwegianTravelQuoteRequestDataBuilder(
                coInsured = 6
            )
        ).build()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                NorwegianTravelCoInsuredNotMoreThan5.errorMessage
            )
        )
    }

    @Test
    fun underwritingGuidelineHitAllUpperApartmentRulesOnCreatesNorwegianTravelYouthQuote() {
        val debtChecker = mockk<DebtChecker>()
        val productPricingService = mockk<ProductPricingService>()
        val priceEngineService = mockk<PriceEngineService>()

        val cut = UnderwriterImpl(debtChecker, productPricingService, priceEngineService)
        val quoteRequest = a.NorwegianTravelQuoteRequestBuilder(
            birthDate = LocalDate.now().minusYears(31).minusDays(1),
            data = a.NorwegianTravelQuoteRequestDataBuilder(
                coInsured = 1,
                isYouth = true
            )
        ).build()

        val result = cut.createQuote(quoteRequest, UUID.randomUUID(), QuoteInitiatedFrom.WEBONBOARDING, null)
        require(result is Either.Left)
        assertThat(result.a.second).isEqualTo(
            listOf(
                NorwegianSsnNotMatchesBirthDate.errorMessage,
                NorwegianYouthTravelAgeNotMoreThan30Years.errorMessage,
                NorwegianYouthTravelCoInsuredNotMoreThan0.errorMessage
            )
        )
    }
}
