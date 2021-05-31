package com.hedvig.underwriter.serviceIntegration.lookupService

import com.hedvig.underwriter.model.AddressData
import com.hedvig.underwriter.model.DanishHomeContentsData
import com.hedvig.underwriter.model.NorwegianHomeContentsData
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.SwedishApartmentData
import com.hedvig.underwriter.model.SwedishHouseData
import com.hedvig.underwriter.serviceIntegration.lookupService.dtos.CompetitorPricing
import com.hedvig.underwriter.util.logger
import org.springframework.stereotype.Service

@Service
class LookupServiceImpl(
    val lookupServiceClient: LookupServiceClient
) : LookupService {

    private val TYPE_HOUSE = "houseContentInsurance"
    private val STREET_AND_BR_REGEX = "\\D+\\d+".toRegex()

    override fun getMatchingCompetitorPrice(quote: Quote): CompetitorPricing? {
        try {

            val insuranceType = getInsuranceType(quote) ?: return null

            val response = lookupServiceClient.getMatchingCompetitorPrice(quote.dataCollectionId!!,
                quote.currencyWithFallbackOnMarket, insuranceType
            )

            if (response.statusCode.is2xxSuccessful) {

                // Ensure the found competitor price matches the quote details
                if (isMatching(quote, response.body!!)) {
                    return response.body
                } else {
                    return null
                }
            }
        } catch (exception: Exception) {
            logger.error("Something went wrong when fetching MonthlyPremiumData from lookup service ${exception.message}")
        }
        return null
    }

    private fun isMatching(quote: Quote, response: CompetitorPricing): Boolean {

        val livingAreaMatches = (response.livingArea != null && response.livingArea == quote.getLivingArea())
        val postalCodeMatches =
            (response.postalCode != null && response.postalCode == (quote.data as AddressData).zipCode)

        if (livingAreaMatches || postalCodeMatches) {
            logger.info("LivingArea($livingAreaMatches) or postalCode($postalCodeMatches) matches, so we're assuming this is the same object")
            return true
        } else {
            // Do some logging only, so we can evaluate
            if (streetAddressMatches(quote, response)) {
                logger.info("(Test) Street and number matched, but not postalCode/livingArea. quoteId: ${quote.id}, dataCollectionId: ${quote.dataCollectionId}")
            }

            return false
        }
    }

    private fun getInsuranceType(quote: Quote): String? {

        return when (quote.data) {
            is SwedishApartmentData,
            is SwedishHouseData,
            is NorwegianHomeContentsData,
            is DanishHomeContentsData
            -> TYPE_HOUSE

            else -> {
                logger.warn("Unexpected insurance type, for getting competiror pricing")
                null
            }
        }
    }

    private fun streetAddressMatches(quote: Quote, competitorPricing: CompetitorPricing): Boolean {

        // Basic matching test of SE addresses, if the street+nunber is contained within our address
        val quoteStreet = (quote.data as AddressData).street
        val competitorHolderAddress = competitorPricing.insuranceObjectAddress

        if (quoteStreet != null && competitorHolderAddress != null) {

            val streetAndNumber = STREET_AND_BR_REGEX.find(competitorHolderAddress)

            if (streetAndNumber != null) {
                if (quoteStreet.contains(streetAndNumber.value, true)) {
                    return true
                }
            }
        }

        return false
    }
}
