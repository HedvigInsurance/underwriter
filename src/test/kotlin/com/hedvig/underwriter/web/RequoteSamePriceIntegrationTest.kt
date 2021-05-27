package com.hedvig.underwriter.web

import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import io.mockk.every
import org.javamoney.moneta.Money
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.hedvig.productPricingObjects.dtos.Agreement
import com.hedvig.productPricingObjects.enums.AgreementStatus
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.Flag
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.HelloHedvigResponseDto
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.PersonStatusDto
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.LineItem
import com.hedvig.underwriter.testhelp.IntegrationTest
import com.hedvig.underwriter.testhelp.QuoteClient
import io.mockk.mockk
import org.jdbi.v3.core.Jdbi
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import java.lang.RuntimeException
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class RequoteSamePriceIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var jdbi: Jdbi

    @Autowired
    private lateinit var quoteClient: QuoteClient

    val activeAgreement = Agreement.SwedishApartment(UUID.randomUUID(), mockk(), mockk(), mockk(), null, AgreementStatus.ACTIVE, mockk(), mockk(), 0, 100)

    @Before
    fun setup() {
        every { memberServiceClient.personStatus(any()) } returns ResponseEntity.status(200).body(PersonStatusDto(Flag.GREEN))
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto("12345"))
        every { memberServiceClient.signQuote(any(), any()) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { productPricingClient.createContract(any(), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
        every { productPricingClient.getAgreement(any()) } returns ResponseEntity.status(200).body(activeAgreement)
        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "NOK"))
    }

    @Test
    fun `Test re-quoting of SE apartment`() {

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "SEK"))

        // Create a quote
        with(quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            memberId = "1234"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(20, "SEK"))

        // Create same quote again, should reuse old price
        with(quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            memberId = "1234"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another address, should trigger the new price
        with(quoteClient.createSwedishApartmentQuote(
            street = "Another Test Apa",
            memberId = "1234"
            )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another livingspace, should trigger the new price
        with(quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            memberId = "1234",
            livingSpace = 112
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another householdSize, should trigger the new price
        with(quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            memberId = "1234",
            householdSize = 2
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another subType, should trigger the new price
        with(quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            memberId = "1234",
            subType = "RENT"
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another name, should reuse old price
        with(quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            memberId = "1234",
            firstName = "Diddy"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another email, should reuse old price
        with(quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            memberId = "1234",
            email = "diddy@kong.com"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another phoneNumber, should reuse old price
        with(quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            memberId = "1234",
            phoneNumber = "46987654321"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another memberid, should reuse old price
        with(quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            memberId = "1553"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another ssn/birthdate, should trigger the new price
        with(quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            memberId = "1234",
            ssn = "200001011234",
            birthdate = "2000-01-01"
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }
    }

    @Test
    fun `Test re-quoting of SE house`() {

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "SEK"))

        // Create a quote
        with(quoteClient.createSwedishHouseQuote(
            street = "Test Banan"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(20, "SEK"))

        // Create same quote again, should reuse old price
        with(quoteClient.createSwedishHouseQuote(
            street = "Test Banan"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another address, should trigger the new price
        with(quoteClient.createSwedishHouseQuote(
            street = "Another Test Banan"
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another livingspace, should trigger the new price
        with(quoteClient.createSwedishHouseQuote(
            street = "Test Banan",
            livingSpace = 112
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another householdSize, should trigger the new price
        with(quoteClient.createSwedishHouseQuote(
            street = "Test Banan",
            householdSize = 2
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another ancillaryArea, should trigger the new price
        with(quoteClient.createSwedishHouseQuote(
            street = "Test Banan",
            ancillaryArea = 12
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another yearOfConstruction, should trigger the new price
        with(quoteClient.createSwedishHouseQuote(
            street = "Test Banan",
            yearOfConstruction = 1971
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another numberOfBathrooms, should trigger the new price
        with(quoteClient.createSwedishHouseQuote(
            street = "Test Banan",
            numberOfBathrooms = 2
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another subleted, should trigger the new price
        with(quoteClient.createSwedishHouseQuote(
            street = "Test Banan",
            subleted = true
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }
    }

    @Test
    fun `Test re-quoting of NO home content`() {

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "SEK"))

        // Create a quote
        with(quoteClient.createNorwegianHomeContentQuote(
            street = "Test Citron"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(20, "SEK"))

        // Create same quote again, should reuse old price
        with(quoteClient.createNorwegianHomeContentQuote(
            street = "Test Citron"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another address, should trigger the new price
        with(quoteClient.createNorwegianHomeContentQuote(
            street = "Another Test Citron"
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another livingspace, should trigger the new price
        with(quoteClient.createNorwegianHomeContentQuote(
            street = "Test Citron",
            livingSpace = 112
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another birthdate, should trigger the new price
        with(quoteClient.createNorwegianHomeContentQuote(
            street = "Test Citron",
            birthdate = "1980-08-04"
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }
    }

    @Test
    fun `Test re-quoting of DK home content`() {

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "SEK"))

        // Create a quote
        with(quoteClient.createDanishHomeContentQuote(
            street = "Test Duffy"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(20, "SEK"))

        // Create same quote again, should reuse old price
        with(quoteClient.createDanishHomeContentQuote(
            street = "Test Duffy"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another address, should trigger the new price
        with(quoteClient.createDanishHomeContentQuote(
            street = "Another Test Duffy"
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another livingspace, should trigger the new price
        with(quoteClient.createDanishHomeContentQuote(
            street = "Test Duffy",
            livingSpace = 112
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another bbrid, should not trigger the new price since its ok if old is null
        with(quoteClient.createDanishHomeContentQuote(
            street = "Test Duffy",
            bbrId = "112"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another floor, should trigger the new price
        with(quoteClient.createDanishHomeContentQuote(
            street = "Test Duffy",
            floor = "2"
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }
        // Create quote with another apartment, should trigger the new price
        with(quoteClient.createDanishHomeContentQuote(
            street = "Test Duffy",
            apartment = "2"
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }
    }

    @Test
    fun `Test re-quoting of DK accident`() {

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "SEK"))

        // Create a quote
        with(quoteClient.createDanishAccidentQuote(
            street = "Test Elephant"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(20, "SEK"))

        // Create same quote again, should reuse old price
        with(quoteClient.createDanishAccidentQuote(
            street = "Test Elephant"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another address, should trigger the new price
        with(quoteClient.createDanishAccidentQuote(
            street = "Another Test Elephant"
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another livingspace, should trigger the new price
        with(quoteClient.createDanishAccidentQuote(
            street = "Test Elephant",
            coInsured = 2
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }
    }

    @Test
    fun `Test re-quoting of DK travel`() {

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "SEK"))

        // Create a quote
        with(quoteClient.createDanishTravelQuote(
            street = "Test Floody"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(20, "SEK"))

        // Create same quote again, should reuse old price
        with(quoteClient.createDanishTravelQuote(
            street = "Test Floody"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another address, should trigger the new price
        with(quoteClient.createDanishTravelQuote(
            street = "Another Test Floody"
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        // Create quote with another livingspace, should trigger the new price
        with(quoteClient.createDanishTravelQuote(
            street = "Test Floody",
            coInsured = 2
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }
    }

    @Test
    fun `Test re-quoting with older than 30d quotes`() {

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "SEK"))

        // Create a quote
        val id = with(quoteClient.createDanishTravelQuote(
            street = "Test Greedy"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")

            id
        }

        updateCreatedAt(id, Instant.now().minus(31, ChronoUnit.DAYS))

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(20, "SEK"))

        // Create same quote again, should not reuse old price since > 30d
        with(quoteClient.createDanishTravelQuote(
            street = "Test Greedy"
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }
    }

    @Test
    fun `Test re-quoting with older than 30d quotes and 1 change in recent 30d`() {

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "SEK"))

        // Create a quote
        val id = with(quoteClient.createDanishTravelQuote(
            street = "Test Halo"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")

            id
        }

        updateCreatedAt(id, Instant.now().minus(31, ChronoUnit.DAYS))

        // Create same quote again
        with(quoteClient.createDanishTravelQuote(
            street = "Test Halo"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(20, "SEK"))

        // Create same quote again, should not reuse old price since no price change in last 30d
        with(quoteClient.createDanishTravelQuote(
            street = "Test Halo"
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(30, "SEK"))

        // Create same quote again, should reuse old price since previous price change not >30d
        with(quoteClient.createDanishTravelQuote(
            street = "Test Halo"
        )) {
            assertThat(price.toPlainString()).isEqualTo("20")
            assertThat(currency).isEqualTo("SEK")
        }
    }

    @Test
    fun `Test re-quoting of lineitems and priceFrom`() {

        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "SEK"))

        // Create a quote
        with(quoteClient.createSwedishApartmentQuote(
            street = "Test Monkey"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")
        }

        every { priceEngineClient.queryPrice(any()) } returns
            PriceQueryResponse(
                UUID.randomUUID(),
                Money.of(12, "SEK"),
                listOf(
                    LineItem("ApType1", "ApSubType1", BigDecimal.TEN),
                    LineItem("ApType2", "ApSubType2", BigDecimal.ONE),
                    LineItem("ApType3", "ApSubType3", BigDecimal.ONE)
                )
            )

        // Create same quote again, should use new price since it has line items and the old not
        val quoteId = with(quoteClient.createSwedishApartmentQuote(
            street = "Test Monkey"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")

            val quote = quoteClient.getQuote(id)!!
            assertThat(quote.priceFrom).isNull()
            assertThat(quote.lineItems.size).isEqualTo(3)
            assertThat(quote.lineItems[0].amount.compareTo(BigDecimal.TEN) == 0)
            assertThat(quote.lineItems[0].amount.compareTo(BigDecimal.ONE) == 0)
            assertThat(quote.lineItems[0].amount.compareTo(BigDecimal.ONE) == 0)

            id
        }

        every { priceEngineClient.queryPrice(any()) } returns
            PriceQueryResponse(
                UUID.randomUUID(),
                Money.of(20, "SEK"),
                listOf(
                    LineItem("ApType1", "ApSubType1", BigDecimal.TEN),
                    LineItem("ApType2", "ApSubType2", BigDecimal.TEN)
                )
            )

        // Create same quote again, should use old price since price changed (and both got line items)
        with(quoteClient.createSwedishApartmentQuote(
            street = "Test Monkey"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")

            val quote = quoteClient.getQuote(id)!!
            assertThat(quote.priceFrom).isEqualTo(quoteId)
            assertThat(quote.lineItems.size).isEqualTo(3)
            assertThat(quote.lineItems[0].amount.compareTo(BigDecimal.TEN) == 0)
            assertThat(quote.lineItems[0].amount.compareTo(BigDecimal.ONE) == 0)
            assertThat(quote.lineItems[0].amount.compareTo(BigDecimal.ONE) == 0)
        }

        // Create same quote again, should use old price since price changed
        with(quoteClient.createSwedishApartmentQuote(
            street = "Test Monkey"
        )) {
            assertThat(price.toPlainString()).isEqualTo("12")
            assertThat(currency).isEqualTo("SEK")

            val quote = quoteClient.getQuote(id)!!
            assertThat(quote.priceFrom).isEqualTo(quoteId)
            assertThat(quote.lineItems.size).isEqualTo(3)
            assertThat(quote.lineItems[0].amount.compareTo(BigDecimal.TEN) == 0)
            assertThat(quote.lineItems[0].amount.compareTo(BigDecimal.ONE) == 0)
            assertThat(quote.lineItems[0].amount.compareTo(BigDecimal.ONE) == 0)
        }
    }

    private fun updateCreatedAt(quoteId: UUID, createdAt: Instant): Unit =
        jdbi.withHandle<Unit, RuntimeException> { handle ->
            handle.createUpdate("UPDATE master_quotes SET created_at = :createdAt WHERE id = :quoteId")
                .bind("quoteId", quoteId)
                .bind("createdAt", createdAt)
                .execute()
        }
}
