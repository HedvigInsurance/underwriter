--liquibase formatted.sql

--changeset ostenforshed:20210511-1400-quote-revisions-price-from.sql

/*
    The new price_from column will refer to the master_quotes.id if price of that quote is reused.
    To avoid problems deleting quotes it is not declared as a foreign key constraint.
 */

ALTER TABLE quote_revisions
    ADD COLUMN price_from uuid NULL;

COMMENT ON COLUMN quote_revisions.price_from IS 'Reference to master_quotes.id if price is reused from another quote';

--rollback ALTER TABLE quote_revisions DROP COLUMN price_from