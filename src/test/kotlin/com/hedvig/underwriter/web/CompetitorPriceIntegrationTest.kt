package com.hedvig.underwriter.web

import assertk.assertThat
import assertk.assertions.isEqualByComparingTo
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
import io.mockk.CapturingSlot
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
import java.math.BigDecimal
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

    val activeAgreement = Agreement.SwedishApartment(
        UUID.randomUUID(),
        mockk(),
        mockk(),
        mockk(),
        null,
        AgreementStatus.ACTIVE,
        mockk(),
        mockk(),
        0,
        100
    )

    @Before
    fun setup() {
        every { memberServiceClient.personStatus(any()) } returns ResponseEntity.status(200)
            .body(PersonStatusDto(Flag.GREEN))
        every { memberServiceClient.checkPersonDebt(any()) } returns ResponseEntity.status(200).body(null)
        every { memberServiceClient.checkIsSsnAlreadySignedMemberEntity(any()) } returns IsSsnAlreadySignedMemberResponse(
            false
        )
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200)
            .body(HelloHedvigResponseDto("12345"))
        every { memberServiceClient.updateMemberSsn(any(), any()) } returns Unit
        every { memberServiceClient.signQuote(any(), any()) } returns ResponseEntity.status(200)
            .body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(any(), any()) } returns ResponseEntity.status(200).body("")
        every {
            productPricingClient.createContract(
                any(),
                any()
            )
        } returns listOf(CreateContractResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
        every { productPricingClient.getAgreement(any()) } returns ResponseEntity.status(200).body(activeAgreement)
        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "NOK"))
    }

    @Test
    fun `Test quoting with competitor price`() {

        val hedvigPrice = 99.0.toBigDecimal()
        val competitorMonthlyGrossPrice = 89.toBigDecimal()
        val competitorMonthlyDiscount = 10.toBigDecimal()
        val competitorMonthlyNetPrice = competitorMonthlyGrossPrice - competitorMonthlyDiscount
        val competitorLivingArea = 55
        val competitorNrInsured = 2

        val priceRequestSlot = slot<PriceQueryRequest>()
        every { priceEngineClient.queryPrice(capture(priceRequestSlot)) } answers {

            // Return the competitorPrice if present - otherwise the hedvig price
            val priceToReturn = firstArg<PriceQueryRequest>().competitorPrice?.price ?: hedvigPrice
            PriceQueryResponse(
                UUID.randomUUID(),
                Money.of(priceToReturn, "SEK")
            )
        }

        val dataCollectionSlot = slot<UUID>()
        mockInsurelyResponse(
            dataCollectionSlot,
            competitorMonthlyNetPrice,
            competitorMonthlyGrossPrice,
            competitorMonthlyDiscount,
            "H Street 14",
            "12345",
            competitorLivingArea,
            competitorNrInsured
        )

        // Create a quote WITHOUT data collectionId, should not call lookupService and not forward any competitor price to PE

        var quote = quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            zip = "12345",
            livingSpace = 55,
            dataCollectionId = null
        )

        assertThat(dataCollectionSlot.isCaptured).isFalse()
        assertThat(priceRequestSlot.captured.competitorPrice).isNull()
        assertThat(quote.price).isEqualByComparingTo(hedvigPrice)

        // Create a quote with data collectionId, and matching zipcode/livingSpace
        dataCollectionSlot.clear()
        var dataCollectionId = UUID.randomUUID()
        quote = quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            zip = "12345",
            livingSpace = 55,
            dataCollectionId = dataCollectionId
        )

        val expectedPrice = competitorMonthlyNetPrice

        assertThat(dataCollectionSlot.captured).isEqualTo(dataCollectionId)
        assertThat(priceRequestSlot.captured.competitorPrice!!.price.compareTo(expectedPrice)).isEqualTo(
            0
        )
        assertThat(priceRequestSlot.captured.competitorPrice!!.numberInsured).isEqualTo(competitorNrInsured)
        assertThat(quote.price).isEqualByComparingTo(expectedPrice)

        // Create a quote with data collectionId, and matching livingSpace but NOT zipcode
        // Should call the lookupService, AND should forward the competitor price
        dataCollectionSlot.clear()
        dataCollectionId = UUID.randomUUID()
        quote = quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            zip = "54321",
            livingSpace = 55,
            dataCollectionId = dataCollectionId
        )

        assertThat(dataCollectionSlot.captured).isEqualTo(dataCollectionId)
        assertThat(priceRequestSlot.captured.competitorPrice!!.price).isEqualByComparingTo(expectedPrice)
        assertThat(priceRequestSlot.captured.competitorPrice!!.numberInsured).isEqualTo(competitorNrInsured)
        assertThat(quote.price).isEqualByComparingTo(expectedPrice)

        // Create a quote with data collectionId, and matching zipCode but NOT livingSpace
        // Should call the lookupService, and should forward the competitor price
        dataCollectionSlot.clear()
        dataCollectionId = UUID.randomUUID()
        quote = quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            zip = "12345",
            livingSpace = 56,
            dataCollectionId = dataCollectionId
        )

        assertThat(dataCollectionSlot.captured).isEqualTo(dataCollectionId)
        assertThat(priceRequestSlot.captured.competitorPrice!!.price).isEqualByComparingTo(expectedPrice)
        assertThat(priceRequestSlot.captured.competitorPrice!!.numberInsured).isEqualTo(competitorNrInsured)
        assertThat(quote.price).isEqualTo(expectedPrice)

        // Create a quote with data collectionId, and NOT matching zipCode or livingSpace,
        // BUT with matching street+nr
        // Should call the lookupService, and should forward the competitor price
        dataCollectionSlot.clear()
        dataCollectionId = UUID.randomUUID()
        quote = quoteClient.createSwedishApartmentQuote(
            street = "H Street 14, th. 1111 Wherever",
            zip = "11111",
            livingSpace = 99,
            dataCollectionId = dataCollectionId
        )

        assertThat(dataCollectionSlot.captured).isEqualTo(dataCollectionId)
        assertThat(priceRequestSlot.captured.competitorPrice!!.price).isEqualByComparingTo(expectedPrice)
        assertThat(priceRequestSlot.captured.competitorPrice!!.numberInsured).isEqualTo(competitorNrInsured)
        assertThat(quote.price).isEqualTo(expectedPrice)

        // Create a quote with data collectionId, and NO zipCode or livingSpace supplied by Insurely,
        // but with matching street+nr
        // Should call the lookupService, and should forward the competitor price
        mockInsurelyResponse(
            dataCollectionSlot,
            competitorMonthlyNetPrice,
            competitorMonthlyGrossPrice,
            competitorMonthlyDiscount,
            "H Street 14",
            null,
            null,
            competitorNrInsured
        )

        dataCollectionSlot.clear()
        dataCollectionId = UUID.randomUUID()
        quote = quoteClient.createSwedishApartmentQuote(
            street = "H Street 14, th. 1111 Wherever",
            zip = "11111",
            livingSpace = 99,
            dataCollectionId = dataCollectionId
        )

        assertThat(dataCollectionSlot.captured).isEqualTo(dataCollectionId)
        assertThat(priceRequestSlot.captured.competitorPrice!!.price).isEqualByComparingTo(expectedPrice)
        assertThat(priceRequestSlot.captured.competitorPrice!!.numberInsured).isEqualTo(competitorNrInsured)
        assertThat(quote.price).isEqualTo(expectedPrice)

        // Set back the default Insurely response
        mockInsurelyResponse(
            dataCollectionSlot,
            competitorMonthlyNetPrice,
            competitorMonthlyGrossPrice,
            competitorMonthlyDiscount,
            "H Street 14",
            "12345",
            competitorLivingArea,
            competitorNrInsured
        )

        // Create a quote with data collectionId, and NOT matching zipCode or livingSpace,
        // but with almost, but not exactly, matching street+nr
        // Should call the lookupService, and NOT forward the competitor price
        dataCollectionSlot.clear()
        dataCollectionId = UUID.randomUUID()
        quote = quoteClient.createSwedishApartmentQuote(
            street = "H Street 13, th. 1111 Wherever",
            zip = "11111",
            livingSpace = 99,
            dataCollectionId = dataCollectionId
        )

        assertThat(dataCollectionSlot.captured).isEqualTo(dataCollectionId)
        assertThat(priceRequestSlot.captured.competitorPrice).isNull()
        assertThat(quote.price).isEqualByComparingTo(hedvigPrice)

        // When the lookupService call fails, due to 404, 500 or error returned, the flow
        // should not break and no competitor price will be forwarded
        every {
            lookupServiceClient.getMatchingCompetitorPrice(any(), any(), any())
        } returns
            ResponseEntity(HttpStatus.NOT_FOUND)

        dataCollectionId = UUID.randomUUID()
        quote = quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            zip = "12345",
            livingSpace = 57, // New size, to not get old quote
            dataCollectionId = dataCollectionId
        )

        assertThat(priceRequestSlot.captured.competitorPrice).isNull()
        assertThat(quote.price).isEqualByComparingTo(hedvigPrice)

        every {
            lookupServiceClient.getMatchingCompetitorPrice(any(), any(), any())
        } returns
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)

        dataCollectionId = UUID.randomUUID()
        quote = quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            zip = "12345",
            livingSpace = 58, // New size, to not get old quote
            dataCollectionId = dataCollectionId
        )

        assertThat(priceRequestSlot.captured.competitorPrice).isNull()
        assertThat(quote.price).isEqualByComparingTo(hedvigPrice)

        every {
            lookupServiceClient.getMatchingCompetitorPrice(any(), any(), any())
        } throws Exception("LookupServive error")

        dataCollectionId = UUID.randomUUID()
        quote = quoteClient.createSwedishApartmentQuote(
            street = "Test Apa",
            zip = "12345",
            livingSpace = 59, // New size, to not get old quote
            dataCollectionId = dataCollectionId
        )

        assertThat(priceRequestSlot.captured.competitorPrice).isNull()
        assertThat(quote.price).isEqualByComparingTo(hedvigPrice)
    }

    private fun mockInsurelyResponse(
        dataCollectionSlot: CapturingSlot<UUID>,
        competitorMonthlyNetPrice: BigDecimal,
        competitorMonthlyGrossPrice: BigDecimal,
        competitorMonthlyDiscount: BigDecimal,
        insuranceObjectAddress: String?,
        competitorPostalCode: String?,
        competitorLivingArea: Int?,
        competitorNrInsured: Int
    ) {
        every {
            lookupServiceClient.getMatchingCompetitorPrice(
                capture(dataCollectionSlot),
                "SEK",
                "houseContentInsurance"
            )
        } returns
            ResponseEntity(
                CompetitorPricing(
                    monthlyNetPremium = Money.of(competitorMonthlyNetPrice, "SEK"),
                    monthlyGrossPremium = Money.of(competitorMonthlyGrossPrice, "SEK"),
                    monthlyDiscount = Money.of(competitorMonthlyDiscount, "SEK"),
                    insuranceObjectAddress = insuranceObjectAddress,
                    postalCode = competitorPostalCode,
                    livingArea = competitorLivingArea,
                    numberInsured = competitorNrInsured
                ),
                HttpStatus.OK
            )
    }
}
