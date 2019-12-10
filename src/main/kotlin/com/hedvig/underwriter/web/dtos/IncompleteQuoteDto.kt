package com.hedvig.underwriter.web.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.hedvig.underwriter.model.ApartmentData
import com.hedvig.underwriter.model.ApartmentProductSubType
import com.hedvig.underwriter.model.HouseData
import com.hedvig.underwriter.model.Partner
import com.hedvig.underwriter.model.ProductType
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ExtraBuildingRequestDto
import java.time.LocalDate
import java.util.UUID
import javax.validation.Valid

data class IncompleteQuoteDto(
    val firstName: String?,
    val lastName: String?,
    val currentInsurer: String?,
    val birthDate: LocalDate?,
    val ssn: String?,
    val quotingPartner: Partner?,
    @get: Valid val productType: ProductType?,
    val incompleteQuoteData: IncompleteQuoteRequestData?,
    val incompleteHouseQuoteData: IncompleteHouseQuoteDataDto?,
    val incompleteApartmentQuoteData: IncompleteApartmentQuoteDataDto?,
    val memberId: String? = null,
    val originatingProductId: UUID? = null
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = IncompleteApartmentQuoteDataDto::class, name = "apartment"),
    JsonSubTypes.Type(value = IncompleteHouseQuoteDataDto::class, name = "house")
)
sealed class IncompleteQuoteRequestData

data class IncompleteHouseQuoteDataDto(
    val street: String?,
    val zipCode: String?,
    val city: String?,
    val livingSpace: Int?,
    val householdSize: Int?,
    val ancillaryArea: Int?,
    val yearOfConstruction: Int?,
    val numberOfBathrooms: Int?,
    val extraBuildings: List<ExtraBuildingRequestDto>?,
    @field:JsonProperty("subleted")
    val isSubleted: Boolean?,
    val floor: Int? = 0
) : IncompleteQuoteRequestData() {
    companion object {
        fun fromHouseData(house: HouseData) = IncompleteHouseQuoteDataDto(
            street = house.street,
            zipCode = house.zipCode,
            city = house.city,
            livingSpace = house.livingSpace,
            householdSize = house.householdSize,
            ancillaryArea = house.ancillaryArea,
            yearOfConstruction = house.yearOfConstruction,
            numberOfBathrooms = house.numberOfBathrooms,
            extraBuildings = house.extraBuildings?.map { ExtraBuildingRequestDto(null, it.type, it.area, it.hasWaterConnected) },
            isSubleted = house.isSubleted
        )
    }
}

data class IncompleteApartmentQuoteDataDto(
    val street: String?,
    val zipCode: String?,
    val city: String?,
    val livingSpace: Int?,
    val householdSize: Int?,
    val floor: Int?,
    val subType: ApartmentProductSubType?
) : IncompleteQuoteRequestData() {
    companion object {
        fun fromApartmentData(apartment: ApartmentData) = IncompleteApartmentQuoteDataDto(
            street = apartment.street,
            zipCode = apartment.zipCode,
            city = apartment.city,
            livingSpace = apartment.livingSpace,
            householdSize = apartment.householdSize,
            floor = null,
            subType = apartment.subType
        )
    }
}
