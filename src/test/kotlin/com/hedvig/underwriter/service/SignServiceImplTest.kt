package com.hedvig.underwriter.service

import arrow.core.Right
import com.hedvig.underwriter.model.Name
import com.hedvig.underwriter.model.Partner
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteRepository
import com.hedvig.underwriter.model.SignSessionRepository
import com.hedvig.underwriter.model.ssn
import com.hedvig.underwriter.service.SignServiceImpl.Companion.MEMBER_HAS_ALREADY_SIGNED_ERROR_MESSAGE
import com.hedvig.underwriter.service.SignServiceImpl.Companion.SIGNING_QUOTE_WITH_OUT_MEMBER_ID_ERROR_MESSAGE
import com.hedvig.underwriter.service.SignServiceImpl.Companion.TARGET_URL_NOT_PROVIDED_ERROR_MESSAGE
import com.hedvig.underwriter.service.SignServiceImpl.Companion.VARIOUS_MEMBER_ID_ERROR_MESSAGE
import com.hedvig.underwriter.service.model.StartSignResponse
import com.hedvig.underwriter.serviceIntegration.customerio.CustomerIO
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsMemberAlreadySignedResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsSsnAlreadySignedMemberResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.StartNorwegianBankIdSignResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.StartSwedishBankIdSignResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.testhelp.databuilder.a
import com.hedvig.underwriter.web.dtos.ErrorCodes
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
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
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity

@RunWith(MockitoJUnitRunner::class)
class SignServiceImplTest {

    @MockK
    lateinit var quoteService: QuoteService

    @MockK
    lateinit var underwriter: Underwriter

    @MockK
    lateinit var memberService: MemberService

    @MockK
    lateinit var productPricingService: ProductPricingService

    @MockK
    lateinit var quoteRepository: QuoteRepository

    @MockK
    lateinit var signSessionRepository: SignSessionRepository

    @MockK
    lateinit var customerIO: CustomerIO

    @MockK
    lateinit var env: Environment

    lateinit var cut: SignService

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        cut = SignServiceImpl(
            quoteService,
            quoteRepository,
            memberService,
            productPricingService,
            signSessionRepository,
            customerIO,
            env
        )
    }

    @Test
    fun givenPartnerSendsPartnerIdToCustomerIO() {
        val quoteId = UUID.randomUUID()
        val quote = a.QuoteBuilder(id = quoteId, attributedTo = Partner.COMPRICER).build()

        every { quoteRepository.find(any()) } returns quote
        every { quoteRepository.update(any(), any()) } returnsArgument 0
        every { quoteService.getQuoteStateNotSignableErrorOrNull(any()) } returns null

        every { memberService.createMember() } returns "1234"
        every {
            productPricingService.createContractsFromQuotes(
                any(),
                any()
            )
        } returns listOf(CreateContractResponse(agreementId = UUID.randomUUID(), quoteId = quoteId, contractId = UUID.randomUUID()))
        every { productPricingService.redeemCampaign(any()) } returns ResponseEntity.ok().build()
        every { memberService.signQuote(any(), any()) } returns Right(UnderwriterQuoteSignResponse(1234, true))
        every { memberService.isSsnAlreadySignedMemberEntity(any()) } returns IsSsnAlreadySignedMemberResponse(false)
        every { env.activeProfiles } returns arrayOf<String>()

        cut.signQuote(quoteId, SignQuoteRequest(Name("", ""), LocalDate.now(), "null"))
        verify { customerIO.postSignUpdate(ofType(Quote::class)) }
    }

    @Test
    fun givenPartnerIsHedvigSendPartnerIdToCustomerIO() {
        val quoteId = UUID.randomUUID()
        val quote = a.QuoteBuilder(attributedTo = Partner.HEDVIG).build()

        every { quoteRepository.find(any()) } returns quote
        every { quoteRepository.update(any(), any()) } returnsArgument 0
        every { quoteService.getQuoteStateNotSignableErrorOrNull(any()) } returns null

        every { memberService.createMember() } returns "1234"
        every {
            productPricingService.createContractsFromQuotes(
                any(),
                any()
            )
        } returns listOf(CreateContractResponse(agreementId = UUID.randomUUID(), quoteId = quoteId, contractId = UUID.randomUUID()))
        every { memberService.signQuote(any(), any()) } returns Right(UnderwriterQuoteSignResponse(1234, true))
        every { memberService.isSsnAlreadySignedMemberEntity(any()) } returns IsSsnAlreadySignedMemberResponse(false)
        every { env.activeProfiles } returns arrayOf<String>()

        cut.signQuote(quoteId, SignQuoteRequest(Name("", ""), LocalDate.now(), "null"))
        verify { customerIO.postSignUpdate(any()) }
    }

    @Test
    fun startSigningOfSwedishQuote_startSwedishSign() {
        val quoteIds = listOf(UUID.randomUUID())
        val quote = a.QuoteBuilder(id = quoteIds[0], memberId = memberId).build()
        val signSessionReference = UUID.randomUUID()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote)
        every { signSessionRepository.insert(quoteIds) } returns signSessionReference
        every { quoteService.getQuoteStateNotSignableErrorOrNull(any()) } returns null
        every { memberService.startSwedishBankIdSignQuotes(quote.memberId!!.toLong(), signSessionReference, quote.ssn, ipAddress, false) } returns StartSwedishBankIdSignResponse(
            "autoStartToken"
        )

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        verify(exactly = 1) { signSessionRepository.insert(any()) }
        assertThat(result).isInstanceOf(StartSignResponse.SwedishBankIdSession::class.java)
    }

    @Test
    fun startSigningOfSwedishQuotes_returnsFailResponse() {
        val quoteIds = listOf(UUID.randomUUID())
        val quote = a.QuoteBuilder(id = quoteIds[0], memberId = memberId).build()
        val signSessionReference = UUID.randomUUID()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote)
        every { quoteService.getQuoteStateNotSignableErrorOrNull(any()) } returns null
        every { signSessionRepository.insert(quoteIds) } returns signSessionReference
        every { memberService.startSwedishBankIdSignQuotes(memberId.toLong(), signSessionReference, quote.ssn, ipAddress, false) } returns StartSwedishBankIdSignResponse(
            autoStartToken = null,
            internalErrorMessage = "Failed"
        )

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
        assertThat((result as StartSignResponse.FailedToStartSign).errorMessage).isEqualTo("Failed")
    }

    @Test
    fun startSigningOfSwedishQuotes_getQuoteStateNotSignableErrorOrNullReturnsError_returnsFailResponse() {
        val quoteIds = listOf(UUID.randomUUID())
        val quote = a.QuoteBuilder(id = quoteIds[0], memberId = memberId).build()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote)
        every { quoteService.getQuoteStateNotSignableErrorOrNull(any()) } returns ErrorResponseDto(ErrorCodes.MEMBER_QUOTE_HAS_EXPIRED, "")

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
    }

    @Test
    fun startSigningOfNorwegianQuote_startNorwegianSign() {
        val quoteIds = listOf(UUID.randomUUID())
        val quote = a.QuoteBuilder(id = quoteIds[0], data = a.NorwegianHomeContentDataBuilder(), memberId = memberId).build()
        val signSessionReference = UUID.randomUUID()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote)
        every { signSessionRepository.insert(quoteIds) } returns signSessionReference
        every { quoteService.getQuoteStateNotSignableErrorOrNull(any()) } returns null
        every { memberService.startNorwegianBankIdSignQuotes(quote.memberId!!.toLong(), signSessionReference, quote.ssn, successUrl, failUrl) } returns StartNorwegianBankIdSignResponse(
            "redirect url"
        )

        val result = cut.startSigningQuotes(quoteIds, memberId, null, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.NorwegianBankIdSession::class.java)
    }

    @Test
    fun startSigningOfNorwegianQuotes_startNorwegianSign() {
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val quote = a.QuoteBuilder(id = quoteIds[0], data = a.NorwegianHomeContentDataBuilder(), memberId = memberId).build()
        val quote2 = a.QuoteBuilder(id = quoteIds[1], data = a.NorwegianTravelDataBuilder(), memberId = memberId).build()
        val signSessionReference = UUID.randomUUID()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote, quote2)
        every { signSessionRepository.insert(quoteIds) } returns signSessionReference
        every { quoteService.getQuoteStateNotSignableErrorOrNull(any()) } returns null
        every { memberService.startNorwegianBankIdSignQuotes(quote.memberId!!.toLong(), signSessionReference, quote.ssn, successUrl, failUrl) } returns StartNorwegianBankIdSignResponse(
            "redirect url"
        )

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.NorwegianBankIdSession::class.java)
    }

    @Test
    fun startSigningOfSwedishAndNorwegianQuotes_returnsFailResponse() {
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val quote = a.QuoteBuilder(id = quoteIds[0], data = a.NorwegianHomeContentDataBuilder(), memberId = memberId).build()
        val quote2 = a.QuoteBuilder(id = quoteIds[1], data = a.SwedishHouseDataBuilder(), memberId = memberId).build()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote, quote2)
        every { quoteService.getQuoteStateNotSignableErrorOrNull(any()) } returns null

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        verify(exactly = 0) { signSessionRepository.insert(any()) }
        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
    }

    @Test
    fun startSigningZeroQuotes_returnsFailResponse() {
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf()

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        verify(exactly = 0) { signSessionRepository.insert(any()) }
        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
    }

    @Test
    fun startSigningThreeQuotes_returnsFailResponse() {
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val quote1 = a.QuoteBuilder(id = quoteIds[0], data = a.NorwegianHomeContentDataBuilder(), memberId = memberId).build()
        val quote2 = a.QuoteBuilder(id = quoteIds[1], data = a.NorwegianHomeContentDataBuilder(), memberId = memberId).build()
        val quote3 = a.QuoteBuilder(id = quoteIds[1], data = a.NorwegianTravelDataBuilder(), memberId = memberId).build()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote1, quote2, quote3)
        every { quoteService.getQuoteStateNotSignableErrorOrNull(any()) } returns null

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        verify(exactly = 0) { signSessionRepository.insert(any()) }
        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
    }

    @Test
    fun failStartSignQuotesWithNoMemberId() {
        val quoteIds = listOf(UUID.randomUUID())
        val quote1 = a.QuoteBuilder(id = quoteIds[0], data = a.NorwegianHomeContentDataBuilder(), memberId = null).build()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote1)

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
        assertThat((result as StartSignResponse.FailedToStartSign).errorMessage).isEqualTo(SIGNING_QUOTE_WITH_OUT_MEMBER_ID_ERROR_MESSAGE)
    }

    @Test
    fun failStartSignQuotesWithDifferentMemberIdFromHedvigToken() {
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val quote1 = a.QuoteBuilder(id = quoteIds[0], data = a.NorwegianHomeContentDataBuilder(), memberId = memberId).build()
        val quote2 = a.QuoteBuilder(id = quoteIds[1], data = a.NorwegianHomeContentDataBuilder(), memberId = memberId).build()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote1, quote2)

        val result = cut.startSigningQuotes(quoteIds, "1234", ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
        assertThat((result as StartSignResponse.FailedToStartSign).errorMessage).isEqualTo(VARIOUS_MEMBER_ID_ERROR_MESSAGE)
    }

    @Test
    fun failStartSignNorwegianQuotesNoTargetUrls() {
        val quoteIds = listOf(UUID.randomUUID())
        val quote = a.QuoteBuilder(id = quoteIds[0], data = a.NorwegianHomeContentDataBuilder(), memberId = memberId).build()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote)
        every { quoteService.getQuoteStateNotSignableErrorOrNull(any()) } returns null

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, null, null)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
        assertThat((result as StartSignResponse.FailedToStartSign).errorMessage).isEqualTo(TARGET_URL_NOT_PROVIDED_ERROR_MESSAGE)
    }

    @Test
    fun failStartSignIfMemberAlreadySigned() {
        every { memberService.isMemberIdAlreadySignedMemberEntity(memberId.toLong()) } returns IsMemberAlreadySignedResponse(true)

        val result = cut.startSigningQuotes(listOf(UUID.randomUUID()), memberId, ipAddress, null, null)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
        assertThat((result as StartSignResponse.FailedToStartSign).errorMessage).isEqualTo(
            MEMBER_HAS_ALREADY_SIGNED_ERROR_MESSAGE
        )
    }

    companion object {
        private val memberId = "1337"
        private val ipAddress = "127.0.0.1"
        private val successUrl = "http://hedvig.com"
        private val failUrl = "http://hedvig.com"
    }
}
