package com.hedvig.underwriter.web

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.hedvig.underwriter.model.ExtraBuildingType
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/_/v2/quotes")
class V2QuoteController(
    val quoteService: QuoteService,
    objectMapper: ObjectMapper
) {

    val jsonSchemaConfig = JsonSchemaConfig
        .html5EnabledSchema()
        .withJsonSchemaDraft(JsonSchemaDraft.DRAFT_07)
    val jsonSchemaGenerator = JsonSchemaGenerator(objectMapper, false, jsonSchemaConfig)

    @GetMapping("{quoteId}/schema")
    fun getSchemaForQuote(@PathVariable quoteId: UUID): Any {
        val quote =
            quoteService.getQuote(quoteId) ?: return ResponseEntity.status(404).body(
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

    @PostMapping("{quoteId}/update")
    fun updateSchema(@PathVariable("quoteId") quoteId: UUID, @RequestBody data: QuoteDataSchemas) {

        // quoteService.updateQuote(QuoteRequest())
    }
}

sealed class QuoteDataSchemas

data class SwedishHouse(
    val street: String,
    val zipCode: String,
    val city: String?,
    val livingSpace: Int,
    val numberCoInsured: Int,
    val ancillaryArea: Int,
    val yearOfConstruction: Int,
    val numberOfBathrooms: Int,
    val extraBuildings: List<ExtraBuilding>,
    @field:JsonProperty("subleted")
    val isSubleted: Boolean
) : QuoteDataSchemas() {
    data class ExtraBuilding(
        val type: ExtraBuildingType,
        val area: Int,
        val hasWaterConnected: Boolean
    )
}
