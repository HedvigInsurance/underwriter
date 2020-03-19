package com.hedvig.underwriter.service

import com.hedvig.underwriter.graphql.type.QuoteBundle
import java.util.Locale
import java.util.UUID

interface BundleQuotesService {
    fun bundleQuotes(memberId: String, ids: List<UUID>, locale: Locale): QuoteBundle
}
