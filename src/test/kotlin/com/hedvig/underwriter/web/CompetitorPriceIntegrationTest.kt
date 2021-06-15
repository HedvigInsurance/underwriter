package com.hedvig.underwriter.web

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import com.hedvig.productPricingObjects.dtos.Agreement
import com.hedvig.productPricingObjects.enums.AgreementStatus
import com.hedvig.underwriter.serviceIntegration.lookupService.LookupServiceClient
import com.hedvig.underwriter.serviceIntegration.lookupService.dtos.CompetitorPricing
import com.hedvig.underwriter.serviceIntegration.memberService.MemberServiceClient
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.Flag
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.HelloHedvigResponseDto
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsSsnAlreadySignedMemberResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.PersonStatusDto
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.notificationService.NotificationServiceClient
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineClient
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryRequest
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingClient
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.testhelp.QuoteClient
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.javamoney.moneta.Money
import org.jdbi.v3.core.Jdbi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner
import java.util.UUID

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CompetitorPriceIntegrationTest {

    @Autowired
    private lateinit var jdbi: Jdbi

    @Autowired
    private lateinit var quoteClient: QuoteClient

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @MockkBean(relaxed = true)
    lateinit var notificationServiceClient: NotificationServiceClient

    @MockkBean
    lateinit var priceEngineClient: PriceEngineClient

    @MockkBean
    lateinit var memberServiceClient: MemberServiceClient

    @MockkBean
    lateinit var productPricingClient: ProductPricingClient

    @MockkBean
    lateinit var lookupServiceClient: LookupServiceClient

    val activeAgreement = Agreement.SwedishApartment(UUID.randomUUID(),
        mockk(),
        mockk(),
        mockk(),
        null,
        AgreementStatus.ACTIVE,
        mockk(),
        mockk(),
        0,
        100)

    @Before
    fun setup() {
        every { memberServiceClient.personStatus(any()) } returns ResponseEntity.status(200)
            .body(PersonStatusDto(Flag.GREEN))
        every { memberServiceClient.checkPersonDebt(any()) } returns ResponseEntity.status(200).body(null)
        every { memberServiceClient.checkIsSsnAlreadySignedMemberEntity(any()) } returns IsSsnAlreadySignedMemberResponse(
            false)
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200)
            .body(HelloHedvigResponseDto("12345"))
        every { memberServiceClient.updateMemberSsn(any(), any()) } returns Unit
        every { memberServiceClient.signQuote(any(), any()) } returns ResponseEntity.status(200)
            .body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(any(), any()) } returns ResponseEntity.status(200).body("")
        every {
            productPricingClient.createContract(any(),
                any())
        } returns listOf(CreateContractResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
        every { productPricingClient.getAgreement(any()) } returns ResponseEntity.status(200).body(activeAgreement)
        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "NOK"))
    }

    @Test
    fun `Test quoting with competitor price`() {

        val priceRequestSlot = slot<PriceQueryRequest>()
        every { priceEngineClient.queryPrice(capture(priceRequestSlot)) } returns PriceQueryResponse(UUID.randomUUID(),
            Money.of(99, "SEK"))

        val competitorPrice = 89
        val competitorLivingArea = 55
        val competitorNrInsured = 2

        val dataCollectionSlot = slot<UUID>()
        every {
            lookupServiceClient.getMatchingCompetitorPrice(capture(dataCollectionSlot),
                "SEK",
                "houseContentInsurance")
        } returns
            ResponseEntity(CompetitorPricing(
                Money.of(81, "SEK"),
                Money.of(competitorPrice, "SEK"),
                Money.of(0, "SEK"),
                "H Street 14",
                "12345",
                competitorLivingArea,
                competitorNrInsured),
                HttpStatus.OK)

        // Create a quote WITHOUT data collectionId, should not call lookupService and not forward any competitor price to PE

        quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            zip = "12345",
            livingSpace = 55,
            householdSize = 3,
            dataCollectionId = null)

        assertThat(dataCollectionSlot.isCaptured).isFalse()
        assertThat(priceRequestSlot.captured.competitorPrice).isNull()

        // Create a quote with data collectionId, and matching zipcode/livingSpace
        dataCollectionSlot.clear()
        var dataCollectionId = UUID.randomUUID()
        quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            zip = "12345",
            livingSpace = 55,
            dataCollectionId = dataCollectionId)

        assertThat(dataCollectionSlot.captured).isEqualTo(dataCollectionId)
        assertThat(priceRequestSlot.captured.competitorPrice!!.price.compareTo(competitorPrice.toBigDecimal())).isEqualTo(
            0)
        assertThat(priceRequestSlot.captured.competitorPrice!!.numberInsured).isEqualTo(competitorNrInsured)

        // Create a quote with data collectionId, and matching livingSpace but NOT zipcode
        // Should call the lookupService, AND should forward the competitor price
        dataCollectionSlot.clear()
        dataCollectionId = UUID.randomUUID()
        quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            zip = "54321",
            livingSpace = 55,
            dataCollectionId = dataCollectionId)

        assertThat(dataCollectionSlot.captured).isEqualTo(dataCollectionId)
        assertThat(priceRequestSlot.captured.competitorPrice!!.price.compareTo(competitorPrice.toBigDecimal())).isEqualTo(
            0)
        assertThat(priceRequestSlot.captured.competitorPrice!!.numberInsured).isEqualTo(competitorNrInsured)

        // Create a quote with data collectionId, and matching zipCode but NOT livingSpace
        // Should call the lookupService, and should forward the competitor price
        dataCollectionSlot.clear()
        dataCollectionId = UUID.randomUUID()
        quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            zip = "12345",
            livingSpace = 56,
            dataCollectionId = dataCollectionId)

        assertThat(dataCollectionSlot.captured).isEqualTo(dataCollectionId)
        assertThat(priceRequestSlot.captured.competitorPrice!!.price.compareTo(competitorPrice.toBigDecimal())).isEqualTo(
            0)
        assertThat(priceRequestSlot.captured.competitorPrice!!.numberInsured).isEqualTo(competitorNrInsured)

        // Create a quote with data collectionId, and NOT matching zipCode or livingSpace
        // Should call the lookupService, and NOT forward the competitor price
        dataCollectionSlot.clear()
        dataCollectionId = UUID.randomUUID()
        quoteClient.createSwedishApartmentQuote(
            street = "H Street 14, th. 1111 Wherever",
            zip = "11111",
            livingSpace = 99,
            dataCollectionId = dataCollectionId)

        assertThat(dataCollectionSlot.captured).isEqualTo(dataCollectionId)
        assertThat(priceRequestSlot.captured.competitorPrice).isNull()

        // When the lookupService call fails, due to 404, 500 or error returned, the flow
        // should not break and no competitor price will be forwarded
        every {
            lookupServiceClient.getMatchingCompetitorPrice(any(), any(), any())
        } returns
            ResponseEntity(HttpStatus.NOT_FOUND)

        dataCollectionId = UUID.randomUUID()
        quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            zip = "12345",
            livingSpace = 56,
            dataCollectionId = dataCollectionId)

        assertThat(priceRequestSlot.captured.competitorPrice).isNull()

        every {
            lookupServiceClient.getMatchingCompetitorPrice(any(), any(), any())
        } returns
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)

        dataCollectionId = UUID.randomUUID()
        quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            zip = "12345",
            livingSpace = 56,
            dataCollectionId = dataCollectionId)

        assertThat(priceRequestSlot.captured.competitorPrice).isNull()

        every {
            lookupServiceClient.getMatchingCompetitorPrice(any(), any(), any())
        } throws Exception("LookupServive error")

        dataCollectionId = UUID.randomUUID()
        quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            zip = "12345",
            livingSpace = 56,
            dataCollectionId = dataCollectionId)

        assertThat(priceRequestSlot.captured.competitorPrice).isNull()
    }
}
