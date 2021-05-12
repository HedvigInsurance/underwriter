package com.hedvig.underwriter.serviceIntegration.notificationService

import com.hedvig.underwriter.model.Quote
import com.hedvig.underwriter.model.email
import com.hedvig.underwriter.model.firstName
import com.hedvig.underwriter.model.lastName
import com.hedvig.underwriter.model.ssnMaybe
import com.hedvig.underwriter.service.exceptions.NotFoundException
import com.hedvig.underwriter.service.quoteStrategies.QuoteStrategyService
import com.hedvig.underwriter.serviceIntegration.notificationService.dtos.QuoteCreatedEvent
import com.hedvig.underwriter.util.logger
import feign.FeignException
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.time.Instant

@Service
class NotificationServiceImpl(
    private val client: NotificationServiceClient,
    private val quoteStrategyService: QuoteStrategyService
) : NotificationService {
    override fun sendQuoteCreatedEvent(quote: Quote) {
        val event = quoteStrategyService.createQuoteCreatedEvent(quote)
        client.quoteCreated(event)
    }

    override fun postSignUpdate(quote: Quote) {
        if (quote.memberId != null) {
            val isSwitcher = quote.currentInsurer != null
            try {
                val map = mapOf(
                    "partner_code" to quote.attributedTo.name,
                    "sign_source" to quote.initiatedFrom.name,
                    "sign_date" to Instant.now(),
                    "switcher_company" to quote.currentInsurer,
                    "is_switcher" to isSwitcher
                )
                client.post(quote.memberId, map)
            } catch (ex: Exception) {
                logger.error("Could not update member with id ${quote.memberId}", ex)
            }
        }
    }

    override fun deleteMember(memberId: String) {
        try {
            val response = client.deleteMember(memberId)

            if (response.statusCodeValue == 404) {
                throw NotFoundException("Failed to delete member $memberId in Notification Service, member not found")
            }

            if (response.statusCode.isError) {
                throw RuntimeException("Failed to delete member $memberId in Notification Service: $response")
            }
        } catch (e: FeignException) {
            if (e.status() == 404) {
                throw NotFoundException("Failed to delete member $memberId in Notification Service, member not found")
            }

            throw RuntimeException("Failed to delete member $memberId in Notification Service", e)
        }
    }
}

fun quoteCreatedEvent(
    quote: Quote,
    street: String?,
    postalCode: String?,
    insuranceType: String
): QuoteCreatedEvent {
    return QuoteCreatedEvent(
        memberId = quote.memberId!!,
        quoteId = quote.id,
        firstName = quote.firstName,
        lastName = quote.lastName,
        street = street,
        postalCode = postalCode,
        email = quote.email!!,
        ssn = quote.ssnMaybe,
        initiatedFrom = quote.initiatedFrom.name,
        attributedTo = quote.attributedTo.name,
        productType = quote.productType.name,
        insuranceType = insuranceType,
        currentInsurer = quote.currentInsurer,
        price = quote.price,
        currency = quote.currencyWithFallbackOnMarket,
        originatingProductId = quote.originatingProductId
    )
}
