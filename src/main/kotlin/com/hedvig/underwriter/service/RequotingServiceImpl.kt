package com.hedvig.underwriter.service

import com.hedvig.productPricingObjects.enums.AgreementStatus
import com.hedvig.underwriter.model.AddressData
import com.hedvig.underwriter.model.DanishAccidentData
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.DanishTravelData
import com.hedvig.underwriter.model.ExtraBuilding
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteData
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.QuoteRepository
import com.hedvig.underwriter.model.QuoteState
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.model.birthDateMaybe
import com.hedvig.underwriter.model.ssnMaybe
import com.hedvig.underwriter.service.model.PersonPolicyHolder
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.LineItem
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.util.logger
import org.javamoney.moneta.Money
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.money.Monetary
import kotlin.reflect.full.memberProperties

@Service
class RequotingServiceImpl(
    private val quoteRepository: QuoteRepository,
    private val productPricingService: ProductPricingService
) : RequotingService {

    override fun blockDueToExistingAgreement(quote: Quote): Boolean {
        try {

            logger.debug("Check if existing agreement for quote. Initiated from ${quote.initiatedFrom}")

            // Do not block "internal" requests
            if (!quote.initiatedFrom.isAnyOf(
                    QuoteInitiatedFrom.ANDROID,
                    QuoteInitiatedFrom.IOS,
                    QuoteInitiatedFrom.RAPIO,
                    QuoteInitiatedFrom.WEBONBOARDING)) {
                return false
            }

            return hasExistingAgreement(quote)
        } catch (e: Exception) {
            logger.warn("Failed to check if to block due to active agreement", e)
            return false
        }
    }

    override fun useOldOrNewPrice(quote: Quote, newPrice: Price): Price {

        logger.debug("Check if to reuse old price for quote. Initiated from ${quote.initiatedFrom}")

        // Do not reuse price for "internal" requests
        if (!quote.initiatedFrom.isAnyOf(
                QuoteInitiatedFrom.ANDROID,
                QuoteInitiatedFrom.IOS,
                QuoteInitiatedFrom.RAPIO,
                QuoteInitiatedFrom.WEBONBOARDING)) {
            return newPrice
        }

        // Get all existing quotes for user's address and ssn / birthday
        // having the same props as for requested quote
        val quotes = getOldQuotesByFingerprint(quote)
            .filter { it.price != null && it.currency != null }
            .filter { it.isSame(quote) }

        // First time user?
        if (quotes.isEmpty()) {
            return newPrice
        }

        logger.debug("Found ${quotes.size} equal quote(s) for user: {}", quotes.joinToString(separator = ", ") { "${it.id}: ${it.price} ${it.currency} (${it.createdAt})" })

        val nowMinus30d = Instant.now().minus(30, ChronoUnit.DAYS)

        val lastPrice = getLastPrice(quotes)
        val anyOlderThan30d = quotes.any() { it.createdAt.isBefore(nowMinus30d) }
        val anyChangeLessThan30d = quotes
            .filter { it.createdAt.isAfter(nowMinus30d) }
            .all { lastPrice.isSameAmount(it.price) }
            .not()

        // If new price has not changed then use it (line items may have changed thou)
        if (lastPrice.price == newPrice.price) {
            return newPrice
        }

        // During migration to line items feature we favour the new price if last does not have any line items
        if (lastPrice.lineItems.isEmpty() && newPrice.lineItems.isNotEmpty()) {
            return newPrice
        }

        // If user do not have older quotes than 30 days then reuse last price
        if (!anyOlderThan30d) {
            logger.info("Member has other equal quotes but not older than 30 days, reuse last quote price: $lastPrice instead of $newPrice")
            return lastPrice
        }

        // If user has any older quotes than 30 days then reuse last price
        // if there where a change in last 30 days
        if (anyOlderThan30d && anyChangeLessThan30d) {
            logger.info("Member has other equal quotes older than 30 days and there was a price change during last 30 days, reuse last quote price: $lastPrice instead of $newPrice")
            return lastPrice
        }

        // If user has older quotes than 30 days and no changes since then it is time to change price
        return newPrice
    }

    private fun hasExistingAgreement(quote: Quote): Boolean {

        // Get all existing quotes user's address and ssn or birthday
        var quotes = getOldQuotesByFingerprint(quote)

        if (quotes.isEmpty()) {
            return false
        }

        logger.debug("Found ${quotes.size} existing quote(s) for same address: {}", quotes.joinToString(separator = ", ") { it.id.toString() })

        // Filter out all signed quotes having an agreement
        quotes = quotes
            .filter { it.state == QuoteState.SIGNED }
            .filter { it.agreementId != null }

        logger.debug("Found ${quotes.size} existing signed quote(s) for same birth date/ssn: {}", quotes.joinToString(separator = ", ") { it.id.toString() })

        for (existingQuote in quotes) {
            val agreement = productPricingService.getAgreement(existingQuote.agreementId!!)

            // Is agreement active?
            val active = agreement.status.isAnyOf(
                AgreementStatus.PENDING,
                AgreementStatus.ACTIVE,
                AgreementStatus.ACTIVE_IN_FUTURE
            )

            if (active) {
                logger.info("Quote matches an already signed quote: ${existingQuote.id} with an active agreement: ${agreement.id} - ${agreement.status}")
                return true
            }
        }

        return false
    }

    private fun getOldQuotesByFingerprint(quote: Quote): List<Quote> {

        val data = quote.data

        // We require ssn or birthdate to be able to identify/fingerprint customer
        if (data !is PersonPolicyHolder<*> || (data.ssn == null && data.birthDate == null)) {
            return listOf()
        }

        // We require address to be able to identify/fingerprint customer
        if (data !is AddressData || data.street == null || data.zipCode == null) {
            return listOf()
        }

        // Get all existing quotes for user's address
        val quotes = quoteRepository.findQuotesByAddress(data.street!!, data.zipCode!!, quote.data)
            .sortedBy { it.createdAt }

        // Filter out all signed quotes for user's ssn and birth date having an agreement
        return quotes
            .filter { it.birthDateMaybe == quote.birthDateMaybe }
            .filter { quote.ssnMaybe == null || it.ssnMaybe == null || it.ssnMaybe == quote.ssnMaybe }
    }

    private fun QuoteInitiatedFrom.isAnyOf(vararg initiatedFrom: QuoteInitiatedFrom): Boolean =
        initiatedFrom.any { this == it }

    private fun AgreementStatus.isAnyOf(vararg statuses: AgreementStatus): Boolean =
        statuses.any { this == it }

    private fun getLastPrice(quotes: List<Quote>): Price {

        val quote = quotes.sortedBy { it.createdAt }.last()
        val lineItems = quote.lineItems.map { LineItem(it.type, it.subType, it.amount) }

        return Price(Money.of(quote.price, quote.currency), lineItems, quote.id)
    }
}

fun Price.isSameAmount(amount: BigDecimal?): Boolean {
    if (amount == null) {
        return false
    }
    val other = Money.of(amount, this.currency)
    return this.price.with(Monetary.getDefaultRounding()) == other.with(Monetary.getDefaultRounding())
}

fun Quote.isSame(quote: Quote) =
    QuoteComparator.isSameQuotes(this, quote)

object QuoteComparator {

    val justToMakeSureNoComparatorsAreForgottenWhenAddingNewTypes = fun (type: QuoteData) =
        when (type) {
            is SwedishApartmentData,
            is SwedishHouseData,
            is NorwegianHomeContentsData,
            is NorwegianTravelData,
            is DanishAccidentData,
            is DanishHomeContentsData,
            is DanishTravelData -> "Done"
        }

    private val comparators = mapOf(
        "ssn" to ::isSameAllowNull,
        "birthDate" to ::isSameAllowNull,
        "street" to ::isSame,
        "floor" to ::isSame,
        "zipCode" to ::isSame,
        "city" to ::isSame,
        "livingSpace" to ::isSame,
        "householdSize" to ::isSame,
        "ancillaryArea" to ::isSame,
        "yearOfConstruction" to ::isSame,
        "numberOfBathrooms" to ::isSame,
        "isSubleted" to ::isSame,
        "coInsured" to ::isSame,
        "isYouth" to ::isSame,
        "isStudent" to ::isSame,
        "subType" to ::isSame,
        "bbrId" to ::isSameAllowNull,
        "apartment" to ::isSame,
        "extraBuildings" to ::isSameExtraBuildings
    )

    private fun hasSameProps(obj1: Any?, obj2: Any?): Boolean {
        if (obj1 == null && obj2 == null) {
            return true
        }

        if (obj1 == null || obj2 == null) {
            return false
        }

        val props1 = obj1.asMap()
        val props2 = obj2.asMap()

        for ((field, value1) in props1) {

            val value2 = props2[field]

            val same = comparators[field]?.invoke(value1, value2) ?: true

            if (!same) {
                return false
            }
        }

        return true
    }

    private fun Any.asMap(): Map<String, Any?> {
        val props = this.javaClass.kotlin.memberProperties.associateBy { it.name }
        return props.keys.associateWith { props[it]?.get(this) }
    }

    private fun isSame(obj1: Any?, obj2: Any?): Boolean {
        return obj1 == obj2
    }

    private fun isSameAllowNull(obj1: Any?, obj2: Any?): Boolean {
        if (obj1 == null || obj2 == null) {
            return true
        }
        return obj1 == obj2
    }

    @Suppress("UNCHECKED_CAST")
    private fun isSameExtraBuildings(obj1: Any?, obj2: Any?): Boolean {
        val buildings1 = obj1 as List<ExtraBuilding>? ?: listOf()
        val buildings2 = obj2 as List<ExtraBuilding>? ?: listOf()

        val fingerprint1 = buildings1
            .map { "${it.type}|${it.area}|${it.hasWaterConnected}" }
            .sorted()
            .joinToString(",")
        val fingerprint2 = buildings2
            .map { "${it.type}|${it.area}|${it.hasWaterConnected}" }
            .sorted()
            .joinToString(",")

        return fingerprint1 == fingerprint2
    }

    fun isSameQuotes(quote1: Quote, quote2: Quote): Boolean {
        if (quote1.data::class != quote2.data::class) {
            return false
        }

        return hasSameProps(quote1.data, quote2.data)
    }
}
