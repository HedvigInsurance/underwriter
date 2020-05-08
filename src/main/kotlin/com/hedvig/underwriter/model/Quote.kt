package com.hedvig.underwriter.model

import com.hedvig.underwriter.service.model.PersonPolicyHolder
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.service.model.QuoteRequestData
import com.hedvig.underwriter.service.model.QuoteRequestData.NorwegianHomeContents
import com.hedvig.underwriter.service.model.QuoteRequestData.NorwegianTravel
import com.hedvig.underwriter.service.model.QuoteRequestData.SwedishApartment
import com.hedvig.underwriter.service.model.QuoteRequestData.SwedishHouse
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
    get() = (data as? PersonPolicyHolder<*>)?.birthDate
        ?: recoverBirthDateFromSSN()
        ?: throw RuntimeException("No birthDate on Quote! $this")

val Quote.email
    get() = (data as? PersonPolicyHolder<*>)?.email

val Quote.swedishApartment
    get() = (data as? SwedishApartmentData)

val Quote.swedishHouse
    get() = (data as? SwedishHouseData)

val Quote.norwegianHomeContents
    get() = (data as? NorwegianHomeContentsData)

val Quote.norwegianTravel
    get() = (data as? NorwegianTravelData)

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

fun String.birthDateFromNorwegianSsn(): LocalDate {
    return LocalDate.parse(this.birthDateStringFromNorwegianSsn())
}

fun String.dayMonthAndTwoDigitYearFromNorwegianSsn(): Triple<String, String, String> {
    val trimmedInput = this.trim().replace("-", "").replace(" ", "")
    val day = trimmedInput.substring(0, 2)
    val month = trimmedInput.substring(2, 4)
    val twoDigitYear = trimmedInput.substring(4, 6)
    return Triple(day, month, twoDigitYear)
}

fun String.birthDateStringFromNorwegianSsn(): String {
    val dayMonthYear = this.dayMonthAndTwoDigitYearFromNorwegianSsn()
    val breakPoint = LocalDate.now().minusYears(10).year.toString().substring(2, 4).toInt()

    val year = if (dayMonthYear.third.toInt() > breakPoint) {
        "19${dayMonthYear.third}"
    } else {
        "20${dayMonthYear.third}"
    }
    return "$year-${dayMonthYear.second}-${dayMonthYear.first}"
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
    val quoteApartmentDataId: Int?,
    val quoteHouseDataId: Int?,
    val quoteNorwegianHomeContentsDataId: Int?,
    val quoteNorwegianTravelDataId: Int?,
    val memberId: String?,
    val breachedUnderwritingGuidelines: List<String>?,
    val underwritingGuidelinesBypassedBy: String?,
    val initiatedFrom: QuoteInitiatedFrom?,
    val createdAt: Instant?,
    val originatingProductId: UUID?,
    val signedProductId: UUID?,
    val dataCollectionId: UUID?
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
                memberId = quote.memberId,
                breachedUnderwritingGuidelines = quote.breachedUnderwritingGuidelines,
                underwritingGuidelinesBypassedBy = quote.underwritingGuidelinesBypassedBy,
                createdAt = quote.createdAt,
                initiatedFrom = quote.initiatedFrom,
                originatingProductId = quote.originatingProductId,
                signedProductId = quote.signedProductId,
                dataCollectionId = quote.dataCollectionId
            )
    }
}

const val ONE_DAY = 86_400L

data class Quote(
    val id: UUID,
    val createdAt: Instant,
    val price: BigDecimal? = null,
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
    val signedProductId: UUID? = null,
    val dataCollectionId: UUID? = null
) {
    val isComplete: Boolean
        get() = when {
            price == null -> false
            productType == ProductType.UNKNOWN -> false
            !data.isComplete -> false
            else -> true
        }

    val currency: String
        get() = when (this.data) {
            is SwedishApartmentData -> SEK
            is SwedishHouseData -> SEK
            is NorwegianTravelData -> NOK
            is NorwegianHomeContentsData -> NOK
        }

    fun update(quoteRequest: QuoteRequest): Quote {
        var newQuote = copy(
            productType = quoteRequest.productType ?: productType,
            startDate = quoteRequest.startDate?.toStockholmLocalDate() ?: startDate,
            data = when (data) {
                is SwedishApartmentData -> data.copy(
                    ssn = quoteRequest.ssn ?: data.ssn,
                    birthDate = quoteRequest.birthDate ?: data.birthDate,
                    firstName = quoteRequest.firstName ?: data.firstName,
                    lastName = quoteRequest.lastName ?: data.lastName,
                    email = quoteRequest.email ?: data.email,
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
                    email = quoteRequest.email ?: data.email
                )
                is NorwegianHomeContentsData -> data.copy(
                    ssn = quoteRequest.ssn ?: data.ssn,
                    birthDate = quoteRequest.birthDate ?: data.birthDate,
                    firstName = quoteRequest.firstName ?: data.firstName,
                    lastName = quoteRequest.lastName ?: data.lastName,
                    email = quoteRequest.email ?: data.email
                )
                is NorwegianTravelData -> data.copy(
                    ssn = quoteRequest.ssn ?: data.ssn,
                    birthDate = quoteRequest.birthDate ?: data.birthDate,
                    firstName = quoteRequest.firstName ?: data.firstName,
                    lastName = quoteRequest.lastName ?: data.lastName,
                    email = quoteRequest.email ?: data.email
                )
            }
        )

        when (val requestData = quoteRequest.incompleteQuoteData) {
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
                            livingSpace = data.livingSpace
                        )
                    }
                    else -> throw IllegalTypeCangeOnQuote(newQuote.data, requestData)
                }
                newQuote = newQuote.copy(
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
                            livingSpace = data.livingSpace
                        )
                    }
                    else -> throw IllegalTypeCangeOnQuote(newQuote.data, requestData)
                }
                newQuote = newQuote.copy(
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
                    is NorwegianHomeContentsData -> newQuote.data as NorwegianHomeContentsData
                    else -> throw IllegalTypeCangeOnQuote(newQuote.data, requestData)
                }

                newQuote = newQuote.copy(
                    data = newQuoteData.copy(
                        street = requestData.street ?: newQuoteData.street,
                        zipCode = requestData.zipCode ?: newQuoteData.zipCode,
                        city = requestData.city ?: newQuoteData.city,
                        livingSpace = requestData.livingSpace ?: newQuoteData.livingSpace,
                        coInsured = requestData.coInsured ?: newQuoteData.coInsured,
                        isYouth = requestData.isYouth ?: newQuoteData.isYouth,
                        type = requestData.type ?: newQuoteData.type
                    )
                )
            }
            is NorwegianTravel -> {
                val newQuoteData: NorwegianTravelData = when (newQuote.data) {
                    is NorwegianHomeContentsData -> newQuote.data as NorwegianTravelData
                    else -> throw IllegalTypeCangeOnQuote(newQuote.data, requestData)
                }

                newQuote = newQuote.copy(
                    data = newQuoteData.copy(
                        coInsured = requestData.coInsured ?: newQuoteData.coInsured,
                        isYouth = requestData.isYouth ?: newQuoteData.isYouth
                    )
                )
            }
        }
        return newQuote
    }

    fun recoverBirthDateFromSSN() = when {
        this.swedishApartment != null || this.swedishHouse != null -> this.ssn.birthDateFromSwedishSsn()
        this.norwegianHomeContents != null || this.norwegianTravel != null -> this.ssn.birthDateFromNorwegianSsn()
        else -> null
    }

    fun getTimeZoneId() = when (this.data) {
        is SwedishHouseData,
        is SwedishApartmentData -> ZoneId.of("Europe/Stockholm")
        is NorwegianHomeContentsData,
        is NorwegianTravelData -> ZoneId.of("Europe/Oslo")
    }

    companion object {
        private const val SEK = "SEK"
        private const val NOK = "NOK"
    }
}

class IllegalTypeCangeOnQuote(
    quoteData: QuoteData,
    requestData: QuoteRequestData
) : Exception(
    "Illegal to cange from type [${quoteData::class}] to [${requestData::class}]. [QuoteData: $quoteData] [QuoteRequestData: $requestData]"
)
