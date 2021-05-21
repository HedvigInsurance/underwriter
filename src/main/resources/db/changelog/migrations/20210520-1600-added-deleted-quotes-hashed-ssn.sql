--liquibase formatted.sql

--changeset ostenforshed:20210520-1600-added-deleted-quotes-hashed-ssn.sql

ALTER TABLE deleted_quotes
    ADD COLUMN hashed_ssn varchar(32) NULL;

--rollback ALTER TABLE deleted_quotes DROP COLUMN hashed_ssn