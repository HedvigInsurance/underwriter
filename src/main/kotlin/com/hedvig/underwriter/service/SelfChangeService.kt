package com.hedvig.underwriter.service

import java.util.UUID

interface SelfChangeService {
    fun changeToQuotes(quoteIds: List<UUID>, memberId: String)
}
