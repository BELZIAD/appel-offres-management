-- Suppression du nom d'entreprise enregistre (application en marque blanche)
ALTER TABLE company_settings ALTER COLUMN company_name DROP NOT NULL;
UPDATE company_settings SET company_name = NULL;
-- Coordonnees de demonstration liees a l'ancien nom
UPDATE company_settings SET email = NULL WHERE email ILIKE '%interlink%';
