package com.hedvig.underwriter.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.hedvig.underwriter.service.model.PersonPolicyHolder
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.service.model.QuoteRequestData
import com.hedvig.underwriter.service.model.QuoteRequestData.DanishAccident
import com.hedvig.underwriter.service.model.QuoteRequestData.DanishHomeContents
import com.hedvig.underwriter.service.model.QuoteRequestData.DanishTravel
import com.hedvig.underwriter.service.model.QuoteRequestData.NorwegianHomeContents
import com.hedvig.underwriter.service.model.QuoteRequestData.NorwegianTravel
import com.hedvig.underwriter.service.model.QuoteRequestData.SwedishApartment
import com.hedvig.underwriter.service.model.QuoteRequestData.SwedishHouse
import com.hedvig.underwriter.serviceIntegration.lookupService.dtos.CompetitorPricing
import com.hedvig.underwriter.util.toStockholmLocalDate
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

val FIRST_BIRTH_CONTROL_SEQUENCE = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2, 1)
val SECOND_BIRTH_CONTROL_SEQUENCE = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2, 1)

val Quote.firstName
    get() = (data as? PersonPolicyHolder<*>)?.firstName
        ?: throw RuntimeException("No firstName on Quote! $this")

val Quote.lastName
    get() = (data as? PersonPolicyHolder<*>)?.lastName
        ?: throw RuntimeException("No lastName on Quote! $this")

val Quote.ssn
    get() = (data as? PersonPolicyHolder<*>)?.ssn
        ?: throw RuntimeException("No ssn on Quote! $this")

val Quote.ssnMaybe
    get() = (data as? PersonPolicyHolder<*>)?.ssn

val Quote.birthDate
    get() = birthDateMaybe
        ?: throw RuntimeException("No birthDate on Quote! $this")

val Quote.birthDateMaybe
    get() = (data as? PersonPolicyHolder<*>)?.birthDate
        ?: recoverBirthDateFromSSN()

val Quote.email
    get() = (data as? PersonPolicyHolder<*>)?.email

val Quote.phoneNumber
    get() = (data as? PersonPolicyHolder<*>)?.phoneNumber

val Quote.swedishApartment
    get() = (data as? SwedishApartmentData)

val Quote.swedishHouse
    get() = (data as? SwedishHouseData)

val Quote.norwegianHomeContents
    get() = (data as? NorwegianHomeContentsData)

val Quote.norwegianTravel
    get() = (data as? NorwegianTravelData)

val Quote.danishHomeContents
    get() = (data as? DanishHomeContentsData)

val Quote.danishAccident
    get() = (data as? DanishAccidentData)

val Quote.danishTravel
    get() = (data as? DanishTravelData)

val Quote.validTo
    get() = this.createdAt.plusSeconds(this.validity)!!

fun String.birthDateFromSwedishSsn(): LocalDate {
    val trimmedInput = this.trim().replace("-", "").replace(" ", "")
    return LocalDate.parse(
        trimmedInput.substring(0, 4) + "-" + trimmedInput.substring(
            4,
            6
        ) + "-" + trimmedInput.substring(6, 8)
    )
}

fun String.birthDateFromNorwegianSsn(): LocalDate = this.birthDateFromDDMMYYSsn()

fun String.birthDateFromDanishSsn(): LocalDate = this.birthDateFromDDMMYYSsn()

private fun String.birthDateFromDDMMYYSsn(): LocalDate {
    val dayMonthTwoDigitYear = this.dayMonthAndTwoDigitYearFromDDMMYYSsn()
    val year = yearFromTwoDigitYear(dayMonthTwoDigitYear.third.toInt())
    val month = dayMonthTwoDigitYear.second.toInt()
    val day = dayMonthTwoDigitYear.first.toInt()
    return LocalDate.of(year, month, day)
}

fun String.dayMonthAndTwoDigitYearFromDDMMYYSsn(): Triple<String, String, String> {
    val trimmedInput = this.trim().replace("-", "").replace(" ", "")
    val day = trimmedInput.substring(0, 2)
    val month = trimmedInput.substring(2, 4)
    val twoDigitYear = trimmedInput.substring(4, 6)
    return Triple(day, month, twoDigitYear)
}

fun yearFromTwoDigitYear(year: Int): Int {
    val breakPoint = LocalDate.now().minusYears(10).year % 100
    return if (year > breakPoint) {
        "19$year".toInt()
    } else {
        "20$year".toInt()
    }
}

fun String.isValidNorwegianSsn(): Boolean {
    this.toLongOrNull() ?: return false

    if (this.length != 11) {
        return false
    }

    val ssnAsArray = this.map { Character.getNumericValue(it) }.toIntArray()

    return isValidCheckSum(FIRST_BIRTH_CONTROL_SEQUENCE, ssnAsArray) &&
        isValidCheckSum(SECOND_BIRTH_CONTROL_SEQUENCE, ssnAsArray)
}

fun String.isValidDanishSsn(): Boolean {
    this.toLongOrNull() ?: return false

    if (this.length != 10) {
        return false
    }

    return true
}

private fun isValidCheckSum(
    sequence: IntArray,
    ssn: IntArray
): Boolean {
    val checkSum = (sequence.indices).sumBy { sequence[it] * ssn[it] }

    return checkSum % 11 == 0
}

data class DatabaseQuoteRevision(
    val id: Int?,
    val masterQuoteId: UUID,
    val timestamp: Instant,
    val validity: Long,
    val productType: ProductType = ProductType.UNKNOWN,
    val state: QuoteState,
    val attributedTo: Partner,
    val currentInsurer: String? = "",
    val startDate: LocalDate? = null,
    val price: BigDecimal? = null,
    val priceFrom: UUID? = null,
    val currency: String? = null,
    val quoteApartmentDataId: Int?,
    val quoteHouseDataId: Int?,
    val quoteNorwegianHomeContentsDataId: Int?,
    val quoteNorwegianTravelDataId: Int?,
    val quoteDanishHomeContentsDataId: Int?,
    val quoteDanishAccidentDataId: Int?,
    val quoteDanishTravelDataId: Int?,
    val memberId: String?,
    val breachedUnderwritingGuidelines: List<String>?,
    val underwritingGuidelinesBypassedBy: String?,
    val initiatedFrom: QuoteInitiatedFrom?,
    val createdAt: Instant?,
    val originatingProductId: UUID?,
    val agreementId: UUID?,
    val contractId: UUID?,
    val dataCollectionId: UUID?,
    val signFromHopeTriggeredBy: String?
) {

    companion object {
        fun from(quote: Quote, id: Int? = null, timestamp: Instant = Instant.now()) =
            DatabaseQuoteRevision(
                id = id,
                masterQuoteId = quote.id,
                timestamp = timestamp,
                validity = quote.validity,
                productType = quote.productType,
                state = quote.state,
                attributedTo = quote.attributedTo,
                currentInsurer = quote.currentInsurer,
                startDate = quote.startDate,
                price = quote.price,
                currency = quote.currency,
                priceFrom = quote.priceFrom,
                quoteApartmentDataId = when (quote.data) {
                    is SwedishApartmentData -> quote.data.internalId
                    else -> null
                },
                quoteHouseDataId = when (quote.data) {
                    is SwedishHouseData -> quote.data.internalId
                    else -> null
                },
                quoteNorwegianHomeContentsDataId = when (quote.data) {
                    is NorwegianHomeContentsData -> quote.data.internalId
                    else -> null
                },
                quoteNorwegianTravelDataId = when (quote.data) {
                    is NorwegianTravelData -> quote.data.internalId
                    else -> null
                },
                quoteDanishHomeContentsDataId = when (quote.data) {
                    is DanishHomeContentsData -> quote.data.internalId
                    else -> null
                },
                quoteDanishAccidentDataId = when (quote.data) {
                    is DanishAccidentData -> quote.data.internalId
                    else -> null
                },
                quoteDanishTravelDataId = when (quote.data) {
                    is DanishTravelData -> quote.data.internalId
                    else -> null
                },
                memberId = quote.memberId,
                breachedUnderwritingGuidelines = quote.breachedUnderwritingGuidelines,
                underwritingGuidelinesBypassedBy = quote.underwritingGuidelinesBypassedBy,
                createdAt = quote.createdAt,
                initiatedFrom = quote.initiatedFrom,
                originatingProductId = quote.originatingProductId,
                agreementId = quote.agreementId,
                contractId = quote.contractId,
                dataCollectionId = quote.dataCollectionId,
                signFromHopeTriggeredBy = quote.signFromHopeTriggeredBy
            )
    }
}

const val ONE_DAY = 86_400L

data class Quote(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant? = null,
    val price: BigDecimal? = null,
    val currency: String? = null,
    val priceFrom: UUID? = null,
    val productType: ProductType = ProductType.UNKNOWN,
    val state: QuoteState,
    val initiatedFrom: QuoteInitiatedFrom,
    val attributedTo: Partner,
    val data: QuoteData,

    val currentInsurer: String? = null,

    val startDate: LocalDate? = null,

    val validity: Long = ONE_DAY * 30,
    val breachedUnderwritingGuidelines: List<String>?,
    val underwritingGuidelinesBypassedBy: String? = null,
    val memberId: String? = null,
    val originatingProductId: UUID? = null,
    val agreementId: UUID? = null,
    val dataCollectionId: UUID? = null,
    val signFromHopeTriggeredBy: String? = null,
    val contractId: UUID? = null,
    val lineItems: List<LineItem> = emptyList(),
    @JsonIgnore
    val competitorPricing: CompetitorPricing? = null, // This field is not persisted
    @JsonIgnore
    val overriddenPrice: BigDecimal? = null, // This field is not persisted
    @JsonIgnore
    val priceOverriddenBy: String? = null // This field is not persisted
) {
    val isComplete: Boolean
        get() = when {
            price == null -> false
            currency == null -> false
            productType == ProductType.UNKNOWN -> false
            !data.isComplete -> false
            else -> true
        }

    val currencyWithFallbackOnMarket: String
        get() = currency ?: when (market) {
            Market.SWEDEN -> SEK
            Market.NORWAY -> NOK
            Market.DENMARK -> DKK
        }

    val market: Market
        get() = when (this.data) {
            is SwedishHouseData, is SwedishApartmentData -> Market.SWEDEN
            is NorwegianHomeContentsData, is NorwegianTravelData -> Market.NORWAY
            is DanishHomeContentsData, is DanishAccidentData, is DanishTravelData -> Market.DENMARK
        }

    fun update(quoteRequest: QuoteRequest): Quote {
        val newQuote = copy(
            productType = quoteRequest.productType ?: productType,
            startDate = quoteRequest.startDate?.toStockholmLocalDate() ?: startDate,
            data = when (data) {
                is SwedishApartmentData -> data.copy(
                    ssn = quoteRequest.ssn ?: data.ssn,
                    birthDate = quoteRequest.birthDate ?: data.birthDate,
                    firstName = quoteRequest.firstName ?: data.firstName,
                    lastName = quoteRequest.lastName ?: data.lastName,
                    email = quoteRequest.email ?: data.email,
                    phoneNumber = quoteRequest.phoneNumber ?: data.phoneNumber,
                    subType = when (val quoteData = quoteRequest.incompleteQuoteData) {
                        is SwedishApartment? -> quoteData?.subType ?: data.subType
                        else -> null
                    }
                )
                is SwedishHouseData -> data.copy(
                    ssn = quoteRequest.ssn ?: data.ssn,
                    birthDate = quoteRequest.birthDate ?: data.birthDate,
                    firstName = quoteRequest.firstName ?: data.firstName,
                    lastName = quoteRequest.lastName ?: data.lastName,
                    email = quoteRequest.email ?: data.email,
                    phoneNumber = quoteRequest.phoneNumber ?: data.phoneNumber
                )
                is NorwegianHomeContentsData -> data.copy(
                    ssn = quoteRequest.ssn ?: data.ssn,
                    birthDate = quoteRequest.birthDate ?: data.birthDate,
                    firstName = quoteRequest.firstName ?: data.firstName,
                    lastName = quoteRequest.lastName ?: data.lastName,
                    email = quoteRequest.email ?: data.email,
                    phoneNumber = quoteRequest.phoneNumber ?: data.phoneNumber
                )
                is NorwegianTravelData -> data.copy(
                    ssn = quoteRequest.ssn ?: data.ssn,
                    birthDate = quoteRequest.birthDate ?: data.birthDate,
                    firstName = quoteRequest.firstName ?: data.firstName,
                    lastName = quoteRequest.lastName ?: data.lastName,
                    email = quoteRequest.email ?: data.email,
                    phoneNumber = quoteRequest.phoneNumber ?: data.phoneNumber
                )
                is DanishHomeContentsData -> data.copy(
                    ssn = quoteRequest.ssn ?: data.ssn,
                    birthDate = quoteRequest.birthDate ?: data.birthDate,
                    firstName = quoteRequest.firstName ?: data.firstName,
                    lastName = quoteRequest.lastName ?: data.lastName,
                    email = quoteRequest.email ?: data.email,
                    phoneNumber = quoteRequest.phoneNumber ?: data.phoneNumber
                )
                is DanishAccidentData -> data.copy(
                    ssn = quoteRequest.ssn ?: data.ssn,
                    birthDate = quoteRequest.birthDate ?: data.birthDate,
                    firstName = quoteRequest.firstName ?: data.firstName,
                    lastName = quoteRequest.lastName ?: data.lastName,
                    email = quoteRequest.email ?: data.email,
                    phoneNumber = quoteRequest.phoneNumber ?: data.phoneNumber
                )
                is DanishTravelData -> data.copy(
                    ssn = quoteRequest.ssn ?: data.ssn,
                    birthDate = quoteRequest.birthDate ?: data.birthDate,
                    firstName = quoteRequest.firstName ?: data.firstName,
                    lastName = quoteRequest.lastName ?: data.lastName,
                    email = quoteRequest.email ?: data.email,
                    phoneNumber = quoteRequest.phoneNumber ?: data.phoneNumber
                )
            }
        )

        val requestData = quoteRequest.incompleteQuoteData ?: return newQuote

        return when (requestData) {
            is SwedishApartment -> {
                val newQuoteData: SwedishApartmentData = when (val data = newQuote.data) {
                    is SwedishApartmentData -> data
                    is SwedishHouseData -> {
                        SwedishApartmentData(
                            id = data.id,
                            firstName = data.firstName,
                            lastName = data.lastName,
                            email = data.email,
                            ssn = data.ssn,
                            birthDate = data.birthDate,
                            street = data.street,
                            zipCode = data.zipCode,
                            city = data.city,
                            householdSize = data.householdSize,
                            livingSpace = data.livingSpace,
                            phoneNumber = data.phoneNumber
                        )
                    }
                    else -> throw IllegalTypeChangeOnQuote(newQuote.data, requestData)
                }
                newQuote.copy(
                    data = newQuoteData.copy(
                        street = requestData.street ?: newQuoteData.street,
                        zipCode = requestData.zipCode ?: newQuoteData.zipCode,
                        city = requestData.city ?: newQuoteData.city,
                        householdSize = requestData.householdSize ?: newQuoteData.householdSize,
                        livingSpace = requestData.livingSpace ?: newQuoteData.livingSpace,
                        subType = requestData.subType ?: newQuoteData.subType
                    )
                )
            }
            is SwedishHouse -> {
                val newQuoteData: SwedishHouseData = when (val data = newQuote.data) {
                    is SwedishHouseData -> newQuote.data as SwedishHouseData
                    is SwedishApartmentData -> {
                        SwedishHouseData(
                            id = data.id,
                            firstName = data.firstName,
                            lastName = data.lastName,
                            email = data.email,
                            ssn = data.ssn,
                            birthDate = data.birthDate,
                            street = data.street,
                            zipCode = data.zipCode,
                            city = data.city,
                            householdSize = data.householdSize,
                            livingSpace = data.livingSpace,
                            phoneNumber = data.phoneNumber
                        )
                    }
                    else -> throw IllegalTypeChangeOnQuote(newQuote.data, requestData)
                }
                newQuote.copy(
                    data = newQuoteData.copy(
                        street = requestData.street ?: newQuoteData.street,
                        zipCode = requestData.zipCode ?: newQuoteData.zipCode,
                        city = requestData.city ?: newQuoteData.city,
                        householdSize = requestData.householdSize ?: newQuoteData.householdSize,
                        livingSpace = requestData.livingSpace ?: newQuoteData.livingSpace,
                        numberOfBathrooms = requestData.numberOfBathrooms ?: newQuoteData.numberOfBathrooms,
                        isSubleted = requestData.isSubleted ?: newQuoteData.isSubleted,
                        extraBuildings = requestData.extraBuildings?.map((ExtraBuilding)::from)
                            ?: newQuoteData.extraBuildings,
                        ancillaryArea = requestData.ancillaryArea ?: newQuoteData.ancillaryArea,
                        yearOfConstruction = requestData.yearOfConstruction ?: newQuoteData.yearOfConstruction
                    )
                )
            }
            is NorwegianHomeContents -> {
                val newQuoteData: NorwegianHomeContentsData = when (newQuote.data) {
                    is NorwegianHomeContentsData -> newQuote.data
                    else -> throw IllegalTypeChangeOnQuote(newQuote.data, requestData)
                }

                newQuote.copy(
                    data = newQuoteData.copy(
                        street = requestData.street ?: newQuoteData.street,
                        zipCode = requestData.zipCode ?: newQuoteData.zipCode,
                        city = requestData.city ?: newQuoteData.city,
                        livingSpace = requestData.livingSpace ?: newQuoteData.livingSpace,
                        coInsured = requestData.coInsured ?: newQuoteData.coInsured,
                        isYouth = requestData.isYouth ?: newQuoteData.isYouth,
                        type = requestData.subType ?: newQuoteData.type
                    )
                )
            }
            is NorwegianTravel -> {
                val newQuoteData: NorwegianTravelData = when (newQuote.data) {
                    is NorwegianHomeContentsData -> newQuote.data as NorwegianTravelData
                    is NorwegianTravelData -> newQuote.data
                    else -> throw IllegalTypeChangeOnQuote(newQuote.data, requestData)
                }

                newQuote.copy(
                    data = newQuoteData.copy(
                        coInsured = requestData.coInsured ?: newQuoteData.coInsured,
                        isYouth = requestData.isYouth ?: newQuoteData.isYouth
                    )
                )
            }
            is DanishHomeContents -> {
                val newQuoteData: DanishHomeContentsData = when (newQuote.data) {
                    is DanishHomeContentsData -> newQuote.data
                    else -> throw IllegalTypeChangeOnQuote(newQuote.data, requestData)
                }

                newQuote.copy(
                    data = newQuoteData.copy(
                        street = requestData.street ?: newQuoteData.street,
                        zipCode = requestData.zipCode ?: newQuoteData.zipCode,
                        apartment = requestData.apartment ?: newQuoteData.apartment,
                        floor = requestData.floor ?: newQuoteData.floor,
                        city = requestData.city ?: newQuoteData.city,
                        bbrId = requestData.bbrId,
                        livingSpace = requestData.livingSpace ?: newQuoteData.livingSpace,
                        coInsured = requestData.coInsured ?: newQuoteData.coInsured,
                        isStudent = requestData.isStudent ?: newQuoteData.isStudent,
                        type = requestData.subType ?: newQuoteData.type
                    )
                )
            }
            is DanishAccident -> {
                val newQuoteData: DanishAccidentData = when (newQuote.data) {
                    is DanishAccidentData -> newQuote.data
                    is DanishHomeContentsData -> newQuote.data as DanishAccidentData
                    is DanishTravelData -> newQuote.data as DanishAccidentData
                    else -> throw IllegalTypeChangeOnQuote(newQuote.data, requestData)
                }

                newQuote.copy(
                    data = newQuoteData.copy(
                        street = requestData.street ?: newQuoteData.street,
                        zipCode = requestData.zipCode ?: newQuoteData.zipCode,
                        apartment = requestData.apartment ?: newQuoteData.apartment,
                        floor = requestData.floor ?: newQuoteData.floor,
                        city = requestData.city ?: newQuoteData.city,
                        bbrId = requestData.bbrId,
                        coInsured = requestData.coInsured ?: newQuoteData.coInsured,
                        isStudent = requestData.isStudent ?: newQuoteData.isStudent
                    )
                )
            }
            is DanishTravel -> {
                val newQuoteData: DanishTravelData = when (newQuote.data) {
                    is DanishAccidentData -> newQuote.data as DanishTravelData
                    is DanishHomeContentsData -> newQuote.data as DanishTravelData
                    is DanishTravelData -> newQuote.data
                    else -> throw IllegalTypeChangeOnQuote(newQuote.data, requestData)
                }

                newQuote.copy(
                    data = newQuoteData.copy(
                        street = requestData.street ?: newQuoteData.street,
                        zipCode = requestData.zipCode ?: newQuoteData.zipCode,
                        apartment = requestData.apartment ?: newQuoteData.apartment,
                        floor = requestData.floor ?: newQuoteData.floor,
                        city = requestData.city ?: newQuoteData.city,
                        bbrId = requestData.bbrId,
                        coInsured = requestData.coInsured ?: newQuoteData.coInsured,
                        isStudent = requestData.isStudent ?: newQuoteData.isStudent
                    )
                )
            }
        }
    }

    fun clearBreachedUnderwritingGuidelines(): Quote = this.copy(breachedUnderwritingGuidelines = listOf())

    fun recoverBirthDateFromSSN() = when {
        this.swedishApartment != null || this.swedishHouse != null -> this.ssn.birthDateFromSwedishSsn()
        this.norwegianHomeContents != null || this.norwegianTravel != null -> this.ssn.birthDateFromNorwegianSsn()
        this.danishHomeContents != null || this.danishAccident != null || this.danishTravel != null -> this.ssn.birthDateFromDanishSsn()
        else -> null
    }

    fun getTimeZoneId() = when (this.data) {
        is SwedishHouseData,
        is SwedishApartmentData -> ZoneId.of("Europe/Stockholm")
        is NorwegianHomeContentsData,
        is NorwegianTravelData -> ZoneId.of("Europe/Oslo")
        is DanishHomeContentsData,
        is DanishAccidentData,
        is DanishTravelData -> ZoneId.of("Europe/Copenhagen")
    }

    fun getLivingArea(): Int? {
        return when (this.data) {
            is SwedishHouseData -> this.data.livingSpace
            is SwedishApartmentData -> this.data.livingSpace
            is NorwegianHomeContentsData -> this.data.livingSpace
            is DanishHomeContentsData -> this.data.livingSpace
            is DanishAccidentData,
            is DanishTravelData,
            is NorwegianTravelData -> null
        }
    }

    companion object {
        private const val SEK = "SEK"
        private const val NOK = "NOK"
        private const val DKK = "DKK"
    }
}

class IllegalTypeChangeOnQuote(
    quoteData: QuoteData,
    requestData: QuoteRequestData
) : Exception(
    "Illegal to change from type [${quoteData::class}] to [${requestData::class}]. [QuoteData: $quoteData] [QuoteRequestData: $requestData]"
)

data class DeletedQuote(
    val quoteId: UUID,
    val createdAt: Instant,
    val deletedAt: Instant,
    val type: String,
    val memberId: String? = null,
    val hashedSsn: String? = null,
    val quote: String,
    val revs: String
)
