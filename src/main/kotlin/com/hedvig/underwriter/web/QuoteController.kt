package com.hedvig.underwriter.web

import arrow.core.Either
import arrow.core.getOrHandle
import com.hedvig.graphql.commons.extensions.isAndroid
import com.hedvig.graphql.commons.extensions.isIOS
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.service.SignService
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuoteDto
import com.hedvig.underwriter.web.dtos.ActivateQuoteRequestDto
import com.hedvig.underwriter.web.dtos.AddAgreementFromQuoteRequest
import com.hedvig.underwriter.web.dtos.ErrorCodes
import com.hedvig.underwriter.web.dtos.ErrorResponseDto
import com.hedvig.underwriter.web.dtos.QuoteRequestDto
import com.hedvig.underwriter.web.dtos.QuoteRequestFromAgreementDto
import com.hedvig.underwriter.web.dtos.SignQuoteRequest
import com.hedvig.underwriter.web.dtos.SignRequest
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import javax.validation.constraints.Email
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    "/_/v1/quote", // Deprecated
    "/_/v1/quotes"
)
class QuoteController @Autowired constructor(
    val quoteService: QuoteService,
    val memberService: MemberService,
    val signService: SignService
) {
    @PostMapping
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

    @PostMapping(
        path = [
            "/{incompleteQuoteId}/completeQuote",
            "/{incompleteQuoteId}/complete"
        ]
    )
    fun completeQuote(
        @Valid @PathVariable incompleteQuoteId: UUID,
        @Valid
        @Email
        @RequestParam("underwritingGuidelinesBypassedBy")
        underwritingGuidelinesBypassedBy: String?
    ): ResponseEntity<Any> {
        logger.error("completeQuote endpoint was used. incompleteQuoteId: $incompleteQuoteId underwritingGuidelinesBypassedBy: $underwritingGuidelinesBypassedBy")
        return ResponseEntity.status(HttpStatus.GONE)
            .body(ErrorResponseDto(ErrorCodes.UNKNOWN_ERROR_CODE, "endpoint is deprecated"))
    }

    @GetMapping("/{id}")
    fun getQuote(@PathVariable id: UUID): ResponseEntity<Quote> {
        val optionalQuote = quoteService.getQuote(id) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(optionalQuote)
    }

    @PatchMapping("/{id}")
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

    @PostMapping("/{completeQuoteId}/sign")
    fun signCompleteQuote(@Valid @PathVariable completeQuoteId: UUID, @RequestBody body: SignQuoteRequest): ResponseEntity<Any> {
        return when (val errorOrQuote = signService.signQuote(completeQuoteId, body)) {
            is Either.Left -> ResponseEntity.status(422).body(errorOrQuote.a)
            is Either.Right -> ResponseEntity.status(200).body(errorOrQuote.b)
        }
    }

    @PostMapping("/{completeQuoteId}/activate")
    fun activateCompleteQuote(
        @PathVariable completeQuoteId: UUID,
        @Valid @RequestBody requestBody: ActivateQuoteRequestDto
    ): ResponseEntity<Any> {
        val result =
            quoteService.activateQuote(completeQuoteId, requestBody.activationDate, requestBody.terminationDate)

        return when (result) {
            is Either.Left -> ResponseEntity.status(422).body(result.a)
            is Either.Right -> ResponseEntity.ok(result.b)
            else -> throw IllegalStateException("Result should be either left or right but was ${result::class.java}")
        }
    }

    @PostMapping("/add/agreement")
    fun addAgreementToContractTimeline(
        @Valid @RequestBody request: AddAgreementFromQuoteRequest
    ): ResponseEntity<Any> {
        val result = quoteService.addAgreementFromQuote(request)

        return when (result) {
            is Either.Left -> ResponseEntity.status(422).body(result.a)
            is Either.Right -> ResponseEntity.ok(result.b)
            else -> throw IllegalStateException("Result should be either left or right but was ${result::class.java}")
        }
    }

    @GetMapping("/members/{memberId}/latestQuote")
    fun getLatestQuoteFromMemberId(@PathVariable memberId: String): ResponseEntity<QuoteDto> {
        val quote = quoteService.getLatestQuoteForMemberId(memberId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(QuoteDto.fromQuote(quote))
    }

    @GetMapping("/members/{memberId}")
    fun getAllQuotesFromMemberId(@PathVariable memberId: String): ResponseEntity<List<QuoteDto>> {
        return ResponseEntity.ok(quoteService.getQuotesForMemberId(memberId))
    }

    @Deprecated("Should start sign session from `/_/v1/signSession` and complete it there")
    @PostMapping("/member/{memberId}/signed")
    fun memberSigned(@PathVariable memberId: String, @RequestBody signRequest: SignRequest): ResponseEntity<Void> {
        signService.memberSigned(memberId, signRequest)
        return ResponseEntity.noContent().build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QuoteController::class.java)
    }
}
