package com.hedvig.underwriter.service

import arrow.core.Right
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.hedvig.underwriter.model.Name
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.QuoteRepositoryImpl
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsSsnAlreadySignedMemberResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.testhelp.JdbiRule
import com.hedvig.underwriter.testhelp.databuilder.a
import com.hedvig.underwriter.web.dtos.SignQuoteFromHopeRequest
import com.hedvig.underwriter.web.dtos.SignQuoteRequest
import com.hedvig.underwriter.web.dtos.SignRequest
import io.mockk.every
import io.mockk.mockk
import org.jdbi.v3.jackson2.Jackson2Config
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class CreateContractsFromQuotesSavesContractIdAndContractIdTest {

    @get:Rule
    val jdbiRule = JdbiRule.create()

    lateinit var quoteRepository: QuoteRepositoryImpl
    lateinit var productPricingService: ProductPricingService
    lateinit var memberService: MemberService

    @Before
    fun setUp() {
        jdbiRule.jdbi.getConfig(Jackson2Config::class.java).mapper =
            ObjectMapper().registerModule(KotlinModule())

        quoteRepository = QuoteRepositoryImpl(jdbiRule.jdbi)
        productPricingService = mockk()
        memberService = mockk()
    }

    @Test
    fun `memberSigned saves contractId`() {
        val quoteId = UUID.randomUUID()
        quoteRepository.insert(
            a.QuoteBuilder(
                quoteId,
                memberId = "1337",
                initiatedFrom = QuoteInitiatedFrom.IOS
            ).build()
        )

        val signServiceImpl = SignServiceImpl(
            mockk(),
            quoteRepository,
            mockk(),
            productPricingService,
            mockk(),
            mockk(),
            mockk()
        )

        val contractId = UUID.randomUUID()
        every { productPricingService.createContractsFromQuotes(any(), any(), any()) } returns listOf(
            CreateContractResponse(quoteId, UUID.randomUUID(), contractId)
        )

        signServiceImpl.memberSigned(
            "1337", SignRequest(
                "referenceToken", signature = "", oscpResponse = ""
            )
        )

        assertThat(quoteRepository.find(quoteId)!!.contractId).isEqualTo(contractId)
    }

    @Test
    fun `signQuoteSavesContractId`() {
        val quoteId = UUID.randomUUID()
        quoteRepository.insert(
            a.QuoteBuilder(
                quoteId,
                memberId = "1337",
                initiatedFrom = QuoteInitiatedFrom.IOS
            ).build()
        )

        val signServiceImpl = SignServiceImpl(
            mockk(),
            quoteRepository,
            memberService,
            productPricingService,
            mockk(),
            mockk(),
            mockk()
        )

        val contractId = UUID.randomUUID()
        val agreementId = UUID.randomUUID()
        every { productPricingService.createContractsFromQuotes(any(), any(), any()) } returns listOf(
            CreateContractResponse(quoteId, agreementId, contractId)
        )
        every { memberService.signQuote(any(), any()) } returns Right(UnderwriterQuoteSignResponse(1L, true))

        signServiceImpl.signQuote(
            quoteId,
            SignQuoteRequest(
                Name("Mr Test", "Tester"), LocalDate.of(2020, 1, 1), "a@email.com"
            )
        )

        assertThat(quoteRepository.find(quoteId)!!).all {
            transform { it.contractId }.isEqualTo(contractId)
            transform { it.agreementId }.isEqualTo(agreementId)
        }
    }

    @Test
    fun `signQuoteFromHome savesContractid`() {
        val quoteId = UUID.randomUUID()
        quoteRepository.insert(
            a.QuoteBuilder(
                quoteId,
                memberId = "1337",
                initiatedFrom = QuoteInitiatedFrom.IOS
            ).build()
        )

        val signServiceImpl = SignServiceImpl(
            mockk(),
            quoteRepository,
            memberService,
            productPricingService,
            mockk(),
            mockk(),
            mockk()
        )

        val contractId = UUID.randomUUID()
        val agreementId = UUID.randomUUID()
        every { productPricingService.createContractsFromQuotes(any(), any(), any()) } returns listOf(
            CreateContractResponse(quoteId, agreementId, contractId)
        )
        every { productPricingService.memberHasContract(any()) } returns true

        signServiceImpl.signQuoteFromHope(
            quoteId,
            SignQuoteFromHopeRequest(null, "aToken")
        )

        assertThat(quoteRepository.find(quoteId)!!).all {
            transform { it.contractId }.isEqualTo(contractId)
            transform { it.agreementId }.isEqualTo(agreementId)
        }
    }
}
