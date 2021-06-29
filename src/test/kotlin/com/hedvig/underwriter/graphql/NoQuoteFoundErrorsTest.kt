package com.hedvig.underwriter.graphql

import com.hedvig.graphql.commons.extensions.getTokenOrNull
import com.hedvig.libs.translations.TranslationsClient
import com.hedvig.underwriter.graphql.type.QuoteMapper
import com.hedvig.underwriter.service.BundleQuotesService
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.service.SignService
import graphql.schema.DataFetchingEnvironment
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import java.util.UUID
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class NoQuoteFoundErrorsTest {
    @MockK
    lateinit var quoteService: QuoteService

    @MockK
    lateinit var signService: SignService

    @MockK
    lateinit var bundleQuoteService: BundleQuotesService

    @MockK
    lateinit var translationsClient: TranslationsClient

    @MockK
    lateinit var dataFetchingEnvironment: DataFetchingEnvironment

    @get:Rule
    val thrown = ExpectedException.none()

    lateinit var sut: Query

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        sut =
            Query(quoteService, signService, bundleQuoteService, QuoteMapper(translationsClient))
    }

    @Test
    fun `lastest quote finds no quote`() {

        every { dataFetchingEnvironment.getTokenOrNull() } returns "1337"
        every { quoteService.getLatestQuoteForMemberId(any()) } returns null

        thrown.expect(QuoteNotFoundQueryException::class.java)
        thrown.expectMessage("No quote found for memberId: 1337")
        sut.lastQuoteOfMember(dataFetchingEnvironment)
    }

    @Test
    fun `get quote finds no quote`() {

        every { quoteService.getQuote(any()) } returns null

        val quoteId = UUID.fromString("4ad0494c-9906-11ea-a02e-3af9d3902f96")

        thrown.expect(QuoteNotFoundQueryException::class.java)
        thrown.expectMessage("No quote with id '$quoteId' was found!")
        sut.quote(quoteId, dataFetchingEnvironment)
    }
}
