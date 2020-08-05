package com.hedvig.underwriter.model

import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.service.model.QuoteRequestData.SwedishApartment
import com.hedvig.underwriter.service.model.QuoteRequestData.SwedishHouse
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class QuoteTest {
    @Test
    fun updatesQuote() {
        val quote = Quote(
            id = UUID.randomUUID(),
            createdAt = Instant.now(),
            data = SwedishApartmentData(id = UUID.randomUUID()),
            productType = ProductType.APARTMENT,
            initiatedFrom = QuoteInitiatedFrom.HOPE,
            attributedTo = Partner.HEDVIG,
            state = QuoteState.QUOTED,
            breachedUnderwritingGuidelines = null,
            price = BigDecimal.valueOf(100)
        )
        val updatedQuote = quote.update(
            QuoteRequest(
                firstName = null,
                lastName = null,
                email = null,
                productType = null,
                ssn = "201212121212",
                currentInsurer = null,
                incompleteQuoteData = null,
                originatingProductId = null,
                quotingPartner = null,
                birthDate = null,
                memberId = null,
                startDate = null,
                dataCollectionId = null,
                dataCollectionIds = emptyList()
            )
        )
        assertThat(updatedQuote.id).isEqualTo(quote.id)
        assertThat((updatedQuote.data as SwedishApartmentData).ssn).isEqualTo("201212121212")
    }

    @Test
    fun updatesQuoteFromApartmentToHouse() {
        val quote = Quote(
            id = UUID.randomUUID(),
            createdAt = Instant.now(),
            data = SwedishApartmentData(
                id = UUID.randomUUID(),
                ssn = "201212121212",
                street = "Storgatan 1"
            ),
            productType = ProductType.APARTMENT,
            initiatedFrom = QuoteInitiatedFrom.HOPE,
            attributedTo = Partner.HEDVIG,
            state = QuoteState.QUOTED,
            breachedUnderwritingGuidelines = null,
            price = BigDecimal.valueOf(100)
        )
        val updatedQuote = quote.update(
            QuoteRequest(
                firstName = null,
                lastName = null,
                email = null,
                productType = ProductType.HOUSE,
                ssn = "201212121213",
                currentInsurer = null,
                incompleteQuoteData = SwedishHouse(
                    street = "Storgatan 2",
                    zipCode = null,
                    city = null,
                    livingSpace = null,
                    householdSize = null,
                    isSubleted = null,
                    yearOfConstruction = null,
                    numberOfBathrooms = null,
                    ancillaryArea = null,
                    extraBuildings = null
                ),
                originatingProductId = null,
                quotingPartner = null,
                birthDate = null,
                memberId = null,
                startDate = null,
                dataCollectionId = null,
                dataCollectionIds = emptyList()
            )
        )
        assertThat(updatedQuote.id).isEqualTo(quote.id)
        assertThat(updatedQuote.productType).isEqualTo(ProductType.HOUSE)
        assertThat((updatedQuote.data as SwedishHouseData).ssn).isEqualTo("201212121213")
        assertThat((updatedQuote.data as SwedishHouseData).street).isEqualTo("Storgatan 2")
    }

    @Test
    fun updatesQuoteFromHouseToApartment() {
        val quote = Quote(
            id = UUID.randomUUID(),
            createdAt = Instant.now(),
            data = SwedishHouseData(
                id = UUID.randomUUID(),
                ssn = "201212121212",
                street = "Storgatan 1"
            ),
            productType = ProductType.HOUSE,
            initiatedFrom = QuoteInitiatedFrom.HOPE,
            attributedTo = Partner.HEDVIG,
            breachedUnderwritingGuidelines = null,
            state = QuoteState.QUOTED,
            price = BigDecimal.valueOf(100)
        )
        val updatedQuote = quote.update(
            QuoteRequest(
                firstName = null,
                lastName = null,
                email = null,
                productType = ProductType.APARTMENT,
                ssn = "201212121213",
                currentInsurer = null,
                incompleteQuoteData = SwedishApartment(
                    street = "Storgatan 2",
                    zipCode = null,
                    city = null,
                    livingSpace = null,
                    householdSize = null,
                    subType = ApartmentProductSubType.BRF,
                    floor = null
                ),
                originatingProductId = null,
                quotingPartner = null,
                birthDate = null,
                memberId = null,
                startDate = null,
                dataCollectionId = null,
                dataCollectionIds = emptyList()
            )
        )
        assertThat(updatedQuote.id).isEqualTo(quote.id)
        assertThat(updatedQuote.productType).isEqualTo(ProductType.APARTMENT)
        assertThat((updatedQuote.data as SwedishApartmentData).ssn).isEqualTo("201212121213")
        assertThat((updatedQuote.data as SwedishApartmentData).street).isEqualTo("Storgatan 2")
        assertThat((updatedQuote.data as SwedishApartmentData).subType).isEqualTo(ApartmentProductSubType.BRF)
    }
}
