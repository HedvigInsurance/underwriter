package com.hedvig.underwriter.model

import com.hedvig.underwriter.service.model.QuoteRequest
import com.hedvig.underwriter.service.model.QuoteRequestData.SwedishApartment
import com.hedvig.underwriter.service.model.QuoteRequestData.SwedishHouse
import com.hedvig.underwriter.testhelp.databuilder.DanishHomeContentsDataBuilder
import com.hedvig.underwriter.testhelp.databuilder.DanishHomeContentsQuoteRequestBuilder
import com.hedvig.underwriter.testhelp.databuilder.DanishHomeContentsQuoteRequestDataBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

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
                phoneNumber = null,
                productType = null,
                ssn = "201212121212",
                currentInsurer = null,
                incompleteQuoteData = null,
                originatingProductId = null,
                quotingPartner = null,
                birthDate = null,
                memberId = null,
                startDate = null,
                dataCollectionId = null
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
                phoneNumber = null,
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
                dataCollectionId = null
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
                phoneNumber = null,
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
                dataCollectionId = null
            )
        )
        assertThat(updatedQuote.id).isEqualTo(quote.id)
        assertThat(updatedQuote.productType).isEqualTo(ProductType.APARTMENT)
        assertThat((updatedQuote.data as SwedishApartmentData).ssn).isEqualTo("201212121213")
        assertThat((updatedQuote.data as SwedishApartmentData).street).isEqualTo("Storgatan 2")
        assertThat((updatedQuote.data as SwedishApartmentData).subType).isEqualTo(ApartmentProductSubType.BRF)
    }

    @Test
    fun `gets birth date from Danish SSN`() {
        val birthDate = "1408300921".birthDateFromDanishSsn()
        assertThat(birthDate).isEqualTo(LocalDate.of(1930, 8, 14))
    }

    @Test
    fun `gets birth date from Norwegian SSN`() {
        val birthDate = "23077421475".birthDateFromDanishSsn()
        assertThat(birthDate).isEqualTo(LocalDate.of(1974, 7, 23))
    }

    @Test
    fun `will update quote but keep previous bbrId if no changes to address have been made`() {
        val quote = Quote(
            id = UUID.randomUUID(),
            createdAt = Instant.now(),
            data = DanishHomeContentsDataBuilder().build(),
            productType = ProductType.HOME_CONTENT,
            initiatedFrom = QuoteInitiatedFrom.HOPE,
            attributedTo = Partner.HEDVIG,
            state = QuoteState.QUOTED,
            breachedUnderwritingGuidelines = null,
            price = BigDecimal.valueOf(100)
        )

        val danishHomeContentsQuoteData = DanishHomeContentsQuoteRequestDataBuilder().build()

        val quoteDataThatDoesNotAmendAddress = danishHomeContentsQuoteData.copy(
            street = null,
            apartment = null,
            floor = null,
            zipCode = null,
            city = null
        )

        val updatedQuote = quote.update(
            DanishHomeContentsQuoteRequestBuilder().build(quoteDataThatDoesNotAmendAddress, "201212121212")
        )
        assertThat(updatedQuote.id).isEqualTo(quote.id)
        assertThat((updatedQuote.data as DanishHomeContentsData).ssn).isEqualTo("201212121212")
        assertThat((updatedQuote.data as DanishHomeContentsData).bbrId).isEqualTo((quote.data as DanishHomeContentsData).bbrId)
    }

    @Test
    fun `if city or street is changed when editing quote but brrId is not updated (before we have autocomplete implemented) set bbrId to null but use zipCode from previous quote`() {
        val quote = Quote(
            id = UUID.randomUUID(),
            createdAt = Instant.now(),
            data = DanishHomeContentsDataBuilder().build(),
            productType = ProductType.HOME_CONTENT,
            initiatedFrom = QuoteInitiatedFrom.HOPE,
            attributedTo = Partner.HEDVIG,
            state = QuoteState.QUOTED,
            breachedUnderwritingGuidelines = null,
            price = BigDecimal.valueOf(100)
        )

        val danishHomeContentsQuoteData = DanishHomeContentsQuoteRequestDataBuilder().build(
            newStreet = "new street",
            newBbrId = null
        )
        val updatedQuote = quote.update(
            DanishHomeContentsQuoteRequestBuilder().build(homeContentsData = danishHomeContentsQuoteData)
        )
        assertThat(updatedQuote.id).isEqualTo(quote.id)
        assertThat((updatedQuote.data as DanishHomeContentsData).zipCode).isEqualTo((quote.data as DanishHomeContentsData).zipCode)
        assertThat((updatedQuote.data as DanishHomeContentsData).bbrId).isNull()
    }
}
