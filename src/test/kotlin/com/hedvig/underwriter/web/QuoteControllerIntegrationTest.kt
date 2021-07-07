package com.hedvig.underwriter.web

import assertk.assertThat
import assertk.assertions.isEqualByComparingTo
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.DanishHomeContentsType
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.FinalizeOnBoardingRequest
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.Flag
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.HelloHedvigResponseDto
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.PersonStatusDto
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.LineItem
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryRequest
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractsRequest
import com.hedvig.underwriter.testhelp.IntegrationTest
import com.hedvig.underwriter.testhelp.QuoteClient
import io.mockk.every
import io.mockk.slot
import java.util.UUID
import org.javamoney.moneta.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity

class QuoteControllerIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var quoteClient: QuoteClient

    @BeforeEach
    fun init() {

        every { memberServiceClient.createMember() } returns ResponseEntity.status(200)
            .body(HelloHedvigResponseDto("12345"))
        every { memberServiceClient.signQuote(any(), any()) } returns ResponseEntity.status(200)
            .body(UnderwriterQuoteSignResponse(1L, true))
        every {
            memberServiceClient.finalizeOnBoarding(
                any(),
                any()
            )
        } returns ResponseEntity.status(200).body("")
        every { memberServiceClient.personStatus(any()) } returns ResponseEntity.status(200)
            .body(PersonStatusDto(Flag.GREEN))

        every { productPricingClient.hasContract(any(), any()) } returns ResponseEntity.status(200).body(false)
        every { productPricingClient.createContract(any(), any()) } returns listOf(
            CreateContractResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        )
    }

    @Test
    fun completeQuote() {

        // GIVEN
        val priceEngineLineItems = listOf(
            LineItem("PREMIUM", "premium", 118.0.toBigDecimal()),
            LineItem("TAX", "tax_dk_gf", 3.33333333.toBigDecimal()),
            LineItem("TAX", "tax_dk_ipt", 1.123456.toBigDecimal())
        )

        val quoteSlot = slot<CreateContractsRequest>()
        val msFinalizeOnboardingRequest = slot<FinalizeOnBoardingRequest>()

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(12, "SEK"),
            priceEngineLineItems
        )
        every { productPricingClient.createContract(capture(quoteSlot), any()) } returns listOf(
            CreateContractResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        )
        every {
            memberServiceClient.finalizeOnBoarding(
                any(),
                capture(msFinalizeOnboardingRequest)
            )
        } returns ResponseEntity.status(200).body("")

        // WHEN
        val quoteResponse = quoteClient.createSwedishApartmentQuote(
            birthdate = "1995-07-28",
            ssn = "199507282383"
        )

        quoteClient.signQuote(
            quoteId = quoteResponse.id,
            firstName = "Mr",
            lastName = "Svensson",
            email = "s@hedvig.com"
        )

        // THEN
        assertThat(msFinalizeOnboardingRequest.isCaptured).isTrue()
        assertThat(msFinalizeOnboardingRequest.captured.email).isEqualTo("s@hedvig.com")
        assertThat(msFinalizeOnboardingRequest.captured.birthDate.toString()).isEqualTo("1995-07-28")
        assertThat(msFinalizeOnboardingRequest.captured.lastName).isEqualTo("Svensson")
        assertThat(msFinalizeOnboardingRequest.captured.firstName).isEqualTo("Mr")
        assertThat(msFinalizeOnboardingRequest.captured.memberId).isEqualTo("12345")
        assertThat(msFinalizeOnboardingRequest.captured.ssn).isEqualTo("199507282383")

        // Verify lineItems, both the ones sent to P&P and the ones exposed in the quote API
        assertProductPricingLineItems(priceEngineLineItems, quoteSlot.captured.quotes.first().lineItems)

        val quote = quoteClient.getQuote(quoteResponse.id)

        assertQuoteApiLineItems(priceEngineLineItems, quote!!.lineItems)
    }

    @Test
    fun `creates, saves and retrieves danish home content quote properly`() {

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(999, "SEK"),
            null
        )

        val quoteResponse = quoteClient.createDanishHomeContentQuote(
            street = "test street",
            apartment = "4",
            zip = "123",
            bbrId = "12345",
            livingSpace = 100,
            coInsured = 1,
            subType = "RENT",
            city = "city",
            floor = "2"
        )

        val quote = quoteClient.getQuote(quoteResponse.id)

        with(quote?.data as DanishHomeContentsData) {
            assertThat(livingSpace).isEqualTo(100)
            assertThat(coInsured).isEqualTo(1)
            assertThat(isStudent).isEqualTo(false)
            assertThat(type).isEqualTo(DanishHomeContentsType.RENT)
            assertThat(street).isEqualTo("test street")
            assertThat(apartment).isEqualTo("4")
            assertThat(zipCode).isEqualTo("123")
            assertThat(city).isEqualTo("city")
            assertThat(floor).isEqualTo("2")
            assertThat(bbrId).isEqualTo("12345")
        }
    }

    @Test
    fun `Creating quote fails and returns appropriate error code when debt check fails`() {
        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(12, "SEK"),
            listOf(LineItem("PREMIUM", "premium", 118.0.toBigDecimal()))
        )
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200)
            .body(HelloHedvigResponseDto("12345"))
        every {
            productPricingClient.createContract(
                any(),
                any()
            )
        } returns listOf(CreateContractResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
        every { memberServiceClient.signQuote(any(), any()) } returns ResponseEntity.status(200)
            .body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(any(), any()) } returns ResponseEntity.status(200).body("")
        every { memberServiceClient.personStatus(any()) } returns ResponseEntity.status(200)
            .body(PersonStatusDto(Flag.RED))

        val result = quoteClient.createSwedishApartmentQuoteAsMap(
            birthdate = "1995-07-28",
            ssn = "199507282383"
        )

        val raw = quoteClient.createSwedishApartmentQuoteRaw(
            birthdate = "1995-07-28",
            ssn = "199507282383"
        )

        assertThat(result.statusCode.value()).isEqualTo(422)
        assertThat(result.body!!["errorCode"]).isEqualTo("MEMBER_BREACHES_UW_GUIDELINES")
        assertThat(result.body!!["breachedUnderwritingGuidelines"][0]["code"]).isEqualTo("DEBT_CHECK")
    }

    @Test
    fun `Test overriding of quote price`() {

        // GIVEN
        val ssn = "199507282383"
        val defaultPremiumPrice = 12.0.toBigDecimal()

        val quoteSlot = slot<CreateContractsRequest>()

        every { priceEngineClient.queryPrice(any()) } answers {
            PriceQueryResponse(
                UUID.randomUUID(),
                Money.of(firstArg<PriceQueryRequest>().overriddenPrice?.price ?: defaultPremiumPrice, "SEK")
            )
        }

        every { productPricingClient.createContract(capture(quoteSlot), any()) } returns listOf(
            CreateContractResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        )

        // WHEN
        val quoteResponse = quoteClient.createSwedishApartmentQuote(
            birthdate = "1995-07-28",
            ssn = ssn
        )

        // Override price and assert the reponse
        val overridePrice = 88.8.toBigDecimal()
        val overriddenPriceQuote =
            quoteClient.overrideQuotePrice(quoteResponse.id, overridePrice, "someone@hedvig,com")

        quoteClient.signQuote(
            quoteId = quoteResponse.id,
            firstName = "Mr",
            lastName = "Overrider",
            email = "s@hedvig.com"
        )

        // THEN

        // Verify the overridden price, both the API responses, the one sent to P&P and the ones exposed in the quote API
        assertThat(quoteResponse.price).isEqualByComparingTo(defaultPremiumPrice)

        assertThat(overriddenPriceQuote.body!!.price!!).isEqualByComparingTo(overridePrice)
        assertThat(quoteSlot.captured.quotes.first().premium).isEqualByComparingTo(overridePrice)

        val quote = quoteClient.getQuote(quoteResponse.id)
        assertThat(quote!!.price!!).isEqualByComparingTo(overridePrice)
    }

    private fun assertProductPricingLineItems(
        expectedLineItems: List<LineItem>,
        actualLineItems: List<com.hedvig.productPricingObjects.dtos.LineItem>?
    ) {
        assertThat(actualLineItems).isNotNull()

        assertThat(actualLineItems!!.size).isEqualTo(expectedLineItems.size)

        expectedLineItems.forEach {
            val lineItem = actualLineItems.firstOrNull() { qli -> it.type == qli.type && it.subType == qli.subType }
            assertThat(lineItem!!).isNotNull()
            assertThat(it.amount.compareTo(lineItem.amount)).isEqualTo(0)
        }
    }

    private fun assertQuoteApiLineItems(
        expectedLineItems: List<LineItem>,
        actualLineItems: List<com.hedvig.underwriter.model.LineItem>
    ) {
        assertThat(actualLineItems).isNotNull()
        assertThat(actualLineItems.size).isEqualTo(expectedLineItems.size)

        expectedLineItems.forEach {
            val lineItem = actualLineItems.firstOrNull() { qli ->
                it.type == qli.type && it.subType == qli.subType
            }
            assertThat(lineItem!!).isNotNull()
            assertThat(it.amount.compareTo(lineItem.amount)).isEqualTo(0)
        }
    }
}

private operator fun Any?.get(key: String): Any? = (this as Map<String, Any>)[key]
private operator fun Any?.get(index: Int): Any = (this as List<Any>)[index]
