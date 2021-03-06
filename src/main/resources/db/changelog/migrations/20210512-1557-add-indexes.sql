--liquibase formatted.sql

--changeset ostenforshed:20210512-1557-add-indexes.sql

CREATE INDEX idx_quote_revisions_member_id ON quote_revisions(member_id);
CREATE INDEX idx_master_quotes_created_at ON master_quotes(created_at);
CREATE INDEX idx_quote_revision_apartment_data_street ON quote_revision_apartment_data(street);
CREATE INDEX idx_quote_revision_house_data_street ON quote_revision_house_data(street);
CREATE INDEX idx_quote_revision_norwegian_home_contents_data_street ON quote_revision_norwegian_home_contents_data(street);
CREATE INDEX idx_quote_revision_danish_home_contents_data_street ON quote_revision_danish_home_contents_data(street);
CREATE INDEX idx_quote_revision_danish_accident_data_street ON quote_revision_danish_accident_data(street);
CREATE INDEX idx_quote_revision_danish_travel_data_street ON quote_revision_danish_travel_data(street);

--rollback DROP INDEX idx_quote_revisions_member_id
--rollback DROP INDEX idx_master_quotes_created_at
--rollback DROP INDEX idx_quote_revision_apartment_data_street
--rollback DROP INDEX idx_quote_revision_house_data_street
--rollback DROP INDEX idx_quote_revision_norwegian_home_contents_data_street
--rollback DROP INDEX idx_quote_revision_danish_home_contents_data_street
--rollback DROP INDEX idx_quote_revision_danish_accident_data_street
--rollback DROP INDEX idx_quote_revision_danish_travel_data_street
