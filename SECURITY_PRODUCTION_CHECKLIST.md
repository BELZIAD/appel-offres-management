# Security Production Checklist

| Item | Status |
|---|---|
| aucun secret par defaut | PASS |
| secrets absents de Git | PARTIAL |
| JWT secret robuste | PARTIAL |
| DB password robuste | PARTIAL |
| admin password change | PARTIAL |
| user_sessions reellement utilisees | PASS |
| refresh token hashe | PASS |
| refresh cookie HttpOnly | PASS |
| Secure cookie production | PARTIAL |
| SameSite configure | PARTIAL |
| login success audite | PASS |
| login failure audite | PASS |
| account lock fonctionnel | PASS |
| rate limit login | PASS |
| CSP effective | PARTIAL |
| X-Frame-Options | PARTIAL |
| nosniff | PARTIAL |
| Referrer-Policy | PARTIAL |
| Permissions-Policy | PARTIAL |
| HSTS HTTPS production | PARTIAL |
| CSRF coherent | PARTIAL |
| CORS restrictif | PARTIAL |
| PostgreSQL non expose production | PASS |
| utilisateur DB non-superuser | PARTIAL |
| backup pg_dump reel | PARTIAL |
| checksum backup | PARTIAL |
| restauration testee | FAIL |
| retention backup | FAIL |
| MFA admin | FAIL |
| logs sans secrets | PARTIAL |
| Actuator securise | PARTIAL |
| dependency scanning | PASS |
| Dependabot | PASS |
| CI | PASS |
| secret scanning | PASS |
| Docker non-root | PASS |
| uploads securises | PARTIAL |
| tests securite passent | PARTIAL |
