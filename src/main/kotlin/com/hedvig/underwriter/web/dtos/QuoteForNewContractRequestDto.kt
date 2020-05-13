package com.hedvig.underwriter.web.dtos

data class QuoteForNewContractRequestDto(
    val quoteRequestDto: QuoteRequestDto,
    val underwritingGuidelinesBypassedBy: String?
)
