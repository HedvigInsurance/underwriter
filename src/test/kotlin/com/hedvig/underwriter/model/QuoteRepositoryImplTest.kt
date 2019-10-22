package com.hedvig.underwriter.model

import com.hedvig.underwriter.testhelp.JdbiRule
import java.time.Instant
import java.util.UUID
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

class QuoteRepositoryImplTest {
    @get:Rule
    val jdbiRule = JdbiRule.create()

    @Test
    fun insertsAndFindsApartmentQuotes() {
        val quoteDao = QuoteRepositoryImpl(jdbiRule.jdbi)

        val timestamp = Instant.now()
        val quote = Quote(
            productType = ProductType.APARTMENT,
            data = ApartmentData(
                firstName = "Sherlock",
                lastName = "Holmes",
                ssn = "199003041234",
                street = "221 Baker street",
                zipCode = "11216",
                livingSpace = 33,
                householdSize = 4,
                city = "London",
                id = UUID.randomUUID(),
                subType = ApartmentProductSubType.BRF
            ),
            initiatedFrom = QuoteInitiatedFrom.APP,
            attributedTo = Partner.HEDVIG,
            id = UUID.randomUUID(),
            currentInsurer = null,
            memberId = "123456",
            createdAt = timestamp,
            state = QuoteState.INCOMPLETE
            )
        quoteDao.insert(quote, timestamp)
        assertQuotesDeepEqualExceptInternalId(quote, quoteDao.find(quote.id))
    }

    @Test
    fun updatesApartmentQuotes() {
        val quoteDao = QuoteRepositoryImpl(jdbiRule.jdbi)

        val timestamp = Instant.now()
        val quote = Quote(
            createdAt = timestamp,
            productType = ProductType.APARTMENT,
            data = ApartmentData(
                firstName = "Sherlock",
                lastName = "Holmes",
                ssn = "199003041234",
                street = "221 Baker street",
                zipCode = "11216",
                livingSpace = 33,
                householdSize = 4,
                city = "London",
                id = UUID.randomUUID(),
                subType = ApartmentProductSubType.BRF
            ),
            initiatedFrom = QuoteInitiatedFrom.APP,
            attributedTo = Partner.HEDVIG,
            id = UUID.randomUUID(),
            currentInsurer = null,
            state = QuoteState.QUOTED
        )
        quoteDao.insert(quote, timestamp)
        val updatedQuote = quote.copy(
            data = (quote.data as ApartmentData).copy(
                firstName = "John",
                lastName = "Watson"
            ),
            memberId = "123456",
            state = QuoteState.SIGNED
        )
        quoteDao.update(updatedQuote)

        assertQuotesDeepEqualExceptInternalId(updatedQuote, quoteDao.find(quote.id))
    }

    @Test
    fun insertsAndFindsHouseQuotes() {
        val quoteDao = QuoteRepositoryImpl(jdbiRule.jdbi)

        val timestamp = Instant.now()
        val quote = Quote(
            createdAt = timestamp,
            productType = ProductType.APARTMENT,
            data = HouseData(
                firstName = "Sherlock",
                lastName = "Holmes",
                ssn = "199003041234",
                street = "221 Baker street",
                zipCode = "11216",
                livingSpace = 33,
                householdSize = 4,
                city = "London",
                id = UUID.randomUUID()
            ),
            initiatedFrom = QuoteInitiatedFrom.APP,
            attributedTo = Partner.HEDVIG,
            id = UUID.randomUUID(),
            currentInsurer = null,
            memberId = "123456",
            state = QuoteState.SIGNED
        )
        quoteDao.insert(quote, timestamp)
        assertQuotesDeepEqualExceptInternalId(quote, quoteDao.find(quote.id))
    }

    @Test
    fun updatesHouseQuotes() {
        val quoteDao = QuoteRepositoryImpl(jdbiRule.jdbi)

        val timestamp = Instant.now()
        val quote = Quote(
            createdAt = timestamp,
            productType = ProductType.APARTMENT,
            data = HouseData(
                firstName = "Sherlock",
                lastName = "Holmes",
                ssn = "199003041234",
                street = "221 Baker street",
                zipCode = "11216",
                livingSpace = 33,
                householdSize = 4,
                city = "London",
                id = UUID.randomUUID()
            ),
            initiatedFrom = QuoteInitiatedFrom.APP,
            attributedTo = Partner.HEDVIG,
            id = UUID.randomUUID(),
            currentInsurer = null,
            state = QuoteState.QUOTED
        )
        quoteDao.insert(quote, timestamp)

        val updatedQuote = quote.copy(
            state = QuoteState.SIGNED,
            data = (quote.data as HouseData).copy(
                firstName = "John",
                lastName = "Watson"
            ),
            memberId = "123456"
        )
        quoteDao.update(updatedQuote)

        assertQuotesDeepEqualExceptInternalId(updatedQuote, quoteDao.find(quote.id))
    }

    private fun assertQuotesDeepEqualExceptInternalId(
        expected: Quote,
        result: Quote?
    ) {
        expected::class.memberProperties.forEach { prop ->
            if (prop.name == "data") {
                prop.javaGetter!!.invoke(expected)::class.memberProperties.forEach { dataProp ->
                    if (dataProp.name != "internalId") {
                        assertThat(dataProp.javaGetter!!.invoke(expected.data)).isEqualTo(
                            dataProp.javaGetter!!.invoke(
                                result?.data
                            )
                        )
                    }
                }
            } else {
                assertThat(prop.javaGetter!!.invoke(expected)).isEqualTo(prop.javaGetter!!.invoke(result))
            }
        }
    }
}
