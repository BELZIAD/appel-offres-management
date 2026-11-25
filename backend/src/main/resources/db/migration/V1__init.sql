CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tenders (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(120) NOT NULL UNIQUE,
    market_number VARCHAR(120),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    organization VARCHAR(255) NOT NULL,
    publication_date DATE,
    deadline DATE NOT NULL,
    estimated_amount NUMERIC(18,2),
    provisional_guarantee NUMERIC(18,2),
    priority VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    result VARCHAR(30) NOT NULL,
    assigned_employee_id BIGINT REFERENCES users(id),
    created_by_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tender_documents (
    id BIGSERIAL PRIMARY KEY,
    tender_id BIGINT NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    uploaded_by_id BIGINT REFERENCES users(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tenders_reference ON tenders(reference);
CREATE INDEX idx_tenders_deadline ON tenders(deadline);
CREATE INDEX idx_tenders_status ON tenders(status);
CREATE INDEX idx_tenders_result ON tenders(result);
CREATE INDEX idx_tenders_assigned_employee ON tenders(assigned_employee_id);
