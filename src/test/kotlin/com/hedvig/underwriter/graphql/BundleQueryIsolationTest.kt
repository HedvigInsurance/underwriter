package com.hedvig.underwriter.graphql

import com.hedvig.graphql.commons.extensions.getAcceptLanguage
import com.hedvig.graphql.commons.extensions.getToken
import com.hedvig.libs.translations.Translations
import com.hedvig.underwriter.graphql.type.QuoteBundleInputInput
import com.hedvig.underwriter.graphql.type.QuoteMapper
import com.hedvig.underwriter.service.BundleQuotesService
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.service.SignService
import graphql.schema.DataFetchingEnvironment
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class BundleQueryIsolationTest {

    @MockK
    lateinit var quoteService: QuoteService

    @MockK
    lateinit var signService: SignService

    @MockK
    lateinit var bundleQuoteService: BundleQuotesService

    @MockK
    lateinit var translations: Translations

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
            signService,
            bundleQuoteService,
            QuoteMapper(translations)
        )

        every { bundleQuoteService.bundleQuotes(any(), any()) } throws RuntimeException("Should not get here")
        every { dataFetchingEnvironment.getToken() } returns "1337"
        every { dataFetchingEnvironment.getAcceptLanguage() } returns "sv-SE"

        thrown.expect(EmptyBundleQueryException::class.java)
        query.quoteBundle(QuoteBundleInputInput(listOf()), dataFetchingEnvironment)
    }
}
