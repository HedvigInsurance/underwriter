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
import com.hedvig.underwriter.testhelp.databuilder.quote
import com.hedvig.underwriter.web.dtos.SignQuoteFromHopeRequest
import com.hedvig.underwriter.web.dtos.SignQuoteRequestDto
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

    private lateinit var quoteRepository: QuoteRepositoryImpl
    private lateinit var productPricingService: ProductPricingService
    private lateinit var memberService: MemberService

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
            quote {
                id = quoteId
                memberId = "1337"
                initiatedFrom = QuoteInitiatedFrom.IOS
            }
        )

        val signServiceImpl = SignServiceImpl(
            mockk(),
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
    fun signQuoteSavesContractId() {
        val quoteId = UUID.randomUUID()
        quoteRepository.insert(
            quote {
                id = quoteId
                memberId = "1337"
                initiatedFrom = QuoteInitiatedFrom.IOS
            }
        )

        val signServiceImpl = SignServiceImpl(
            mockk(),
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
        every { productPricingService.hasContract(any()) } returns false
        every { memberService.signQuote(any(), any()) } returns Right(UnderwriterQuoteSignResponse(1L, true))

        signServiceImpl.signQuoteFromRapio(
            quoteId,
            SignQuoteRequestDto(
                Name("Mr Test", "Tester"), null, LocalDate.of(2020, 1, 1), null, "a@email.com", null
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
            quote {
                id = quoteId
                memberId = "1337"
                initiatedFrom = QuoteInitiatedFrom.IOS
            }
        )

        val signServiceImpl = SignServiceImpl(
            mockk(),
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
        every { memberService.isSsnAlreadySignedMemberEntity(any()) } returns IsSsnAlreadySignedMemberResponse(true)

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
