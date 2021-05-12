package com.hedvig.underwriter.web

import arrow.core.Either
import arrow.core.getOrHandle
import com.hedvig.graphql.commons.extensions.isAndroid
import com.hedvig.graphql.commons.extensions.isIOS
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.service.BundleQuotesService
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.service.SignService
import com.hedvig.underwriter.service.exceptions.ErrorException
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuoteDto
import com.hedvig.underwriter.util.logger
import com.hedvig.libs.logging.calls.LogCall
import com.hedvig.underwriter.service.exceptions.NotFoundException
import com.hedvig.underwriter.web.dtos.AddAgreementFromQuoteRequest
import com.hedvig.underwriter.web.dtos.ErrorCodes
import com.hedvig.underwriter.web.dtos.ErrorQuoteResponseDto
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import com.hedvig.underwriter.web.dtos.MarketInfo
import com.hedvig.underwriter.web.dtos.QuoteBundleRequestDto
import com.hedvig.underwriter.web.dtos.QuoteBundleResponseDto
import com.hedvig.underwriter.web.dtos.QuoteForNewContractRequestDto
import com.hedvig.underwriter.web.dtos.QuoteRequestDto
import com.hedvig.underwriter.web.dtos.QuoteRequestFromAgreementDto
import com.hedvig.underwriter.web.dtos.SignQuoteFromHopeRequest
import com.hedvig.underwriter.web.dtos.SignQuoteRequestDto
import com.hedvig.underwriter.web.dtos.SignQuotesRequestDto
import com.hedvig.underwriter.web.dtos.SignRequest
import com.hedvig.underwriter.web.dtos.SignedQuotesResponseDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import javax.validation.constraints.Email

@RestController
@RequestMapping(
    "/_/v1/quote", // Deprecated
    "/_/v1/quotes"
)
class QuoteController @Autowired constructor(
    val quoteService: QuoteService,
    val memberService: MemberService,
    val signService: SignService,
    val bundleQuotesService: BundleQuotesService
) {
    @PostMapping
    @LogCall
    fun createQuote(
        @Valid @RequestBody requestDto: QuoteRequestDto,
        httpServletRequest: HttpServletRequest
    ): ResponseEntity<out Any> {
        val houseOrApartmentIncompleteQuoteDto = QuoteRequest.from(requestDto)

        val quoteInitiatedFrom = when {
            httpServletRequest.isAndroid() -> QuoteInitiatedFrom.ANDROID
            httpServletRequest.isIOS() -> QuoteInitiatedFrom.IOS
            requestDto.originatingProductId != null -> QuoteInitiatedFrom.HOPE
            else -> QuoteInitiatedFrom.RAPIO
        }

        logger.info("Initiated from $quoteInitiatedFrom")

        return quoteService.createQuote(
            houseOrApartmentIncompleteQuoteDto,
            initiatedFrom = quoteInitiatedFrom,
            underwritingGuidelinesBypassedBy = requestDto.underwritingGuidelinesBypassedBy,
            updateMemberService = false
        )
            .bimap(
                { ResponseEntity.status(422).body(it) },
                { ResponseEntity.status(200).body(it) }
            ).getOrHandle { it }
    }

    @PostMapping("/createQuoteFromAgreement")
    @LogCall
    fun createQuoteFromAgreement(
        @RequestBody quoteRequest: QuoteRequestFromAgreementDto
    ): ResponseEntity<out Any> {
        return quoteService.createQuoteFromAgreement(
            agreementId = quoteRequest.agreementId,
            memberId = quoteRequest.memberId,
            underwritingGuidelinesBypassedBy = quoteRequest.underwritingGuidelinesBypassedBy
        ).bimap(
            { ResponseEntity.status(422).body(it) },
            { ResponseEntity.status(200).body(it) }
        ).getOrHandle { it }
    }

    @PostMapping("/createQuoteForNewContract")
    @LogCall
    fun createQuoteFromAgreement(
        @RequestBody request: QuoteForNewContractRequestDto
    ): ResponseEntity<out Any> {
        return quoteService.createQuoteForNewContractFromHope(
            quoteRequest = QuoteRequest.from(request.quoteRequestDto),
            underwritingGuidelinesBypassedBy = request.underwritingGuidelinesBypassedBy
        ).bimap(
            { ResponseEntity.status(422).body(it) },
            { ResponseEntity.status(200).body(it) }
        ).getOrHandle { it }
    }

    @PostMapping(
        path = [
            "/{incompleteQuoteId}/completeQuote",
            "/{incompleteQuoteId}/complete"
        ]
    )
    @LogCall
    fun completeQuote(
        @Valid @PathVariable incompleteQuoteId: UUID,
        @Valid
        @Email
        @RequestParam("underwritingGuidelinesBypassedBy")
        underwritingGuidelinesBypassedBy: String?
    ): ResponseEntity<Any> {
        return ResponseEntity.status(HttpStatus.GONE)
            .body(ErrorResponseDto(ErrorCodes.UNKNOWN_ERROR_CODE, "endpoint is deprecated"))
    }

    @GetMapping("/{id}")
    @LogCall
    fun getQuote(@PathVariable id: UUID): ResponseEntity<Quote> {
        val optionalQuote = quoteService.getQuote(id) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(optionalQuote)
    }

    @PatchMapping("/{id}")
    @LogCall
    fun updateQuoteInfo(
        @PathVariable id: UUID,
        @RequestBody @Valid quoteRequestDto: QuoteRequestDto,
        @Valid
        @Email
        @RequestParam("underwritingGuidelinesBypassedBy")
        underwritingGuidelinesBypassedBy: String?
    ): ResponseEntity<Any> {
        val houseOrApartmentIncompleteQuoteDto = QuoteRequest.from(quoteRequestDto)

        return when (val quoteOrError =
            quoteService.updateQuote(houseOrApartmentIncompleteQuoteDto, id, underwritingGuidelinesBypassedBy)) {
            is Either.Left -> ResponseEntity.status(422).body(quoteOrError.a)
            is Either.Right -> ResponseEntity.status(200).body(QuoteDto.fromQuote(quoteOrError.b))
        }
    }

    @PostMapping("/bundle")
    @LogCall
    fun quoteBundle(@RequestBody @Valid request: QuoteBundleRequestDto): QuoteBundleResponseDto {

        val cost = bundleQuotesService.bundleQuotes(
            memberId = null,
            ids = request.quoteIds
        )

        return QuoteBundleResponseDto.from(cost)
    }

    @Deprecated("Use /{completeQuoteId}/signFromRapio")
    @PostMapping("/{completeQuoteId}/sign")
    @LogCall
    fun signQuoteFromRapioDeprecated(
        @Valid @PathVariable completeQuoteId: UUID,
        @RequestBody request: SignQuoteRequestDto
    ): ResponseEntity<Any> {
        return when (val errorOrQuote = signService.signQuoteFromRapio(completeQuoteId, request)) {
            is Either.Left -> ResponseEntity.status(422).body(errorOrQuote.a)
            is Either.Right -> ResponseEntity.status(200).body(errorOrQuote.b)
        }
    }

    @PostMapping("/{completeQuoteId}/signFromRapio")
    @LogCall
    fun signQuoteFromRapio(
        @Valid @PathVariable completeQuoteId: UUID,
        @RequestBody request: SignQuoteRequestDto
    ): ResponseEntity<Any> {
        return when (val errorOrQuote = signService.signQuoteFromRapio(completeQuoteId, request)) {
            is Either.Left -> ResponseEntity.status(422).body(errorOrQuote.a)
            is Either.Right -> ResponseEntity.status(200).body(errorOrQuote.b)
        }
    }

    @PostMapping("/bundle/signFromRapio")
    @LogCall
    fun signQuotesFromRapio(
        @RequestBody request: SignQuotesRequestDto
    ): ResponseEntity<Any> {
        return try {

            require(request.quoteIds.size > 1) { "Not a bundle" }

            val response = signService.signQuotesFromRapio(request)

            ResponseEntity.status(200).body(SignedQuotesResponseDto.from(response))
        } catch (e: ErrorException) {
            ResponseEntity.status(422).body(ErrorResponseDto.from(e))
        }
    }

    @PostMapping("/{completeQuoteId}/signFromHope")
    @LogCall
    fun signQuoteFromHope(
        @Valid @PathVariable completeQuoteId: UUID,
        @RequestBody request: SignQuoteFromHopeRequest
    ): ResponseEntity<Any> {
        return when (val errorOrQuote = signService.signQuoteFromHope(completeQuoteId, request)) {
            is Either.Left -> ResponseEntity.status(422).body(errorOrQuote.a)
            is Either.Right -> ResponseEntity.status(200).body(errorOrQuote.b)
        }
    }

    @PostMapping("/add/agreement")
    @LogCall
    fun addAgreementToContractTimeline(
        @Valid @RequestBody request: AddAgreementFromQuoteRequest,
        @RequestHeader("Authorization") token: String?
    ): ResponseEntity<Any> {
        val result = quoteService.addAgreementFromQuote(request, token)

        return when (result) {
            is Either.Left -> ResponseEntity.status(422).body(result.a)
            is Either.Right -> ResponseEntity.ok(result.b)
            else -> throw IllegalStateException("Result should be either left or right but was ${result::class.java}")
        }
    }

    @GetMapping("/members/{memberId}/latestQuote")
    @LogCall
    fun getLatestQuoteFromMemberId(@PathVariable memberId: String): ResponseEntity<QuoteDto> {
        val quote = quoteService.getLatestQuoteForMemberId(memberId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(QuoteDto.fromQuote(quote))
    }

    @GetMapping("/members/{memberId}")
    @LogCall
    fun getAllQuotesFromMemberId(@PathVariable memberId: String): ResponseEntity<List<QuoteDto>> {
        return ResponseEntity.ok(quoteService.getQuotesForMemberId(memberId))
    }

    @Deprecated("Should start sign session from `/_/v1/signSession` and complete it there")
    @PostMapping("/member/{memberId}/signed")
    @LogCall
    fun memberSignedDeprecated(@PathVariable memberId: String, @RequestBody signRequest: SignRequest): ResponseEntity<Void> {
        signService.memberSigned(memberId, signRequest)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/expire")
    @LogCall
    fun expireInvalidQuotes(@PathVariable id: UUID): ResponseEntity<Quote> {
        val quote = quoteService.expireQuote(id) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(quote)
    }

    @DeleteMapping("/{id}")
    @LogCall
    fun deleteQuote(@PathVariable id: UUID): ResponseEntity<Void> {
        try {
            quoteService.deleteQuote(id)

            return ResponseEntity.noContent().build()
        } catch (e: NotFoundException) {
            return ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    @GetMapping("/contracts/{contractId}")
    @LogCall
    fun getContractById(@PathVariable contractId: UUID): ResponseEntity<Any> {
        val quote = quoteService.getQuoteByContractId(contractId = contractId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorQuoteResponseDto(
                    errorCode = ErrorCodes.NO_SUCH_QUOTE,
                    errorMessage = "QuoteNotFound"
                )
            )

        return ResponseEntity.ok(QuoteDto.fromQuote(quote))
    }

    @GetMapping("/members/{memberId}/latestQuote/marketInfo")
    @LogCall
    fun getMarketInfoFromLatestQuote(@PathVariable memberId: String): ResponseEntity<MarketInfo> {
        val market = quoteService.getMarketFromLatestQuote(memberId)
        return ResponseEntity.ok(MarketInfo(market = market))
    }
}
