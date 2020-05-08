package com.hedvig.underwriter.graphql.type

sealed class QuoteDetails {

    abstract val typeOfContract: TypeOfContract

    data class SwedishApartmentQuoteDetails(
        val street: String,
        val zipCode: String,
        val householdSize: Int,
        val livingSpace: Int,
        val type: ApartmentType
    ) : QuoteDetails() {
        override val typeOfContract: TypeOfContract
            get() = when (type) {
                ApartmentType.BRF -> TypeOfContract.SE_APARTMENT_BRF
                ApartmentType.RENT -> TypeOfContract.SE_APARTMENT_RENT
                ApartmentType.STUDENT_BRF -> TypeOfContract.SE_APARTMENT_STUDENT_BRF
                ApartmentType.STUDENT_RENT -> TypeOfContract.SE_APARTMENT_STUDENT_RENT
            }
    }

    data class SwedishHouseQuoteDetails(
        val street: String,
        val zipCode: String,
        val householdSize: Int,
        val livingSpace: Int,
        val ancillarySpace: Int,
        val extraBuildings: List<ExtraBuilding.ExtraBuildingCore>,
        val numberOfBathrooms: Int,
        val yearOfConstruction: Int,
        val isSubleted: Boolean
    ) : QuoteDetails() {
        override val typeOfContract: TypeOfContract
            get() = TypeOfContract.SE_HOUSE
    }

    data class NorwegianHomeContentsDetails(
        val street: String,
        val zipCode: String,
        val coInsured: Int,
        val livingSpace: Int,
        val isYouth: Boolean,
        val type: NorwegianHomeContentsType
    ) : QuoteDetails() {
        override val typeOfContract: TypeOfContract
            get() = when (type) {
                NorwegianHomeContentsType.OWN -> {
                    when (isYouth) {
                        true -> TypeOfContract.NO_HOME_CONTENT_YOUTH_OWN
                        false -> TypeOfContract.NO_HOME_CONTENT_OWN
                    }
                }
                NorwegianHomeContentsType.RENT -> {
                    when (isYouth) {
                        true -> TypeOfContract.NO_HOME_CONTENT_YOUTH_RENT
                        false -> TypeOfContract.NO_HOME_CONTENT_RENT
                    }
                }
            }
    }

    data class NorwegianTravelDetails(
        val coInsured: Int,
        val isYouth: Boolean
    ) : QuoteDetails() {
        override val typeOfContract: TypeOfContract
            get() = when (isYouth) {
                true -> TypeOfContract.NO_TRAVEL_YOUTH
                false -> TypeOfContract.NO_TRAVEL
            }
    }
}
