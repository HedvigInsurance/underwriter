package com.hedvig.underwriter.web

import com.hedvig.underwriter.model.Market
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsSsnAlreadySignedMemberResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UpdateSsnRequest
import com.hedvig.underwriter.serviceIntegration.notificationService.NotificationServiceClient
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryRequest
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.web.dtos.CompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.SignedQuoteResponseDto
import com.hedvig.underwriter.web.dtos.UnderwriterQuoteSignRequest
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
import org.javamoney.moneta.Money
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.hedvig.productPricingObjects.dtos.AgreementQuote
import com.hedvig.underwriter.serviceIntegration.memberService.MemberServiceClient
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.FinalizeOnBoardingRequest
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.HelloHedvigResponseDto
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineClient
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingClient
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractsRequest
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForObject
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random.Default.nextLong

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RapioNorwayIntegrationTest {

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

    @Test
    fun `Create travel quote and sign it successfully`() {

        val ssn = "11077941012"
        val memberId = nextLong(Long.MAX_VALUE).toString()
        val agreementId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val now = Instant.now()
        val today = LocalDate.now()

        val msSsnAlreadySignedRequest = slot<String>()
        val msUpdateSsnRequest1 = slot<Long>()
        val msUpdateSsnRequest2 = slot<UpdateSsnRequest>()
        val msSignQuoteRequest1 = slot<Long>()
        val msSignQuoteRequest2 = slot<UnderwriterQuoteSignRequest>()
        val msFinalizeOnboardingReques1 = slot<String>()
        val msFinalizeOnboardingReques2 = slot<FinalizeOnBoardingRequest>()
        val peQueryPriceRequest = slot<PriceQueryRequest.NorwegianTravel>()
        val ppCreateContractRequest = slot<CreateContractsRequest>()

        // Mock clients and capture the outgoing requests for later validation
        every { memberServiceClient.checkIsSsnAlreadySignedMemberEntity(capture(msSsnAlreadySignedRequest)) } returns IsSsnAlreadySignedMemberResponse(false)
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto(memberId))
        every { memberServiceClient.updateMemberSsn(capture(msUpdateSsnRequest1), capture(msUpdateSsnRequest2)) } returns Unit
        every { memberServiceClient.signQuote(capture(msSignQuoteRequest1), capture(msSignQuoteRequest2)) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(capture(msFinalizeOnboardingReques1), capture(msFinalizeOnboardingReques2)) } returns ResponseEntity.status(200).body("")
        every { productPricingClient.createContract(capture(ppCreateContractRequest), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), agreementId, contractId))
        every { priceEngineClient.queryPrice(capture(peQueryPriceRequest)) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "NOK"))

        val quoteRequest = """
            {
                "firstName":null,
                "lastName":null,
                "currentInsurer":null,
                "birthDate":"1988-01-01",
                "ssn":null,
                "quotingPartner":"HEDVIG",
                "productType":"TRAVEL",
                "incompleteQuoteData":{
                    "type":"norwegianTravel",
                    "coInsured":1,
                    "youth":false
                },
                "shouldComplete":true,
                "underwritingGuidelinesBypassedBy":null
            }
        """.trimIndent()

        // Create quote
        val quoteResponse = postJson<CompleteQuoteResponseDto>("/_/v1/quotes", quoteRequest)!!

        // Validate quote response
        assertThat(quoteResponse.price.toString()).isEqualTo("12")
        assertThat(quoteResponse.currency).isEqualTo("NOK")
        assertThat(quoteResponse.validTo.isAfter(now)).isEqualTo(true)

        val signRequest = """
            {
                "name": {
                    "firstName": "Apan",
                    "lastName": "Apansson"
                },
                "ssn": "$ssn",
                "startDate": "$today",
                "email": "apan@apansson.se"
            }
        """.trimIndent()

        // Sign quote
        val signResponse = postJson<SignedQuoteResponseDto>("/_/v1/quotes/${quoteResponse.id}/sign", signRequest)!!

        // Validate sign response
        assertThat(signResponse.memberId).isEqualTo(memberId)
        assertThat(signResponse.market).isEqualTo(Market.NORWAY)

        // Get quote
        val quote = restTemplate.getForObject("/_/v1/quotes/${quoteResponse.id}", Quote::class.java)

        // Validate stored quote
        assertThat(quote.id).isEqualTo(quoteResponse.id)
        assertThat(quote.createdAt.isAfter(now)).isEqualTo(true)
        assertThat(quote.price).isEqualTo(quoteResponse.price)
        assertThat(quote.currency).isEqualTo("NOK")
        assertThat(quote.productType.name).isEqualTo("TRAVEL")
        assertThat(quote.state.name).isEqualTo("SIGNED")
        assertThat(quote.initiatedFrom.name).isEqualTo("RAPIO")
        assertThat(quote.attributedTo.name).isEqualTo("HEDVIG")
        assertThat(quote.startDate).isEqualTo(today)
        assertThat(quote.validity).isEqualTo(30 * 24 * 60 * 60)
        assertThat(quote.breachedUnderwritingGuidelines).isNull()
        assertThat(quote.underwritingGuidelinesBypassedBy).isNull()
        assertThat(quote.memberId).isEqualTo(memberId)
        assertThat(quote.agreementId).isEqualTo(agreementId)
        assertThat(quote.contractId).isEqualTo(contractId)
        assertThat(quote.data).isInstanceOf(NorwegianTravelData::class.java)
        val data = quote.data as NorwegianTravelData
        assertThat(data.ssn).isEqualTo(ssn)
        assertThat(data.birthDate.toString()).isEqualTo("1988-01-01")
        assertThat(data.firstName).isEqualTo("Apan")
        assertThat(data.lastName).isEqualTo("Apansson")
        assertThat(data.email).isEqualTo("apan@apansson.se")
        assertThat(data.phoneNumber).isNull()
        assertThat(data.coInsured).isEqualTo(1)
        assertThat(data.isYouth).isEqualTo(false)
        assertThat(data.internalId).isNull()

        // Validate requests to Member Service
        assertThat(msSsnAlreadySignedRequest.captured).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msUpdateSsnRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest2.captured.nationality.name).isEqualTo("NORWAY")
        assertThat(msSignQuoteRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msSignQuoteRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msFinalizeOnboardingReques1.captured).isEqualTo(memberId)
        with(msFinalizeOnboardingReques2) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.ssn).isEqualTo(ssn)
            assertThat(captured.firstName).isEqualTo("Apan")
            assertThat(captured.lastName).isEqualTo("Apansson")
            assertThat(captured.email).isEqualTo("apan@apansson.se")
            assertThat(captured.phoneNumber).isNull()
            assertThat(captured.address).isNull()
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.birthDate.toString()).isEqualTo("1988-01-01")
        }

        // Validate request to Price Enging
        with(peQueryPriceRequest) {
            assertThat(captured.holderMemberId).isNull()
            assertThat(captured.quoteId).isEqualTo(quoteResponse.id)
            assertThat(captured.holderBirthDate.toString()).isEqualTo("1988-01-01")
            assertThat(captured.numberCoInsured).isEqualTo(1)
            assertThat(captured.lineOfBusiness.name).isEqualTo("REGULAR")
        }

        // Validate request to Product Pricing Service
        with(ppCreateContractRequest) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.mandate!!.firstName).isEqualTo("Apan")
            assertThat(captured.mandate!!.lastName).isEqualTo("Apansson")
            assertThat(captured.mandate!!.ssn).isEqualTo(ssn)
            assertThat(captured.mandate!!.referenceToken).isEmpty()
            assertThat(captured.mandate!!.signature).isEmpty()
            assertThat(captured.mandate!!.oscpResponse).isEmpty()
            assertThat(captured.signSource.name).isEqualTo("RAPIO")
            assertThat(captured.quotes.size).isEqualTo(1)
            val ppQuote = captured.quotes[0] as AgreementQuote.NorwegianTravelQuote
            assertThat(ppQuote.quoteId).isEqualTo(quoteResponse.id)
            assertThat(ppQuote.fromDate).isEqualTo(today)
            assertThat(ppQuote.toDate).isNull()
            assertThat(ppQuote.premium).isEqualTo(quoteResponse.price)
            assertThat(ppQuote.currency).isEqualTo("NOK")
            assertThat(ppQuote.currentInsurer).isNull()
            assertThat(ppQuote.coInsured.size).isEqualTo(1)
            assertThat(ppQuote.coInsured[0].firstName).isNull()
            assertThat(ppQuote.coInsured[0].lastName).isNull()
            assertThat(ppQuote.coInsured[0].ssn).isNull()
            assertThat(ppQuote.lineOfBusiness.name).isEqualTo("REGULAR")
        }
    }

    @Test
    fun `Create travel quote and sign it without or invalid ssn should fail`() {

        val memberId = nextLong(Long.MAX_VALUE).toString()
        val agreementId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val now = Instant.now()
        val today = LocalDate.now()

        every { memberServiceClient.checkIsSsnAlreadySignedMemberEntity(any()) } returns IsSsnAlreadySignedMemberResponse(false)
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto(memberId))
        every { memberServiceClient.updateMemberSsn(any(), any()) } returns Unit
        every { memberServiceClient.signQuote(any(), any()) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(any(), any()) } returns ResponseEntity.status(200).body("")
        every { productPricingClient.createContract(any(), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), agreementId, contractId))
        every { priceEngineClient.queryPrice(any()) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "NOK"))

        val quoteRequest = """
            {
                "firstName":null,
                "lastName":null,
                "currentInsurer":null,
                "birthDate":"1912-12-12",
                "ssn":null,
                "quotingPartner":"HEDVIG",
                "productType":"TRAVEL",
                "incompleteQuoteData":{
                    "type":"norwegianTravel",
                    "coInsured":1,
                    "youth":false
                },
                "shouldComplete":true,
                "underwritingGuidelinesBypassedBy":null
            }
        """.trimIndent()

        val quoteResponse = postJson<CompleteQuoteResponseDto>("/_/v1/quotes", quoteRequest)!!

        assertThat(quoteResponse.price.toString(), "12")
        assertThat(quoteResponse.currency, "NOK")
        assertThat(quoteResponse.validTo.isAfter(now)).isEqualTo(true)

        val signRequestNoSsn = """
            {
                "name": {
                    "firstName": "Apan",
                    "lastName": "Banansson"
                },
                "startDate": "$today",
                "email": "apan@apansson.se"
            }
        """.trimIndent()

        assertThat {
            postJson<SignedQuoteResponseDto>("/_/v1/quotes/${quoteResponse.id}/sign", signRequestNoSsn)!!
        }.isFailure()

        val signRequestInvalidSsn = """
            {
                "name": {
                    "firstName": "Apan",
                    "lastName": "Banansson"
                },
                "ssn": "11077900000",
                "startDate": "$today",
                "email": "apan@apansson.se"
            }
        """.trimIndent()

        assertThat {
            postJson<SignedQuoteResponseDto>("/_/v1/quotes/${quoteResponse.id}/sign", signRequestInvalidSsn)!!
        }.isFailure()
    }

    @Test
    fun `Create travel quote with ssn fails`() {

        val quoteRequestWithSsn = """
            {
                "firstName":null,
                "lastName":null,
                "currentInsurer":null,
                "birthDate":"1912-12-12",
                "ssn":11077941012,
                "quotingPartner":"HEDVIG",
                "productType":"TRAVEL",
                "incompleteQuoteData":{
                    "type":"norwegianTravel",
                    "coInsured":1,
                    "youth":false
                },
                "shouldComplete":true,
                "underwritingGuidelinesBypassedBy":null
            }
        """.trimIndent()

        assertThat {
            postJson<CompleteQuoteResponseDto>("/_/v1/quotes", quoteRequestWithSsn)!!
        }.isFailure()
    }

    @Test
    fun `Create home content quote and sign it successfully`() {

        val ssn = "11077941012"
        val memberId = nextLong(Long.MAX_VALUE).toString()
        val agreementId = UUID.randomUUID()
        val contractId = UUID.randomUUID()
        val now = Instant.now()
        val today = LocalDate.now()

        val msSsnAlreadySignedRequest = slot<String>()
        val msUpdateSsnRequest1 = slot<Long>()
        val msUpdateSsnRequest2 = slot<UpdateSsnRequest>()
        val msSignQuoteRequest1 = slot<Long>()
        val msSignQuoteRequest2 = slot<UnderwriterQuoteSignRequest>()
        val msFinalizeOnboardingReques1 = slot<String>()
        val msFinalizeOnboardingReques2 = slot<FinalizeOnBoardingRequest>()
        val peQueryPriceRequest = slot<PriceQueryRequest.NorwegianHomeContent>()
        val ppCreateContractRequest = slot<CreateContractsRequest>()

        // Mock clients and capture the outgoing requests for later validation
        every { memberServiceClient.checkIsSsnAlreadySignedMemberEntity(capture(msSsnAlreadySignedRequest)) } returns IsSsnAlreadySignedMemberResponse(false)
        every { memberServiceClient.createMember() } returns ResponseEntity.status(200).body(HelloHedvigResponseDto(memberId))
        every { memberServiceClient.updateMemberSsn(capture(msUpdateSsnRequest1), capture(msUpdateSsnRequest2)) } returns Unit
        every { memberServiceClient.signQuote(capture(msSignQuoteRequest1), capture(msSignQuoteRequest2)) } returns ResponseEntity.status(200).body(UnderwriterQuoteSignResponse(1L, true))
        every { memberServiceClient.finalizeOnBoarding(capture(msFinalizeOnboardingReques1), capture(msFinalizeOnboardingReques2)) } returns ResponseEntity.status(200).body("")
        every { productPricingClient.createContract(capture(ppCreateContractRequest), any()) } returns listOf(CreateContractResponse(UUID.randomUUID(), agreementId, contractId))
        every { priceEngineClient.queryPrice(capture(peQueryPriceRequest)) } returns PriceQueryResponse(UUID.randomUUID(), Money.of(12, "NOK"))

        val quoteRequest = """
            {
                "firstName": null,
                "lastName": null,
                "currentInsurer": null,
                "birthDate": "1988-01-01",
                "ssn": null,
                "quotingPartner": "HEDVIG",
                "productType": "HOME_CONTENT",
                "incompleteQuoteData": {
                    "type": "norwegianHomeContents",
                    "street": "ApGatan",
                    "zipCode": "1234",
                    "city": "ApCity",
                    "livingSpace": 122,
                    "coInsured": 0,
                    "youth": false,
                    "subType": "OWN"
                },
                "shouldComplete": true,
                "underwritingGuidelinesBypassedBy": null
            }
        """.trimIndent()

        // Create quote
        val quoteResponse = postJson<CompleteQuoteResponseDto>("/_/v1/quotes", quoteRequest)!!

        // Validate quote response
        assertThat(quoteResponse.price.toString()).isEqualTo("12")
        assertThat(quoteResponse.currency).isEqualTo("NOK")
        assertThat(quoteResponse.validTo.isAfter(now)).isEqualTo(true)

        val signRequest = """
            {
                "name": {
                    "firstName": "Apan",
                    "lastName": "Apansson"
                },
                "ssn": "$ssn",
                "startDate": "$today",
                "email": "apan@apansson.se"
            }
        """.trimIndent()

        // Sign quote
        val signResponse = postJson<SignedQuoteResponseDto>("/_/v1/quotes/${quoteResponse.id}/sign", signRequest)!!

        // Validate sign response
        assertThat(signResponse.memberId).isEqualTo(memberId)
        assertThat(signResponse.market).isEqualTo(Market.NORWAY)

        // Get quote
        val quote = restTemplate.getForObject("/_/v1/quotes/${quoteResponse.id}", Quote::class.java)

        // Validate stored quote
        assertThat(quote.id).isEqualTo(quoteResponse.id)
        assertThat(quote.createdAt.isAfter(now)).isEqualTo(true)
        assertThat(quote.price).isEqualTo(quoteResponse.price)
        assertThat(quote.currency).isEqualTo("NOK")
        assertThat(quote.productType.name).isEqualTo("HOME_CONTENT")
        assertThat(quote.state.name).isEqualTo("SIGNED")
        assertThat(quote.initiatedFrom.name).isEqualTo("RAPIO")
        assertThat(quote.attributedTo.name).isEqualTo("HEDVIG")
        assertThat(quote.startDate).isEqualTo(today)
        assertThat(quote.validity).isEqualTo(30 * 24 * 60 * 60)
        assertThat(quote.breachedUnderwritingGuidelines).isNull()
        assertThat(quote.underwritingGuidelinesBypassedBy).isNull()
        assertThat(quote.memberId).isEqualTo(memberId)
        assertThat(quote.agreementId).isEqualTo(agreementId)
        assertThat(quote.contractId).isEqualTo(contractId)
        assertThat(quote.data).isInstanceOf(NorwegianHomeContentsData::class.java)
        val data = quote.data as NorwegianHomeContentsData
        assertThat(data.ssn).isEqualTo(ssn)
        assertThat(data.birthDate.toString()).isEqualTo("1988-01-01")
        assertThat(data.firstName).isEqualTo("Apan")
        assertThat(data.lastName).isEqualTo("Apansson")
        assertThat(data.email).isEqualTo("apan@apansson.se")
        assertThat(data.phoneNumber).isNull()
        assertThat(data.street).isEqualTo("ApGatan")
        assertThat(data.city).isEqualTo("ApCity")
        assertThat(data.zipCode).isEqualTo("1234")
        assertThat(data.livingSpace).isEqualTo(122)
        assertThat(data.coInsured).isEqualTo(0)
        assertThat(data.isYouth).isEqualTo(false)
        assertThat(data.type.name).isEqualTo("OWN")
        assertThat(data.internalId).isNull()

        // Validate requests to Member Service
        assertThat(msSsnAlreadySignedRequest.captured).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msUpdateSsnRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msUpdateSsnRequest2.captured.nationality.name).isEqualTo("NORWAY")
        assertThat(msSignQuoteRequest1.captured).isEqualTo(memberId.toLong())
        assertThat(msSignQuoteRequest2.captured.ssn).isEqualTo(ssn)
        assertThat(msFinalizeOnboardingReques1.captured).isEqualTo(memberId)
        with(msFinalizeOnboardingReques2) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.ssn).isEqualTo(ssn)
            assertThat(captured.firstName).isEqualTo("Apan")
            assertThat(captured.lastName).isEqualTo("Apansson")
            assertThat(captured.email).isEqualTo("apan@apansson.se")
            assertThat(captured.phoneNumber).isNull()
            assertThat(captured.address!!.street).isEqualTo("ApGatan")
            assertThat(captured.address!!.city).isEqualTo("ApCity")
            assertThat(captured.address!!.zipCode).isEqualTo("1234")
            assertThat(captured.address!!.apartmentNo).isEqualTo("")
            assertThat(captured.address!!.floor).isEqualTo(0)
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.birthDate.toString()).isEqualTo("1988-01-01")
        }

        // Validate request to Price Enging
        with(peQueryPriceRequest) {
            assertThat(captured.holderMemberId).isNull()
            assertThat(captured.quoteId).isEqualTo(quoteResponse.id)
            assertThat(captured.holderBirthDate.toString()).isEqualTo("1988-01-01")
            assertThat(captured.numberCoInsured).isEqualTo(0)
            assertThat(captured.lineOfBusiness.name).isEqualTo("OWN")
            assertThat(captured.postalCode).isEqualTo("1234")
            assertThat(captured.squareMeters).isEqualTo(122)
        }

        // Validate request to Product Pricing Service
        with(ppCreateContractRequest) {
            assertThat(captured.memberId).isEqualTo(memberId)
            assertThat(captured.mandate!!.firstName).isEqualTo("Apan")
            assertThat(captured.mandate!!.lastName).isEqualTo("Apansson")
            assertThat(captured.mandate!!.ssn).isEqualTo(ssn)
            assertThat(captured.mandate!!.referenceToken).isEmpty()
            assertThat(captured.mandate!!.signature).isEmpty()
            assertThat(captured.mandate!!.oscpResponse).isEmpty()
            assertThat(captured.signSource.name).isEqualTo("RAPIO")
            assertThat(captured.quotes.size).isEqualTo(1)

            val ppQuote = captured.quotes[0] as AgreementQuote.NorwegianHomeContentQuote
            assertThat(ppQuote.quoteId).isEqualTo(quoteResponse.id)
            assertThat(ppQuote.fromDate).isEqualTo(today)
            assertThat(ppQuote.toDate).isNull()
            assertThat(ppQuote.premium).isEqualTo(quoteResponse.price)
            assertThat(ppQuote.currency).isEqualTo("NOK")
            assertThat(ppQuote.currentInsurer).isNull()
            assertThat(ppQuote.address.city).isEqualTo("ApCity")
            assertThat(ppQuote.address.postalCode).isEqualTo("1234")
            assertThat(ppQuote.address.country.name).isEqualTo("NO")
            assertThat(ppQuote.address.street).isEqualTo("ApGatan")
            assertThat(ppQuote.address.coLine).isEqualTo(null)
            assertThat(ppQuote.coInsured).isEmpty()
            assertThat(ppQuote.squareMeters).isEqualTo(122)
            assertThat(ppQuote.lineOfBusiness.name).isEqualTo("OWN")
        }
    }

    private inline fun <reified T : Any> postJson(url: String, data: String): T? {

        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.contentType = MediaType.APPLICATION_JSON

        return restTemplate
            .postForObject(
                url,
                HttpEntity(
                    data, headers
                )
            )
    }
}