package com.hedvig.underwriter.graphql

import com.hedvig.graphql.commons.extensions.getAcceptLanguage
import com.hedvig.graphql.commons.extensions.getToken
import com.hedvig.localization.service.LocalizationService
import com.hedvig.localization.service.TextKeysLocaleResolver
import com.hedvig.localization.service.TextKeysLocaleResolverImpl
import com.hedvig.underwriter.graphql.type.QuoteBundleInputInput
import com.hedvig.underwriter.graphql.type.TypeMapper
import com.hedvig.underwriter.service.BundleQuotesService
import com.hedvig.underwriter.service.QuoteService
import graphql.schema.DataFetchingEnvironment
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class BundleQueryIsolationTest {

    val textKeysLocaleResolver: TextKeysLocaleResolver = TextKeysLocaleResolverImpl()

    @MockK
    lateinit var quoteService: QuoteService

    @MockK
    lateinit var bundleQuoteService: BundleQuotesService

    @MockK
    lateinit var localizationService: LocalizationService

    @MockK
    lateinit var dataFetchingEnvironment: DataFetchingEnvironment

    @get:Rule
    val thrown = ExpectedException.none()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `need at least one contractId`() {

        val query = Query(
            quoteService,
            bundleQuoteService,
            textKeysLocaleResolver,
            TypeMapper(localizationService)
            )

        every { bundleQuoteService.bundleQuotes(any(), any(), any()) } throws RuntimeException("Should not get here")
        every { dataFetchingEnvironment.getToken() } returns "1337"
        every { dataFetchingEnvironment.getAcceptLanguage() } returns "sv-SE"

        thrown.expect(EmptyBundleQueryException::class.java)
        query.quoteBundle(QuoteBundleInputInput(listOf()), dataFetchingEnvironment)
    }
}
