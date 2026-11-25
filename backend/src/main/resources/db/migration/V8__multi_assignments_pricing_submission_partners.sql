ALTER TABLE tenders
    ADD COLUMN IF NOT EXISTS participation_mode VARCHAR(40) NOT NULL DEFAULT 'SOLE';

DROP INDEX IF EXISTS uk_email_reminder_tender_type;
CREATE UNIQUE INDEX IF NOT EXISTS uk_email_reminder_tender_employee_type ON email_reminders(tender_id, employee_id, reminder_type);

CREATE TABLE tender_assignments (
    id BIGSERIAL PRIMARY KEY,
    tender_id BIGINT NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    employee_id BIGINT NOT NULL REFERENCES users(id),
    assignment_role VARCHAR(40) NOT NULL,
    assigned_by_id BIGINT REFERENCES users(id),
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tender_assignment_role_active UNIQUE (tender_id, employee_id, assignment_role, active)
);

CREATE INDEX idx_tender_assignment_tender ON tender_assignments(tender_id);
CREATE INDEX idx_tender_assignment_employee ON tender_assignments(employee_id);
CREATE INDEX idx_tender_assignment_role ON tender_assignments(assignment_role);

INSERT INTO tender_assignments (tender_id, employee_id, assignment_role, assigned_by_id, active)
SELECT id, assigned_employee_id, 'TENDER_MANAGER', created_by_id, TRUE
FROM tenders
WHERE assigned_employee_id IS NOT NULL
ON CONFLICT DO NOTHING;

CREATE TABLE tender_pricings (
    id BIGSERIAL PRIMARY KEY,
    tender_id BIGINT NOT NULL UNIQUE REFERENCES tenders(id) ON DELETE CASCADE,
    status VARCHAR(40) NOT NULL DEFAULT 'NOT_STARTED',
    assigned_pricing_manager_id BIGINT REFERENCES users(id),
    estimated_total NUMERIC(19,2),
    final_total NUMERIC(19,2),
    currency VARCHAR(10) NOT NULL DEFAULT 'MAD',
    notes TEXT,
    validated_at TIMESTAMP,
    validated_by_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pricing_tender ON tender_pricings(tender_id);
CREATE INDEX idx_pricing_status ON tender_pricings(status);

CREATE TABLE tender_pricing_lines (
    id BIGSERIAL PRIMARY KEY,
    pricing_id BIGINT NOT NULL REFERENCES tender_pricings(id) ON DELETE CASCADE,
    line_number VARCHAR(50),
    reference VARCHAR(120),
    designation TEXT NOT NULL,
    unit VARCHAR(50),
    quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    unit_price NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_price NUMERIC(19,2) NOT NULL DEFAULT 0,
    notes TEXT,
    position INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pricing_line_pricing ON tender_pricing_lines(pricing_id);

CREATE TABLE tender_submissions (
    id BIGSERIAL PRIMARY KEY,
    tender_id BIGINT NOT NULL UNIQUE REFERENCES tenders(id) ON DELETE CASCADE,
    submission_manager_id BIGINT REFERENCES users(id),
    platform VARCHAR(40) NOT NULL DEFAULT 'PMMP',
    status VARCHAR(40) NOT NULL DEFAULT 'NOT_READY',
    submission_date DATE,
    submission_time TIME,
    external_reference VARCHAR(255),
    receipt_file_path VARCHAR(500),
    notes TEXT,
    submitted_by_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_submission_tender ON tender_submissions(tender_id);
CREATE INDEX idx_submission_status ON tender_submissions(status);

CREATE TABLE partner_companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    ice VARCHAR(80),
    rc VARCHAR(80),
    if_number VARCHAR(80),
    address TEXT,
    city VARCHAR(120),
    country VARCHAR(120),
    email VARCHAR(255),
    phone VARCHAR(80),
    contact_person VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_partner_company_name ON partner_companies(name);
CREATE INDEX idx_partner_company_ice ON partner_companies(ice);

CREATE TABLE tender_company_participations (
    id BIGSERIAL PRIMARY KEY,
    tender_id BIGINT NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    partner_company_id BIGINT REFERENCES partner_companies(id),
    participation_type VARCHAR(40) NOT NULL,
    is_our_company BOOLEAN NOT NULL DEFAULT FALSE,
    lead_company BOOLEAN NOT NULL DEFAULT FALSE,
    share_percentage NUMERIC(5,2),
    role_description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_participant_company CHECK (
        (is_our_company = TRUE AND partner_company_id IS NULL)
        OR (is_our_company = FALSE AND partner_company_id IS NOT NULL)
    )
);

CREATE INDEX idx_participation_tender ON tender_company_participations(tender_id);
CREATE INDEX idx_participation_partner ON tender_company_participations(partner_company_id);
