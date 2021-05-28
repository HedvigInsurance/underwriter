package com.hedvig.underwriter.service.quotesSignDataStrategies

import com.hedvig.underwriter.model.DanishAccidentData
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.DanishTravelData
import com.hedvig.underwriter.model.Market
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.NorwegianTravelData
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.service.model.SignMethod
import com.hedvig.underwriter.service.model.StartSignErrors
import com.hedvig.underwriter.service.model.StartSignResponse
import com.hedvig.underwriter.util.logger
import org.springframework.stereotype.Service

@Service
class SignStrategyService(
    private val swedishBankIdSignStrategy: SwedishBankIdSignStrategy,
    private val simpleSignStrategy: SimpleSignStrategy,
    private val selfChangeCommittingStrategy: SelfChangeCommittingStrategy
) : SignStrategy {
    override fun startSign(quotes: List<Quote>, signData: SignData): StartSignResponse {
        val strategy = quotes.getStrategiesFromQuotes()

        if (strategy.isEmpty()) {
            return StartSignErrors.noQuotes
        }

        if (strategy.size > 1 || quotes.markets.size > 1) {
            return StartSignErrors.quotesCanNotBeBundled
        }

        val error = validateBundling(quotes)

        if (error != null) {
            return error
        }

        return strategy.first().startSign(quotes, signData)
    }

    fun validateBundling(quotes: List<Quote>): StartSignResponse.FailedToStartSign? {

        when (quotes.safelyMarket()) {
            Market.SWEDEN -> {
                if (quotes.size > 1) {
                    logger.error("Can not bundle swedish quotes [Quotes: $quotes]")
                    return StartSignErrors.quotesCanNotBeBundled
                }
            }
            Market.NORWAY -> {
                if (quotes.size > 1 && !quotes.areTwoValidNorwegianQuotes()) {
                    logger.error("Norwegian quotes is not valid [Quotes: $quotes]")
                    return StartSignErrors.quotesCanNotBeBundled
                }
            }
            Market.DENMARK -> {
                when {
                    quotes.size == 1 -> {
                        if (quotes[0].data !is DanishHomeContentsData) {
                            logger.error("Single danish quote can not be signed alone [Quotes: $quotes]")
                            return StartSignErrors.singleQuoteCanNotBeSignedAlone
                        }
                    }
                    quotes.size > 1 -> {
                        if (!quotes.isValidDanishQuoteBundle()) {
                            logger.error("Danish quotes is not valid [Quotes: $quotes]")
                            return StartSignErrors.quotesCanNotBeBundled
                        }
                    }
                }
            }
        }
        return null
    }

    private fun List<Quote>.getStrategiesFromQuotes() = this.map {
        if (it.initiatedFrom == QuoteInitiatedFrom.SELF_CHANGE)
            selfChangeCommittingStrategy
        else when (it.data) {
            is SwedishHouseData,
            is SwedishApartmentData -> swedishBankIdSignStrategy
            is NorwegianHomeContentsData,
            is NorwegianTravelData,
            is DanishHomeContentsData,
            is DanishAccidentData,
            is DanishTravelData -> simpleSignStrategy
        }
    }.toSet()

    private fun List<Quote>.areTwoValidNorwegianQuotes(): Boolean =
        this.size == 2 &&
            this.any { it.data is NorwegianHomeContentsData } &&
            this.any { it.data is NorwegianTravelData }

    private fun List<Quote>.isValidDanishQuoteBundle(): Boolean =
        this.areTwoValidDanishQuotes() || this.areThreeValidDanishQuotes()

    private fun List<Quote>.areTwoValidDanishQuotes(): Boolean =
        this.size == 2 &&
            (
                this.any { it.data is DanishHomeContentsData } &&
                    (this.any { it.data is DanishAccidentData } || this.any { it.data is DanishTravelData })
                )

    private fun List<Quote>.areThreeValidDanishQuotes(): Boolean =
        this.size == 3 &&
            this.any { it.data is DanishHomeContentsData } &&
            this.any { it.data is DanishAccidentData } &&
            this.any { it.data is DanishTravelData }

    override fun getSignMethod(quotes: List<Quote>): SignMethod {
        val strategy = quotes.getStrategiesFromQuotes()

        if (strategy.isEmpty()) {
            throw RuntimeException("No strategy on getSignMethod for [Quotes: $quotes]")
        }

        if (strategy.size > 1) {
            throw RuntimeException("Multiple strategies on getSignMethod for [Quotes: $quotes]")
        }

        return strategy.first().getSignMethod(quotes)
    }
}
