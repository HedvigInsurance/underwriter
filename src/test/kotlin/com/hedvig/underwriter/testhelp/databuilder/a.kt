package com.hedvig.underwriter.testhelp.databuilder

import com.hedvig.underwriter.model.ApartmentProductSubType
import com.hedvig.underwriter.model.ExtraBuilding
import com.hedvig.underwriter.model.ExtraBuildingType
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianHomeContentsType
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.ONE_DAY
import com.hedvig.underwriter.model.Partner
import com.hedvig.underwriter.model.ProductType
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteData
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.QuoteState
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.model.birthDateFromSwedishSsn
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.service.model.QuoteRequestData
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ExtraBuildingRequestDto
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class a {

    data class QuoteBuilder(
        val id: UUID = UUID.fromString("1c3463c4-0c71-11ea-8fd9-4865ee119be4"),
        val createdAt: Instant = Instant.now(),
        val price: BigDecimal? = BigDecimal.ZERO,
        val productType: ProductType = ProductType.APARTMENT,
        val state: QuoteState = QuoteState.INCOMPLETE,
        val initiatedFrom: QuoteInitiatedFrom = QuoteInitiatedFrom.RAPIO,
        val attributedTo: Partner = Partner.HEDVIG,
        val data: DataBuilder<QuoteData> = ApartmentDataBuilder(),

        val currentInsurer: String? = null,

        val startDate: LocalDate? = null,

        val validity: Long = ONE_DAY * 30,
        val breachedUnderwritingGuidelines: List<String>? = null,
        val underwritingGuidelinesBypassedBy: String? = null,
        val memberId: String? = null,
        val originatingProductId: UUID? = null,
        val agreementId: UUID? = null,
        val contractId: UUID? = null

    ) {
        fun w(quoteData: DataBuilder<QuoteData>? = null): QuoteBuilder {
            return this.copy(data = quoteData ?: this.data)
        }

        fun build() = Quote(
            id,
            createdAt,
            price,
            productType,
            state,
            initiatedFrom,
            attributedTo,
            data.build(),
            currentInsurer,
            startDate,
            validity,
            breachedUnderwritingGuidelines,
            underwritingGuidelinesBypassedBy,
            memberId,
            originatingProductId,
            agreementId,
            null,
            null,
            contractId
        )
    }

    interface DataBuilder<T> {
        fun build(): T
    }

    data class ApartmentDataBuilder(
        val id: UUID = UUID.fromString("ab5924e4-0c72-11ea-a337-4865ee119be4"),
        val ssn: String? = "191212121212",
        val birthDate: LocalDate? = LocalDate.of(1912, 12, 12),
        val firstName: String? = "",
        val lastName: String? = "",
        val email: String? = "em@i.l",

        val street: String? = "",
        val city: String? = "",
        val zipCode: String? = "",
        val householdSize: Int? = 3,
        val livingSpace: Int? = 2,
        val subType: ApartmentProductSubType? = ApartmentProductSubType.BRF,
        val internalId: Int? = null
    ) : DataBuilder<QuoteData> {

        override fun build() = SwedishApartmentData(
            id,
            ssn,
            birthDate,
            firstName,
            lastName,
            email,
            street,
            city,
            zipCode,
            householdSize,
            livingSpace,
            subType,
            internalId
        )
    }

    data class SwedishHouseDataBuilder(
        val id: UUID = UUID.fromString("ab5924e4-0c72-11ea-a337-4865ee119be4"),
        val ssn: String? = "191212121212",
        val birthDate: LocalDate? = LocalDate.of(1912, 12, 12),
        val firstName: String? = "",
        val lastName: String? = "",
        val email: String? = "em@i.l",

        val street: String = "",
        val city: String = "",
        val zipCode: String = "",
        val householdSize: Int = 3,
        val livingSpace: Int = 2,
        val ancillaryArea: Int = 50,
        val yearOfConstruction: Int = 1925,
        val numberOfBathrooms: Int = 1,
        val extraBuildings: List<ExtraBuilding> = emptyList(),
        val isSubleted: Boolean = false,
        val internalId: Int? = null
    ) : DataBuilder<QuoteData> {

        override fun build() = SwedishHouseData(
            id,
            ssn,
            birthDate,
            firstName,
            lastName,
            email,
            street,
            zipCode,
            city,
            livingSpace,
            householdSize,
            ancillaryArea,
            yearOfConstruction,
            numberOfBathrooms,
            extraBuildings,
            isSubleted,
            null,
            internalId
        )
    }

    data class NorwegianHomeContentDataBuilder(
        val id: UUID = UUID.fromString("ab5924e4-0c72-11ea-a337-4865ee119be4"),
        val ssn: String? = "12121200000",
        val birthDate: LocalDate = LocalDate.of(1912, 12, 12),
        val firstName: String = "",
        val lastName: String = "",
        val email: String? = "em@i.l",

        val street: String = "",
        val city: String? = "",
        val zipCode: String = "",
        val coInsured: Int = 3,
        val livingSpace: Int = 2,
        val isYouth: Boolean = false,
        val type: NorwegianHomeContentsType = NorwegianHomeContentsType.OWN,
        val internalId: Int? = null
    ) : DataBuilder<QuoteData> {

        override fun build() = NorwegianHomeContentsData(
            id,
            ssn,
            birthDate,
            firstName,
            lastName,
            email,
            street,
            city,
            zipCode,
            coInsured,
            livingSpace,
            isYouth,
            type,
            internalId
        )
    }

    data class NorwegianTravelDataBuilder(
        val id: UUID = UUID.fromString("ab5924e4-0c72-11ea-a337-4865ee119be4"),
        val ssn: String? = "12121200000",
        val birthDate: LocalDate = LocalDate.of(1912, 12, 12),
        val firstName: String = "",
        val lastName: String = "",
        val email: String? = "em@i.l",

        val street: String = "",
        val city: String? = "",
        val zipCode: String = "",
        val coInsured: Int = 3,
        val livingSpace: Int = 2,
        val isYouth: Boolean = false,
        val type: NorwegianHomeContentsType = NorwegianHomeContentsType.OWN,
        val internalId: Int? = null
    ) : DataBuilder<QuoteData> {

        override fun build() = NorwegianTravelData(
            id,
            ssn,
            birthDate,
            firstName,
            lastName,
            email,
            coInsured,
            isYouth,
            internalId
        )
    }

    data class SwedishApartmentQuoteRequestDataBuilder(
        val street: String = "",
        val city: String = "",
        val zipCode: String = "",
        val householdSize: Int = 3,
        val livingSpace: Int = 2,
        val subType: ApartmentProductSubType = ApartmentProductSubType.BRF,
        val floor: Int? = null
    ) : DataBuilder<QuoteRequestData.SwedishApartment> {
        override fun build() = QuoteRequestData.SwedishApartment(
            street = street,
            zipCode = zipCode,
            city = city,
            livingSpace = livingSpace,
            householdSize = householdSize,
            floor = floor,
            subType = subType
        )
    }

    data class SwedishApartmentQuoteRequestBuilder(
        val id: UUID = UUID.fromString("ab5924e4-0c72-11ea-a337-4865ee119be4"),
        val firstName: String = "",
        val lastName: String = "",
        val ssn: String = "191212121212",
        val email: String = "em@i.l",
        val quotingPartner: Partner = Partner.HEDVIG,
        val memberId: String? = null,
        val originatingProductId: UUID? = null,
        val startDate: Instant? = Instant.now(),
        val dataCollectionId: UUID? = null,
        val currentInsurer: String? = null,
        val data: DataBuilder<QuoteRequestData.SwedishApartment> = SwedishApartmentQuoteRequestDataBuilder(),
        val productType: ProductType? = ProductType.APARTMENT
    ) : DataBuilder<QuoteRequest> {
        override fun build(): QuoteRequest = QuoteRequest(
            firstName = firstName,
            lastName = lastName,
            email = email,
            currentInsurer = currentInsurer,
            birthDate = ssn?.birthDateFromSwedishSsn(),
            ssn = ssn,
            quotingPartner = quotingPartner,
            productType = productType,
            incompleteQuoteData = data.build(),
            memberId = memberId,
            originatingProductId = originatingProductId,
            startDate = startDate,
            dataCollectionId = dataCollectionId,
            dataCollectionIds = listOf(dataCollectionId)
        )
    }

    data class SwedishHouseQuoteRequestDataBuilder(
        val street: String = "",
        val city: String = "",
        val zipCode: String = "",
        val householdSize: Int = 3,
        val livingSpace: Int = 2,
        val ancillaryArea: Int = 50,
        val yearOfConstruction: Int = 1925,
        val numberOfBathrooms: Int = 1,
        val extraBuildings: List<ExtraBuildingRequestDto> = emptyList(),
        val isSubleted: Boolean = false
    ) : DataBuilder<QuoteRequestData.SwedishHouse> {
        override fun build() = QuoteRequestData.SwedishHouse(
            street = street,
            zipCode = zipCode,
            city = city,
            livingSpace = livingSpace,
            householdSize = householdSize,
            ancillaryArea = ancillaryArea,
            yearOfConstruction = yearOfConstruction,
            numberOfBathrooms = numberOfBathrooms,
            extraBuildings = extraBuildings,
            isSubleted = isSubleted
        )
    }

    data class SwedishHouseQuoteRequestDataExtraBuildingsBuilder(
        val id: UUID? = UUID.randomUUID(),
        val type: ExtraBuildingType? = ExtraBuildingType.GARAGE,
        val area: Int? = 5,
        val hasWaterConnected: Boolean? = true
    ) : DataBuilder<ExtraBuildingRequestDto> {
        override fun build() = ExtraBuildingRequestDto(
            id = id,
            type = type!!,
            area = area!!,
            hasWaterConnected = hasWaterConnected!!
        )
    }

    data class SwedishHouseQuoteRequestBuilder(
        val id: UUID = UUID.fromString("ab5924e4-0c72-11ea-a337-4865ee119be4"),
        val firstName: String = "",
        val lastName: String = "",
        val ssn: String = "191212121212",
        val email: String = "em@i.l",
        val quotingPartner: Partner = Partner.HEDVIG,
        val memberId: String? = null,
        val originatingProductId: UUID? = null,
        val startDate: Instant? = Instant.now(),
        val dataCollectionId: UUID? = null,
        val currentInsurer: String? = null,
        val data: DataBuilder<QuoteRequestData.SwedishHouse> = SwedishHouseQuoteRequestDataBuilder(),
        val productType: ProductType? = ProductType.APARTMENT
    ) : DataBuilder<QuoteRequest> {
        override fun build(): QuoteRequest = QuoteRequest(
            firstName = firstName,
            lastName = lastName,
            email = email,
            currentInsurer = currentInsurer,
            birthDate = ssn.birthDateFromSwedishSsn(),
            ssn = ssn,
            quotingPartner = quotingPartner,
            productType = productType,
            incompleteQuoteData = data.build(),
            memberId = memberId,
            originatingProductId = originatingProductId,
            startDate = startDate,
            dataCollectionId = dataCollectionId,
            dataCollectionIds = listOf(dataCollectionId)
        )
    }

    data class NorwegianHomeContentsQuoteRequestDataBuilder(
        val street: String = "",
        val city: String = "",
        val zipCode: String = "",
        val coInsured: Int = 3,
        val livingSpace: Int = 2,
        val isYouth: Boolean = false,
        val type: NorwegianHomeContentsType = NorwegianHomeContentsType.OWN,
        val floor: Int? = null
    ) : DataBuilder<QuoteRequestData.NorwegianHomeContents> {
        override fun build() = QuoteRequestData.NorwegianHomeContents(
            street = street,
            zipCode = zipCode,
            city = city,
            livingSpace = livingSpace,
            coInsured = coInsured,
            isYouth = isYouth,
            subType = type
        )
    }

    data class NorwegianHomeContentsQuoteRequestBuilder(
        val id: UUID = UUID.fromString("ab5924e4-0c72-11ea-a337-4865ee119be4"),
        val firstName: String = "",
        val lastName: String = "",
        val ssn: String? = "28026400734",
        val birthDate: LocalDate = LocalDate.of(1964, 2, 28),
        val email: String = "em@i.l",
        val quotingPartner: Partner = Partner.HEDVIG,
        val memberId: String? = null,
        val originatingProductId: UUID? = null,
        val startDate: Instant? = Instant.now(),
        val dataCollectionId: UUID? = null,
        val currentInsurer: String? = null,
        val data: DataBuilder<QuoteRequestData.NorwegianHomeContents> = NorwegianHomeContentsQuoteRequestDataBuilder(),
        val productType: ProductType? = ProductType.APARTMENT
    ) : DataBuilder<QuoteRequest> {
        override fun build(): QuoteRequest = QuoteRequest(
            firstName = firstName,
            lastName = lastName,
            email = email,
            currentInsurer = currentInsurer,
            birthDate = birthDate,
            ssn = ssn,
            quotingPartner = quotingPartner,
            productType = productType,
            incompleteQuoteData = data.build(),
            memberId = memberId,
            originatingProductId = originatingProductId,
            startDate = startDate,
            dataCollectionId = dataCollectionId,
            dataCollectionIds = listOf(dataCollectionId)
        )
    }

    data class NorwegianTravelQuoteRequestDataBuilder(
        val coInsured: Int = 3,
        val isYouth: Boolean = false
    ) : DataBuilder<QuoteRequestData.NorwegianTravel> {
        override fun build() = QuoteRequestData.NorwegianTravel(
            coInsured = coInsured,
            isYouth = isYouth
        )
    }

    data class NorwegianTravelQuoteRequestBuilder(
        val id: UUID = UUID.fromString("ab5924e4-0c72-11ea-a337-4865ee119be4"),
        val firstName: String = "",
        val lastName: String = "",
        val ssn: String? = "28026400734",
        val birthDate: LocalDate = LocalDate.of(1964, 2, 28),
        val email: String = "em@i.l",
        val quotingPartner: Partner = Partner.HEDVIG,
        val memberId: String? = null,
        val originatingProductId: UUID? = null,
        val startDate: Instant? = Instant.now(),
        val dataCollectionId: UUID? = null,
        val currentInsurer: String? = null,
        val data: DataBuilder<QuoteRequestData.NorwegianTravel> = NorwegianTravelQuoteRequestDataBuilder(),
        val productType: ProductType? = ProductType.APARTMENT
    ) : DataBuilder<QuoteRequest> {
        override fun build(): QuoteRequest = QuoteRequest(
            firstName = firstName,
            lastName = lastName,
            email = email,
            currentInsurer = currentInsurer,
            birthDate = birthDate,
            ssn = ssn,
            quotingPartner = quotingPartner,
            productType = productType,
            incompleteQuoteData = data.build(),
            memberId = memberId,
            originatingProductId = originatingProductId,
            startDate = startDate,
            dataCollectionId = dataCollectionId,
            dataCollectionIds = listOf(dataCollectionId)
        )
    }
}
