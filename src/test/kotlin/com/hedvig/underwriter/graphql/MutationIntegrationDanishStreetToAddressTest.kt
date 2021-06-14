package com.hedvig.underwriter.graphql

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.graphql.spring.boot.test.GraphQLTestTemplate
import com.hedvig.graphql.commons.type.MonetaryAmountV2
import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.localization.LocalizationService
import com.hedvig.underwriter.model.DanishHomeContentsType
import com.hedvig.underwriter.service.DebtChecker
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.service.SignService
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.notificationService.NotificationService
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineService
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryRequest
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.javamoney.moneta.Money
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MutationIntegrationDanishStreetToAddressTest {

    @Autowired
    private lateinit var graphQLTestTemplate: GraphQLTestTemplate

    @Autowired
    private lateinit var quoteService: QuoteService

    @MockkBean(relaxUnitFun = true)
    lateinit var memberService: MemberService

    @MockkBean
    lateinit var debtChecker: DebtChecker

    @MockkBean
    lateinit var productPricingService: ProductPricingService

    @MockkBean
    lateinit var priceEngineService: PriceEngineService

    @MockkBean(relaxed = true)
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var signService: SignService

    @MockkBean
    lateinit var localizationService: LocalizationService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `splits address data from street when address data is input in street field only`() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        every {
            priceEngineService.queryDanishHomeContentPrice(
                PriceQueryRequest.DanishHomeContent(
                    holderMemberId = "123",
                    quoteId = UUID.fromString("2b9e3b30-5c87-11ea-aa95-fbfb43d88ae5"),
                    holderBirthDate = LocalDate.of(1961, 12, 21),
                    numberCoInsured = 0,
                    postalCode = "1234",
                    squareMeters = 30,
                    bbrId = "123",
                    apartment = "1",
                    floor = "4",
                    street = "Kungsgatan 2",
                    city = null,
                    student = false,
                    housingType = DanishHomeContentsType.RENT
                )
            )
        } returns
            PriceQueryResponse(
                UUID.randomUUID(),
                Money.of(BigDecimal.ONE, "DKK")
            )

        every {
            productPricingService.calculateInsuranceCost(
                Money.of(BigDecimal(9999), "DKK"), "123"
            )
        } returns
            InsuranceCost(
                MonetaryAmountV2.of(BigDecimal.ONE, "DKK"),
                MonetaryAmountV2.of(BigDecimal.ONE, "DKK"),
                MonetaryAmountV2.of(BigDecimal.ONE, "DKK"),
                null
            )

        graphQLTestTemplate.addHeader("hedvig.token", "123")

        val response = graphQLTestTemplate.perform(
            "/mutations/danishStreetToAddress/createDanishHomeContentsQuoteWithStreetOnly.graphql",
            null
        )
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assertThat(createQuote["quoteDetails"]["street"].textValue()).isEqualTo("Kungsgatan 2")
        assertThat(createQuote["quoteDetails"]["apartment"].textValue()).isEqualTo("1")
        assertThat(createQuote["quoteDetails"]["floor"].textValue()).isEqualTo("4")
    }

    @Test
    fun `doesn't crash if street was an empty string`() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        every {
            priceEngineService.queryDanishHomeContentPrice(
                PriceQueryRequest.DanishHomeContent(
                    holderMemberId = "123",
                    quoteId = UUID.fromString("2b9e3b30-5c87-11ea-aa95-fbfb43d88ae5"),
                    holderBirthDate = LocalDate.of(1961, 12, 21),
                    numberCoInsured = 0,
                    postalCode = "1234",
                    squareMeters = 30,
                    bbrId = "123",
                    apartment = "1",
                    floor = "4",
                    street = "Kungsgatan 2",
                    city = null,
                    student = false,
                    housingType = DanishHomeContentsType.RENT
                )
            )
        } returns
            PriceQueryResponse(
                UUID.randomUUID(),
                Money.of(BigDecimal.ONE, "DKK")
            )

        every {
            productPricingService.calculateInsuranceCost(
                Money.of(BigDecimal(9999), "DKK"), "123"
            )
        } returns
            InsuranceCost(
                MonetaryAmountV2.of(BigDecimal.ONE, "DKK"),
                MonetaryAmountV2.of(BigDecimal.ONE, "DKK"),
                MonetaryAmountV2.of(BigDecimal.ONE, "DKK"),
                null
            )

        graphQLTestTemplate.addHeader("hedvig.token", "123")

        val response = graphQLTestTemplate.perform(
            "/mutations/danishStreetToAddress/createDanishHomeContentsQuoteWithStreetAsEmptyString.graphql",
            null
        )
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assertThat(createQuote["quoteDetails"]["street"].textValue()).isEqualTo("")
        assertThat(createQuote["quoteDetails"]["apartment"].textValue()).isEqualTo(null)
        assertThat(createQuote["quoteDetails"]["floor"].textValue()).isEqualTo(null)
    }

    @Test
    fun `if apartment and floor aren't null use these values instead getting values from street`() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        every {
            priceEngineService.queryDanishHomeContentPrice(
                PriceQueryRequest.DanishHomeContent(
                    holderMemberId = "123",
                    quoteId = UUID.fromString("2b9e3b30-5c87-11ea-aa95-fbfb43d88ae5"),
                    holderBirthDate = LocalDate.of(1961, 12, 21),
                    numberCoInsured = 0,
                    postalCode = "1234",
                    squareMeters = 30,
                    bbrId = "123",
                    apartment = "tr.",
                    floor = "k9",
                    street = "Kungsgatan 2",
                    city = null,
                    student = false,
                    housingType = DanishHomeContentsType.RENT
                )
            )
        } returns
            PriceQueryResponse(
                UUID.randomUUID(),
                Money.of(BigDecimal.ONE, "DKK")
            )

        every {
            productPricingService.calculateInsuranceCost(
                Money.of(BigDecimal(9999), "DKK"), "123"
            )
        } returns
            InsuranceCost(
                MonetaryAmountV2.of(BigDecimal.ONE, "DKK"),
                MonetaryAmountV2.of(BigDecimal.ONE, "DKK"),
                MonetaryAmountV2.of(BigDecimal.ONE, "DKK"),
                null
            )

        graphQLTestTemplate.addHeader("hedvig.token", "123")

        val response = graphQLTestTemplate.perform(
            "/mutations/danishStreetToAddress/createDanishHomeContentsQuoteWithApartmentAndStreetNotNull.graphql",
            null
        )
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assertThat(createQuote["quoteDetails"]["street"].textValue()).isEqualTo("Kungsgatan 2")
        assertThat(createQuote["quoteDetails"]["apartment"].textValue()).isEqualTo("tr.")
        assertThat(createQuote["quoteDetails"]["floor"].textValue()).isEqualTo("k9")
    }

    @Test
    fun `if street has a weird format, format it before saving`() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        every {
            priceEngineService.queryDanishHomeContentPrice(
                PriceQueryRequest.DanishHomeContent(
                    holderMemberId = "123",
                    quoteId = UUID.fromString("2b9e3b30-5c87-11ea-aa95-fbfb43d88ae5"),
                    holderBirthDate = LocalDate.of(1961, 12, 21),
                    numberCoInsured = 0,
                    postalCode = "1234",
                    squareMeters = 30,
                    bbrId = "123",
                    apartment = "1",
                    floor = "4",
                    street = "Kungsgatan 2",
                    city = null,
                    student = false,
                    housingType = DanishHomeContentsType.RENT
                )
            )
        } returns
            PriceQueryResponse(
                UUID.randomUUID(),
                Money.of(BigDecimal.ONE, "DKK")
            )

        every {
            productPricingService.calculateInsuranceCost(
                Money.of(BigDecimal(9999), "DKK"), "123"
            )
        } returns
            InsuranceCost(
                MonetaryAmountV2.of(BigDecimal.ONE, "DKK"),
                MonetaryAmountV2.of(BigDecimal.ONE, "DKK"),
                MonetaryAmountV2.of(BigDecimal.ONE, "DKK"),
                null
            )

        graphQLTestTemplate.addHeader("hedvig.token", "123")

        val response = graphQLTestTemplate.perform(
            "/mutations/danishStreetToAddress/createDanishHomeContentsQuoteWithStrangeStreetFormat.graphql",
            null
        )
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assertThat(createQuote["quoteDetails"]["street"].textValue()).isEqualTo("Kungsgatan 2")
        assertThat(createQuote["quoteDetails"]["apartment"].textValue()).isEqualTo("4")
        assertThat(createQuote["quoteDetails"]["floor"].textValue()).isEqualTo("1")
    }
}
