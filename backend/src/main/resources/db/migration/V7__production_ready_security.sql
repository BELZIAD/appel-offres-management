ALTER TABLE users ADD COLUMN IF NOT EXISTS account_locked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS lock_expires_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_ip VARCHAR(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_logout_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP;

CREATE TABLE security_audits (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    email VARCHAR(255),
    ip_address VARCHAR(120),
    action VARCHAR(80) NOT NULL,
    reason VARCHAR(500),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_security_audits_created_at ON security_audits(created_at);
CREATE INDEX idx_security_audits_email ON security_audits(email);
CREATE INDEX idx_security_audits_action ON security_audits(action);

CREATE TABLE user_sessions (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    ip_address VARCHAR(120),
    user_agent VARCHAR(500),
    login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logout_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_active ON user_sessions(active);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);

UPDATE application_versions
SET version = '1.0.0', environment = 'PRODUCTION', build_date = CURRENT_TIMESTAMP
WHERE id = (SELECT id FROM application_versions ORDER BY build_date DESC LIMIT 1);
