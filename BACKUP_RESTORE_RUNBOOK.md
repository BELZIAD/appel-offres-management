# Backup And Restore Runbook

## Backup

Backups are PostgreSQL custom-format dumps created with `pg_dump -Fc`.

1. Ensure `pg_dump` and `pg_restore` are installed in the backend runtime.
2. Trigger `POST /api/admin/backups` as an admin.
3. Confirm the backup status is `VERIFIED`.
4. Record the file name, size, timestamp, and SHA-256 checksum.

## Verification

Every successful backup must pass:

- `pg_dump` exit code is `0`.
- dump file exists and is non-empty.
- SHA-256 checksum is calculated.
- `pg_restore --list <file.dump>` exits with `0`.

## Restore Test

Run restore tests only against an isolated temporary database.

```powershell
docker compose exec postgres createdb -U $env:POSTGRES_USER ao_restore_test
docker compose exec postgres pg_restore -U $env:POSTGRES_USER -d ao_restore_test /path/to/backup.dump
docker compose exec postgres psql -U $env:POSTGRES_USER -d ao_restore_test -c "\dt"
docker compose exec postgres dropdb -U $env:POSTGRES_USER ao_restore_test
```

Never restore over the primary database without an approved maintenance window and rollback plan.

## Rollback

Keep the previous verified dump until the restored application has passed smoke tests:

- login works,
- key tables are present,
- users and tenders are readable,
- uploads volume is mounted,
- application logs show no migration failure.
