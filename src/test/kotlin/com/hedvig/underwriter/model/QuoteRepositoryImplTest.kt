package com.hedvig.underwriter.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.hedvig.underwriter.testhelp.JdbiRule
import java.time.Instant
import java.util.UUID
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter
import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.jackson2.Jackson2Config
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class QuoteRepositoryImplTest {
    @get:Rule
    val jdbiRule = JdbiRule.create()

    @Before
    fun setUp() {
        jdbiRule.jdbi.getConfig(Jackson2Config::class.java).mapper =
            ObjectMapper().registerModule(KotlinModule())
    }

    @Test
    fun insertsAndFindsApartmentQuotes() {
        val quoteDao = QuoteRepositoryImpl(jdbiRule.jdbi)

        val timestamp = Instant.now()
        val quote = Quote(
            id = UUID.randomUUID(),
            createdAt = timestamp,
            productType = ProductType.APARTMENT,
            state = QuoteState.INCOMPLETE,
            initiatedFrom = QuoteInitiatedFrom.APP,
            attributedTo = Partner.HEDVIG,
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
            currentInsurer = null,
            memberId = "123456",
            originatingProductId = UUID.randomUUID(),
            signedProductId = UUID.randomUUID()
        )
        quoteDao.insert(quote, timestamp)
        assertQuotesDeepEqualExceptInternalId(quote, quoteDao.find(quote.id))
    }

    @Test
    fun updatesApartmentQuotes() {
        val quoteDao = QuoteRepositoryImpl(jdbiRule.jdbi)

        val timestamp = Instant.now()
        val quote = Quote(
            id = UUID.randomUUID(),
            createdAt = timestamp,
            productType = ProductType.APARTMENT,
            state = QuoteState.QUOTED,
            initiatedFrom = QuoteInitiatedFrom.APP,
            attributedTo = Partner.HEDVIG,
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
            currentInsurer = null
        )
        quoteDao.insert(quote, timestamp)
        val updatedQuote = quote.copy(
            data = (quote.data as ApartmentData).copy(
                firstName = "John",
                lastName = "Watson"
            ),
            memberId = "123456",
            state = QuoteState.SIGNED,
            originatingProductId = UUID.randomUUID(),
            signedProductId = UUID.randomUUID()
        )
        quoteDao.update(updatedQuote)

        assertQuotesDeepEqualExceptInternalId(updatedQuote, quoteDao.find(quote.id))
    }

    @Test
    fun insertsAndFindsHouseQuotes() {
        val quoteDao = QuoteRepositoryImpl(jdbiRule.jdbi)

        val timestamp = Instant.now()
        val quote = Quote(
            id = UUID.randomUUID(),
            createdAt = timestamp,
            productType = ProductType.APARTMENT,
            state = QuoteState.SIGNED,
            initiatedFrom = QuoteInitiatedFrom.APP,
            attributedTo = Partner.HEDVIG,
            data = HouseData(
                firstName = "Sherlock",
                lastName = "Holmes",
                ssn = "199003041234",
                street = "221 Baker street",
                zipCode = "11216",
                livingSpace = 33,
                householdSize = 4,
                city = "London",
                id = UUID.randomUUID(),
                ancillaryArea = 42,
                yearOfConstruction = 1995,
                extraBuildings = listOf(
                    ExtraBuilding(
                        type = ExtraBuildingType.ATTEFALL,
                        area = 20,
                        displayName = "Foo",
                        hasWaterConnected = false
                    )
                ),
                numberOfBathrooms = 2,
                isSubleted = false
            ),
            currentInsurer = null,
            memberId = "123456"
        )
        quoteDao.insert(quote, timestamp)
        assertQuotesDeepEqualExceptInternalId(quote, quoteDao.find(quote.id))
    }

    @Test
    fun insertsAndFindsOneHouseQuoteByMemberId() {
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
                id = UUID.randomUUID(),
                ancillaryArea = 42,
                yearOfConstruction = 1995,
                extraBuildings = listOf(
                    ExtraBuilding(
                        type = ExtraBuildingType.ATTEFALL,
                        area = 20,
                        displayName = "Foo",
                        hasWaterConnected = false
                    )
                ),
                numberOfBathrooms = 2,
                isSubleted = false
            ),
            initiatedFrom = QuoteInitiatedFrom.APP,
            attributedTo = Partner.HEDVIG,
            id = UUID.randomUUID(),
            currentInsurer = null,
            memberId = "123456",
            state = QuoteState.SIGNED
        )
        quoteDao.insert(quote, timestamp)
        assertQuotesDeepEqualExceptInternalId(quote, quoteDao.findOneByMemberId(quote.memberId!!))
    }

    @Test
    fun insertsAndFindsOneApartmentQuoteByMemberId() {
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
        assertQuotesDeepEqualExceptInternalId(quote, quoteDao.findOneByMemberId(quote.memberId!!))
    }

    @Test
    fun insertsAndFindsHouseQuotesByMemberId() {
        val quoteDao = QuoteRepositoryImpl(jdbiRule.jdbi)

        val timestamp = Instant.now()
        val quote1 = Quote(
            id = UUID.fromString("4c1f22b6-0aab-4c9c-a00b-fd06af9fe84e"),
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
                id = UUID.randomUUID(),
                ancillaryArea = 42,
                yearOfConstruction = 1995,
                extraBuildings = listOf(
                    ExtraBuilding(
                        type = ExtraBuildingType.ATTEFALL,
                        area = 20,
                        displayName = "Foo",
                        hasWaterConnected = false
                    )
                ),
                numberOfBathrooms = 2,
                isSubleted = false
            ),
            initiatedFrom = QuoteInitiatedFrom.APP,
            attributedTo = Partner.HEDVIG,
            currentInsurer = null,
            memberId = "123456",
            state = QuoteState.SIGNED
        )
        val quote2 = Quote(
            id = UUID.fromString("bfc61528-bdca-45fe-9111-0e4549ed07d4"),
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
                id = UUID.randomUUID(),
                ancillaryArea = 42,
                yearOfConstruction = 1995,
                extraBuildings = listOf(
                    ExtraBuilding(
                        type = ExtraBuildingType.ATTEFALL,
                        area = 20,
                        displayName = "Foo",
                        hasWaterConnected = false
                    )
                ),
                numberOfBathrooms = 2,
                isSubleted = false
            ),
            initiatedFrom = QuoteInitiatedFrom.APP,
            attributedTo = Partner.HEDVIG,
            currentInsurer = null,
            memberId = "123456",
            state = QuoteState.SIGNED
        )
        quoteDao.insert(quote1, timestamp)
        quoteDao.insert(quote2, timestamp)
        val result = quoteDao.findByMemberId(quote1.memberId!!)
        assertQuotesDeepEqualExceptInternalId(quote1, result[0])
        assertQuotesDeepEqualExceptInternalId(quote2, result[1])
    }

    @Test
    fun insertsAndFindsApartmentQuotesByMemberId() {
        val quoteDao = QuoteRepositoryImpl(jdbiRule.jdbi)

        val timestamp = Instant.now()
        val quote1 = Quote(
            id = UUID.fromString("4c1f22b6-0aab-4c9c-a00b-fd06af9fe84e"),
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
            currentInsurer = null,
            memberId = "123456",
            createdAt = timestamp,
            state = QuoteState.INCOMPLETE
        )
        val quote2 = Quote(
            id = UUID.fromString("bfc61528-bdca-45fe-9111-0e4549ed07d4"),
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
            currentInsurer = null,
            memberId = "123456",
            createdAt = timestamp,
            state = QuoteState.INCOMPLETE
        )
        quoteDao.insert(quote1, timestamp)
        quoteDao.insert(quote2, timestamp)
        val result = quoteDao.findByMemberId(quote1.memberId!!)
        assertQuotesDeepEqualExceptInternalId(quote1, result[0])
        assertQuotesDeepEqualExceptInternalId(quote2, result[1])
    }

    @Test
    fun updatesHouseQuotes() {
        val quoteDao = QuoteRepositoryImpl(jdbiRule.jdbi)

        val timestamp = Instant.now()
        val quote = Quote(
            id = UUID.randomUUID(),
            createdAt = timestamp,
            productType = ProductType.APARTMENT,
            state = QuoteState.QUOTED,
            initiatedFrom = QuoteInitiatedFrom.APP,
            attributedTo = Partner.HEDVIG,
            data = HouseData(
                firstName = "Sherlock",
                lastName = "Holmes",
                ssn = "199003041234",
                street = "221 Baker street",
                zipCode = "11216",
                livingSpace = 33,
                householdSize = 4,
                city = "London",
                id = UUID.randomUUID(),
                ancillaryArea = 42,
                yearOfConstruction = 1995,
                extraBuildings = listOf(
                    ExtraBuilding(
                        type = ExtraBuildingType.ATTEFALL,
                        area = 20,
                        displayName = "Foo",
                        hasWaterConnected = false
                    )
                ),
                numberOfBathrooms = 2,
                isSubleted = false
            ),
            currentInsurer = null
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
