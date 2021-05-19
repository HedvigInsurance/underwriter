package com.hedvig.underwriter.model

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.inTransactionUnchecked
import org.jdbi.v3.sqlobject.kotlin.attach
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class QuoteRepositoryImpl(private val jdbi: Jdbi) : QuoteRepository {

    override fun insert(
        quote: Quote,
        timestamp: Instant
    ) = jdbi.useTransaction<RuntimeException> { h ->
        val dao = h.attach<QuoteDao>()
        val quoteData: QuoteData = when (quote.data) {
            is SwedishApartmentData -> dao.insert(quote.data)
            is SwedishHouseData -> dao.insert(quote.data)
            is NorwegianHomeContentsData -> dao.insert(quote.data)
            is NorwegianTravelData -> dao.insert(quote.data)
            is DanishHomeContentsData -> dao.insert(quote.data)
            is DanishAccidentData -> dao.insert(quote.data)
            is DanishTravelData -> dao.insert(quote.data)
        }

        dao.insertMasterQuote(quote.id, quote.initiatedFrom, timestamp)

        val databaseQuote = DatabaseQuoteRevision.from(quote.copy(data = quoteData))
        val revision = dao.insert(databaseQuote, timestamp)
        quote.lineItems.forEach { dao.insertLineItem(it.copy(revisionId = revision.id)) }
    }

    override fun find(quoteId: UUID): Quote? =
        jdbi.inTransaction<Quote?, RuntimeException> { h -> find(quoteId, h) }

    fun find(quoteId: UUID, h: Handle): Quote? {
        val dao = h.attach<QuoteDao>()
        val databaseQuote = dao.find(quoteId) ?: return null
        return loadQuoteData(databaseQuote, dao)
    }

    override fun findQuoteRevisions(quoteId: UUID): List<Quote> =
        jdbi.inTransaction<List<Quote>, RuntimeException> { h ->
            val dao = h.attach<QuoteDao>()

            dao.findRevisions(quoteId)
                .map { loadQuoteData(it, dao) }
                .filterNotNull()
                .toList()
        }

    override fun findQuotes(quoteIds: List<UUID>): List<Quote> =
        jdbi.inTransaction<List<Quote>, RuntimeException> { h -> findQuotes(quoteIds, h) }

    private fun findQuotes(quoteIds: List<UUID>, h: Handle): List<Quote> {
        val dao = h.attach<QuoteDao>()
        val databaseQuote = dao.find(quoteIds)
        return databaseQuote.mapNotNull { loadQuoteData(it, dao) }
    }

    override fun findByMemberId(memberId: String): List<Quote> =
        jdbi.inTransaction<List<Quote>, RuntimeException> { h -> findByMemberId(memberId, h) }

    fun findByMemberId(memberId: String, h: Handle): List<Quote> {
        val dao = h.attach<QuoteDao>()
        return dao.findByMemberId(memberId)
            .mapNotNull { databaseQuote -> loadQuoteData(databaseQuote, dao) }
    }

    override fun findOneByMemberId(memberId: String): Quote? =
        jdbi.inTransaction<Quote?, RuntimeException> { h -> findOneByMemberId(memberId, h) }

    fun findOneByMemberId(memberId: String, h: Handle): Quote? {
        val dao = h.attach<QuoteDao>()
        val databaseQuote = dao.findOneByMemberId(memberId) ?: return null
        return loadQuoteData(databaseQuote, dao)
    }

    override fun findLatestOneByMemberId(memberId: String): Quote? =
        jdbi.inTransaction<Quote?, RuntimeException> { h -> findLatestOneByMemberId(memberId, h) }

    fun findLatestOneByMemberId(memberId: String, h: Handle): Quote? {
        val dao = h.attach<QuoteDao>()
        val databaseQuotes = dao.findByMemberId(memberId)
        val latestQuote = databaseQuotes.maxBy { it.timestamp }
        return latestQuote?.let { quote -> loadQuoteData(quote, dao) }
    }

    override fun expireQuote(id: UUID): Quote? {
        return jdbi.inTransaction<Quote?, RuntimeException> { h -> expireQuote(id, h) }
    }

    fun expireQuote(id: UUID, h: Handle): Quote? {
        val dao = h.attach<QuoteDao>()
        val databaseQuotes = dao.find(id) ?: return null
        val expiredQuote = databaseQuotes.copy(validity = 0)
        update(expiredQuote, Instant.now(), h)
        return loadQuoteData(expiredQuote, dao)
    }

    fun loadQuoteData(databaseQuote: DatabaseQuoteRevision, dao: QuoteDao): Quote? {
        val quoteData: QuoteData = when {
            databaseQuote.quoteApartmentDataId != null -> dao.findApartmentQuoteData(databaseQuote.quoteApartmentDataId)
            databaseQuote.quoteHouseDataId != null -> dao.findHouseQuoteData(databaseQuote.quoteHouseDataId)
            databaseQuote.quoteNorwegianHomeContentsDataId != null -> dao.findNorwegianHomeContentsQuoteData(
                databaseQuote.quoteNorwegianHomeContentsDataId
            )
            databaseQuote.quoteNorwegianTravelDataId != null -> dao.findNorwegianTravelQuoteData(
                databaseQuote.quoteNorwegianTravelDataId
            )
            databaseQuote.quoteDanishHomeContentsDataId != null -> dao.findDanishHomeContentsQuoteData(
                databaseQuote.quoteDanishHomeContentsDataId
            )
            databaseQuote.quoteDanishAccidentDataId != null -> dao.findDanishAccidentQuoteData(
                databaseQuote.quoteDanishAccidentDataId
            )
            databaseQuote.quoteDanishTravelDataId != null -> dao.findDanishTravelQuoteData(
                databaseQuote.quoteDanishTravelDataId
            )
            else -> throw IllegalStateException("Quote must have details set (but was not). Quote ${databaseQuote.masterQuoteId} with quote revision ${databaseQuote.id}")
        }!!

        val lineItems = dao.findLineItems(databaseQuote.id!!)

        return Quote(
            id = databaseQuote.masterQuoteId,
            createdAt = databaseQuote.createdAt!!,
            updatedAt = databaseQuote.timestamp,
            price = databaseQuote.price,
            currency = databaseQuote.currency,
            priceFrom = databaseQuote.priceFrom,
            productType = databaseQuote.productType,
            state = databaseQuote.state,
            initiatedFrom = databaseQuote.initiatedFrom!!,
            attributedTo = databaseQuote.attributedTo,
            data = quoteData,
            currentInsurer = databaseQuote.currentInsurer,
            startDate = databaseQuote.startDate,
            validity = databaseQuote.validity,
            memberId = databaseQuote.memberId,
            breachedUnderwritingGuidelines = databaseQuote.breachedUnderwritingGuidelines,
            underwritingGuidelinesBypassedBy = databaseQuote.underwritingGuidelinesBypassedBy,
            originatingProductId = databaseQuote.originatingProductId,
            agreementId = databaseQuote.agreementId,
            dataCollectionId = databaseQuote.dataCollectionId,
            contractId = databaseQuote.contractId,
            lineItems = lineItems
        )
    }

    override fun update(updatedQuote: Quote, timestamp: Instant): Quote =
        jdbi.inTransaction<Quote, RuntimeException> { h ->
            update(updatedQuote, timestamp, h)
            find(updatedQuote.id, h)!!
        }

    override fun findByContractId(contractId: UUID): Quote? {
        return jdbi.inTransactionUnchecked { h ->
            val dao = h.attach<QuoteDao>()
            val quoteRevision = dao.findByContractId(contractId)
            quoteRevision?.let { loadQuoteData(it, dao) }
        }
    }

    override fun findQuotesByAddress(street: String, zipCode: String, type: QuoteData): List<Quote> {
        return jdbi.inTransaction<List<Quote>, RuntimeException> { h ->
            val dao = h.attach<QuoteDao>()
            val quoteDatas =
                when (type) {
                    is SwedishApartmentData -> dao.findQuoteIdsBySwedishApartmentDataAddress(street, zipCode)
                    is SwedishHouseData -> dao.findQuoteIdsBySwedishHouseDataAddress(street, zipCode)
                    is NorwegianHomeContentsData -> dao.findQuoteIdsByNorwegianHomeContentsDataAddress(
                        street,
                        zipCode
                    )
                    is NorwegianTravelData -> emptyList()
                    is DanishHomeContentsData -> dao.findQuoteIdsByDanishHomeContentsDataAddress(street, zipCode)
                    is DanishAccidentData -> dao.findQuoteIdsByDanishAccidentDataAddress(street, zipCode)
                    is DanishTravelData -> dao.findQuoteIdsByDanishTravelDataAddress(street, zipCode)
                }

            findQuotes(quoteDatas, h)
        }
    }

    override fun findOldQuotesToDelete(before: Instant): List<Quote> {
        return jdbi.inTransaction<List<Quote>, RuntimeException> { h ->
            val dao = h.attach<QuoteDao>()
            val ids = dao.findOldQuoteIdsToDelete(before)
            findQuotes(ids, h)
        }
    }

    private fun update(updatedQuote: Quote, timestamp: Instant, h: Handle) {
        val dao = h.attach<QuoteDao>()
        val quoteData: QuoteData = when (updatedQuote.data) {
            is SwedishApartmentData -> dao.insert(updatedQuote.data)
            is SwedishHouseData -> dao.insert(updatedQuote.data)
            is NorwegianHomeContentsData -> dao.insert(updatedQuote.data)
            is NorwegianTravelData -> dao.insert(updatedQuote.data)
            is DanishHomeContentsData -> dao.insert(updatedQuote.data)
            is DanishAccidentData -> dao.insert(updatedQuote.data)
            is DanishTravelData -> dao.insert(updatedQuote.data)
        }
        val revision = dao.insert(DatabaseQuoteRevision.from(updatedQuote.copy(data = quoteData)), timestamp)
        updatedQuote.lineItems.forEach { dao.insertLineItem(it.copy(revisionId = revision.id)) }
    }

    private fun update(updatedQuote: DatabaseQuoteRevision, timestamp: Instant, h: Handle) {
        val dao = h.attach<QuoteDao>()
        dao.insert(updatedQuote, timestamp)
    }

    override fun delete(quote: Quote) {
        jdbi.inTransaction<Unit, RuntimeException> { h ->

            // Delete any sessions for quote first, due to the FK
            val sessionDao = h.attach<SignSessionDao>()
            sessionDao.delete(quote.id)

            val dao = h.attach<QuoteDao>()
            val revs = dao.findRevisions(quote.id)

            // Delete the line-items before the revisions, due to the FK
            revs.forEach { it.id?.let { dao.deleteLineItems(it) } }

            dao.deleteQuoteRevisions(quote.id)
            dao.deleteMasterQuote(quote.id)
            revs.forEach {
                it.quoteApartmentDataId?.let { dao.deleteApartmentData(it) }
                it.quoteHouseDataId?.let { dao.deleteHouseData(it) }
                it.quoteNorwegianHomeContentsDataId?.let { dao.deleteNorwegianHomeContentData(it) }
                it.quoteNorwegianTravelDataId?.let { dao.deleteNorwegianTravelData(it) }
                it.quoteDanishAccidentDataId?.let { dao.deleteDanishAccidentData(it) }
                it.quoteDanishHomeContentsDataId?.let { dao.deleteDanishHomeContentData(it) }
                it.quoteDanishTravelDataId?.let { dao.deleteDanishTravelData(it) }
            }
        }
    }

    override fun insert(deletedQuote: DeletedQuote) =
        jdbi.useTransaction<RuntimeException> { h ->
            val dao = h.attach<QuoteDao>()
            dao.insert(deletedQuote)
        }
}
