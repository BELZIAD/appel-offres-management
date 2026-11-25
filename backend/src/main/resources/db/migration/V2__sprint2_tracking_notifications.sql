ALTER TABLE tender_documents ADD COLUMN file_size BIGINT NOT NULL DEFAULT 0;

CREATE TABLE email_reminders (
    id BIGSERIAL PRIMARY KEY,
    tender_id BIGINT NOT NULL REFERENCES tenders(id) ON DELETE CASCADE,
    employee_id BIGINT NOT NULL REFERENCES users(id),
    reminder_type VARCHAR(50) NOT NULL,
    scheduled_date DATE NOT NULL,
    sent_at TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_email_reminder_tender_type ON email_reminders(tender_id, reminder_type);
CREATE INDEX idx_email_reminders_status ON email_reminders(status);
CREATE INDEX idx_email_reminders_employee ON email_reminders(employee_id);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tender_id BIGINT REFERENCES tenders(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);

CREATE INDEX idx_notifications_recipient_read ON notifications(recipient_id, read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);

CREATE TABLE tender_history (
    id BIGSERIAL PRIMARY KEY,
    tender_id BIGINT REFERENCES tenders(id) ON DELETE SET NULL,
    performed_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    field_name VARCHAR(120),
    old_value TEXT,
    new_value TEXT,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tender_history_tender ON tender_history(tender_id);
CREATE INDEX idx_tender_history_created_at ON tender_history(created_at);
