package com.hedvig.underwriter.graphql.type

import com.hedvig.underwriter.graphql.type.depricated.EditApartmentInput
import com.hedvig.underwriter.graphql.type.depricated.EditHouseInput
import com.hedvig.underwriter.model.Partner
import com.hedvig.underwriter.model.ProductType
import com.hedvig.underwriter.model.birthDateFromDanishSsn
import com.hedvig.underwriter.model.birthDateFromNorwegianSsn
import com.hedvig.underwriter.model.birthDateFromSwedishSsn
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.util.Pii
import com.hedvig.underwriter.util.toStockholmInstant
import java.time.LocalDate
import java.util.UUID

data class EditQuoteInput(
    val id: UUID,
    @Pii val firstName: String?,
    @Pii val lastName: String?,
    @Pii val email: String?,
    @Pii val phoneNumber: String?,
    val currentInsurer: String?,
    @Pii val ssn: String?,
    val birthDate: LocalDate?,
    val startDate: LocalDate?,
    @Deprecated("Use swedishApartment")
    val apartment: EditApartmentInput?,
    @Deprecated("Use swedishHouse")
    val house: EditHouseInput?,
    val swedishApartment: EditSwedishApartmentInput?,
    val swedishHouse: EditSwedishHouseInput?,
    val norwegianHomeContents: EditNorwegianHomeContentsInput?,
    val norwegianTravel: EditNorwegianTravelInput?,
    val danishHomeContents: EditDanishHomeContentsInput?,
    val danishAccident: EditDanishAccidentInput?,
    val danishTravel: EditDanishTravelInput?,
    val dataCollectionId: UUID?
) {
    fun toQuoteRequest(
        quotingPartner: Partner? = null,
        memberId: String? = null,
        originatingProductId: UUID? = null
    ) = QuoteRequest(
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        phoneNumber = this.phoneNumber,
        currentInsurer = this.currentInsurer,
        birthDate = this.birthDate ?: when {
            this.swedishApartment != null || this.swedishHouse != null || this.apartment != null || this.house != null -> this.ssn?.birthDateFromSwedishSsn()
            this.norwegianHomeContents != null || this.norwegianTravel != null -> this.ssn?.birthDateFromNorwegianSsn()
            this.danishHomeContents != null || this.danishAccident != null || this.danishTravel != null -> this.ssn?.birthDateFromDanishSsn()
            else -> null
        },
        ssn = this.ssn,
        productType = this.getProductType(),
        incompleteQuoteData = when {
            this.swedishApartment != null -> this.swedishApartment.toQuoteRequestDataDto()
            this.swedishHouse != null -> this.swedishHouse.toQuoteRequestDataDto()
            this.norwegianHomeContents != null -> this.norwegianHomeContents.toQuoteRequestDataDto()
            this.norwegianTravel != null -> this.norwegianTravel.toQuoteRequestDataDto()
            this.danishHomeContents != null -> this.danishHomeContents.toQuoteRequestDataDto()
            this.danishAccident != null -> this.danishAccident.toQuoteRequestDataDto()
            this.danishTravel != null -> this.danishTravel.toQuoteRequestDataDto()
            this.apartment != null -> this.apartment.toQuoteRequestDataDto()
            this.house != null -> this.house.toQuoteRequestDataDto()
            else -> null
        },
        quotingPartner = quotingPartner,
        memberId = memberId,
        originatingProductId = originatingProductId,
        startDate = this.startDate?.atStartOfDay()?.toStockholmInstant(),
        dataCollectionId = this.dataCollectionId
    )

    fun getProductType(): ProductType? =
        this.apartment?.let {
            ProductType.APARTMENT
        } ?: this.house?.let {
            ProductType.HOUSE
        }
}
