package com.hedvig.underwriter.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.hedvig.underwriter.model.ApartmentProductSubType
import com.hedvig.underwriter.model.DanishAccidentData
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.DanishHomeContentsType
import com.hedvig.underwriter.model.DanishTravelData
import com.hedvig.underwriter.model.ExtraBuilding
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianHomeContentsType
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.QuoteData
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.ExtraBuildingRequestDto
import com.hedvig.libs.logging.masking.Masked
import java.util.UUID

sealed class QuoteRequestData {

    abstract fun createQuoteData(quoteRequest: QuoteRequest): QuoteData

    data class SwedishHouse(
        @Masked val street: String?,
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
    ) : QuoteRequestData() {
        override fun createQuoteData(quoteRequest: QuoteRequest): QuoteData = SwedishHouseData(
            id = UUID.randomUUID(),
            ssn = quoteRequest.ssn,
            birthDate = quoteRequest.birthDate,
            firstName = quoteRequest.firstName,
            lastName = quoteRequest.lastName,
            email = quoteRequest.email,
            phoneNumber = quoteRequest.phoneNumber,
            street = this.street,
            zipCode = this.zipCode,
            city = this.city,
            householdSize = this.householdSize,
            livingSpace = this.livingSpace,
            numberOfBathrooms = this.numberOfBathrooms,
            isSubleted = this.isSubleted,
            extraBuildings = this.extraBuildings?.map((ExtraBuilding)::from),
            ancillaryArea = this.ancillaryArea,
            yearOfConstruction = this.yearOfConstruction
        )
    }

    data class SwedishApartment(
        @Masked val street: String?,
        val zipCode: String?,
        val city: String?,
        val livingSpace: Int?,
        val householdSize: Int?,
        val floor: Int?,
        val subType: ApartmentProductSubType?
    ) : QuoteRequestData() {
        override fun createQuoteData(quoteRequest: QuoteRequest): QuoteData = SwedishApartmentData(
            id = UUID.randomUUID(),
            ssn = quoteRequest.ssn,
            birthDate = quoteRequest.birthDate,
            firstName = quoteRequest.firstName,
            lastName = quoteRequest.lastName,
            email = quoteRequest.email,
            phoneNumber = quoteRequest.phoneNumber,
            subType = this.subType,
            street = this.street,
            zipCode = this.zipCode,
            city = this.city,
            householdSize = this.householdSize,
            livingSpace = this.livingSpace
        )
    }

    data class NorwegianHomeContents(
        @Masked val street: String?,
        val zipCode: String?,
        val city: String?,
        val coInsured: Int?,
        val livingSpace: Int?,
        @field:JsonProperty("youth")
        val isYouth: Boolean?,
        val subType: NorwegianHomeContentsType?
    ) : QuoteRequestData() {
        override fun createQuoteData(quoteRequest: QuoteRequest): QuoteData = NorwegianHomeContentsData(
            id = UUID.randomUUID(),
            ssn = quoteRequest.ssn,
            birthDate = quoteRequest.birthDate!!,
            firstName = quoteRequest.firstName,
            lastName = quoteRequest.lastName,
            email = quoteRequest.email,
            phoneNumber = quoteRequest.phoneNumber,
            type = this.subType!!,
            street = this.street!!,
            zipCode = this.zipCode!!,
            city = this.city,
            isYouth = this.isYouth!!,
            coInsured = this.coInsured!!,
            livingSpace = this.livingSpace!!
        )
    }

    data class NorwegianTravel(
        val coInsured: Int?,
        @field:JsonProperty("youth")
        val isYouth: Boolean?
    ) : QuoteRequestData() {
        override fun createQuoteData(quoteRequest: QuoteRequest): QuoteData = NorwegianTravelData(
            id = UUID.randomUUID(),
            ssn = quoteRequest.ssn,
            birthDate = quoteRequest.birthDate!!,
            firstName = quoteRequest.firstName,
            lastName = quoteRequest.lastName,
            email = quoteRequest.email,
            phoneNumber = quoteRequest.phoneNumber,
            coInsured = this.coInsured!!,
            isYouth = this.isYouth!!
        )
    }

    data class DanishHomeContents(
        @Masked val street: String?,
        val zipCode: String?,
        @Masked val bbrId: String?,
        val coInsured: Int?,
        val livingSpace: Int?,
        val apartment: String?,
        val floor: String?,
        val city: String?,
        @field:JsonProperty("student")
        val isStudent: Boolean?,
        val subType: DanishHomeContentsType?
    ) : QuoteRequestData() {
        override fun createQuoteData(quoteRequest: QuoteRequest): QuoteData = DanishHomeContentsData(
            id = UUID.randomUUID(),
            ssn = quoteRequest.ssn,
            birthDate = quoteRequest.birthDate!!,
            firstName = quoteRequest.firstName,
            lastName = quoteRequest.lastName,
            email = quoteRequest.email,
            phoneNumber = quoteRequest.phoneNumber,
            street = this.street!!,
            zipCode = this.zipCode!!,
            bbrId = this.bbrId,
            apartment = this.apartment,
            floor = this.floor,
            city = this.city,
            coInsured = this.coInsured!!,
            livingSpace = this.livingSpace!!,
            isStudent = this.isStudent!!,
            type = this.subType!!
        )
    }

    data class DanishAccident(
        @Masked val street: String?,
        val zipCode: String?,
        @Masked val bbrId: String?,
        val apartment: String?,
        val floor: String?,
        val city: String?,
        val coInsured: Int?,
        @field:JsonProperty("student")
        val isStudent: Boolean?
    ) : QuoteRequestData() {
        override fun createQuoteData(quoteRequest: QuoteRequest): QuoteData = DanishAccidentData(
            id = UUID.randomUUID(),
            ssn = quoteRequest.ssn,
            birthDate = quoteRequest.birthDate!!,
            firstName = quoteRequest.firstName,
            lastName = quoteRequest.lastName,
            email = quoteRequest.email,
            phoneNumber = quoteRequest.phoneNumber,
            street = this.street!!,
            zipCode = this.zipCode!!,
            bbrId = this.bbrId,
            apartment = this.apartment,
            floor = this.floor,
            city = this.city,
            coInsured = this.coInsured!!,
            isStudent = this.isStudent!!
        )
    }

    data class DanishTravel(
        @Masked val street: String?,
        val zipCode: String?,
        @Masked val bbrId: String?,
        val apartment: String?,
        val floor: String?,
        val city: String?,
        val coInsured: Int?,
        @field:JsonProperty("student")
        val isStudent: Boolean?
    ) : QuoteRequestData() {
        override fun createQuoteData(quoteRequest: QuoteRequest): QuoteData = DanishTravelData(
            id = UUID.randomUUID(),
            ssn = quoteRequest.ssn,
            birthDate = quoteRequest.birthDate!!,
            firstName = quoteRequest.firstName,
            lastName = quoteRequest.lastName,
            email = quoteRequest.email,
            phoneNumber = quoteRequest.phoneNumber,
            street = this.street!!,
            zipCode = this.zipCode!!,
            bbrId = this.bbrId,
            apartment = this.apartment,
            floor = this.floor,
            city = this.city,
            coInsured = this.coInsured!!,
            isStudent = this.isStudent!!
        )
    }

    companion object {
        fun from(quoteSchema: QuoteSchema) = when (quoteSchema) {
            is QuoteSchema.SwedishApartment -> SwedishApartment(
                subType = quoteSchema.lineOfBusiness,
                street = quoteSchema.street,
                zipCode = quoteSchema.zipCode,
                city = quoteSchema.city,
                livingSpace = quoteSchema.livingSpace,
                householdSize = quoteSchema.numberCoInsured + 1,
                floor = null
            )
            is QuoteSchema.SwedishHouse -> SwedishHouse(
                street = quoteSchema.street,
                zipCode = quoteSchema.zipCode,
                city = quoteSchema.city,
                livingSpace = quoteSchema.livingSpace,
                householdSize = quoteSchema.numberCoInsured + 1,
                ancillaryArea = quoteSchema.ancillaryArea,
                yearOfConstruction = quoteSchema.yearOfConstruction,
                numberOfBathrooms = quoteSchema.numberOfBathrooms,
                extraBuildings = quoteSchema.extraBuildings.map { extraBuildingSchema ->
                    ExtraBuildingRequestDto(
                        id = null,
                        type = extraBuildingSchema.type,
                        area = extraBuildingSchema.area,
                        hasWaterConnected = extraBuildingSchema.hasWaterConnected
                    )
                },
                isSubleted = quoteSchema.isSubleted,
                floor = null
            )
            is QuoteSchema.NorwegianHomeContent -> NorwegianHomeContents(
                subType = quoteSchema.lineOfBusiness,
                isYouth = quoteSchema.isYouth,
                street = quoteSchema.street,
                zipCode = quoteSchema.zipCode,
                city = quoteSchema.city,
                livingSpace = quoteSchema.livingSpace,
                coInsured = quoteSchema.numberCoInsured
            )
            is QuoteSchema.NorwegianTravel -> NorwegianTravel(
                isYouth = quoteSchema.isYouth,
                coInsured = quoteSchema.numberCoInsured
            )
            is QuoteSchema.DanishHomeContent -> DanishHomeContents(
                street = quoteSchema.street,
                zipCode = quoteSchema.zipCode,
                city = quoteSchema.city,
                apartment = quoteSchema.apartment,
                floor = quoteSchema.floor,
                bbrId = null,
                livingSpace = quoteSchema.livingSpace,
                coInsured = quoteSchema.numberCoInsured,
                isStudent = quoteSchema.isStudent,
                subType = quoteSchema.lineOfBusiness
            )
            is QuoteSchema.DanishAccident -> DanishAccident(
                street = quoteSchema.street,
                zipCode = quoteSchema.zipCode,
                city = quoteSchema.city,
                apartment = quoteSchema.apartment,
                floor = quoteSchema.floor,
                bbrId = null,
                coInsured = quoteSchema.numberCoInsured,
                isStudent = quoteSchema.isStudent
            )
            is QuoteSchema.DanishTravel -> DanishTravel(
                street = quoteSchema.street,
                zipCode = quoteSchema.zipCode,
                city = quoteSchema.city,
                apartment = quoteSchema.apartment,
                floor = quoteSchema.floor,
                bbrId = null,
                coInsured = quoteSchema.numberCoInsured,
                isStudent = quoteSchema.isStudent
            )
        }
    }
}
