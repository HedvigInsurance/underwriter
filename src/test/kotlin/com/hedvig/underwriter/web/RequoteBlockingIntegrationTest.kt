package com.hedvig.underwriter.web

import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.web.dtos.CompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.SignedQuoteResponseDto
import io.mockk.every
import org.javamoney.moneta.Money
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.hedvig.productPricingObjects.dtos.Agreement
import com.hedvig.productPricingObjects.enums.AgreementStatus
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.Flag
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.HelloHedvigResponseDto
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.PersonStatusDto
import com.hedvig.underwriter.testhelp.IntegrationTest
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.util.UUID

class RequoteBlockingIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    val activeAgreement = Agreement.SwedishApartment(UUID.randomUUID(), mockk(), mockk(), mockk(), null, AgreementStatus.ACTIVE, mockk(), mockk(), 0, 100)
    val inactiveAgreement = Agreement.SwedishApartment(UUID.randomUUID(), mockk(), mockk(), mockk(), null, AgreementStatus.TERMINATED, mockk(), mockk(), 0, 100)

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

        // Create a quote
        with(createSwedishApartmentQuote<String>(
            ssn = "199110112399",
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)
        }

        // Create same quote agian, should be ok and then sign it
        with(createSwedishApartmentQuote<CompleteQuoteResponseDto>(
            ssn = "199110112399",
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)

            signQuote(body!!.id, null)
        }

        // Cannot requote a signed quote withsame address and ssn
        with(createSwedishApartmentQuote<String>(
            ssn = "199110112399",
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity"
        )) {
            assertThat(statusCode.value()).isEqualTo(500)
            assertThat(body!!).contains("Creation of quote is blocked")
        }

        // withanother ssn it should be ok
        with(createSwedishApartmentQuote<CompleteQuoteResponseDto>(
            ssn = "190905249801",
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)
        }

        // withanother address it should be ok
        with(createSwedishApartmentQuote<CompleteQuoteResponseDto>(
            ssn = "199110112399",
            street = "ApStreet 1",
            zip = "1234",
            city = "ApCity"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)
        }

        every { productPricingClient.getAgreement(any()) } returns ResponseEntity.status(200).body(inactiveAgreement)

        // It should be ok if not an active agreement
        with(createSwedishApartmentQuote<String>(
            ssn = "199110112399",
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)
        }
    }

    @Test
    fun `Test re-quoting of SE house`() {

        // Create a quote
        with(createSwedishHouseQuote<CompleteQuoteResponseDto>(
            ssn = "199110112399",
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)
        }

        // Create same quote agian, should be ok and then sign it
        with(createSwedishHouseQuote<CompleteQuoteResponseDto>(
            ssn = "199110112399",
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)

            signQuote(body!!.id, null)
        }

        // Cannot requote a signed quote withsame address and ssn
        with(createSwedishHouseQuote<String>(
            ssn = "199110112399",
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity"
        )) {
            assertThat(statusCode.value()).isEqualTo(500)
            assertThat(body!!).contains("Creation of quote is blocked")
        }

        // withanother ssn it should be ok
        with(createSwedishHouseQuote<CompleteQuoteResponseDto>(
            ssn = "190905249801",
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)
        }

        // withanother address it should be ok
        with(createSwedishHouseQuote<CompleteQuoteResponseDto>(
            ssn = "199110112399",
            street = "ApStreet 1",
            zip = "1234",
            city = "ApCity"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)
        }

        every { productPricingClient.getAgreement(any()) } returns ResponseEntity.status(200).body(inactiveAgreement)

        // It should be ok if not an active agreement
        with(createSwedishHouseQuote<String>(
            ssn = "199110112399",
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)
        }
    }

    @Test
    fun `Test re-quoting of NO home content`() {

        // Create a quote
        with(createNorwegianHomeContentQuote<CompleteQuoteResponseDto>(
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity",
            birthdate = "1970-01-01"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)
        }

        // Create same quote again, should be ok and then sign it
        with(createNorwegianHomeContentQuote<CompleteQuoteResponseDto>(
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity",
            birthdate = "1970-01-01"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)

            signQuote(body!!.id, "11077941012")
        }

        // Cannot re-quote a signed quote withsame address and birthdate
        with(createNorwegianHomeContentQuote<String>(
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity",
            birthdate = "1970-01-01"
        )) {
            assertThat(statusCode.value()).isEqualTo(500)
            assertThat(body!!).contains("Creation of quote is blocked")
        }

        // withanother birth date it should be ok
        with(createNorwegianHomeContentQuote<CompleteQuoteResponseDto>(
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity",
            birthdate = "1971-01-01"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)
        }

        // withanother address it should be ok
        with(createNorwegianHomeContentQuote<CompleteQuoteResponseDto>(
            street = "ApStreet 1",
            zip = "1234",
            city = "ApCity",
            birthdate = "1970-01-01"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)
        }

        every { productPricingClient.getAgreement(any()) } returns ResponseEntity.status(200).body(inactiveAgreement)

        // It should be ok if not an active agreement
        with(createNorwegianHomeContentQuote<String>(
            street = "ApStreet 1234",
            zip = "1234",
            city = "ApCity",
            birthdate = "1970-01-01"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)
        }
    }

    @Test
    fun `Test re-quoting of NO travel`() {

        // Create quote and then sign it
        with(createNorwegianTravelQuote<CompleteQuoteResponseDto>(
            birthdate = "1970-01-01"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)

            signQuote(body!!.id, "11077941012")
        }

        // Can re-quote a signed quote withsame birthdate
        with(createNorwegianTravelQuote<String>(
            birthdate = "1970-01-01"
        )) {
            assertThat(statusCode.value()).isEqualTo(200)
        }
    }

    private inline fun <reified T : Any> createNorwegianTravelQuote(birthdate: String): ResponseEntity<T> {
        val request = """
            {
                "firstName":null,
                "lastName":null,
                "currentInsurer":null,
                "birthDate":"$birthdate",
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

        return postJson("/_/v1/quotes", request)
    }

    private inline fun <reified T : Any> createNorwegianHomeContentQuote(birthdate: String, street: String, zip: String, city: String): ResponseEntity<T> {
        val request = """
            {
                "firstName": null,
                "lastName": null,
                "currentInsurer": null,
                "birthDate": "$birthdate",
                "ssn": null,
                "quotingPartner": "HEDVIG",
                "productType": "HOME_CONTENT",
                "incompleteQuoteData": {
                    "type": "norwegianHomeContents",
                    "street": "$street",
                    "zipCode": "$zip",
                    "city": "$city",
                    "livingSpace": 111,
                    "coInsured": 1,
                    "youth": false,
                    "subType": "OWN"
                },
                "shouldComplete": true,
                "underwritingGuidelinesBypassedBy": null
            }
        """.trimIndent()

        return postJson("/_/v1/quotes", request)
    }

    private inline fun <reified T : Any> createSwedishApartmentQuote(ssn: String, street: String, zip: String, city: String): ResponseEntity<T> {
        val request = """           
            {
                "firstName": null,
                "lastName": null,
                "currentInsurer": null,
                "birthDate": null,
                "ssn": "$ssn",
                "quotingPartner": "HEDVIG",
                "productType": "APARTMENT",
                "incompleteQuoteData": {
                    "type": "apartment",
                    "street": "$street",
                    "zipCode": "$zip",
                    "city": "$city",
                    "livingSpace": 111,
                    "householdSize": 1,
                    "floor": 0,
                    "subType": "BRF"
                },
                "shouldComplete": true
            }
        """.trimIndent()

        return postJson("/_/v1/quotes", request)
    }

    private inline fun <reified T : Any> createSwedishHouseQuote(ssn: String, street: String, zip: String, city: String): ResponseEntity<T> {
        val request = """           
            {
                "firstName": null,
                "lastName": null,
                "currentInsurer": null,
                "birthDate": null,
                "ssn": "$ssn",
                "quotingPartner": "HEDVIG",
                "productType": "HOUSE",
                "incompleteQuoteData": {
                    "type": "house",
                    "street": "$street",
                    "zipCode": "$zip",
                    "city": "$city",
                    "livingSpace": 111,
                    "householdSize": 1,
                    "ancillaryArea": 11,
                    "yearOfConstruction": 1970,
                    "numberOfBathrooms": 1,
                    "extraBuildings": [{
                        "id": null,
                        "type": "CARPORT",
                        "area": 11,
                        "hasWaterConnected": true
                    }],
                    "floor": 0,
                    "subleted": false
                },
                "shouldComplete": true
            }
        """.trimIndent()

        return postJson("/_/v1/quotes", request)
    }

    private fun signQuote(quoteId: UUID, ssn: String?): ResponseEntity<SignedQuoteResponseDto> {

        val ssnString = if (ssn != null) "\"$ssn\"" else "null"

        val request = """
            {
                "name": {
                    "firstName": "Apan",
                    "lastName": "Apansson"
                },
                "ssn": $ssnString,
                "startDate": "${LocalDate.now()}",
                "email": "apan@apansson.se"
            }
        """.trimIndent()

        return postJson("/_/v1/quotes/$quoteId/sign", request)
    }

    private inline fun <reified T : Any> postJson(url: String, data: String): ResponseEntity<T> {

        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.contentType = MediaType.APPLICATION_JSON

        return restTemplate.exchange(url, HttpMethod.POST, HttpEntity(data, headers), T::class.java)
    }
}
