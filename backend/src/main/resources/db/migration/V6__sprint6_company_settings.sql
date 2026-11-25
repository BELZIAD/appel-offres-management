CREATE TABLE company_settings (
    id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    application_name VARCHAR(255) NOT NULL,
    company_logo VARCHAR(1000),
    company_logo_dark VARCHAR(1000),
    favicon VARCHAR(1000),
    primary_color VARCHAR(20) NOT NULL DEFAULT '#172033',
    secondary_color VARCHAR(20) NOT NULL DEFAULT '#475569',
    accent_color VARCHAR(20) NOT NULL DEFAULT '#2563eb',
    danger_color VARCHAR(20) NOT NULL DEFAULT '#dc2626',
    warning_color VARCHAR(20) NOT NULL DEFAULT '#d97706',
    success_color VARCHAR(20) NOT NULL DEFAULT '#059669',
    sidebar_color VARCHAR(20) NOT NULL DEFAULT '#ffffff',
    header_color VARCHAR(20) NOT NULL DEFAULT '#ffffff',
    address TEXT,
    city VARCHAR(120),
    postal_code VARCHAR(40),
    country VARCHAR(120),
    phone VARCHAR(80),
    mobile VARCHAR(80),
    email VARCHAR(160),
    website VARCHAR(255),
    ice VARCHAR(80),
    if_number VARCHAR(80),
    rc VARCHAR(80),
    patente VARCHAR(80),
    tax_identifier VARCHAR(80),
    currency VARCHAR(20) NOT NULL DEFAULT 'MAD',
    timezone VARCHAR(80) NOT NULL DEFAULT 'Africa/Casablanca',
    language VARCHAR(20) NOT NULL DEFAULT 'fr',
    linkedin VARCHAR(255),
    facebook VARCHAR(255),
    instagram VARCHAR(255),
    youtube VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO company_settings(company_name, application_name, email, country, currency, timezone, language)
VALUES ('Interlink Distribution', 'Gestion des Appels d''Offres', 'contact@interlink.local', 'Maroc', 'MAD', 'Africa/Casablanca', 'fr');

ALTER TABLE users ADD COLUMN photo VARCHAR(1000);
ALTER TABLE users ADD COLUMN signature VARCHAR(1000);
ALTER TABLE users ADD COLUMN job_title VARCHAR(160);
ALTER TABLE users ADD COLUMN department VARCHAR(160);
ALTER TABLE users ADD COLUMN phone VARCHAR(80);
ALTER TABLE users ADD COLUMN language VARCHAR(20) DEFAULT 'fr';
ALTER TABLE users ADD COLUMN timezone VARCHAR(80) DEFAULT 'Africa/Casablanca';

CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    theme VARCHAR(40) NOT NULL DEFAULT 'SYSTEM',
    language VARCHAR(20) NOT NULL DEFAULT 'fr',
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    browser_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    timezone VARCHAR(80) NOT NULL DEFAULT 'Africa/Casablanca',
    date_format VARCHAR(40) NOT NULL DEFAULT 'dd/MM/yyyy',
    time_format VARCHAR(40) NOT NULL DEFAULT 'HH:mm',
    page_size INTEGER NOT NULL DEFAULT 20,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_preferences_user UNIQUE(user_id)
);

CREATE TABLE application_versions (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(80) NOT NULL,
    build_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    commit_git VARCHAR(120),
    environment VARCHAR(40) NOT NULL DEFAULT 'DEV'
);

INSERT INTO application_versions(version, environment)
VALUES ('1.6.0', 'DEV');
