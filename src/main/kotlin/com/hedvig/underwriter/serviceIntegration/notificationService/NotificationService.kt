package com.hedvig.underwriter.serviceIntegration.notificationService

import com.hedvig.underwriter.model.Quote

interface NotificationService {
    fun sendQuoteCreatedEvent(quote: Quote)
}
