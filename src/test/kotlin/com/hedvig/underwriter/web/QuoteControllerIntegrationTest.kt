package com.hedvig.underwriter.web

import assertk.assertThat
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
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractsRequest
import com.hedvig.underwriter.testhelp.IntegrationTest
import com.hedvig.underwriter.testhelp.QuoteClient
import io.mockk.every
import io.mockk.slot
import org.javamoney.moneta.Money
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import java.util.UUID

class QuoteControllerIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var quoteClient: QuoteClient

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

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "SEK"), priceEngineLineItems)
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto("12345"))
        every { productPricingClient.createContract(capture(quoteSlot), any()) } returns listOf(
            CreateContractResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        )
        every { memberServiceClient.signQuote(any(), any()) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(any(), capture(msFinalizeOnboardingRequest)) } returns ResponseEntity.status(200).body("")
        every { memberServiceClient.personStatus(any()) } returns ResponseEntity.status(200).body(PersonStatusDto(Flag.GREEN))

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

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(999, "SEK"), null)

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
