package com.hedvig.underwriter.graphql

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.graphql.spring.boot.test.GraphQLTestTemplate
import com.hedvig.graphql.commons.type.MonetaryAmountV2
import com.hedvig.productPricingObjects.enums.NorwegianHomeContentLineOfBusiness
import com.hedvig.productPricingObjects.enums.NorwegianTravelLineOfBusiness
import com.hedvig.underwriter.graphql.type.CreateNorwegianTravelInput
import com.hedvig.underwriter.graphql.type.CreateQuoteInput
import com.hedvig.underwriter.graphql.type.InsuranceCost
import com.hedvig.underwriter.localization.LocalizationService
import com.hedvig.underwriter.model.DanishHomeContentsType
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.birthDateFromNorwegianSsn
import com.hedvig.underwriter.service.DebtChecker
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.service.RequotingService
import com.hedvig.underwriter.service.SignService
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.notificationService.NotificationService
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineService
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryRequest
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.testhelp.databuilder.SwedishApartmentQuoteRequestBuilder
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.javamoney.moneta.Money
import org.junit.Before
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
internal class GraphQlMutationsIntegrationTest {

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
    lateinit var requotingService: RequotingService

    @MockkBean(relaxed = true)
    lateinit var notificationService: NotificationService

    @MockkBean
    lateinit var signService: SignService

    @MockkBean
    lateinit var localizationService: LocalizationService

    @Autowired
    lateinit var objectMapper: ObjectMapper

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

        every { requotingService.useOldOrNewPrice(any(), any()) } answers { secondArg() }
    }

    @Test
    fun createSuccessfulOldApartmentQuote() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        every {
            productPricingService.calculateInsuranceCost(
                Money.of(BigDecimal.ONE, "SEK"), "123"
            )
        } returns
            InsuranceCost(
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
                null
            )

        graphQLTestTemplate.addHeader("hedvig.token", "123")

        val response = graphQLTestTemplate.perform("/mutations/createApartmentQuote.graphql", null)
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assert(createQuote["id"].textValue() == "00000000-0000-0000-0000-000000000000")
        assert(createQuote["insuranceCost"]["monthlyGross"]["amount"].textValue() == "1.00")
        assert(createQuote["insuranceCost"]["monthlyGross"]["currency"].textValue() == "SEK")
        assert(createQuote["details"]["street"].textValue() == "Kungsgatan 1")
        assert(createQuote["details"]["zipCode"].textValue() == "12345")
        assert(createQuote["details"]["livingSpace"].intValue() == 30)
        assert(createQuote["details"]["householdSize"].intValue() == 2)
        assert(createQuote["details"]["type"].textValue() == "BRF")
    }

    @Test
    fun createSuccessfulOldHouseQuote() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        every {
            productPricingService.calculateInsuranceCost(
                Money.of(BigDecimal.ONE, "SEK"), "123"
            )
        } returns
            InsuranceCost(
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
                null
            )

        graphQLTestTemplate.addHeader("hedvig.token", "123")

        val response = graphQLTestTemplate.perform("/mutations/createHouseQuote.graphql", null)
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assert(createQuote["id"].textValue() == "f87654f0-3eed-11eb-a2f2-275abc72e5ce")
        assert(createQuote["insuranceCost"]["monthlyGross"]["amount"].textValue() == "1.00")
        assert(createQuote["insuranceCost"]["monthlyGross"]["currency"].textValue() == "SEK")
        assert(createQuote["details"]["street"].textValue() == "Kungsgatan 1")
        assert(createQuote["details"]["zipCode"].textValue() == "12345")
        assert(createQuote["details"]["livingSpace"].intValue() == 30)
        assert(createQuote["details"]["householdSize"].intValue() == 2)
    }

    @Test
    fun createSuccessfulSwedishApartmentQuote() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        every {
            productPricingService.calculateInsuranceCost(
                Money.of(BigDecimal.ONE, "SEK"), "123"
            )
        } returns
            InsuranceCost(
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
                null
            )

        graphQLTestTemplate.addHeader("hedvig.token", "123")

        val response = graphQLTestTemplate.perform("/mutations/createSwedishApartmentQuote.graphql", null)
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assert(createQuote["id"].textValue() == "00000000-0000-0000-0000-000000000004")
        assert(createQuote["insuranceCost"]["monthlyGross"]["amount"].textValue() == "1.00")
        assert(createQuote["insuranceCost"]["monthlyGross"]["currency"].textValue() == "SEK")
        assert(createQuote["quoteDetails"]["street"].textValue() == "Kungsgatan 1")
        assert(createQuote["quoteDetails"]["zipCode"].textValue() == "12345")
        assert(createQuote["quoteDetails"]["livingSpace"].intValue() == 30)
        assert(createQuote["quoteDetails"]["householdSize"].intValue() == 2)
        assert(createQuote["quoteDetails"]["apartmentType"].textValue() == "BRF")
    }

    @Test
    fun createSuccessfulSwedishHouseQuote() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        every {
            productPricingService.calculateInsuranceCost(
                Money.of(BigDecimal.ONE, "SEK"), "123"
            )
        } returns
            InsuranceCost(
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
                null
            )

        graphQLTestTemplate.addHeader("hedvig.token", "123")

        val response = graphQLTestTemplate.perform("/mutations/createSwedishHouseQuote.graphql", null)
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assert(createQuote["id"].textValue() == "00000000-0000-0000-0000-000000000005")
        assert(createQuote["insuranceCost"]["monthlyGross"]["amount"].textValue() == "1.00")
        assert(createQuote["insuranceCost"]["monthlyGross"]["currency"].textValue() == "SEK")
        assert(createQuote["quoteDetails"]["street"].textValue() == "Kungsgatan 1")
        assert(createQuote["quoteDetails"]["zipCode"].textValue() == "12345")
        assert(createQuote["quoteDetails"]["livingSpace"].intValue() == 30)
        assert(createQuote["quoteDetails"]["householdSize"].intValue() == 2)
    }

    @Test
    fun createSuccessfulNorwegianHomeContentsQuote() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every {
            priceEngineService.queryNorwegianHomeContentPrice(
                PriceQueryRequest.NorwegianHomeContent(
                    holderMemberId = "123",
                    quoteId = UUID.fromString("00000000-0000-0000-0000-000000000006"),
                    holderBirthDate = "21126114165".birthDateFromNorwegianSsn(),
                    numberCoInsured = 0,
                    lineOfBusiness = NorwegianHomeContentLineOfBusiness.OWN,
                    postalCode = "12345",
                    squareMeters = 30
                )
            )
        } returns
            PriceQueryResponse(
                UUID.randomUUID(),
                Money.of(BigDecimal.ONE, "NOK")
            )

        every {
            productPricingService.calculateInsuranceCost(
                Money.of(BigDecimal.ONE, "NOK"), "123"
            )
        } returns
            InsuranceCost(
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "NOK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "NOK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "NOK"),
                null
            )

        graphQLTestTemplate.addHeader("hedvig.token", "123")

        val response = graphQLTestTemplate.perform("/mutations/createNorwegianHomeContentsQuote.graphql", null)
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assert(createQuote["id"].textValue() == "00000000-0000-0000-0000-000000000006")
        assert(createQuote["insuranceCost"]["monthlyGross"]["amount"].textValue() == "1.00")
        assert(createQuote["insuranceCost"]["monthlyGross"]["currency"].textValue() == "NOK")
        assert(createQuote["quoteDetails"]["street"].textValue() == "Kungsgatan 2")
        assert(createQuote["quoteDetails"]["zipCode"].textValue() == "12345")
        assert(createQuote["quoteDetails"]["livingSpace"].intValue() == 30)
        assert(createQuote["quoteDetails"]["coInsured"].intValue() == 0)
        assert(createQuote["quoteDetails"]["norwegianType"].textValue() == "OWN")
    }

    @Test
    fun createSuccessfulNorwegianTravelQuote() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every {
            priceEngineService.queryNorwegianTravelPrice(
                PriceQueryRequest.NorwegianTravel(
                    holderMemberId = "123",
                    quoteId = UUID.fromString("2b9e3b30-5c87-11ea-aa95-fbfb43d88ae7"),
                    holderBirthDate = "21126114165".birthDateFromNorwegianSsn(),
                    numberCoInsured = 0,
                    lineOfBusiness = NorwegianTravelLineOfBusiness.REGULAR
                )
            )
        } returns
            PriceQueryResponse(
                UUID.randomUUID(),
                Money.of(BigDecimal.ONE, "NOK")
            )

        every {
            productPricingService.calculateInsuranceCost(
                Money.of(BigDecimal.ONE, "NOK"), "123"
            )
        } returns
            InsuranceCost(
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "NOK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "NOK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "NOK"),
                null
            )

        graphQLTestTemplate.addHeader("hedvig.token", "123")

        val createQuoteInput = CreateQuoteInput(
            id = UUID.fromString("2b9e3b30-5c87-11ea-aa95-fbfb43d88ae7"),
            firstName = "",
            lastName = "",
            email = "test@email.com",
            phoneNumber = null,
            currentInsurer = null,
            ssn = "21126114165",
            birthDate = "21126114165".birthDateFromNorwegianSsn(),
            startDate = null,
            apartment = null,
            house = null,
            swedishApartment = null,
            swedishHouse = null,
            norwegianHomeContents = null,
            norwegianTravel = CreateNorwegianTravelInput(
                coInsured = 0,
                isYouth = false
            ),
            danishHomeContents = null,
            danishAccident = null,
            danishTravel = null,
            dataCollectionId = null
        )

        val response = graphQLTestTemplate.perform(
            "/mutations/createNorwegianTravelQuote.graphql",
            objectMapper.valueToTree(mapOf("input" to createQuoteInput))
        )
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assert(createQuote["id"].textValue() == "2b9e3b30-5c87-11ea-aa95-fbfb43d88ae7")
        assert(createQuote["insuranceCost"]["monthlyGross"]["amount"].textValue() == "1.00")
        assert(createQuote["insuranceCost"]["monthlyGross"]["currency"].textValue() == "NOK")
        assert(createQuote["quoteDetails"]["coInsured"].intValue() == 0)
    }

    @Test
    fun createSuccessfulDanishHomeContentsQuote() {
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
                    city = "testCity",
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
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "DKK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "DKK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "DKK"),
                null
            )

        graphQLTestTemplate.addHeader("hedvig.token", "123")

        val response = graphQLTestTemplate.perform(
            "/mutations/createDanishHomeContentsQuote.graphql",
            null
        )
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assert(createQuote["id"].textValue() == "2b9e3b30-5c87-11ea-aa95-fbfb43d88ae5")
        assert(createQuote["insuranceCost"]["monthlyGross"]["amount"].textValue() == "9999.00")
        assert(createQuote["insuranceCost"]["monthlyGross"]["currency"].textValue() == "DKK")
        assertThat(createQuote["quoteDetails"]["street"].textValue()).isEqualTo("Kungsgatan 2")
        assertThat(createQuote["quoteDetails"]["apartment"].textValue()).isEqualTo("1")
        assertThat(createQuote["quoteDetails"]["floor"].textValue()).isEqualTo("4")
        assert(createQuote["quoteDetails"]["zipCode"].textValue() == "1234")
        assert(createQuote["quoteDetails"]["livingSpace"].intValue() == 30)
        assert(createQuote["quoteDetails"]["coInsured"].intValue() == 0)
        assert(createQuote["quoteDetails"]["isStudent"].booleanValue() == false)
        assert(createQuote["quoteDetails"]["danishHomeContentType"].textValue() == "RENT")
        assert(createQuote["quoteDetails"]["bbrId"].textValue() == "123")
    }

    @Test
    fun createSuccessfulDanishAccidentQuote() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        every {
            priceEngineService.queryDanishAccidentPrice(
                PriceQueryRequest.DanishAccident(
                    holderMemberId = "123",
                    quoteId = UUID.fromString("2b9e3b30-5c87-11ea-aa95-fbfb43d88ae3"),
                    holderBirthDate = LocalDate.of(1961, 12, 21),
                    numberCoInsured = 0,
                    postalCode = "1234",
                    bbrId = "123",
                    apartment = "1",
                    floor = "4",
                    street = "Kungsgatan 2",
                    city = "testCity",
                    student = false
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
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "DKK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "DKK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "DKK"),
                null
            )

        graphQLTestTemplate.addHeader("hedvig.token", "123")

        val response = graphQLTestTemplate.perform(
            "/mutations/createDanishAccidentQuote.graphql",
            null
        )
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assert(createQuote["id"].textValue() == "2b9e3b30-5c87-11ea-aa95-fbfb43d88ae3")
        assert(createQuote["insuranceCost"]["monthlyGross"]["amount"].textValue() == "9999.00")
        assert(createQuote["insuranceCost"]["monthlyGross"]["currency"].textValue() == "DKK")
        assertThat(createQuote["quoteDetails"]["street"].textValue()).isEqualTo("Kungsgatan 2")
        assertThat(createQuote["quoteDetails"]["apartment"].textValue()).isEqualTo("1")
        assertThat(createQuote["quoteDetails"]["floor"].textValue()).isEqualTo("4")
        assert(createQuote["quoteDetails"]["zipCode"].textValue() == "1234")
        assert(createQuote["quoteDetails"]["coInsured"].intValue() == 0)
        assert(createQuote["quoteDetails"]["isStudent"].booleanValue() == false)
        assert(createQuote["quoteDetails"]["bbrId"].textValue() == "123")
    }

    @Test
    fun createSuccessfulDanishTravelQuote() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        every {
            priceEngineService.queryDanishTravelPrice(
                PriceQueryRequest.DanishTravel(
                    holderMemberId = "123",
                    quoteId = UUID.fromString("2b9e3b30-5c87-11ea-aa95-fbfb43d88ae1"),
                    holderBirthDate = LocalDate.of(2001, 12, 21),
                    numberCoInsured = 0,
                    postalCode = "1234",
                    bbrId = "123",
                    apartment = "tv",
                    floor = "4",
                    street = "Kungsgatan 2",
                    city = "testCity",
                    student = true
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
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "DKK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "DKK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "DKK"),
                null
            )

        graphQLTestTemplate.addHeader("hedvig.token", "123")

        val response = graphQLTestTemplate.perform(
            "/mutations/createDanishTravelQuote.graphql",
            null
        )
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assert(createQuote["id"].textValue() == "2b9e3b30-5c87-11ea-aa95-fbfb43d88ae1")
        assert(createQuote["insuranceCost"]["monthlyGross"]["amount"].textValue() == "9999.00")
        assert(createQuote["insuranceCost"]["monthlyGross"]["currency"].textValue() == "DKK")
        assertThat(createQuote["quoteDetails"]["street"].textValue()).isEqualTo("Kungsgatan 2")
        assertThat(createQuote["quoteDetails"]["apartment"].textValue()).isEqualTo("tv")
        assertThat(createQuote["quoteDetails"]["floor"].textValue()).isEqualTo("4")
        assert(createQuote["quoteDetails"]["zipCode"].textValue() == "1234")
        assert(createQuote["quoteDetails"]["coInsured"].intValue() == 0)
        assert(createQuote["quoteDetails"]["isStudent"].booleanValue() == true)
        assert(createQuote["quoteDetails"]["bbrId"].textValue() == "123")
    }

    @Test
    fun createQuoteFinalizeOnbaordingInMemberServiceQuote() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every {
            priceEngineService.queryNorwegianTravelPrice(
                PriceQueryRequest.NorwegianTravel(
                    holderMemberId = "123",
                    quoteId = UUID.fromString("2b9e3b30-5c87-11ea-aa95-fbfb43d88ae6"),
                    holderBirthDate = "1212121212".birthDateFromNorwegianSsn(),
                    numberCoInsured = 0,
                    lineOfBusiness = NorwegianTravelLineOfBusiness.REGULAR
                )
            )
        } returns
            PriceQueryResponse(
                UUID.randomUUID(),
                Money.of(BigDecimal.ONE, "NOK")
            )

        every {
            productPricingService.calculateInsuranceCost(
                Money.of(BigDecimal.ONE, "NOK"), "123"
            )
        } returns
            InsuranceCost(
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "NOK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "NOK"),
                MonetaryAmountV2.Companion.of(BigDecimal.ONE, "NOK"),
                null
            )

        graphQLTestTemplate.addHeader("hedvig.token", "123")

        val createQuoteInput = CreateQuoteInput(
            id = UUID.fromString("2b9e3b30-5c87-11ea-aa95-fbfb43d88ae6"),
            firstName = "",
            lastName = "",
            email = "test@email.com",
            phoneNumber = null,
            currentInsurer = null,
            ssn = "1212121212",
            birthDate = null,
            startDate = null,
            apartment = null,
            house = null,
            swedishApartment = null,
            swedishHouse = null,
            norwegianHomeContents = null,
            norwegianTravel = CreateNorwegianTravelInput(
                coInsured = 0,
                isYouth = false
            ),
            danishHomeContents = null,
            danishAccident = null,
            danishTravel = null,
            dataCollectionId = null
        )
        val response = graphQLTestTemplate.perform(
            "/mutations/createNorwegianTravelQuote.graphql",
            ObjectMapper().valueToTree(mapOf("input" to createQuoteInput))
        )
        val createQuote = response.readTree()["data"]["createQuote"]

        verify { memberService.finalizeOnboarding(any(), "test@email.com") }
    }

    @Test
    fun createUnderwritingLimitsHitQuote() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        val response = graphQLTestTemplate.perform("/mutations/createUnderwritingLimitHitQuote.graphql", null)
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assert(createQuote["limits"][0]["description"].textValue() != null)
    }

    @Test
    fun createQuoteWithPhoneNumber() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        every {
            productPricingService.calculateInsuranceCost(
                any(),
                any()
            )
        } returns InsuranceCost(
            MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
            MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
            MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
            null
        )

        graphQLTestTemplate.addHeader("hedvig.token", "12345")
        val response = graphQLTestTemplate.perform("/mutations/createQuoteWithPhoneNumber.graphql", null)
        val createQuote = response.readTree()["data"]["createQuote"]

        assert(response.isOk)
        assert(createQuote["phoneNumber"].textValue() == "0812331321")
    }

    @Test
    fun modifyQuoteWithPhoneNumber() {
        every { debtChecker.passesDebtCheck(any()) } returns listOf()

        every {
            productPricingService.calculateInsuranceCost(
                any(),
                any()
            )
        } returns InsuranceCost(
            MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
            MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
            MonetaryAmountV2.Companion.of(BigDecimal.ONE, "SEK"),
            null
        )

        val req = SwedishApartmentQuoteRequestBuilder(phoneNumber = null, memberId = "12345").build()
        val quoteId = UUID.fromString("a64d8f3a-3edf-11eb-a021-6f36afe75b8f")
        quoteService.createQuote(req, quoteId, QuoteInitiatedFrom.ANDROID, null, false)

        graphQLTestTemplate.addHeader("hedvig.token", "12345")
        val response = graphQLTestTemplate.perform("/mutations/editQuoteWithPhoneNumber.graphql", null)
        val createQuote = response.readTree()["data"]["editQuote"]

        assert(response.isOk)
        assert(createQuote["phoneNumber"].textValue() == "0812331321")
    }
}
