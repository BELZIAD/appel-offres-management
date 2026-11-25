CREATE TABLE document_analyses (
    id BIGSERIAL PRIMARY KEY,
    document_version_id BIGINT NOT NULL REFERENCES document_versions(id) ON DELETE CASCADE,
    status VARCHAR(40) NOT NULL,
    extraction_method VARCHAR(40) NOT NULL,
    detected_language VARCHAR(20) NOT NULL,
    page_count INTEGER NOT NULL DEFAULT 0,
    text_length INTEGER NOT NULL DEFAULT 0,
    requires_ocr BOOLEAN NOT NULL DEFAULT FALSE,
    raw_text TEXT,
    normalized_text TEXT,
    analysis_engine_version VARCHAR(80) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_document_analyses_document_version ON document_analyses(document_version_id);
CREATE INDEX idx_document_analyses_status ON document_analyses(status);
CREATE INDEX idx_document_analyses_created_at ON document_analyses(created_at);

CREATE TABLE extracted_tender_information (
    id BIGSERIAL PRIMARY KEY,
    analysis_id BIGINT NOT NULL REFERENCES document_analyses(id) ON DELETE CASCADE,
    field_name VARCHAR(100) NOT NULL,
    field_value TEXT,
    confidence NUMERIC(5,4) NOT NULL DEFAULT 0,
    source_page INTEGER,
    source_excerpt VARCHAR(500),
    validated BOOLEAN NOT NULL DEFAULT FALSE,
    ignored BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_extracted_tender_information_analysis ON extracted_tender_information(analysis_id);

CREATE TABLE extracted_requirements (
    id BIGSERIAL PRIMARY KEY,
    analysis_id BIGINT NOT NULL REFERENCES document_analyses(id) ON DELETE CASCADE,
    category VARCHAR(40) NOT NULL,
    label VARCHAR(255) NOT NULL,
    description TEXT,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    source_page INTEGER,
    confidence NUMERIC(5,4) NOT NULL DEFAULT 0,
    status VARCHAR(40) NOT NULL
);

CREATE INDEX idx_extracted_requirements_analysis ON extracted_requirements(analysis_id);

CREATE TABLE extracted_important_dates (
    id BIGSERIAL PRIMARY KEY,
    analysis_id BIGINT NOT NULL REFERENCES document_analyses(id) ON DELETE CASCADE,
    date_type VARCHAR(60) NOT NULL,
    detected_date DATE,
    label VARCHAR(255) NOT NULL,
    source_page INTEGER,
    confidence NUMERIC(5,4) NOT NULL DEFAULT 0
);

CREATE INDEX idx_extracted_important_dates_analysis ON extracted_important_dates(analysis_id);

CREATE TABLE document_summaries (
    id BIGSERIAL PRIMARY KEY,
    analysis_id BIGINT NOT NULL REFERENCES document_analyses(id) ON DELETE CASCADE,
    short_summary TEXT,
    key_points TEXT,
    important_dates TEXT,
    required_documents TEXT,
    financial_conditions TEXT,
    technical_conditions TEXT,
    attention_points TEXT
);

CREATE INDEX idx_document_summaries_analysis ON document_summaries(analysis_id);
