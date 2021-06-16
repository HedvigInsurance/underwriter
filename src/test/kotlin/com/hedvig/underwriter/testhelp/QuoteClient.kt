package com.hedvig.underwriter.testhelp

import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.web.dtos.CompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.QuoteBundleResponseDto
import com.hedvig.underwriter.web.dtos.SignedQuoteResponseDto
import com.hedvig.underwriter.web.dtos.SignedQuotesResponseDto
import org.apache.commons.lang.RandomStringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class QuoteClient {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    fun createSwedishApartmentQuote(
        firstName: String? = "King",
        lastName: String? = "Kong",
        email: String? = "king@kong.com",
        phoneNumber: String? = "46123456789",
        memberId: String? = null,
        ssn: String = "199110112399",
        birthdate: String? = null,
        street: String = "ApStreet ${RandomStringUtils.randomNumeric(2)}",
        zip: String = "12345",
        city: String = "ApCity",
        livingSpace: Int = 111,
        householdSize: Int = 1,
        subType: String = "BRF",
        dataCollectionId: UUID? = null
    ): CompleteQuoteResponseDto {
        return createSwedishApartmentQuote<CompleteQuoteResponseDto>(
            firstName,
            lastName,
            email,
            phoneNumber,
            memberId,
            ssn,
            birthdate,
            street,
            zip,
            city,
            livingSpace,
            householdSize,
            subType,
            dataCollectionId
        ).body!!
    }

    fun createSwedishApartmentQuoteRaw(
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        phoneNumber: String? = null,
        memberId: String? = null,
        ssn: String = "199110112399",
        birthdate: String = "19911011",
        street: String = "ApStreet ${RandomStringUtils.randomNumeric(2)}",
        zip: String = "12345",
        city: String = "ApCity",
        livingSpace: Int = 111,
        householdSize: Int = 1,
        subType: String = "BRF"
    ): ResponseEntity<String> {
        return createSwedishApartmentQuote<String>(
            firstName,
            lastName,
            email,
            phoneNumber,
            memberId,
            ssn,
            birthdate,
            street,
            zip,
            city,
            livingSpace,
            householdSize,
            subType
        )
    }

    fun createSwedishHouseQuote(
        ssn: String = "199110112399",
        street: String = "ApStreet ${RandomStringUtils.randomNumeric(2)}",
        zip: String = "12345",
        city: String = "ApCity",
        livingSpace: Int = 111,
        householdSize: Int = 1,
        ancillaryArea: Int = 11,
        yearOfConstruction: Int = 1970,
        numberOfBathrooms: Int = 1,
        subleted: Boolean = false
    ): CompleteQuoteResponseDto {
        return createSwedishHouseQuote<CompleteQuoteResponseDto>(
            ssn,
            street,
            zip,
            city,
            livingSpace,
            householdSize,
            ancillaryArea,
            yearOfConstruction,
            numberOfBathrooms,
            subleted
        ).body!!
    }

    fun createSwedishHouseQuoteRaw(
        ssn: String = "199110112399",
        street: String = "ApStreet ${RandomStringUtils.randomNumeric(2)}",
        zip: String = "12345",
        city: String = "ApCity",
        livingSpace: Int = 111,
        householdSize: Int = 1,
        ancillaryArea: Int = 11,
        yearOfConstruction: Int = 1970,
        numberOfBathrooms: Int = 1,
        subleted: Boolean = false
    ): ResponseEntity<String> {
        return createSwedishHouseQuote<String>(
            ssn,
            street,
            zip,
            city,
            livingSpace,
            householdSize,
            ancillaryArea,
            yearOfConstruction,
            numberOfBathrooms,
            subleted
        )
    }

    fun createNorwegianHomeContentQuote(
        birthdate: String = "1944-08-04",
        street: String = "ApStreet ${RandomStringUtils.randomNumeric(2)}",
        zip: String = "12345",
        city: String = "ApCity",
        livingSpace: Int = 111,
        coInsured: Int = 1,
        youth: Boolean = false,
        subType: String = "OWN"
    ): CompleteQuoteResponseDto {
        return createNorwegianHomeContentQuote<CompleteQuoteResponseDto>(
            birthdate,
            street,
            zip,
            city,
            livingSpace,
            coInsured,
            youth,
            subType
        ).body!!
    }

    fun createNorwegianHomeContentQuoteRaw(
        birthdate: String = "1944-08-04",
        street: String = "ApStreet ${RandomStringUtils.randomNumeric(2)}",
        zip: String = "12345",
        city: String = "ApCity",
        livingSpace: Int = 111,
        coInsured: Int = 1,
        youth: Boolean = false,
        subType: String = "OWN"
    ): ResponseEntity<String> {
        return createNorwegianHomeContentQuote<String>(
            birthdate,
            street,
            zip,
            city,
            livingSpace,
            coInsured,
            youth,
            subType
        )
    }

    fun createNorwegianTravelQuote(
        birthdate: String = "1944-08-04",
        coInsured: Int = 1,
        youth: Boolean = false
    ): CompleteQuoteResponseDto {
        return createNorwegianTravelQuote<CompleteQuoteResponseDto>(birthdate, coInsured, youth).body!!
    }

    fun createNorwegianTravelQuoteRaw(
        birthdate: String = "1944-08-04",
        coInsured: Int = 1,
        youth: Boolean = false
    ): ResponseEntity<String> {
        return createNorwegianTravelQuote<String>(birthdate, coInsured, youth)
    }

    fun createDanishHomeContentQuote(
        birthdate: String = "1944-08-04",
        street: String = "ApStreet ${RandomStringUtils.randomNumeric(2)}",
        apartment: String = "1tv",
        floor: String = "1",
        bbrId: String? = null,
        zip: String = "1234",
        city: String = "ApCity",
        livingSpace: Int = 111,
        coInsured: Int = 1,
        student: Boolean = false,
        subType: String = "OWN"
    ): CompleteQuoteResponseDto {
        return createDanishHomeContentQuote<CompleteQuoteResponseDto>(
            birthdate,
            street,
            apartment,
            floor,
            bbrId,
            zip,
            city,
            livingSpace,
            coInsured,
            student,
            subType
        ).body!!
    }

    fun createDanishHomeContentQuoteRaw(
        birthdate: String = "1944-08-04",
        street: String = "ApStreet ${RandomStringUtils.randomNumeric(2)}",
        apartment: String = "1tv",
        floor: String = "1",
        bbrId: String? = null,
        zip: String = "1234",
        city: String = "ApCity",
        livingSpace: Int = 111,
        coInsured: Int = 1,
        student: Boolean = false,
        subType: String = "OWN"
    ): ResponseEntity<String> {
        return createDanishHomeContentQuote<String>(
            birthdate,
            street,
            apartment,
            floor,
            bbrId,
            zip,
            city,
            livingSpace,
            coInsured,
            student,
            subType
        )
    }

    fun createDanishAccidentQuote(
        birthdate: String = "1944-08-04",
        street: String = "ApStreet ${RandomStringUtils.randomNumeric(2)}",
        apartment: String = "1tv",
        floor: String = "1",
        bbrId: String? = null,
        zip: String = "1234",
        city: String = "ApCity",
        coInsured: Int = 1,
        student: Boolean = false
    ): CompleteQuoteResponseDto {
        return createDanishAccidentQuote<CompleteQuoteResponseDto>(
            birthdate,
            street,
            apartment,
            floor,
            bbrId,
            zip,
            city,
            coInsured,
            student
        ).body!!
    }

    fun createDanishAccidentQuoteRaw(
        birthdate: String = "1944-08-04",
        street: String = "ApStreet 11",
        apartment: String = "1tv",
        floor: String = "1",
        bbrId: String? = null,
        zip: String = "1234",
        city: String = "ApCity",
        coInsured: Int = 1,
        student: Boolean = false

    ): ResponseEntity<String> {
        return createDanishAccidentQuote<String>(
            birthdate,
            street,
            apartment,
            floor,
            bbrId,
            zip,
            city,
            coInsured,
            student
        )
    }

    fun createDanishTravelQuote(
        birthdate: String = "1944-08-04",
        street: String = "ApStreet ${RandomStringUtils.randomNumeric(2)}",
        apartment: String = "1tv",
        floor: String = "1",
        bbrId: String? = null,
        zip: String = "1234",
        city: String = "ApCity",
        coInsured: Int = 1,
        student: Boolean = false
    ): CompleteQuoteResponseDto {
        return createDanishTravelQuote<CompleteQuoteResponseDto>(
            birthdate,
            street,
            apartment,
            floor,
            bbrId,
            zip,
            city,
            coInsured,
            student
        ).body!!
    }

    fun createDanishTravelQuoteRaw(
        birthdate: String = "1944-08-04",
        street: String = "ApStreet ${RandomStringUtils.randomNumeric(2)}",
        apartment: String = "1tv",
        floor: String = "1",
        bbrId: String? = null,
        zip: String = "1234",
        city: String = "ApCity",
        coInsured: Int = 1,
        student: Boolean = false

    ): ResponseEntity<String> {
        return createDanishTravelQuote<String>(
            birthdate,
            street,
            apartment,
            floor,
            bbrId,
            zip,
            city,
            coInsured,
            student
        )
    }

    fun signQuote(
        quoteId: UUID,
        firstName: String = "Apan",
        lastName: String = "Apansson",
        email: String = "apan@apansson.se",
        ssn: String? = null,
        startDate: String? = null,
        memberId: String? = null
    ): SignedQuoteResponseDto {
        return signQuote<SignedQuoteResponseDto>(quoteId, firstName, lastName, email, ssn, startDate, memberId).body!!
    }

    fun signQuoteRaw(
        quoteId: UUID,
        firstName: String,
        lastName: String,
        email: String,
        ssn: String? = null,
        startDate: String? = null
    ): ResponseEntity<String> {

        return signQuote<String>(quoteId, firstName, lastName, email, ssn, startDate)
    }

    fun deleteQuote(quoteId: UUID): ResponseEntity<String> {

        val headers = HttpHeaders()

        return restTemplate.exchange(
            "/_/v1/quotes/$quoteId",
            HttpMethod.DELETE,
            HttpEntity(null, headers),
            String::class.java
        )
    }

    fun getQuote(quoteId: UUID): Quote? {
        return restTemplate.getForObject("/_/v1/quotes/$quoteId", Quote::class.java)
    }

    fun createBundle(vararg quoteIds: UUID): QuoteBundleResponseDto {

        val ids = quoteIds.joinToString("\", \"", "\"", "\"")

        val request = """
            { "quoteIds": [$ids] }
        """.trimIndent()

        return postJson<QuoteBundleResponseDto>("/_/v1/quotes/bundle", request).body!!
    }

    fun createBundleRaw(vararg quoteIds: UUID): ResponseEntity<String> {

        val ids = quoteIds.joinToString("\", \"", "\"", "\"")

        val request = """
            { "quoteIds": [$ids] }
        """.trimIndent()

        return postJson("/_/v1/quotes/bundle", request)
    }

    private inline fun <reified T : Any> createNorwegianHomeContentQuote(
        birthdate: String,
        street: String,
        zip: String,
        city: String,
        livingSpace: Int,
        coInsured: Int,
        youth: Boolean,
        subType: String
    ): ResponseEntity<T> {
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
                    "livingSpace": $livingSpace,
                    "coInsured": $coInsured,
                    "youth": $youth,
                    "subType": "$subType"
                },
                "shouldComplete": true,
                "underwritingGuidelinesBypassedBy": null
            }
        """.trimIndent()

        return postJson("/_/v1/quotes", request)
    }

    private inline fun <reified T : Any> createNorwegianTravelQuote(
        birthdate: String,
        coInsured: Int,
        youth: Boolean
    ): ResponseEntity<T> {
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
                    "coInsured":$coInsured,
                    "youth":$youth
                },
                "shouldComplete":true,
                "underwritingGuidelinesBypassedBy":null
            }
        """.trimIndent()

        return postJson("/_/v1/quotes", request)
    }

    private inline fun <reified T : Any> createSwedishApartmentQuote(
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        phoneNumber: String? = null,
        memberId: String? = null,
        ssn: String,
        birthdate: String? = null,
        street: String,
        zip: String,
        city: String,
        livingSpace: Int,
        householdSize: Int,
        subType: String,
        dataCollectionId: UUID? = null
    ): ResponseEntity<T> {
        val request = """           
            {
                "firstName": ${getNullableString(firstName)},
                "lastName": ${getNullableString(lastName)},
                "email": ${getNullableString(email)},
                "phoneNumber": ${getNullableString(phoneNumber)},
                "currentInsurer": null,
                "birthDate": ${getNullableString(birthdate)},
                "ssn": "$ssn",
                "memberId": ${getNullableString(memberId)},
                "quotingPartner": "HEDVIG",
                "productType": "APARTMENT",
                "dataCollectionId": ${dataCollectionId?.let { "\"$it\"" } ?: "null"},
                "incompleteQuoteData": {
                    "type": "apartment",
                    "street": "$street",
                    "zipCode": "$zip",
                    "city": "$city",
                    "livingSpace": $livingSpace,
                    "householdSize": $householdSize,
                    "subType": "$subType"
                },
                "shouldComplete": true
            }
        """.trimIndent()

        return postJson("/_/v1/quotes", request)
    }

    private fun getNullableString(value: String?) = if (value == null) null else "\"$value\""

    private inline fun <reified T : Any> createSwedishHouseQuote(
        ssn: String,
        street: String,
        zip: String,
        city: String,
        livingSpace: Int,
        householdSize: Int,
        ancillaryArea: Int,
        yearOfConstruction: Int,
        numberOfBathrooms: Int,
        subleted: Boolean
    ): ResponseEntity<T> {
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
                    "livingSpace": $livingSpace,
                    "householdSize": $householdSize,
                    "ancillaryArea": $ancillaryArea,
                    "yearOfConstruction": $yearOfConstruction,
                    "numberOfBathrooms": $numberOfBathrooms,
                    "extraBuildings": [{
                        "id": null,
                        "type": "CARPORT",
                        "area": 11,
                        "hasWaterConnected": true
                    }],
                    "subleted": $subleted
                },
                "shouldComplete": true
            }
        """.trimIndent()

        return postJson("/_/v1/quotes", request)
    }

    private inline fun <reified T : Any> createDanishHomeContentQuote(
        birthdate: String,
        street: String,
        apartment: String,
        floor: String,
        bbrId: String? = null,
        zip: String,
        city: String,
        livingSpace: Int,
        coInsured: Int,
        student: Boolean,
        subType: String
    ): ResponseEntity<T> {

        val bbrIdString = if (bbrId != null) "\"$bbrId\"" else "null"

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
                    "type": "danishHomeContents",
                    "street": "$street",
                    "apartment": "$apartment",
                    "floor": "$floor",
                    "bbrId": $bbrIdString,
                    "zipCode": "$zip",
                    "city": "$city",
                    "livingSpace": $livingSpace,
                    "coInsured": $coInsured,
                    "student": $student,
                    "subType": "$subType"
                },
                "shouldComplete": true,
                "underwritingGuidelinesBypassedBy": null
            }
        """.trimIndent()

        return postJson("/_/v1/quotes", request)
    }

    private inline fun <reified T : Any> createDanishAccidentQuote(
        birthdate: String,
        street: String,
        apartment: String,
        floor: String,
        bbrId: String? = null,
        zip: String,
        city: String,
        coInsured: Int,
        student: Boolean
    ): ResponseEntity<T> {
        val bbrIdString = if (bbrId != null) "\"$bbrId\"" else "null"

        val request = """
            {
                "firstName": null,
                "lastName": null,
                "currentInsurer": null,
                "birthDate": "$birthdate",
                "ssn": null,
                "quotingPartner": "HEDVIG",
                "productType": "ACCIDENT",
                "incompleteQuoteData": {
                    "type": "danishAccident",
                    "street": "$street",
                    "apartment": "$apartment",
                    "floor": "$floor",
                    "bbrId": $bbrIdString,
                    "zipCode": "$zip",
                    "city": "$city",
                    "coInsured": $coInsured,
                    "student": $student
                },
                "shouldComplete": true,
                "underwritingGuidelinesBypassedBy": null
            }
        """.trimIndent()

        return postJson("/_/v1/quotes", request)
    }

    private inline fun <reified T : Any> createDanishTravelQuote(
        birthdate: String,
        street: String,
        apartment: String,
        floor: String,
        bbrId: String? = null,
        zip: String,
        city: String,
        coInsured: Int,
        student: Boolean
    ): ResponseEntity<T> {
        val bbrIdString = if (bbrId != null) "\"$bbrId\"" else "null"

        val request = """
            {
                "firstName": null,
                "lastName": null,
                "currentInsurer": null,
                "birthDate": "$birthdate",
                "ssn": null,
                "quotingPartner": "HEDVIG",
                "productType": "TRAVEL",
                "incompleteQuoteData": {
                    "type": "danishTravel",
                    "street": "$street",
                    "apartment": "$apartment",
                    "floor": "$floor",
                    "bbrId": $bbrIdString,
                    "zipCode": "$zip",
                    "city": "$city",
                    "coInsured": $coInsured,
                    "student": $student
                },
                "shouldComplete": true,
                "underwritingGuidelinesBypassedBy": null
            }
        """.trimIndent()

        return postJson("/_/v1/quotes", request)
    }

    private inline fun <reified T : Any> signQuote(
        quoteId: UUID,
        firstName: String,
        lastName: String,
        email: String,
        ssn: String? = null,
        startDate: String? = null,
        memberId: String? = null
    ): ResponseEntity<T> {

        val ssnString = if (ssn != null) "\"$ssn\"" else "null"

        val request = """
            {
                "name": {
                    "firstName": "$firstName",
                    "lastName": "$lastName"
                },
                "ssn": $ssnString,
                "startDate": "${startDate ?: LocalDate.now()}",
                "email": "$email",
                "memberId": ${memberId?.let { "\"$it\"" }}
            }
        """.trimIndent()

        return postJson("/_/v1/quotes/$quoteId/sign", request)
    }

    fun signQuoteBundle(
        firstName: String = "Apan",
        lastName: String = "Apansson",
        ssn: String?,
        email: String = "apan@apansson.se",
        startDate: LocalDate = LocalDate.now(),
        price: Double?,
        currency: String?,
        vararg quoteIds: UUID
    ): SignedQuotesResponseDto {

        val currencyString = currency?.let { "\"" + currency + "\"" }
        val ssnString = ssn?.let { "\"" + ssn + "\"" }

        val request = """
            {
                "name": {
                    "firstName": "$firstName",
                    "lastName": "$lastName"
                },
                "ssn": $ssnString,
                "startDate": "$startDate",
                "email": "$email",
                "quoteIds": [${quoteIds.joinToString("\", \"", "\"", "\"")}],
                "price": $price,
                "currency": $currencyString
            }
        """.trimIndent()

        return postJson<SignedQuotesResponseDto>("/_/v1/quotes/bundle/signFromRapio", request).body!!
    }

    fun signQuoteBundleRaw(
        firstName: String = "Apan",
        lastName: String = "Apansson",
        ssn: String?,
        email: String = "apan@apansson.se",
        startDate: LocalDate = LocalDate.now(),
        price: Double?,
        currency: String?,
        vararg quoteIds: UUID
    ): ResponseEntity<String> {

        val currencyString = currency?.let { "\"" + currency + "\"" }
        val ssnString = ssn?.let { "\"" + ssn + "\"" }

        val request = """
            {
                "name": {
                    "firstName": "$firstName",
                    "lastName": "$lastName"
                },
                "ssn": $ssnString,
                "startDate": "$startDate",
                "email": "$email",
                "quoteIds": [${quoteIds.joinToString("\", \"", "\"", "\"")}],
                "price": $price,
                "currency": $currencyString
            }
        """.trimIndent()

        return postJson("/_/v1/quotes/bundle/signFromRapio", request)
    }

    private inline fun <reified T : Any> postJson(url: String, data: String): ResponseEntity<T> {

        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.contentType = MediaType.APPLICATION_JSON

        return restTemplate.exchange(url, HttpMethod.POST, HttpEntity(data, headers), T::class.java)
    }
}
