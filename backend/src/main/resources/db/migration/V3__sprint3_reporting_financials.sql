ALTER TABLE tenders ADD COLUMN final_amount NUMERIC(18,2);
ALTER TABLE tenders ADD COLUMN loss_reason TEXT;
ALTER TABLE tenders ADD COLUMN result_date DATE;

CREATE INDEX idx_tenders_publication_date ON tenders(publication_date);
CREATE INDEX idx_tenders_result_date ON tenders(result_date);
CREATE INDEX idx_tenders_organization ON tenders(organization);
