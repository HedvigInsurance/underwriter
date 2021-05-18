package com.hedvig.underwriter.service

import arrow.core.Either
import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.hedvig.underwriter.model.Market
import com.hedvig.underwriter.model.QuoteData
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.QuoteRepository
import com.hedvig.underwriter.service.guidelines.BaseGuideline
import com.hedvig.underwriter.service.guidelines.BreachedGuidelineCode
import com.hedvig.underwriter.service.quoteStrategies.QuoteStrategyService
import com.hedvig.underwriter.serviceIntegration.priceEngine.PriceEngineService
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryRequest
import com.hedvig.underwriter.serviceIntegration.priceEngine.dtos.PriceQueryResponse
import com.hedvig.underwriter.testhelp.databuilder.DanishHomeContentsQuoteRequestBuilder
import com.hedvig.underwriter.testhelp.databuilder.NorwegianHomeContentsQuoteRequestBuilder
import com.hedvig.underwriter.web.dtos.ErrorCodes
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.javamoney.moneta.Money
import org.junit.Test
import java.util.UUID

class CreateQuoteTest {
    val strategyService = mockk<QuoteStrategyService>()
    val priceEngineService = mockk<PriceEngineService>()
    val quoteRepository = mockk<QuoteRepository>()
    val metrics = mockk<UnderwriterImpl.BreachedGuidelinesCounter>(relaxed = true)

    val cut = QuoteServiceImpl(
        UnderwriterImpl(priceEngineService, strategyService, mockk(relaxed = true), mockk(), metrics),
        mockk(),
        mockk(),
        quoteRepository,
        mockk(),
        strategyService
    )

    @Test
    fun successfully_create_quote() {

        val request = NorwegianHomeContentsQuoteRequestBuilder().build()

        every { strategyService.getAllGuidelines(any()) } returns setOf()
        every { priceEngineService.queryNorwegianHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(1, "NOK")
        )
        every { quoteRepository.insert(any(), any()) } returns Unit

        val result = cut.createQuote(request, UUID.randomUUID(), QuoteInitiatedFrom.ANDROID, null, true)
        assert(result is Either.Right)
    }

    @Test
    fun successfully_send_bbrId_to_price_engine_for_DanishHomeContentQuote() {

        val request = DanishHomeContentsQuoteRequestBuilder().build()

        val danishHomeContentData = slot<PriceQueryRequest.DanishHomeContent>()

        every { strategyService.getAllGuidelines(any()) } returns setOf()
        every { priceEngineService.queryDanishHomeContentPrice(capture(danishHomeContentData)) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(1, "DKK")
        )
        every { quoteRepository.insert(any(), any()) } returns Unit

        val result = cut.createQuote(request, UUID.randomUUID(), QuoteInitiatedFrom.ANDROID, null, false)
        assertThat(result).isInstanceOf(Either.Right.right().javaClass)

        verify(exactly = 1) {
            priceEngineService.queryDanishHomeContentPrice(
                capture(danishHomeContentData)
            )
        }
        assertThat(danishHomeContentData.captured.bbrId).isEqualTo("1234")
    }

    @Test
    fun fail_uw_guideline_returns_error_response_dto() {

        val request = NorwegianHomeContentsQuoteRequestBuilder().build()

        every { strategyService.getAllGuidelines(any()) } returns setOf(
            object : BaseGuideline<QuoteData> {
                val breachedGuideline: BreachedGuidelineCode
                    get() = "errorcode"

                override fun validate(data: QuoteData): BreachedGuidelineCode {
                    return breachedGuideline
                }
            }
        )

        every { priceEngineService.queryNorwegianHomeContentPrice(any()) } returns PriceQueryResponse(
            UUID.randomUUID(),
            Money.of(1, "NOK")
        )
        every { quoteRepository.insert(any(), any()) } returns Unit

        val result = cut.createQuote(request, UUID.randomUUID(), QuoteInitiatedFrom.ANDROID, null, true)
        require(result is Either.Left)
        assert(result.a.errorCode == ErrorCodes.MEMBER_BREACHES_UW_GUIDELINES)
        verify { metrics.increment(Market.NORWAY, "errorcode") }
    }
}
