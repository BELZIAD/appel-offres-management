# Data Protection

The application stores business tender data, user identities, uploaded documents, audit events, and operational backups.

Current controls:

- Passwords are hashed with BCrypt.
- Refresh tokens are stored only as SHA-256 hashes.
- Secrets are required from environment variables and are not exposed to the frontend.
- PostgreSQL traffic stays inside the Docker network in production compose.
- Backups include SHA-256 checksums and `pg_restore --list` verification.

Production requirements:

- Use encrypted host disks or encrypted managed volumes for PostgreSQL and backup storage.
- Restrict backup file access to operators only.
- Store deployment secrets in the deployment platform secret manager, not in Git.
- Enable TLS at the reverse proxy.
- Consider application-level encryption only for clearly identified sensitive fields with externally managed keys.
