package com.hedvig.underwriter.service

import arrow.core.Either
import arrow.core.Right
import arrow.core.getOrElse
import com.hedvig.underwriter.model.Name
import com.hedvig.underwriter.model.Partner
import com.hedvig.underwriter.model.QuoteRepository
import com.hedvig.underwriter.model.QuoteState
import com.hedvig.underwriter.serviceIntegration.customerio.CustomerIO
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsSsnAlreadySignedMemberResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.ModifiedProductCreatedDto
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.RapioProductCreatedResponseDto
import com.hedvig.underwriter.testhelp.databuilder.a
import com.hedvig.underwriter.web.dtos.SignQuoteRequest
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.http.ResponseEntity

@RunWith(MockitoJUnitRunner::class)
class QuoteServiceImplTest {

    @MockK
    lateinit var debtChecker: DebtChecker

    @MockK
    lateinit var memberService: MemberService

    @MockK
    lateinit var productPricingService: ProductPricingService

    @MockK
    lateinit var quoteRepository: QuoteRepository

    @MockK
    lateinit var customerIO: CustomerIO

    @Before
    fun setUp() = MockKAnnotations.init(this, relaxUnitFun = true)

    @Test
    fun givenPartnerSendsPartnerIdToCustomerIO() {

        val cut = QuoteServiceImpl(debtChecker, memberService, productPricingService, quoteRepository, customerIO)

        val quoteId = UUID.randomUUID()
        val quote = a.QuoteBuilder(id = quoteId, attributedTo = Partner.COMPRICER).build()

        every { quoteRepository.find(any()) } returns quote
        every { quoteRepository.update(any(), any()) } returnsArgument 0
        every { memberService.createMember() } returns "1234"
        every {
            productPricingService.createProduct(
                any(),
                any()
            )
        } returns RapioProductCreatedResponseDto(UUID.randomUUID())
        every { productPricingService.redeemCampaign(any()) } returns ResponseEntity.ok().build()
        every { memberService.signQuote(any(), any()) } returns Right(UnderwriterQuoteSignResponse(1234, true))
        every { memberService.isSsnAlreadySignedMemberEntity(any()) } returns IsSsnAlreadySignedMemberResponse(false)
        cut.signQuote(quoteId, SignQuoteRequest(Name("", ""), LocalDate.now(), "null"))
        verify { customerIO.setPartnerCode("1234", Partner.COMPRICER) }
    }

    @Test
    fun givenPartnerIsHedvigDoNotSendPartnerIdToCustomerIO() {
        val cut = QuoteServiceImpl(debtChecker, memberService, productPricingService, quoteRepository, customerIO)

        val quoteId = UUID.randomUUID()
        val quote = a.QuoteBuilder(attributedTo = Partner.HEDVIG).build()

        every { quoteRepository.find(any()) } returns quote
        every { quoteRepository.update(any(), any()) } returnsArgument 0
        every { memberService.createMember() } returns "1234"
        every {
            productPricingService.createProduct(
                any(),
                any()
            )
        } returns RapioProductCreatedResponseDto(UUID.randomUUID())
        every { memberService.signQuote(any(), any()) } returns Right(UnderwriterQuoteSignResponse(1234, true))
        every { memberService.isSsnAlreadySignedMemberEntity(any()) } returns IsSsnAlreadySignedMemberResponse(false)
        cut.signQuote(quoteId, SignQuoteRequest(Name("", ""), LocalDate.now(), "null"))
        verify(exactly = 0) { customerIO.setPartnerCode(any(), any()) }
    }

    @Test
    fun activatesQuoteByCreatingModifiedProduct() {
        val service = QuoteServiceImpl(
            debtChecker = debtChecker,
            memberService = memberService,
            productPricingService = productPricingService,
            quoteRepository = quoteRepository,
            customerIOClient = customerIO
        )

        val quote = a.QuoteBuilder(originatingProductId = UUID.randomUUID(), memberId = "12345").build()

        val createdProductResponse = ModifiedProductCreatedDto(id = UUID.randomUUID())

        every { quoteRepository.find(quote.id) } returns quote
        val signedQuote = quote.copy(
            signedProductId = createdProductResponse.id,
            state = QuoteState.SIGNED
        )
        every {
            quoteRepository.update(
                match { passedQuote -> passedQuote.id == quote.id },
                any()
            )
        } returns signedQuote
        every { productPricingService.createModifiedProductFromQuote(any()) } returns createdProductResponse

        val result = service.activateQuote(
            completeQuoteId = quote.id,
            activationDate = LocalDate.now().plusDays(10)
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat(result.getOrElse { null }).isEqualTo(signedQuote)
        verify(exactly = 1) { productPricingService.createModifiedProductFromQuote(any()) } // FIXME better assertion?
    }
}
