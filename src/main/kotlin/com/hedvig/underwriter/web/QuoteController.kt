package com.hedvig.underwriter.web

import arrow.core.Either
import arrow.core.getOrHandle
import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.serviceIntegration.memberService.MemberService
import com.hedvig.underwriter.serviceIntegration.productPricing.dtos.QuoteDto
import com.hedvig.underwriter.web.dtos.IncompleteQuoteDto
import com.hedvig.underwriter.web.dtos.IncompleteQuoteResponseDto
import com.hedvig.underwriter.web.dtos.RedeemCodeRequest
import com.hedvig.underwriter.web.dtos.SignQuoteRequest
import java.util.UUID
import javax.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    "/_/v1/quote", // Deprecated
    "/_/v1/quotes"
)
class QuoteController @Autowired constructor(
    val quoteService: QuoteService,
    val memberService: MemberService
) {
    @PostMapping
    fun createIncompleteQuote(@Valid @RequestBody incompleteQuoteDto: IncompleteQuoteDto): ResponseEntity<IncompleteQuoteResponseDto> {
        val quote = quoteService.createApartmentQuote(incompleteQuoteDto)
        return ResponseEntity.ok(quote)
    }

    @PostMapping("/{incompleteQuoteId}/completeQuote")
    fun createCompleteQuote(@Valid @PathVariable incompleteQuoteId: UUID): ResponseEntity<Any> {
        return when (val quoteOrError = quoteService.completeQuote(incompleteQuoteId)) {
            is Either.Left -> ResponseEntity.status(422).body(quoteOrError.a)
            is Either.Right -> ResponseEntity.status(200).body(quoteOrError.b)
        }
    }

    @GetMapping("/{id}")
    fun getQuote(@PathVariable id: UUID): ResponseEntity<Quote> {
        val optionalQuote = quoteService.getQuote(id) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(optionalQuote)
    }

    @GetMapping("/members/{memberId}/latestQuote")
    fun getLatestQuoteFromMemberId(@PathVariable memberId: String): ResponseEntity<QuoteDto> {
        val quoteDto = quoteService.getQuoteFromMemberId(memberId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(quoteDto)
    }

    @PatchMapping("/{id}")
    fun updateQuoteInfo(@PathVariable id: UUID, @RequestBody @Valid incompleteQuoteDto: IncompleteQuoteDto): ResponseEntity<Any> {
        return when (val quoteOrError = quoteService.updateQuote(incompleteQuoteDto, id)) {
            is Either.Left -> ResponseEntity.status(422).body(quoteOrError.a)
            is Either.Right -> ResponseEntity.status(200).body(quoteOrError.b)
        }
    }

    @PostMapping("/{completeQuoteId}/sign")
    fun signCompleteQuote(@Valid @PathVariable completeQuoteId: UUID, @RequestBody body: SignQuoteRequest): ResponseEntity<Any> {
        return when (val errorOrQuote = quoteService.signQuote(completeQuoteId, body)) {
            is Either.Left -> ResponseEntity.status(422).body(errorOrQuote.a)
            is Either.Right -> ResponseEntity.status(200).body(errorOrQuote.b)
        }
    }

    @PostMapping("/{completeQuoteId}/redeemCode")
    fun redeemCode(@PathVariable completeQuoteId: UUID,  @RequestBody body: RedeemCodeRequest): Any {
        return quoteService.redeemCode(completeQuoteId, body.code).bimap(
            {ResponseEntity.status(422).body(it)},
            {ResponseEntity.status(200).body(it.Incentive)}
        ).getOrHandle { it }
    }
}
