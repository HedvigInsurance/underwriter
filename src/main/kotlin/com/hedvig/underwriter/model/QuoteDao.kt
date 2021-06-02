package com.hedvig.underwriter.model

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.customizer.BindList
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.time.Instant
import java.util.UUID

interface QuoteDao {
    @SqlUpdate(
        """
            INSERT INTO quote_revisions (
                master_quote_id,
                timestamp,
                validity,
                product_type,
                state,
                attributed_to,
                current_insurer,
                start_date,
                price,
                currency,
                price_from,
                quote_apartment_data_id,
                quote_house_data_id,
                quote_norwegian_home_contents_data_id,
                quote_norwegian_travel_data_id,
                quote_danish_home_contents_data_id,
                quote_danish_accident_data_id,
                quote_danish_travel_data_id,
                member_id,
                breached_underwriting_guidelines,
                underwriting_guidelines_bypassed_by,
                originating_product_id,
                agreement_id,
                contract_id,
                data_collection_id,
                sign_from_hope_triggered_by
            )
            VALUES (
                :masterQuoteId,
                :timestamp,
                :validity,
                :productType,
                :state,
                :attributedTo,
                :currentInsurer,
                :startDate,
                :price,
                :currency,
                :priceFrom,
                :quoteApartmentDataId,
                :quoteHouseDataId,
                :quoteNorwegianHomeContentsDataId,
                :quoteNorwegianTravelDataId,
                :quoteDanishHomeContentsDataId,
                :quoteDanishAccidentDataId,
                :quoteDanishTravelDataId,
                :memberId,
                :breachedUnderwritingGuidelines,
                :underwritingGuidelinesBypassedBy,
                :originatingProductId,
                :agreementId,
                :contractId,
                :dataCollectionId,
                :signFromHopeTriggeredBy
            )
            RETURNING *
        """
    )
    @GetGeneratedKeys("id")
    fun insert(@BindBean quote: DatabaseQuoteRevision, @Bind timestamp: Instant): DatabaseQuoteRevision

    @SqlQuery(
        """
            SELECT
            qr.*,
            mq.created_at,
            mq.initiated_from

            FROM quote_revisions qr
            INNER JOIN master_quotes mq
                ON mq.id = qr.master_quote_id
            WHERE qr.master_quote_id = :quoteId
            ORDER BY id
        """
    )
    fun findRevisions(@Bind quoteId: UUID): List<DatabaseQuoteRevision>

    @SqlQuery(
        """
            SELECT
            DISTINCT ON (qr.master_quote_id)

            qr.*,
            mq.created_at,
            mq.initiated_from

            FROM quote_revisions qr
            INNER JOIN master_quotes mq
                ON mq.id = qr.master_quote_id
            WHERE qr.master_quote_id = :quoteId
            ORDER BY qr.master_quote_id ASC, qr.id DESC
        """
    )
    fun find(@Bind quoteId: UUID): DatabaseQuoteRevision?

    @SqlQuery(
        """
            SELECT
            DISTINCT ON (qr.master_quote_id)

            qr.*,
            mq.created_at,
            mq.initiated_from

            FROM quote_revisions qr
            INNER JOIN master_quotes mq
                ON mq.id = qr.master_quote_id
            WHERE qr.master_quote_id in (<quoteIds>)
            ORDER BY qr.master_quote_id ASC, qr.id DESC
        """
    )
    fun find(
        @BindList(
            "quoteIds",
            onEmpty = BindList.EmptyHandling.NULL_STRING
        ) quoteIds: List<UUID>
    ): List<DatabaseQuoteRevision>

    @SqlUpdate(
        """
            INSERT INTO quote_revision_apartment_data
            (id, ssn, birth_date, first_name, last_name, email, phone_number, street, city, zip_code, household_size, living_space, sub_type)
            VALUES
            (:id, :ssn, :birthDate, :firstName, :lastName, :email, :phoneNumber, :street, :city, :zipCode, :householdSize, :livingSpace, :subType)
            RETURNING *
        """
    )
    @GetGeneratedKeys("internal_id")
    fun insert(@BindBean quoteData: SwedishApartmentData): SwedishApartmentData

    @SqlQuery("""SELECT * FROM quote_revision_apartment_data WHERE internal_id = :id""")
    fun findApartmentQuoteData(@Bind id: Int): SwedishApartmentData?

    @SqlQuery(
        """
            SELECT DISTINCT
            qr.master_quote_id
            FROM quote_revision_apartment_data qrd
            INNER JOIN quote_revisions qr
            ON qrd.internal_id = qr.quote_apartment_data_id
            WHERE 
            qrd.street = :street
            AND qrd.zip_code = :zipCode
        """
    )
    fun findQuoteIdsBySwedishApartmentDataAddress(@Bind street: String, @Bind zipCode: String): List<UUID>

    @SqlQuery(
        """
            SELECT DISTINCT
            qr.master_quote_id
            FROM quote_revision_house_data qrd
            INNER JOIN quote_revisions qr
            ON qrd.internal_id = qr.quote_house_data_id
            WHERE 
            qrd.street = :street
            AND qrd.zip_code = :zipCode
        """
    )
    fun findQuoteIdsBySwedishHouseDataAddress(@Bind street: String, @Bind zipCode: String): List<UUID>

    @SqlQuery(
        """
            SELECT DISTINCT
            qr.master_quote_id
            FROM quote_revision_norwegian_home_contents_data qrd
            INNER JOIN quote_revisions qr
            ON qrd.internal_id = qr.quote_norwegian_home_contents_data_id
            WHERE 
            qrd.street = :street
            AND qrd.zip_code = :zipCode
        """
    )
    fun findQuoteIdsByNorwegianHomeContentsDataAddress(@Bind street: String, @Bind zipCode: String): List<UUID>

    @SqlQuery(
        """
            SELECT DISTINCT
            qr.master_quote_id
            FROM quote_revision_danish_home_contents_data qrd
            INNER JOIN quote_revisions qr
            ON qrd.internal_id = qr.quote_danish_home_contents_data_id
            WHERE 
            qrd.street = :street
            AND qrd.zip_code = :zipCode
        """
    )
    fun findQuoteIdsByDanishHomeContentsDataAddress(@Bind street: String, @Bind zipCode: String): List<UUID>

    @SqlQuery(
        """
            SELECT DISTINCT
            qr.master_quote_id
            FROM quote_revision_danish_accident_data qrd
            INNER JOIN quote_revisions qr
            ON qrd.internal_id = qr.quote_danish_accident_data_id
            WHERE 
            qrd.street = :street
            AND qrd.zip_code = :zipCode
        """
    )
    fun findQuoteIdsByDanishAccidentDataAddress(@Bind street: String, @Bind zipCode: String): List<UUID>

    @SqlQuery(
        """
            SELECT DISTINCT
            qr.master_quote_id
            FROM quote_revision_danish_travel_data qrd
            INNER JOIN quote_revisions qr
            ON qrd.internal_id = qr.quote_danish_travel_data_id
            WHERE 
            qrd.street = :street
            AND qrd.zip_code = :zipCode
        """
    )
    fun findQuoteIdsByDanishTravelDataAddress(@Bind street: String, @Bind zipCode: String): List<UUID>

    @SqlQuery(
        """
            SELECT
            DISTINCT ON (qr.master_quote_id)

            qr.*, 
            mq.created_at, 
            mq.initiated_from 
            
            FROM master_quotes mq
            JOIN quote_revisions qr
            ON qr.master_quote_id = mq.id 
            
            WHERE qr.member_id = :memberId
            ORDER BY qr.master_quote_id ASC, qr.id DESC
            LIMIT 1
        """
    )
    fun findOneByMemberId(@Bind memberId: String): DatabaseQuoteRevision?

    @SqlQuery(
        """
            SELECT
            DISTINCT ON (qr.master_quote_id)

            qr.*, 
            mq.created_at, 
            mq.initiated_from 
            
            FROM master_quotes mq
            JOIN quote_revisions qr
            ON qr.master_quote_id = mq.id 
            
            WHERE qr.member_id = :memberId
            ORDER BY qr.master_quote_id ASC, qr.id DESC
        """
    )
    fun findByMemberId(@Bind memberId: String): List<DatabaseQuoteRevision>

    @SqlUpdate(
        """
            INSERT INTO quote_revision_house_data
            (
                id,
                ssn,
                birth_date,
                first_name,
                last_name,
                email,
                phone_number,
                street,
                city,
                zip_code,
                household_size,
                living_space,
                ancillary_area,
                year_of_construction,
                number_of_bathrooms,
                extra_buildings,
                is_subleted,
                floor
            )
            VALUES
            (
                :id,
                :ssn,
                :birthDate,
                :firstName,
                :lastName,
                :email,
                :phoneNumber,
                :street,
                :city,
                :zipCode,
                :householdSize,
                :livingSpace,
                :ancillaryArea,
                :yearOfConstruction,
                :numberOfBathrooms,
                :extraBuildings,
                :isSubleted,
                :floor
            )
            RETURNING *
        """
    )
    @GetGeneratedKeys("internal_id")
    fun insert(@BindBean data: SwedishHouseData): SwedishHouseData

    @SqlQuery(
        """
            SELECT * FROM quote_revision_house_data WHERE internal_id = :id
        """
    )
    fun findHouseQuoteData(@Bind id: Int): SwedishHouseData?

    @SqlUpdate(
        """
            INSERT INTO quote_revision_norwegian_home_contents_data
            (
                id,
                ssn,
                birth_date,
                first_name,
                last_name,
                email,
                phone_number,
                street,
                city,
                zip_code,
                living_space,
                co_insured,
                type,
                is_youth
            )
            VALUES
            (
                :id,
                :ssn,
                :birthDate,
                :firstName,
                :lastName,
                :email,
                :phoneNumber,
                :street,
                :city,
                :zipCode,
                :livingSpace,
                :coInsured,
                :type,
                :isYouth
            )
            RETURNING *
        """
    )
    @GetGeneratedKeys("internal_id")
    fun insert(@BindBean data: NorwegianHomeContentsData): NorwegianHomeContentsData

    @SqlQuery(
        """
            SELECT * FROM quote_revision_norwegian_home_contents_data WHERE internal_id = :id
        """
    )
    fun findNorwegianHomeContentsQuoteData(@Bind id: Int): NorwegianHomeContentsData?

    @SqlUpdate(
        """
            INSERT INTO quote_revision_norwegian_travel_data
            (
                id,
                ssn,
                birth_date,
                first_name,
                last_name,
                email,
                phone_number,
                co_insured,
                is_youth
            )
            VALUES
            (
                :id,
                :ssn,
                :birthDate,
                :firstName,
                :lastName,
                :email,
                :phoneNumber,
                :coInsured,
                :isYouth
            )
            RETURNING *
        """
    )
    @GetGeneratedKeys("internal_id")
    fun insert(@BindBean data: NorwegianTravelData): NorwegianTravelData

    @SqlQuery(
        """
            SELECT * FROM quote_revision_norwegian_travel_data WHERE internal_id = :id
        """
    )
    fun findNorwegianTravelQuoteData(@Bind id: Int): NorwegianTravelData?

    @SqlUpdate(
        """
            INSERT INTO quote_revision_danish_home_contents_data
            (
                id,
                ssn,
                birth_date,
                first_name,
                last_name,
                email,
                phone_number,
                street,
                apartment,
                floor,
                zip_code,
                city,
                living_space,
                co_insured,                
                is_student,                
                type,
                bbr_id
            )
            VALUES
            (
                :id,
                :ssn,
                :birthDate,
                :firstName,
                :lastName,
                :email,
                :phoneNumber,
                :street,
                :apartment,
                :floor,
                :zipCode,
                :city,
                :livingSpace,
                :coInsured,
                :isStudent,
                :type,
                :bbrId
            )
            RETURNING *
        """
    )
    @GetGeneratedKeys("internal_id")
    fun insert(@BindBean data: DanishHomeContentsData): DanishHomeContentsData

    @SqlQuery(
        """
            SELECT * FROM quote_revision_danish_home_contents_data WHERE internal_id = :id
        """
    )
    fun findDanishHomeContentsQuoteData(@Bind id: Int): DanishHomeContentsData?

    @SqlUpdate(
        """
            INSERT INTO quote_revision_danish_accident_data
            (
                id,
                ssn,
                birth_date,
                first_name,
                last_name,
                email,
                phone_number,
                street,
                zip_code,
                apartment,
                floor,
                bbr_id,
                city,
                co_insured,                
                is_student      
            )
            VALUES
            (
                :id,
                :ssn,
                :birthDate,
                :firstName,
                :lastName,
                :email,
                :phoneNumber,
                :street,
                :zipCode,
                :apartment,
                :floor,
                :bbrId,
                :city,
                :coInsured,
                :isStudent
            )
            RETURNING *
        """
    )
    @GetGeneratedKeys("internal_id")
    fun insert(@BindBean data: DanishAccidentData): DanishAccidentData

    @SqlQuery(
        """
            SELECT * FROM quote_revision_danish_accident_data WHERE internal_id = :id
        """
    )
    fun findDanishAccidentQuoteData(@Bind id: Int): DanishAccidentData?

    @SqlUpdate(
        """
            INSERT INTO quote_revision_danish_travel_data
            (
                id,
                ssn,
                birth_date,
                first_name,
                last_name,
                email,
                phone_number,
                street,
                zip_code,
                bbr_id,
                apartment,
                floor,
                city,
                co_insured,                
                is_student      
            )
            VALUES
            (
                :id,
                :ssn,
                :birthDate,
                :firstName,
                :lastName,
                :email,
                :phoneNumber,
                :street,
                :zipCode,
                :bbrId,
                :apartment,
                :floor,
                :city,
                :coInsured,
                :isStudent
            )
            RETURNING *
        """
    )
    @GetGeneratedKeys("internal_id")
    fun insert(@BindBean data: DanishTravelData): DanishTravelData

    @SqlQuery(
        """
            SELECT * FROM quote_revision_danish_travel_data WHERE internal_id = :id
        """
    )
    fun findDanishTravelQuoteData(@Bind id: Int): DanishTravelData?

    @SqlUpdate(
        """
            INSERT INTO master_quotes (id, initiated_from, created_at) VALUES (:quoteId, :initiatedFrom, :createdAt)
        """
    )
    fun insertMasterQuote(
        @Bind quoteId: UUID,
        @Bind initiatedFrom: QuoteInitiatedFrom,
        @Bind createdAt: Instant = Instant.now()
    )

    @SqlQuery(
        """
            SELECT
            DISTINCT ON (qr.master_quote_id)

            qr.*, 
            mq.created_at, 
            mq.initiated_from 
            
            FROM master_quotes mq
            JOIN quote_revisions qr
            ON qr.master_quote_id = mq.id 
            
            --For some quotes the contract_id is saved in the agreement_ud field
            --remove the _agreement_id check_ below once this is fixed.
            WHERE (qr.contract_id = :contractId OR qr.agreement_id = :contractId) AND qr.originating_product_id IS NULL
            ORDER BY qr.master_quote_id ASC, qr.id DESC
        """
    )
    fun findByContractId(@Bind contractId: UUID): DatabaseQuoteRevision?

    @SqlQuery(
        """
            SELECT
                id
            FROM (
                SELECT DISTINCT ON (qr.master_quote_id)
                    mq.id,
                    qr.agreement_id
                FROM master_quotes mq
                JOIN quote_revisions qr ON qr.master_quote_id = mq.id 
                WHERE 
                    created_at < :before
                ORDER BY qr.master_quote_id ASC, qr.id DESC
            ) a
            WHERE 
                agreement_id is null
        """
    )
    fun findOldQuoteIdsToDelete(@Bind before: Instant): List<UUID>

    @SqlUpdate(
        """
            INSERT INTO deleted_quotes (
                quote_id,
                created_at,
                deleted_at,
                type,
                member_id,
                hashed_ssn,
                quote,
                revs
            )
            VALUES (
                :quoteId,
                :createdAt,
                :deletedAt,
                :type,
                :memberId,
                :hashedSsn,
                CAST(:quote as jsonb),
                CAST(:revs as jsonb)
            )
        """
    )
    fun insert(@BindBean deletedQuote: DeletedQuote)

    @SqlUpdate(
        """
            DELETE FROM master_quotes 
            WHERE id = :id
        """
    )
    fun deleteMasterQuote(@Bind id: UUID)

    @SqlUpdate(
        """
            DELETE FROM quote_revisions 
            WHERE master_quote_id = :masterQuoteId
        """
    )
    fun deleteQuoteRevisions(@Bind masterQuoteId: UUID)

    @SqlUpdate(
        """
            DELETE FROM quote_revision_apartment_data 
            WHERE internal_id = :internalId
        """
    )
    fun deleteApartmentData(@Bind internalId: Int)

    @SqlUpdate(
        """
            DELETE FROM quote_revision_house_data 
            WHERE internal_id = :internalId
        """
    )
    fun deleteHouseData(@Bind internalId: Int)

    @SqlUpdate(
        """
            DELETE FROM quote_revision_danish_accident_data 
            WHERE internal_id = :internalId
        """
    )
    fun deleteDanishAccidentData(@Bind internalId: Int)

    @SqlUpdate(
        """
            DELETE FROM quote_revision_danish_home_contents_data 
            WHERE internal_id = :internalId
        """
    )
    fun deleteDanishHomeContentData(@Bind internalId: Int)

    @SqlUpdate(
        """
            DELETE FROM quote_revision_danish_travel_data 
            WHERE internal_id = :internalId
        """
    )
    fun deleteDanishTravelData(@Bind internalId: Int)

    @SqlUpdate(
        """
            DELETE FROM quote_revision_norwegian_home_contents_data 
            WHERE internal_id = :internalId
        """
    )
    fun deleteNorwegianHomeContentData(@Bind internalId: Int)

    @SqlUpdate(
        """
            DELETE FROM quote_revision_norwegian_travel_data 
            WHERE internal_id = :internalId
        """
    )
    fun deleteNorwegianTravelData(@Bind internalId: Int)

    @SqlUpdate(
        """
                INSERT INTO quote_line_item (revision_id, type, subType, amount) VALUES(:revisionId, :type, :subType, :amount)
            """
    )
    fun insertLineItem(@BindBean lineItem: LineItem)

    @SqlQuery(
        """
                SELECT
                qli.*
                FROM quote_line_item qli
                WHERE qli.revision_id = :revisionId
            """
    )
    fun findLineItems(@Bind revisionId: Int): List<LineItem>

    @SqlUpdate(
        """
                DELETE FROM quote_line_item 
                WHERE revision_id = :revisionId
            """
    )
    fun deleteLineItems(@Bind revisionId: Int)
}
