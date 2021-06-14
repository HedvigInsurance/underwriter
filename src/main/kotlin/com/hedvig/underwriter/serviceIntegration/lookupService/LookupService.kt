package com.hedvig.underwriter.serviceIntegration.lookupService

import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.serviceIntegration.lookupService.dtos.CompetitorPricing

interface LookupService {
    fun getMatchingCompetitorPrice(quote: Quote): CompetitorPricing?
}
