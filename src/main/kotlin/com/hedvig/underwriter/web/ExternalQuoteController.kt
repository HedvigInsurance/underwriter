package com.hedvig.underwriter.web

import arrow.core.getOrHandle
import com.hedvig.libs.logging.calls.LogCall
import com.hedvig.productPricingObjects.dtos.SelfChangeResult
import com.hedvig.underwriter.service.QuoteService
import com.hedvig.underwriter.service.SelfChangeService
import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.web.dtos.ExternalQuoteRequestDto
import com.hedvig.underwriter.web.dtos.QuoteDto
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/quotes")
class ExternalQuoteController(
    val quoteService: QuoteService,
    val selfChangeService: SelfChangeService
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

    @PostMapping("/connectToContracts")
    @LogCall
    fun connectToContracts(
        @RequestHeader("Hedvig.token") memberId: String,
        @RequestBody body: SelfChangeToQuotesInput
    ): SelfChangeToQuotesOutput {
        val result = selfChangeService.changeToQuotes(
            body.quoteIds, body.contractIds, memberId
        )
        return SelfChangeToQuotesOutput.from(result)
    }

    data class SelfChangeToQuotesInput(
        val contractIds: List<UUID>,
        val quoteIds: List<UUID>
    )

    data class SelfChangeToQuotesOutput(
        val updatedContractIds: List<UUID>,
        val createdContractIds: List<UUID>,
        val terminatedContractIds: List<UUID>
    ) {
        companion object {
            fun from(result: SelfChangeResult) = SelfChangeToQuotesOutput(
                updatedContractIds = result.updatedContracts.map { it.contractId },
                createdContractIds = result.createdContracts.map { it.contractId },
                terminatedContractIds = result.terminatedContractIds
            )
        }
    }
}
