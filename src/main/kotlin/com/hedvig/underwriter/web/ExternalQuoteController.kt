package com.hedvig.underwriter.web

import arrow.core.getOrHandle
import com.hedvig.graphql.commons.extensions.isAndroid
import com.hedvig.graphql.commons.extensions.isIOS
import com.hedvig.underwriter.model.QuoteInitiatedFrom
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.web.dtos.ExternalQuoteRequestDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("/quotes")
class ExternalQuoteController(
    val quoteService: QuoteService
) {
    @PostMapping
    fun createQuote(
        @Valid @RequestBody body: ExternalQuoteRequestDto,
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
}
