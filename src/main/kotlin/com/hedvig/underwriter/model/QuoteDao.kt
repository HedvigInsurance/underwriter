package com.hedvig.underwriter.model

import java.time.Instant
import java.util.UUID
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.customizer.BindList
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

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
                quote_apartment_data_id,
                quote_house_data_id,
                quote_norwegian_home_contents_data_id,
                quote_norwegian_travel_data_id,
                member_id,
                breached_underwriting_guidelines,
                underwriting_guidelines_bypassed_by,
                originating_product_id,
                signed_product_id,
                data_collection_id
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
                :quoteApartmentDataId,
                :quoteHouseDataId,
                :quoteNorwegianHomeContentsDataId,
                :quoteNorwegianTravelDataId,
                :memberId,
                :breachedUnderwritingGuidelines,
                :underwritingGuidelinesBypassedBy,
                :originatingProductId,
                :signedProductId,
                :dataCollectionId
            )
            RETURNING *
    """
    )
    @GetGeneratedKeys("id")
    fun insert(@BindBean quote: DatabaseQuoteRevision, @Bind timestamp: Instant): DatabaseQuoteRevision

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
    fun find(@BindList("quoteIds", onEmpty = BindList.EmptyHandling.NULL_STRING) quoteIds: List<UUID>): List<DatabaseQuoteRevision>

    @SqlUpdate(
        """
            INSERT INTO quote_revision_apartment_data
            (id, ssn, birth_date, first_name, last_name, email, street, city, zip_code, household_size, living_space, sub_type)
            VALUES
            (:id, :ssn, :birthDate, :firstName, :lastName, :email, :street, :city, :zipCode, :householdSize, :livingSpace, :subType)
            RETURNING *
        """
    )
    @GetGeneratedKeys("internal_id")
    fun insert(@BindBean quoteData: SwedishApartmentData): SwedishApartmentData

    @SqlQuery("""SELECT * FROM quote_revision_apartment_data WHERE internal_id = :id""")
    fun findApartmentQuoteData(@Bind id: Int): SwedishApartmentData?

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
                street,
                city,
                zip_code,
                living_space,
                co_insured ,
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
        INSERT INTO master_quotes (id, initiated_from, created_at) VALUES (:quoteId, :initiatedFrom, :createdAt)
    """
    )
    fun insertMasterQuote(
        @Bind quoteId: UUID,
        @Bind initiatedFrom: QuoteInitiatedFrom,
        @Bind createdAt: Instant = Instant.now()
    )
}
