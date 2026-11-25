CREATE TABLE tender_tasks (
    id BIGSERIAL PRIMARY KEY,
    tender_id BIGINT NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    assigned_employee_id BIGINT REFERENCES users(id),
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    deadline DATE NOT NULL,
    created_by_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_tender_tasks_assigned_employee ON tender_tasks(assigned_employee_id);
CREATE INDEX idx_tender_tasks_tender ON tender_tasks(tender_id);
CREATE INDEX idx_tender_tasks_status ON tender_tasks(status);
CREATE INDEX idx_tender_tasks_deadline ON tender_tasks(deadline);

CREATE TABLE checklist_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE checklist_template_items (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES checklist_templates(id) ON DELETE CASCADE,
    label VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    expected_document_type VARCHAR(50)
);

CREATE INDEX idx_checklist_template_items_template ON checklist_template_items(template_id);

CREATE TABLE tender_checklist_items (
    id BIGSERIAL PRIMARY KEY,
    tender_id BIGINT NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    label VARCHAR(255) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_by_id BIGINT REFERENCES users(id),
    completed_at TIMESTAMP,
    position INTEGER NOT NULL,
    expected_document_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tender_checklist_items_tender ON tender_checklist_items(tender_id);

CREATE TABLE document_versions (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES tender_documents(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_by_id BIGINT REFERENCES users(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    comment TEXT,
    CONSTRAINT uk_document_versions_document_version UNIQUE(document_id, version_number)
);

CREATE INDEX idx_document_versions_document ON document_versions(document_id);

CREATE TABLE tender_comments (
    id BIGSERIAL PRIMARY KEY,
    tender_id BIGINT NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    author_id BIGINT REFERENCES users(id),
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_tender_comments_tender ON tender_comments(tender_id);
CREATE INDEX idx_tender_comments_author ON tender_comments(author_id);

CREATE TABLE favorite_tenders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tender_id BIGINT NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_favorite_tenders_user_tender UNIQUE(user_id, tender_id)
);

CREATE INDEX idx_favorite_tenders_user ON favorite_tenders(user_id);
CREATE INDEX idx_favorite_tenders_tender ON favorite_tenders(tender_id);

CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    tender_id BIGINT REFERENCES tenders(id) ON DELETE SET NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id BIGINT,
    action VARCHAR(80) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activity_logs_created_at ON activity_logs(created_at);
CREATE INDEX idx_activity_logs_tender ON activity_logs(tender_id);
CREATE INDEX idx_activity_logs_user ON activity_logs(user_id);

INSERT INTO checklist_templates(name, description, active)
VALUES ('Checklist Appel d''offres standard', 'Checklist standard de preparation d''un dossier AO.', TRUE);

INSERT INTO checklist_template_items(template_id, label, position, required, expected_document_type)
SELECT id, 'RC recu', 1, TRUE, 'RC' FROM checklist_templates WHERE name = 'Checklist Appel d''offres standard';
INSERT INTO checklist_template_items(template_id, label, position, required, expected_document_type)
SELECT id, 'CPS recu', 2, TRUE, 'CPS' FROM checklist_templates WHERE name = 'Checklist Appel d''offres standard';
INSERT INTO checklist_template_items(template_id, label, position, required, expected_document_type)
SELECT id, 'Offre technique prete', 3, TRUE, 'TECHNICAL_DOCUMENT' FROM checklist_templates WHERE name = 'Checklist Appel d''offres standard';
INSERT INTO checklist_template_items(template_id, label, position, required, expected_document_type)
SELECT id, 'Offre financiere prete', 4, TRUE, 'FINANCIAL_DOCUMENT' FROM checklist_templates WHERE name = 'Checklist Appel d''offres standard';
INSERT INTO checklist_template_items(template_id, label, position, required, expected_document_type)
SELECT id, 'Attestation CNSS', 5, TRUE, 'ADMINISTRATIVE_DOCUMENT' FROM checklist_templates WHERE name = 'Checklist Appel d''offres standard';
INSERT INTO checklist_template_items(template_id, label, position, required, expected_document_type)
SELECT id, 'Attestation fiscale', 6, TRUE, 'ADMINISTRATIVE_DOCUMENT' FROM checklist_templates WHERE name = 'Checklist Appel d''offres standard';
INSERT INTO checklist_template_items(template_id, label, position, required, expected_document_type)
SELECT id, 'Registre de commerce', 7, TRUE, 'RC' FROM checklist_templates WHERE name = 'Checklist Appel d''offres standard';
INSERT INTO checklist_template_items(template_id, label, position, required, expected_document_type)
SELECT id, 'Caution bancaire', 8, TRUE, 'FINANCIAL_DOCUMENT' FROM checklist_templates WHERE name = 'Checklist Appel d''offres standard';
INSERT INTO checklist_template_items(template_id, label, position, required, expected_document_type)
SELECT id, 'Signature', 9, TRUE, NULL FROM checklist_templates WHERE name = 'Checklist Appel d''offres standard';
INSERT INTO checklist_template_items(template_id, label, position, required, expected_document_type)
SELECT id, 'Depot effectue', 10, TRUE, NULL FROM checklist_templates WHERE name = 'Checklist Appel d''offres standard';
