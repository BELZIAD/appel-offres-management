-- Les logos / favicon enregistres contenaient le nom de l'ancienne entreprise : on les retire.
UPDATE company_settings SET company_logo = NULL, company_logo_dark = NULL, favicon = NULL;
