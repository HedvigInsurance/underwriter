package com.hedvig.underwriter.web

import arrow.core.getOrHandle
import com.hedvig.libs.logging.calls.LogCall
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.web.dtos.ExternalQuoteRequestDto
import com.hedvig.underwriter.web.dtos.QuoteDto
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/quotes")
class ExternalQuoteController(
    val quoteService: QuoteService
) {
    @PostMapping
    @LogCall
    fun createQuote(
        @RequestBody body: ExternalQuoteRequestDto
    ): ResponseEntity<out Any> {
        return quoteService.createQuote(
            quoteRequest = QuoteRequest.from(body),
            initiatedFrom = body.initiatedFrom,
            underwritingGuidelinesBypassedBy = null,
            updateMemberService = false
        ).bimap(
            { ResponseEntity.status(422).body(it) },
            { ResponseEntity.status(200).body(it) }
        ).getOrHandle { it }
    }

    @GetMapping("/{id}")
    @LogCall
    fun getQuote(
        @PathVariable id: UUID
    ): ResponseEntity<QuoteDto> {
        val quote = quoteService.getQuote(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(QuoteDto.from(quote))
    }
}
