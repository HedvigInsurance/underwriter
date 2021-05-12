package com.hedvig.underwriter.model

import java.time.Instant
import java.util.UUID
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

interface SignSessionDao {

    @SqlUpdate(
        """
            INSERT INTO sign_sessions (
                id, created_at
            )
            VALUES (
                :id, :createdAt
            )
    """
    )
    fun insert(id: UUID, @Bind createdAt: Instant = Instant.now())

    @SqlUpdate(
        """
            INSERT INTO sign_session_master_quote (
                sign_session_id, master_quote_id
            ) 
            VALUES (
                :signSessionId, :masterQuoteId
            )
    """
    )
    fun insert(signSessionId: UUID, masterQuoteId: UUID)

    @SqlQuery(
        """
            SELECT master_quote_id 
            FROM sign_session_master_quote
            WHERE sign_session_id = :sessionId
        """
    )
    fun find(sessionId: UUID): List<UUID>

    @SqlUpdate(
        """
                DELETE FROM sign_session_master_quote 
                WHERE master_quote_id = :quoteId
            """
    )
    fun delete(@Bind quoteId: UUID)
}
