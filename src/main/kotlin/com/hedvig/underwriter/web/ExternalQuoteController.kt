package com.hedvig.underwriter.web

import arrow.core.getOrHandle
import com.hedvig.graphql.commons.extensions.isAndroid
import com.hedvig.graphql.commons.extensions.isIOS
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.libs.logging.calls.LogCall
import com.hedvig.underwriter.web.dtos.ExternalQuoteRequestDto
import com.hedvig.underwriter.web.dtos.QuoteDto
import com.hedvig.underwriter.web.dtos.UpdateQuoteContractDto
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping

@RestController
@RequestMapping("/quotes")
class ExternalQuoteController(
    val quoteService: QuoteService
) {
    @PostMapping
    @LogCall
    fun createQuote(
        @RequestBody body: ExternalQuoteRequestDto,
        httpServletRequest: HttpServletRequest
    ): ResponseEntity<out Any> {
        val quoteInitiatedFrom = when {
            httpServletRequest.isAndroid() -> QuoteInitiatedFrom.ANDROID
            httpServletRequest.isIOS() -> QuoteInitiatedFrom.IOS
            else -> QuoteInitiatedFrom.APP
        }

        return quoteService.createQuote(
            quoteRequest = QuoteRequest.from(body),
            initiatedFrom = quoteInitiatedFrom,
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

    @PutMapping("/{quoteId}/contract")
    @LogCall
    fun bindQuoteToContract(
        @PathVariable quoteId: UUID,
        @RequestBody body: UpdateQuoteContractDto
    ): ResponseEntity<out Any> {
        return quoteService.bindQuoteToContract(
            quoteId, body.contractId, body.agreementId
        ).bimap(
            { ResponseEntity.status(422).body(it) },
            { ResponseEntity.status(200).body(QuoteDto.from(it)) }
        ).getOrHandle { it }
    }
}
