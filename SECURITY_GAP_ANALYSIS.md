# Security Gap Analysis

| CONTROL | CURRENT STATE | RISK | FIX | TEST | STATUS |
|---|---|---|---|---|---|
| Secrets | Defaults removed from runtime config; startup validator added | Weak defaults could become production secrets | Required env vars and fail-fast validation | `mvn test`, `mvn package` | PASS |
| Sessions | Server sessions now create hashed refresh tokens | Token theft impact if long-lived bearer token stored in browser | HttpOnly refresh cookie + short access JWT | `mvn test`, `npm run build` | PASS |
| Audit | AuthService records login, refresh, logout, lock events | Incidents not traceable | SecurityAuditService wired into auth | `mvn test` | PASS |
| Account lock | Login increments failures and locks account | Brute force risk | Configurable lockout | `mvn test` | PASS |
| Rate limit | In-memory login limiter added | Repeated login attempts | 429 with Retry-After | `mvn test` | PASS |
| Headers | Spring Security sends CSP, frame deny, nosniff, referrer, permissions policy | Browser attack surface | Headers configured in SecurityConfig | `mvn test` startup + compile | PARTIAL |
| CSRF | Disabled; access tokens use Authorization header, refresh cookie limited to auth path | Cookie endpoints need review | Documented as architecture decision | Manual review | PARTIAL |
| PostgreSQL exposure | Prod compose removes published DB port | DB exposure | docker-compose.prod.yml | `docker compose -f docker-compose.yml -f docker-compose.prod.yml config` | PASS |
| Backups | pg_dump custom-format with checksum and pg_restore list verification | Fake backups unrecoverable | BackupHistory + real dump | `mvn test`; Docker pg_dump runtime pending | PARTIAL |
| Dependencies | Dependabot and CI added; React Router updated | Vulnerable dependencies linger | Weekly PRs, npm audit, tests | `npm audit --audit-level=high` | PASS |
| MFA | Not implemented | Admin account takeover impact | Future TOTP phase | Not tested | NOT IMPLEMENTED |
| TLS | Production flags prepared; cert/proxy not included | Cleartext transport if deployed over HTTP | Reverse proxy/TLS runbook required | Deployment test pending | PARTIAL |
