BEGIN;

DELETE FROM extracted_important_dates;
DELETE FROM extracted_requirements;
DELETE FROM extracted_tender_information;
DELETE FROM document_summaries;
DELETE FROM document_analyses;
DELETE FROM document_versions;
DELETE FROM favorite_tenders;
DELETE FROM notifications;
DELETE FROM email_reminders;
DELETE FROM tender_history;
DELETE FROM activity_logs;
DELETE FROM tender_comments;
DELETE FROM tender_checklist_items;
DELETE FROM tender_tasks;
DELETE FROM tender_pricing_lines;
DELETE FROM tender_pricings;
DELETE FROM tender_submissions;
DELETE FROM tender_company_participations;
DELETE FROM tender_assignments;
DELETE FROM tender_documents;
DELETE FROM tenders;
DELETE FROM partner_companies;
DELETE FROM user_sessions;
DELETE FROM user_preferences;
DELETE FROM security_audits;

WITH official_admin AS (
    SELECT id
    FROM users
    WHERE role = 'ADMIN'
    ORDER BY id
    LIMIT 1
)
DELETE FROM users
WHERE id NOT IN (SELECT id FROM official_admin);

COMMIT;
