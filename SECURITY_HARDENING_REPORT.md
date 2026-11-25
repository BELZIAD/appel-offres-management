# Security Hardening Report

| CONTROL | BEFORE | AFTER | TEST | RESULT |
|---|---|---|---|---|
| Secrets | Weak defaults in YAML/Compose | Required env vars, startup validation, placeholder `.env.example` | `mvn test`, `mvn package` | PASS |
| Sessions | JWT generated without active server session | Login creates session and refresh cookie; refresh rotates token hash | `mvn test`, `npm run build` | PASS |
| Refresh tokens | No real refresh flow | Raw token only in HttpOnly cookie; SHA-256 stored | `mvn test` | PASS |
| Cookies | JWT in `localStorage` | Refresh cookie `HttpOnly`, access token in memory | `npm run build`, code scan | PASS |
| Login audit | Audit service unused by auth | Login success/failure/lock/refresh/logout recorded | `mvn test` | PASS |
| Account lock | Fields existed but not enforced | Configurable failed-attempt lockout | `mvn test` | PASS |
| Rate limit | Missing | In-memory login limiter returns 429 | `mvn test` | PASS |
| Headers/CSP | CSP value existed but was not wired | Spring Security sends CSP and security headers | HTTP check pending | PARTIAL |
| CSRF | Disabled | Still disabled; acceptable only while auth APIs use Authorization header and refresh cookie is scoped | Review pending | PARTIAL |
| TLS | Not configured | Production flags and compose override prepared; cert proxy remains deployment work | Not run | PARTIAL |
| PostgreSQL exposure | Compose published DB port | Prod compose has no Postgres port; dev override exposes it only for local use | `docker compose -f docker-compose.yml -f docker-compose.prod.yml config` | PASS |
| Backups | `.txt` marker file | `pg_dump -Fc`, checksum, `pg_restore --list`, metadata | Docker runtime pending | PARTIAL |
| Restore | Not available | Runbook added | Not executed | FAIL |
| MFA | Missing | Deferred | Not executed | FAIL |
| Dependencies | React Router vulnerable version | React Router updated; Dependabot + CI audit/tests added | `npm audit --audit-level=high` | PASS |
