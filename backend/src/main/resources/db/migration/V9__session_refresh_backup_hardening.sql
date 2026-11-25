ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS last_ip_address VARCHAR(120);
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS refresh_token_hash VARCHAR(128);
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS revoked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP;

UPDATE user_sessions
SET refresh_token_hash = id,
    active = FALSE,
    revoked = TRUE,
    revoked_at = CURRENT_TIMESTAMP
WHERE refresh_token_hash IS NULL;

ALTER TABLE user_sessions ALTER COLUMN refresh_token_hash SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_sessions_refresh_token_hash ON user_sessions(refresh_token_hash);
CREATE INDEX IF NOT EXISTS idx_user_sessions_revoked ON user_sessions(revoked);

CREATE TABLE backup_history (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    checksum_sha256 VARCHAR(64),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    status VARCHAR(40) NOT NULL,
    error_message VARCHAR(500),
    created_by_id BIGINT REFERENCES users(id),
    backup_type VARCHAR(40) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_backup_history_started_at ON backup_history(started_at);
CREATE INDEX IF NOT EXISTS idx_backup_history_status ON backup_history(status);
