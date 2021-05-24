package com.hedvig.underwriter.graphql.type

import com.hedvig.libs.logging.masking.Masked

sealed class QuoteDetails {

    abstract val typeOfContract: ContractAgreementType

    data class SwedishApartmentQuoteDetails(
        @Masked val street: String,
        val zipCode: String,
        val householdSize: Int,
        val livingSpace: Int,
        val type: ApartmentType
    ) : QuoteDetails() {
        override val typeOfContract: ContractAgreementType
            get() = when (type) {
                ApartmentType.BRF -> ContractAgreementType.SE_APARTMENT_BRF
                ApartmentType.RENT -> ContractAgreementType.SE_APARTMENT_RENT
                ApartmentType.STUDENT_BRF -> ContractAgreementType.SE_APARTMENT_STUDENT_BRF
                ApartmentType.STUDENT_RENT -> ContractAgreementType.SE_APARTMENT_STUDENT_RENT
            }
    }

    data class SwedishHouseQuoteDetails(
        @Masked val street: String,
        val zipCode: String,
        val householdSize: Int,
        val livingSpace: Int,
        val ancillarySpace: Int,
        val extraBuildings: List<ExtraBuildingValue>,
        val numberOfBathrooms: Int,
        val yearOfConstruction: Int,
        val isSubleted: Boolean
    ) : QuoteDetails() {
        override val typeOfContract: ContractAgreementType
            get() = ContractAgreementType.SE_HOUSE
    }

    data class NorwegianHomeContentsDetails(
        @Masked val street: String,
        val zipCode: String,
        val coInsured: Int,
        val livingSpace: Int,
        val isYouth: Boolean,
        val type: NorwegianHomeContentsType
    ) : QuoteDetails() {
        override val typeOfContract: ContractAgreementType
            get() = when (type) {
                NorwegianHomeContentsType.OWN -> {
                    when (isYouth) {
                        true -> ContractAgreementType.NO_HOME_CONTENT_YOUTH_OWN
                        false -> ContractAgreementType.NO_HOME_CONTENT_OWN
                    }
                }
                NorwegianHomeContentsType.RENT -> {
                    when (isYouth) {
                        true -> ContractAgreementType.NO_HOME_CONTENT_YOUTH_RENT
                        false -> ContractAgreementType.NO_HOME_CONTENT_RENT
                    }
                }
            }
    }

    data class NorwegianTravelDetails(
        val coInsured: Int,
        val isYouth: Boolean
    ) : QuoteDetails() {
        override val typeOfContract: ContractAgreementType
            get() = when (isYouth) {
                true -> ContractAgreementType.NO_TRAVEL_YOUTH
                false -> ContractAgreementType.NO_TRAVEL
            }
    }

    data class DanishHomeContentsDetails(
        @Masked val street: String,
        val apartment: String?,
        val floor: String?,
        val zipCode: String,
        val city: String?,
        @Masked val bbrId: String?,
        val coInsured: Int,
        val livingSpace: Int,
        val isStudent: Boolean,
        val type: DanishHomeContentsType
    ) : QuoteDetails() {
        override val typeOfContract: ContractAgreementType
            get() = when (type) {
                DanishHomeContentsType.OWN -> {
                    when (isStudent) {
                        true -> ContractAgreementType.DK_HOME_CONTENT_STUDENT_OWN
                        false -> ContractAgreementType.DK_HOME_CONTENT_OWN
                    }
                }
                DanishHomeContentsType.RENT -> {
                    when (isStudent) {
                        true -> ContractAgreementType.DK_HOME_CONTENT_STUDENT_RENT
                        false -> ContractAgreementType.DK_HOME_CONTENT_RENT
                    }
                }
            }
    }

    data class DanishAccidentDetails(
        @Masked val street: String,
        val zipCode: String,
        val apartment: String?,
        val floor: String?,
        val city: String?,
        @Masked val bbrId: String?,
        val coInsured: Int,
        val isStudent: Boolean
    ) : QuoteDetails() {
        override val typeOfContract: ContractAgreementType
            get() = when (isStudent) {
                true -> ContractAgreementType.DK_ACCIDENT_STUDENT
                false -> ContractAgreementType.DK_ACCIDENT
            }
    }

    data class DanishTravelDetails(
        @Masked val street: String,
        val zipCode: String,
        val apartment: String?,
        val floor: String?,
        val city: String?,
        @Masked val bbrId: String?,
        val coInsured: Int,
        val isStudent: Boolean
    ) : QuoteDetails() {
        override val typeOfContract: ContractAgreementType
            get() = when (isStudent) {
                true -> ContractAgreementType.DK_TRAVEL_STUDENT
                false -> ContractAgreementType.DK_TRAVEL
            }
    }
}
