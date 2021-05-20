package com.hedvig.underwriter.service

import arrow.core.Right
import arrow.core.getOrElse
import com.hedvig.underwriter.model.Name
import com.hedvig.underwriter.model.Partner
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.model.QuoteRepository
import com.hedvig.underwriter.model.QuoteState
import com.hedvig.underwriter.model.SignSessionRepository
import com.hedvig.underwriter.model.email
import com.hedvig.underwriter.model.ssn
import com.hedvig.underwriter.service.model.StartSignErrors
import com.hedvig.underwriter.service.model.StartSignResponse
import com.hedvig.underwriter.service.quotesSignDataStrategies.SignStrategyService
import com.hedvig.underwriter.service.quotesSignDataStrategies.SimpleSignStrategy
import com.hedvig.underwriter.service.quotesSignDataStrategies.SwedishBankIdSignStrategy
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsMemberAlreadySignedResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.IsSsnAlreadySignedMemberResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.NationalIdentification
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.Nationality
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterQuoteSignResponse
import com.hedvig.underwriter.serviceIntegration.memberService.dtos.UnderwriterStartSignSessionResponse
import com.hedvig.underwriter.serviceIntegration.notificationService.NotificationService
import com.hedvig.underwriter.serviceIntegration.productPricing.ProductPricingService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.contract.CreateContractResponse
import com.hedvig.underwriter.testhelp.databuilder.DanishAccidentDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.DanishHomeContentsDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.DanishTravelDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.NorwegianHomeContentDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.NorwegianTravelDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.SwedishApartmentDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.SwedishHouseDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.quote
import com.hedvig.underwriter.web.dtos.ErrorCodes
import com.hedvig.underwriter.web.dtos.SignQuoteFromHopeRequest
import com.hedvig.underwriter.web.dtos.SignQuoteRequestDto
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.util.UUID

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
    lateinit var notificationService: NotificationService

    private lateinit var signStrategyService: SignStrategyService

    private lateinit var swedishBankIdSignStrategy: SwedishBankIdSignStrategy
    private lateinit var simpleSignStrategy: SimpleSignStrategy

    private lateinit var cut: SignService

    private val ipAddress = "127.0.0.1"
    private val successUrl = "http://hedvig.com"
    private val failUrl = "http://hedvig.com"

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        swedishBankIdSignStrategy = SwedishBankIdSignStrategy(
            signSessionRepository, memberService
        )
        simpleSignStrategy = SimpleSignStrategy(
            signSessionRepository, memberService
        )

        signStrategyService = SignStrategyService(
            swedishBankIdSignStrategy, simpleSignStrategy
        )

        cut = SignServiceImpl(
            quoteService,
            mockk(),
            quoteRepository,
            memberService,
            productPricingService,
            signSessionRepository,
            signStrategyService,
            notificationService
        )
    }

    @Test
    fun givenPartnerSendsPartnerIdToCustomerIO() {
        val quoteId = UUID.randomUUID()
        val quote = quote {
            id = quoteId
            attributedTo = Partner.COMPRICER
        }

        every { quoteRepository.findQuotes(any()) } returns listOf(quote)
        every { quoteRepository.update(any(), any()) } returnsArgument 0

        every { memberService.createMember() } returns "1234"
        every {
            productPricingService.createContractsFromQuotes(
                any(),
                any(),
                any()
            )
        } returns listOf(
            CreateContractResponse(
                agreementId = UUID.randomUUID(),
                quoteId = quoteId,
                contractId = UUID.randomUUID()
            )
        )
        every { productPricingService.redeemCampaign(any()) } returns ResponseEntity.ok().build()
        every { memberService.signQuote(any(), any()) } returns Right(UnderwriterQuoteSignResponse(1234, true))
        every { memberService.isSsnAlreadySignedMemberEntity(any()) } returns IsSsnAlreadySignedMemberResponse(false)

        cut.signQuoteFromRapio(quoteId, SignQuoteRequestDto(Name("", ""), null, LocalDate.now(), null, "null"))
        verify { notificationService.postSignUpdate(ofType(Quote::class)) }
    }

    @Test
    fun givenPartnerIsHedvigSendPartnerIdToCustomerIO() {
        val quoteId = UUID.randomUUID()
        val quote = quote { attributedTo = Partner.HEDVIG }

        every { quoteRepository.findQuotes(any()) } returns listOf(quote)
        every { quoteRepository.update(any(), any()) } returnsArgument 0

        every { memberService.createMember() } returns "1234"
        every {
            productPricingService.createContractsFromQuotes(
                any(),
                any(),
                any()
            )
        } returns listOf(
            CreateContractResponse(
                agreementId = UUID.randomUUID(),
                quoteId = quoteId,
                contractId = UUID.randomUUID()
            )
        )
        every { memberService.signQuote(any(), any()) } returns Right(UnderwriterQuoteSignResponse(1234, true))
        every { memberService.isSsnAlreadySignedMemberEntity(any()) } returns IsSsnAlreadySignedMemberResponse(false)

        cut.signQuoteFromRapio(quoteId, SignQuoteRequestDto(Name("", ""), null, LocalDate.now(), "if", "null"))
        verify { notificationService.postSignUpdate(any()) }
    }

    @Test
    fun startSigningOfSwedishQuote_startSwedishSign() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID())
        val quote = quote {
            id = quoteIds[0]
            this.memberId = memberId
        }
        val signSessionReference = UUID.randomUUID()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote)
        every { signSessionRepository.insert(quoteIds) } returns signSessionReference

        every {
            memberService.startSwedishBankIdSign(
                quote.memberId!!.toLong(),
                signSessionReference,
                NationalIdentification(
                    quote.ssn,
                    Nationality.SWEDEN
                ),
                ipAddress,
                false
            )
        } returns UnderwriterStartSignSessionResponse.SwedishBankId(
            "autoStartToken"
        )

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        verify(exactly = 1) { signSessionRepository.insert(any()) }
        assertThat(result).isInstanceOf(StartSignResponse.SwedishBankIdSession::class.java)

        verify {
            memberService.finalizeOnboarding(quote, quote.email!!)
        }
    }

    @Test
    fun startSigningOfSwedishQuotes_returnsFailResponse() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID())
        val quote = quote {
            id = quoteIds[0]
            this.memberId = memberId
        }
        val signSessionReference = UUID.randomUUID()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote)
        every { signSessionRepository.insert(quoteIds) } returns signSessionReference
        every {
            memberService.startSwedishBankIdSign(
                quote.memberId!!.toLong(),
                signSessionReference,
                NationalIdentification(
                    quote.ssn,
                    Nationality.SWEDEN
                ),
                ipAddress,
                false
            )
        } returns UnderwriterStartSignSessionResponse.SwedishBankId(
            autoStartToken = null,
            internalErrorMessage = "Failed"
        )

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
        assertThat((result as StartSignResponse.FailedToStartSign).errorMessage).isEqualTo("Failed")
        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun startSigningOfSwedishQuotes_getQuoteStateNotSignableErrorOrNullReturnsError_returnsFailResponse() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID())
        val quote = quote {
            id = quoteIds[0]
            this.memberId = memberId
            state = QuoteState.EXPIRED
        }

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote)

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun startSigningOfNorwegianQuote_startNorwegianSign() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID())
        val quote =
            quote {
                id = quoteIds[0]
                data = NorwegianHomeContentDataBuilder()
                this.memberId = memberId
            }
        val signSessionReference = UUID.randomUUID()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote)
        every { signSessionRepository.insert(quoteIds) } returns signSessionReference
        every {
            memberService.startSimpleSign(
                quote.memberId!!.toLong(),
                signSessionReference,
                NationalIdentification(
                    quote.ssn,
                    Nationality.NORWAY
                )
            )
        } returns UnderwriterStartSignSessionResponse.SimpleSign(
            true
        )

        val result = cut.startSigningQuotes(quoteIds, memberId, null, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.SimpleSignSession::class.java)

        verify {
            memberService.finalizeOnboarding(quote, quote.email!!)
        }
    }

    @Test
    fun startSigningOfNorwegianQuotes_startNorwegianSign() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val quote =
            quote {
                id = quoteIds[0]
                data = NorwegianHomeContentDataBuilder()
                this.memberId = memberId
            }
        val quote2 =
            quote {
                id = quoteIds[1]
                data = NorwegianTravelDataBuilder()
                this.memberId = memberId
            }
        val signSessionReference = UUID.randomUUID()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote, quote2)
        every { signSessionRepository.insert(quoteIds) } returns signSessionReference
        every {
            memberService.startSimpleSign(
                quote.memberId!!.toLong(),
                signSessionReference,
                NationalIdentification(
                    quote.ssn,
                    Nationality.NORWAY
                )
            )
        } returns UnderwriterStartSignSessionResponse.SimpleSign(true)

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.SimpleSignSession::class.java)

        verify {
            memberService.finalizeOnboarding(quote, quote.email!!)
        }
    }

    @Test
    fun startSigningOfSwedishAndNorwegianQuotes_returnsFailResponse() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val quote =
            quote {
                id = quoteIds[0]
                data = NorwegianHomeContentDataBuilder()
                this.memberId = memberId
            }
        val quote2 = quote {
            id = quoteIds[1]
            data = SwedishHouseDataBuilder()
            this.memberId = memberId
        }

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote, quote2)

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        verify(exactly = 0) { signSessionRepository.insert(any()) }
        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun startSigningOfOnlyDanishAccidentQuote_returnsFailResponse() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID())
        val quote =
            quote {
                id = quoteIds[0]
                data = DanishAccidentDataBuilder()
                this.memberId = memberId
            }

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote)

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        verify(exactly = 0) { signSessionRepository.insert(any()) }
        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun startSigningOfOnlyDanishTravelQuote_returnsFailResponse() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID())
        val quote =
            quote {
                id = quoteIds[0]
                data = DanishTravelDataBuilder()
                this.memberId = memberId
            }

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote)

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        verify(exactly = 0) { signSessionRepository.insert(any()) }
        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun startSigningOfDanishAccidentAndDanishTravelQuotes_returnsFailResponse() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val quote =
            quote {
                id = quoteIds[0]
                data = DanishAccidentDataBuilder()
                this.memberId = memberId
            }
        val quote2 = quote {
            id = quoteIds[1]
            data = DanishTravelDataBuilder()
            this.memberId = memberId
        }

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote, quote2)

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        verify(exactly = 0) { signSessionRepository.insert(any()) }
        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun startSigningOfDanishHomeContentAndDanishAccidentQuotes_returnsDanishBankIdSession() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val quote =
            quote {
                id = quoteIds[0]
                data = DanishHomeContentsDataBuilder()
                this.memberId = memberId
            }
        val quote2 = quote {
            id = quoteIds[1]
            data = DanishAccidentDataBuilder()
            this.memberId = memberId
        }
        val signSessionReference = UUID.randomUUID()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote, quote2)
        every { signSessionRepository.insert(quoteIds) } returns signSessionReference
        every {
            memberService.startSimpleSign(
                quote.memberId!!.toLong(),
                signSessionReference,
                NationalIdentification(
                    quote.ssn,
                    Nationality.DENMARK
                )
            )
        } returns UnderwriterStartSignSessionResponse.SimpleSign(
            successfullyStarted = true
        )

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.SimpleSignSession::class.java)

        verify {
            memberService.finalizeOnboarding(quote, quote.email!!)
        }
    }

    @Test
    fun startSigningOfAllDanishTypeOfQuotes_returnsDanishBankIdSession() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val quote =
            quote {
                id = quoteIds[0]
                data = DanishHomeContentsDataBuilder()
                this.memberId = memberId
            }
        val quote2 = quote {
            id = quoteIds[1]
            data = DanishAccidentDataBuilder()
            this.memberId = memberId
        }
        val quote3 = quote {
            id = quoteIds[2]
            data = DanishTravelDataBuilder()
            this.memberId = memberId
        }
        val signSessionReference = UUID.randomUUID()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote, quote2, quote3)
        every { signSessionRepository.insert(quoteIds) } returns signSessionReference
        every {
            memberService.startSimpleSign(
                quote.memberId!!.toLong(),
                signSessionReference,
                NationalIdentification(
                    quote.ssn,
                    Nationality.DENMARK
                )
            )
        } returns UnderwriterStartSignSessionResponse.SimpleSign(
            successfullyStarted = true
        )

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.SimpleSignSession::class.java)

        verify {
            memberService.finalizeOnboarding(quote, quote.email!!)
        }
    }

    @Test
    fun startSigningOfThreeDanishHomeContentQuotes_returnsFailedToStartSign() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val quote =
            quote {
                id = quoteIds[0]
                data = DanishHomeContentsDataBuilder()
                this.memberId = memberId
            }
        val quote2 =
            quote {
                id = quoteIds[1]
                data = DanishHomeContentsDataBuilder()
                this.memberId = memberId
            }
        val quote3 =
            quote {
                id = quoteIds[2]
                data = DanishHomeContentsDataBuilder()
                this.memberId = memberId
            }

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote, quote2, quote3)
        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        verify(exactly = 0) { signSessionRepository.insert(any()) }
        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun startSigningZeroQuotes_returnsFailResponse() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf()

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        verify(exactly = 0) { signSessionRepository.insert(any()) }
        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun startSigningThreeQuotes_returnsFailResponse() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val quote1 =
            quote {
                id = quoteIds[0]
                data = NorwegianHomeContentDataBuilder()
                this.memberId = memberId
            }
        val quote2 =
            quote {
                id = quoteIds[1]
                data = NorwegianHomeContentDataBuilder()
                this.memberId = memberId
            }
        val quote3 =
            quote {
                id = quoteIds[1]
                data = NorwegianTravelDataBuilder()
                this.memberId = memberId
            }

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote1, quote2, quote3)
        cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        verify(exactly = 0) { signSessionRepository.insert(any()) }

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun failStartSignQuotesWithNoMemberId() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID())
        val quote1 = quote {
            id = quoteIds[0]
            data = NorwegianHomeContentDataBuilder()
            this.memberId = null
        }

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote1)

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
        assertThat((result as StartSignResponse.FailedToStartSign).errorMessage).isEqualTo(
            StartSignErrors.noMemberIdOnQuote.errorMessage
        )
        assertThat(result.errorCode).isEqualTo(
            StartSignErrors.noMemberIdOnQuote.errorCode
        )

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun failStartSignQuotesWithDifferentMemberIdFromHedvigToken() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val quote1 =
            quote {
                id = quoteIds[0]
                data = NorwegianHomeContentDataBuilder()
                this.memberId = memberId
            }
        val quote2 =
            quote {
                id = quoteIds[1]
                data = NorwegianHomeContentDataBuilder()
                this.memberId = memberId
            }

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote1, quote2)

        val result = cut.startSigningQuotes(quoteIds, "1234", ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
        assertThat((result as StartSignResponse.FailedToStartSign).errorMessage).isEqualTo(
            StartSignErrors.variousMemberId.errorMessage
        )
        assertThat(result.errorCode).isEqualTo(
            StartSignErrors.variousMemberId.errorCode
        )

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun failStartSignQuotesWithDifferentFirstNameFromHedvigToken() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val quote1 =
            quote {
                id = quoteIds[0]
                data = NorwegianHomeContentDataBuilder().copy(firstName = "Tolvan")
                this.memberId = memberId
            }
        val quote2 =
            quote {
                id = quoteIds[1]
                data = NorwegianTravelDataBuilder().copy(firstName = "Tolvansbror")
                this.memberId = memberId
            }

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote1, quote2)

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
        assertThat((result as StartSignResponse.FailedToStartSign).errorMessage).isEqualTo(
            StartSignErrors.personalInfoNotMatching.errorMessage
        )
        assertThat(result.errorCode).isEqualTo(
            StartSignErrors.personalInfoNotMatching.errorCode
        )

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun failStartSignQuotesWithDifferentLastNameFromHedvigToken() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val quote1 =
            quote {
                id = quoteIds[0]
                data = NorwegianHomeContentDataBuilder().copy(lastName = "Tolvansson")
                this.memberId = memberId
            }
        val quote2 =
            quote {
                id = quoteIds[1]
                data = NorwegianTravelDataBuilder().copy(lastName = "Tolvansbrorsson")
                this.memberId = memberId
            }

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote1, quote2)

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
        assertThat((result as StartSignResponse.FailedToStartSign).errorMessage).isEqualTo(
            StartSignErrors.personalInfoNotMatching.errorMessage
        )
        assertThat(result.errorCode).isEqualTo(
            StartSignErrors.personalInfoNotMatching.errorCode
        )

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun failStartSignQuotesWithDifferentSsnFromHedvigToken() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val quote1 =
            quote {
                id = quoteIds[0]
                data = NorwegianHomeContentDataBuilder().copy(ssn = "123456789")
                this.memberId = memberId
            }
        val quote2 =
            quote {
                id = quoteIds[1]
                data = NorwegianTravelDataBuilder().copy(ssn = "987654321")
                this.memberId = memberId
            }

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote1, quote2)

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
        assertThat((result as StartSignResponse.FailedToStartSign).errorMessage).isEqualTo(
            StartSignErrors.personalInfoNotMatching.errorMessage
        )
        assertThat(result.errorCode).isEqualTo(
            StartSignErrors.personalInfoNotMatching.errorCode
        )

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun failStartSignQuotesWithDifferentEmailFromHedvigToken() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val quote1 =
            quote {
                id = quoteIds[0]
                data = NorwegianHomeContentDataBuilder().copy(email = "test@hedvig.com")
                this.memberId = memberId
            }
        val quote2 =
            quote {
                id = quoteIds[1]
                data = NorwegianTravelDataBuilder().copy(email = "com@hedvig.test")
                this.memberId = memberId
            }

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote1, quote2)

        val result = cut.startSigningQuotes(quoteIds, memberId, ipAddress, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
        assertThat((result as StartSignResponse.FailedToStartSign).errorMessage).isEqualTo(
            StartSignErrors.personalInfoNotMatching.errorMessage
        )
        assertThat(result.errorCode).isEqualTo(
            StartSignErrors.personalInfoNotMatching.errorCode
        )

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun failStartSignIfMemberAlreadySigned() {
        val memberId = "1337"
        every { memberService.isMemberIdAlreadySignedMemberEntity(memberId.toLong()) } returns IsMemberAlreadySignedResponse(
            true
        )

        val result = cut.startSigningQuotes(listOf(UUID.randomUUID()), memberId, ipAddress, null, null)

        assertThat(result).isInstanceOf(StartSignResponse.FailedToStartSign::class.java)
        assertThat((result as StartSignResponse.FailedToStartSign).errorMessage).isEqualTo(
            StartSignErrors.memberIsAlreadySigned.errorMessage
        )
        assertThat(result.errorCode).isEqualTo(
            StartSignErrors.memberIsAlreadySigned.errorCode
        )

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }
    }

    @Test
    fun verifyThatMemberServiceIsNotTriggeredWhenSigningFromHope() {
        val memberId = "1337"
        val quoteId = UUID.fromString("3D6D0502-5E40-4C90-9330-722132F69B94")
        val quote =
            quote {
                id = quoteId
                data = NorwegianHomeContentDataBuilder()
                this.memberId = memberId
                initiatedFrom = QuoteInitiatedFrom.HOPE
            }

        every { memberService.isSsnAlreadySignedMemberEntity(any()) } returns IsSsnAlreadySignedMemberResponse(
            ssnAlreadySignedMember = true
        )
        every { quoteService.getQuotes(listOf(quoteId)) } returns listOf(quote)
        every { quoteRepository.find(any()) } returns quote
        every { quoteRepository.update(any(), any()) } returnsArgument 0
        every {
            productPricingService.createContractsFromQuotes(
                any(),
                any(),
                any()
            )
        } returns listOf(
            CreateContractResponse(
                agreementId = UUID.randomUUID(),
                quoteId = quoteId,
                contractId = UUID.randomUUID()
            )
        )
        every { memberService.signQuote(any(), any()) } returns Right(UnderwriterQuoteSignResponse(1234, true))

        cut.signQuoteFromHope(
            quoteId,
            SignQuoteFromHopeRequest(activationDate = LocalDate.parse("2020-05-11"), token = null)
        )

        verify(inverse = true) {
            memberService.finalizeOnboarding(any(), any())
        }

        verify(inverse = true) {
            memberService.signQuote(
                memberId = memberId.toLong(),
                underwriterQuoteSignRequest = any()
            )
        }
    }

    @Test
    fun startSigningOfDanishQuote_startDanishSign() {
        val memberId = "1337"
        val quoteIds = listOf(UUID.randomUUID())
        val quote =
            quote {
                id = quoteIds[0]
                data = DanishHomeContentsDataBuilder()
                this.memberId = memberId
            }
        val signSessionReference = UUID.randomUUID()

        every { memberService.isMemberIdAlreadySignedMemberEntity(any()) } returns IsMemberAlreadySignedResponse(false)
        every { quoteService.getQuotes(quoteIds) } returns listOf(quote)
        every { signSessionRepository.insert(quoteIds) } returns signSessionReference
        every {
            memberService.startSimpleSign(
                quote.memberId!!.toLong(),
                signSessionReference,
                NationalIdentification(
                    quote.ssn,
                    Nationality.DENMARK
                )
            )
        } returns UnderwriterStartSignSessionResponse.SimpleSign(
            successfullyStarted = true
        )

        val result = cut.startSigningQuotes(quoteIds, memberId, null, successUrl, failUrl)

        assertThat(result).isInstanceOf(StartSignResponse.SimpleSignSession::class.java)

        verify {
            memberService.finalizeOnboarding(quote, quote.email!!)
        }
    }

    @Test
    fun failSignFromRapioIfStartDateAndCurrentInsurerAreNull() {
        val memberId = "1337"
        val quoteId = UUID.randomUUID()

        every {
            quoteRepository.findQuotes(listOf(quoteId))
        } returns listOf(
            quote {
                id = quoteId
                data = SwedishApartmentDataBuilder()
                this.memberId = memberId
            }
        )

        val result = cut.signQuoteFromRapio(
            quoteId, SignQuoteRequestDto(
                name = Name("Tolvan", "Tolvansson"),
                ssn = "191212121212",
                startDate = null,
                insuranceCompany = null,
                email = "tolvan@tolvannsson.com"
            )
        )
        assertThat(result.isLeft()).isTrue()
        val error = result.swap().getOrElse { null }!!

        assertThat(error.errorCode).isEqualTo(ErrorCodes.INVALID_STATE)
        assertThat(error.errorMessage).isEqualTo("currentInsurer is required when startDate is null")
    }
}
