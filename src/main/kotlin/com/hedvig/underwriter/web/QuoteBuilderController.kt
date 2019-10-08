package com.hedvig.underwriter.web

import com.hedvig.underwriter.model.DateWithZone
import com.hedvig.underwriter.model.IncompleteQuote
import com.hedvig.underwriter.service.QuoteService

import com.hedvig.underwriter.service.QuoteBuilderService
import com.hedvig.underwriter.web.Dtos.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import javax.validation.Valid

@RestController
@RequestMapping("/_/v1/incompleteQuote")
class QuoteBuilderController @Autowired constructor(
        val quoteBuilderService: QuoteBuilderService,
        val quoteService: QuoteService
) {

    @PostMapping("/")
    fun createIncompleteQuote(@Valid @RequestBody incompleteQuoteDto: PostIncompleteQuoteRequest): ResponseEntity<IncompleteQuoteResponseDto> {
        val quote = quoteBuilderService.createIncompleteQuote(incompleteQuoteDto)
        return ResponseEntity.ok(quote)
    }

    @GetMapping("/{id}")
    fun getIncompleteQuote(@PathVariable id: UUID): ResponseEntity<IncompleteQuote> {
        val optionalQuote:Optional<IncompleteQuote> = quoteBuilderService.findIncompleteQuoteById(id)

        if (!quoteBuilderService.findIncompleteQuoteById(id).isPresent) {
            return ResponseEntity.notFound().build()
        }
        val incompleteQuote:IncompleteQuote = optionalQuote.get()
        return ResponseEntity.ok(incompleteQuote)
    }

    @PatchMapping("/{id}")
    fun updateQuoteInfo(@PathVariable id: UUID, @RequestBody incompleteQuoteDto: IncompleteQuoteDto): ResponseEntity<IncompleteQuoteDto> {
        quoteBuilderService.updateIncompleteQuoteData(incompleteQuoteDto, id)
        return ResponseEntity.ok(incompleteQuoteDto)
    }

    @PostMapping("/{id}/completeQuote")
    fun createCompleteQuote(@Valid @PathVariable id: UUID): ResponseEntity<CompleteQuoteResponseDto> {
        val quote = quoteService.createCompleteQuote(id)
        return ResponseEntity.ok(quote)
    }
}