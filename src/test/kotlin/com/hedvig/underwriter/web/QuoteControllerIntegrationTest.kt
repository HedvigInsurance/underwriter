package com.hedvig.underwriter.web

import arrow.core.Right
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.fasterxml.jackson.databind.JsonNode
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.DanishHomeContentsType
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.service.DebtChecker
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsMemberAlreadySignedResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsSsnAlreadySignedMemberResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.notificationService.NotificationServiceClient
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineService
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.LineItem
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingClient
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractsRequest
import com.hedvig.underwriter.testhelp.databuilder.DanishHomeContentsQuoteRequestBuilder
import com.hedvig.underwriter.testhelp.databuilder.DanishHomeContentsQuoteRequestDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.SwedishApartmentQuoteRequestBuilder
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.javamoney.moneta.Money
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForObject
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import java.util.UUID

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuoteControllerIntegrationTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @MockkBean(relaxed = true)
    lateinit var notificationServiceClient: NotificationServiceClient

    @MockkBean
    lateinit var priceEngineService: PriceEngineService

    @MockkBean
    lateinit var debtChecker: DebtChecker

    @MockkBean
    lateinit var memberService: MemberService

    @MockkBean
    lateinit var productPricingClient: ProductPricingClient

    @Autowired
    lateinit var quoteService: QuoteService

    @Test
    fun completeQuote() {

        // GIVEN
        val uuid: UUID = UUID.fromString("71919787-70d2-4614-bd4a-26427861991d")
        val priceEngineLineItems = listOf(
            LineItem("PREMIUM", "premium", 118.0.toBigDecimal()),
            LineItem("TAX", "tax_dk_gf", 3.33333333.toBigDecimal()),
            LineItem("TAX", "tax_dk_ipt", 1.123456.toBigDecimal())
        )

        val quoteSlot = slot<CreateContractsRequest>()

        every { debtChecker.passesDebtCheck(any()) } returns listOf()
        every { priceEngineService.querySwedishApartmentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(12, "SEK"),
            priceEngineLineItems
        )
        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { memberService.isSsnAlreadySignedMemberEntity(any()) } returns IsSsnAlreadySignedMemberResponse(false)
        every { memberService.createMember() } returns "12345"
        every { memberService.updateMemberSsn(any(), any()) } returns Unit
        every { productPricingClient.createContract(capture(quoteSlot), any()) } returns listOf(
            CreateContractResponse(uuid, UUID.randomUUID(), UUID.randomUUID())
        )
        every { memberService.signQuote(any(), any()) } returns Right(UnderwriterQuoteSignResponse(1L, true))
        every { memberService.finalizeOnboarding(any(), any()) } returns Unit
        quoteService.createQuote(
            SwedishApartmentQuoteRequestBuilder().build(),
            uuid,
            QuoteInitiatedFrom.RAPIO,
            null,
            false
        )

        // WHEN
        val signData = """
            {
            "name": {
              "firstName": "Mr",
              "lastName": "Svensson"
            },
            "startDate": null,
            "email": "s@hedvig.com",
            "insuranceCompany": "other"
            }
        """.trimIndent()

        val result: JsonNode? = postJson("/_/v1/quotes/$uuid/sign", signData)

        // THEN
        verify { memberService.finalizeOnboarding(any(), eq("s@hedvig.com")) }

        // Verify lineItems, both the ones sent to P&P and the ones exposed in the quote API
        assertProductPricingLineItems(priceEngineLineItems, quoteSlot.captured.quotes.first().lineItems)

        val getQuoteJsonResponse = getJson("/_/v1/quotes/$uuid")
        assertQuoteApiLineItems(priceEngineLineItems, getQuoteJsonResponse!!.withArray("lineItems"))
    }

    @Test
    fun `creates, saves and retrieves danish home content quote properly`() {
        val homeContentsData = DanishHomeContentsQuoteRequestDataBuilder().build(
            newStreet = "test street",
            newApartment = "4",
            newZipCode = "123",
            newBbrId = "12345"
        )

        val quoteRequest = DanishHomeContentsQuoteRequestBuilder().build(homeContentsData)

        every { priceEngineService.queryDanishHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(999, "SEK")
        )

        quoteService.createQuote(
            quoteRequest = quoteRequest,
            initiatedFrom = QuoteInitiatedFrom.RAPIO,
            underwritingGuidelinesBypassedBy = null,
            updateMemberService = false
        )
        assertThat((quoteService.getLatestQuoteForMemberId("123")?.data as DanishHomeContentsData).livingSpace).isEqualTo(
            100
        )
        assertThat((quoteService.getLatestQuoteForMemberId("123")?.data as DanishHomeContentsData).coInsured).isEqualTo(
            1
        )
        assertThat((quoteService.getLatestQuoteForMemberId("123")?.data as DanishHomeContentsData).isStudent).isEqualTo(
            false
        )
        assertThat((quoteService.getLatestQuoteForMemberId("123")?.data as DanishHomeContentsData).type).isEqualTo(
            DanishHomeContentsType.RENT
        )
        assertThat((quoteService.getLatestQuoteForMemberId("123")?.data as DanishHomeContentsData).street).isEqualTo("test street")
        assertThat((quoteService.getLatestQuoteForMemberId("123")?.data as DanishHomeContentsData).apartment).isEqualTo(
            "4"
        )
        assertThat((quoteService.getLatestQuoteForMemberId("123")?.data as DanishHomeContentsData).zipCode).isEqualTo("123")
        assertThat((quoteService.getLatestQuoteForMemberId("123")?.data as DanishHomeContentsData).city).isEqualTo("city")
        assertThat((quoteService.getLatestQuoteForMemberId("123")?.data as DanishHomeContentsData).floor).isEqualTo("2")
        assertThat((quoteService.getLatestQuoteForMemberId("123")?.data as DanishHomeContentsData).bbrId).isEqualTo("12345")
    }

    private fun postJson(url: String, signData: String): JsonNode? {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.contentType = MediaType.APPLICATION_JSON

        val result: JsonNode? = restTemplate
            .postForObject(
                url,
                HttpEntity(
                    signData, headers
                )
            )
        return result
    }

    private fun getJson(url: String): JsonNode? {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.contentType = MediaType.APPLICATION_JSON

        val result: JsonNode? =
            restTemplate
                .getForObject(
                    url,
                    JsonNode::class.java
                )
        return result
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
        actualLineItems: JsonNode?
    ) {
        assertThat(actualLineItems).isNotNull()

        assertThat(actualLineItems!!.size()).isEqualTo(expectedLineItems.size)

        expectedLineItems.forEach {
            val lineItem = actualLineItems.firstOrNull() { qli ->
                it.type == qli.get("type").textValue() && it.subType == qli.get("subType").textValue()
            }
            assertThat(lineItem!!).isNotNull()
            assertThat(it.amount.compareTo(lineItem.get("amount").decimalValue())).isEqualTo(0)
        }
    }
}
