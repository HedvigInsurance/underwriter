package com.hedvig.underwriter.web

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.service.model.QuoteRequestData
import com.hedvig.underwriter.web.dtos.ErrorCodes
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import com.kjetland.jackson.jsonSchema.JsonSchemaConfig
import com.kjetland.jackson.jsonSchema.JsonSchemaDraft
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class QuoteType(val name: String, val schema: String)

@RestController
@RequestMapping("/_/v2/quotes")
class V2QuoteController(
    val quoteService: QuoteService,
    val objectMapper: ObjectMapper
) {

    val jsonSchemaConfig = JsonSchemaConfig
        .html5EnabledSchema()
        .withJsonSchemaDraft(JsonSchemaDraft.DRAFT_07)
    val jsonSchemaGenerator = JsonSchemaGenerator(objectMapper, true, jsonSchemaConfig)

    @GetMapping("types")
    fun getTypes() = listOf<QuoteType>()

    @PostMapping("{quoteName}")
    fun post() {
        // Create quotes
    }

    @GetMapping("{quoteName}/schema")
    fun getSchemaForQuote(@PathVariable quoteName: UUID): Any {
        val quote =
            quoteService.getQuote(quoteName) ?: return ResponseEntity.status(404).body(
                ErrorResponseDto(
                    ErrorCodes.NO_SUCH_QUOTE,
                    errorMessage = "QuoteNotFound"
                )
            )

        val dataClass = when (quote.data) {
            is SwedishHouseData -> QuoteRequestData.SwedishHouse::class.java
            is SwedishApartmentData -> QuoteRequestData.SwedishApartment::class.java
            is NorwegianHomeContentsData -> QuoteRequestData.NorwegianHomeContents::class.java
            is NorwegianTravelData -> QuoteRequestData.NorwegianTravel::class.java
        }

        return jsonSchemaGenerator.generateJsonSchema(dataClass)
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "quoteType")
data class QuoteDK(val id: String, val firstName: String)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "quoteType")
data class QuoteSE(val id: String, val firstName: String, val size: Int, val address: Address)

data class Address(val street: String, val city: String)
