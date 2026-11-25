# Appel Offres Management

Application interne Sprint 1 de gestion des appels d'offres.

## Lancement

```bash
docker compose up -d --build
```

Frontend: http://localhost:5173

Backend API: http://localhost:8080/api

Compte administrateur de developpement cree seulement si aucun admin n'existe:

- Email: `admin@ao.local`
- Mot de passe: `Admin123!`

Les valeurs peuvent etre surchargees par variables d'environnement:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `APP_JWT_SECRET`
- `APP_DEV_ADMIN_EMAIL`
- `APP_DEV_ADMIN_PASSWORD`
- `APP_MAX_FILE_SIZE`
- `VITE_API_URL`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`

Copier `.env.example` vers `.env` localement si necessaire. Ne pas versionner de vrais identifiants SMTP.

## Tests

```bash
cd backend
mvn test
```

```bash
cd frontend
npm install
npm run build
```
