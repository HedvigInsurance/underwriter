package com.hedvig.underwriter.service.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.github.imifou.jsonschema.module.addon.annotation.JsonSchema
import com.hedvig.underwriter.model.ApartmentProductSubType
import com.hedvig.underwriter.model.DanishHomeContentsType
import com.hedvig.underwriter.model.ExtraBuildingType
import com.hedvig.underwriter.model.NorwegianHomeContentsType
import com.hedvig.libs.logging.masking.Masked

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "id")
@JsonSubTypes(
    JsonSubTypes.Type(value = QuoteSchema.SwedishApartment::class, name = "SwedishApartment"),
    JsonSubTypes.Type(value = QuoteSchema.SwedishHouse::class, name = "SwedishHouse"),
    JsonSubTypes.Type(value = QuoteSchema.NorwegianHomeContent::class, name = "NorwegianHomeContent"),
    JsonSubTypes.Type(value = QuoteSchema.NorwegianTravel::class, name = "NorwegianTravel"),
    JsonSubTypes.Type(value = QuoteSchema.DanishHomeContent::class, name = "DanishHomeContent"),
    JsonSubTypes.Type(value = QuoteSchema.DanishAccident::class, name = "DanishAccident"),
    JsonSubTypes.Type(value = QuoteSchema.DanishTravel::class, name = "DanishTravel")
)
sealed class QuoteSchema {
    data class SwedishApartment(
        @JsonSchema(title = "Line Of Business", required = true)
        val lineOfBusiness: ApartmentProductSubType,
        @JsonSchema(title = "Street", required = true)
        @Masked val street: String,
        @JsonSchema(title = "Zip Code", required = true, minLength = 5, maxLength = 5)
        val zipCode: String,
        @JsonSchema(title = "City", required = false)
        val city: String?,
        @JsonSchema(title = "Living Space", required = true, min = 0.0)
        val livingSpace: Int,
        @JsonSchema(title = "Number Co-Insured", required = true, min = 0.0)
        val numberCoInsured: Int
    ) : QuoteSchema()

    data class SwedishHouse(
        @JsonSchema(title = "Street", required = true)
        @Masked val street: String,
        @JsonSchema(title = "Zip Code", required = true, minLength = 5, maxLength = 5)
        val zipCode: String,
        @JsonSchema(title = "City", required = false)
        val city: String?,
        @JsonSchema(title = "Living Space", required = true, min = 0.0)
        val livingSpace: Int,
        @JsonSchema(title = "Number Co-Insured", required = true, min = 0.0)
        val numberCoInsured: Int,
        @JsonSchema(title = "Ancillary Area", required = true, min = 0.0)
        val ancillaryArea: Int,
        @JsonSchema(title = "Year Of Construction", required = true, min = 0.0)
        val yearOfConstruction: Int,
        @JsonSchema(title = "Number Of Bathrooms", required = true, min = 0.0)
        val numberOfBathrooms: Int,
        @get:JsonProperty("isSubleted")
        @param:JsonProperty("isSubleted")
        @JsonSchema(title = "Is Subleted")
        val isSubleted: Boolean = false,
        @JsonSchema(required = true)
        val extraBuildings: List<ExtraBuildingSchema>
    ) : QuoteSchema() {
        data class ExtraBuildingSchema(
            @JsonSchema(title = "Type", required = true, defaultValue = "GARAGE")
            val type: ExtraBuildingType,
            @JsonSchema(title = "Area", required = true, min = 0.0)
            val area: Int,
            @JsonSchema(title = "Has Water Connected")
            val hasWaterConnected: Boolean = false
        )
    }

    data class NorwegianHomeContent(
        @JsonSchema(title = "Line Of Business", required = true)
        val lineOfBusiness: NorwegianHomeContentsType,
        @JsonSchema(title = "Street", required = true)
        @Masked val street: String,
        @JsonSchema(title = "Zip Code", required = true, minLength = 4, maxLength = 4)
        val zipCode: String,
        @JsonSchema(title = "City", required = false)
        val city: String?,
        @JsonSchema(title = "Living Space", required = true, min = 0.0)
        val livingSpace: Int,
        @JsonSchema(title = "Number Co-Insured", required = true, min = 0.0)
        val numberCoInsured: Int,
        @get:JsonProperty("isYouth")
        @param:JsonProperty("isYouth")
        @JsonSchema(title = "Is Youth")
        val isYouth: Boolean
    ) : QuoteSchema()

    data class NorwegianTravel(
        @JsonSchema(title = "Number Co-Insured", required = true, min = 0.0)
        val numberCoInsured: Int,
        @get:JsonProperty("isYouth")
        @param:JsonProperty("isYouth")
        @JsonSchema(title = "Is Youth")
        val isYouth: Boolean
    ) : QuoteSchema()

    data class DanishHomeContent(
        @JsonSchema(title = "Line Of Business", required = true)
        val lineOfBusiness: DanishHomeContentsType,
        @JsonSchema(title = "Street", required = true)
        @Masked val street: String,
        @JsonSchema(title = "Zip Code", required = true, minLength = 4, maxLength = 4)
        val zipCode: String,
        @JsonSchema(title = "Apartment", required = false)
        val apartment: String?,
        @JsonSchema(title = "Floor", required = false)
        val floor: String?,
        @JsonSchema(title = "City", required = false)
        val city: String?,
        @JsonSchema(title = "Living Space", required = true, min = 0.0)
        val livingSpace: Int,
        @JsonSchema(title = "Number Co-Insured", required = true, min = 0.0)
        val numberCoInsured: Int,
        @get:JsonProperty("isStudent")
        @param:JsonProperty("isStudent")
        @JsonSchema(title = "Is Student")
        val isStudent: Boolean
    ) : QuoteSchema()

    data class DanishAccident(
        @JsonSchema(title = "Street", required = true)
        @Masked val street: String,
        @JsonSchema(title = "Zip Code", required = true, minLength = 4, maxLength = 4)
        val zipCode: String,
        @JsonSchema(title = "Apartment", required = false)
        val apartment: String?,
        @JsonSchema(title = "Floor", required = false)
        val floor: String?,
        @JsonSchema(title = "City", required = false)
        val city: String?,
        @JsonSchema(title = "Number Co-Insured", required = true, min = 0.0)
        val numberCoInsured: Int,
        @get:JsonProperty("isStudent")
        @param:JsonProperty("isStudent")
        @JsonSchema(title = "Is Student")
        val isStudent: Boolean
    ) : QuoteSchema()

    data class DanishTravel(
        @JsonSchema(title = "Street", required = true)
        @Masked val street: String,
        @JsonSchema(title = "Zip Code", required = true, minLength = 4, maxLength = 4)
        val zipCode: String,
        @JsonSchema(title = "Apartment", required = false)
        val apartment: String?,
        @JsonSchema(title = "Floor", required = false)
        val floor: String?,
        @JsonSchema(title = "City", required = false)
        val city: String?,
        @JsonSchema(title = "Number Co-Insured", required = true, min = 0.0)
        val numberCoInsured: Int,
        @get:JsonProperty("isStudent")
        @param:JsonProperty("isStudent")
        @JsonSchema(title = "Is Student")
        val isStudent: Boolean
    ) : QuoteSchema()
}
