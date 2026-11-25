# Gestion des Appels d'Offres

Application web interne qui permet à une entreprise de **suivre et préparer ses réponses aux appels d'offres (AO)** de bout en bout : de la réception du dossier de consultation jusqu'à la soumission et au résultat final.

L'administrateur crée les appels d'offres et constitue une équipe par AO. Chaque employé ne voit que les dossiers qui lui sont assignés et intervient selon son rôle : chef de projet, chiffrage ou soumission.

---

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Rôles et droits d'accès](#rôles-et-droits-daccès)
- [Cycle de vie d'un appel d'offres](#cycle-de-vie-dun-appel-doffres)
- [Stack technique](#stack-technique)
- [Architecture](#architecture)
- [Installation et lancement](#installation-et-lancement)
- [Configuration](#configuration)
- [Sécurité](#sécurité)
- [Tests et intégration continue](#tests-et-intégration-continue)
- [Structure du projet](#structure-du-projet)

---

## Fonctionnalités

### Gestion des appels d'offres
- Création et modification d'un AO : référence, objet, organisme acheteur, date limite, priorité.
- Suivi du statut : de *Nouveau* à *Clôturé* (voir le [cycle de vie](#cycle-de-vie-dun-appel-doffres)).
- Résultat final : **Gagné**, **Perdu**, **Annulé** ou **En attente**.
- Vue **Kanban** pour visualiser tous les AO par statut.
- **Favoris** pour garder un accès rapide aux dossiers importants.
- **Historique** complet des modifications de chaque AO : qui a changé quoi, et quand.

### Équipe projet
Pour chaque AO, l'administrateur affecte des employés avec un rôle précis :

| Rôle dans l'AO | Responsabilité |
|---|---|
| Chef de projet (`TENDER_MANAGER`) | Pilote le dossier, gère les documents |
| Responsable chiffrage (`PRICING_MANAGER`) | Prépare l'offre financière |
| Responsable soumission (`SUBMISSION_MANAGER`) | Dépose l'offre et gère les documents |
| Contributeur (`CONTRIBUTOR`) | Consulte le dossier et participe aux échanges |

### Documents
- Upload des pièces du dossier : RC, CPS, avis, bordereau des prix, documents techniques, administratifs et financiers.
- **Gestion des versions** : chaque document peut avoir plusieurs versions, et l'historique est conservé.
- Contrôle des fichiers : vérification de la signature PDF, taille maximale configurable.

### Analyse automatique des documents
L'application lit le texte des PDF (avec Apache PDFBox) et en extrait automatiquement :
- les **dates importantes** : publication, date limite de dépôt, ouverture des plis, visite des lieux, date limite des questions, début d'exécution ;
- les **exigences** du dossier, classées par catégorie : administrative, technique, financière, juridique, certificats, cautions ;
- les **informations clés** de l'AO.

Ces éléments peuvent ensuite être **appliqués d'un clic** à la fiche de l'AO ou à sa checklist. Une recherche plein texte dans les documents est aussi disponible.

> L'analyse fonctionne sur les PDF contenant du texte. L'OCR des PDF scannés est prévu, mais pas encore activé.

### Checklist
- **Modèles de checklist** réutilisables, définis par l'administrateur.
- Génération de la checklist d'un AO à partir d'un modèle.
- Les membres de l'équipe cochent les éléments au fur et à mesure.

### Chiffrage (offre financière)
- Saisie des lignes de prix : désignation, quantité, prix unitaire.
- **Import et export Excel** du bordereau des prix.
- Circuit de validation : *Non commencé* → *En cours* → *Prêt pour revue* → *Validé* (ou *Révision demandée*).

### Soumission
- Suivi de l'état de préparation : *Pas prêt*, *Prêt*, *En cours*, *Soumis*, *Confirmé*, *Échec*.
- Plateforme de dépôt (PMMP ou autre) et confirmation du dépôt.
- Génération d'un **accusé de soumission**.

### Partenaires et groupements
- Annuaire des entreprises partenaires.
- Mode de participation pour chaque AO : soumissionnaire seul, groupement, co-entreprise, sous-traitant ou partenaire.

### Tâches
- Création de tâches avec priorité, échéance et assignation à un employé.
- Statuts : *À faire*, *En cours*, *Bloquée*, *Terminée*.
- Chaque employé dispose de sa propre liste de tâches.

### Collaboration et notifications
- **Commentaires** sur chaque AO.
- **Notifications** dans l'application : affectation, document ajouté, changement de statut, tâche assignée ou bloquée, nouveau commentaire, analyse terminée…
- **Rappels automatiques par email** chaque matin à 8h, à J-2, J-1 et le jour de la date limite.
- Indicateur d'urgence des échéances : normal, attention, urgent, aujourd'hui, dépassé.

### Pilotage (administrateur)
- **Tableau de bord** avec indicateurs et graphiques.
- **Statistiques** : taux de réussite, performance par employé, analyse par organisme acheteur.
- **Rapports** exportables en **PDF**, et liste des AO exportable en **Excel**.
- **Journal d'activité** de toutes les actions des utilisateurs.
- **Sauvegardes** de la base de données (`pg_dump`) lancées et téléchargées depuis l'interface, avec une durée de conservation configurable.
- **Paramètres de l'entreprise** : nom, logo et éléments de marque affichés dans l'application.

### Recherche et profil
- **Recherche globale** dans les AO, les tâches et les documents. Les résultats sont limités aux dossiers accessibles par l'utilisateur.
- **Profil** : photo, préférences, thème clair, sombre ou système, changement de mot de passe.

---

## Rôles et droits d'accès

| | Administrateur (`ADMIN`) | Employé (`EMPLOYEE`) |
|---|---|---|
| Créer / modifier / supprimer des AO | ✅ | ❌ |
| Gérer les employés et les équipes | ✅ | ❌ |
| Voir les AO | Tous | Seulement ceux où il est affecté |
| Documents, chiffrage, soumission | ✅ | Selon son rôle dans l'AO |
| Tableau de bord, statistiques, exports | ✅ | Tableau de bord personnel |
| Sauvegardes, journal d'activité, paramètres | ✅ | ❌ |

Les droits sont vérifiés **côté serveur** à chaque requête. Masquer un bouton dans l'interface ne suffit pas à donner ou retirer un accès.

---

## Cycle de vie d'un appel d'offres

```
 NEW ──► TO_STUDY ──► IN_PROGRESS ──► PREPARING ──► READY_TO_SUBMIT ──► SUBMITTED ──► CLOSED
Nouveau   À étudier     En cours      Préparation     Prêt à soumettre     Soumis       Clôturé
                                                                                          │
                                                               Résultat : WON / LOST / CANCELLED
```

Parcours typique :
1. L'**administrateur** crée l'AO et charge les documents du dossier de consultation (RC, CPS…).
2. Il **lance l'analyse** des documents : les dates limites et les exigences sont extraites automatiquement.
3. Il **constitue l'équipe** et génère la **checklist** à partir d'un modèle.
4. Le **responsable chiffrage** prépare le bordereau des prix, puis le soumet pour validation.
5. Le **responsable soumission** vérifie que tout est prêt, dépose l'offre et confirme le dépôt.
6. L'administrateur enregistre le **résultat** (gagné ou perdu), qui alimente les statistiques.

Pendant tout ce parcours, l'équipe échange par commentaires, reçoit des notifications et des rappels de date limite.

---

## Stack technique

| Couche | Technologies |
|---|---|
| Frontend | React 18, TypeScript, Vite, React Router, Tailwind CSS, Recharts, Axios |
| Backend | Java 21, Spring Boot 3.3 (Web, Data JPA, Security, Validation, Mail, Actuator) |
| Authentification | JWT (jjwt) + refresh token en cookie `HttpOnly` |
| Base de données | PostgreSQL 16, migrations **Flyway** |
| Documents | Apache PDFBox (lecture PDF), Apache POI (Excel) |
| Documentation API | Swagger / OpenAPI (springdoc) |
| Déploiement | Docker, Docker Compose, Nginx |
| CI | GitHub Actions : tests, `npm audit`, détection de secrets (Gitleaks) |

---

## Architecture

```
┌──────────────────────┐      HTTP / JSON      ┌───────────────────────────┐      JDBC      ┌──────────────┐
│  Frontend (React)    │ ────────────────────► │  Backend (Spring Boot)    │ ─────────────► │ PostgreSQL   │
│  servi par Nginx     │   Authorization: JWT  │  API REST  /api/**        │                │ (Flyway)     │
│  :5173               │ ◄──────────────────── │  :8080                    │                └──────────────┘
└──────────────────────┘                       │  • Sécurité (JWT, rôles)  │
                                               │  • Tâches planifiées      │ ──► SMTP (rappels email)
                                               │  • Analyse PDF            │ ──► Stockage fichiers (uploads/)
                                               └───────────────────────────┘
```

Le backend est organisé en couches classiques : `controller` (API REST) → `service` (règles métier et contrôle d'accès) → `repository` (Spring Data JPA) → `entity`.

---

## Installation et lancement

### Prérequis
- Docker et Docker Compose
- Pour développer sans Docker : Java 21, Maven, Node.js 20

### 1. Cloner le projet
```bash
git clone https://github.com/BELZIAD/appel-offres-management.git
cd appel-offres-management
```

### 2. Créer le fichier de configuration
```bash
cp .env.example .env
```
Ouvre ensuite `.env` et remplace **toutes** les valeurs `CHANGE_ME` :
- `POSTGRES_PASSWORD` : mot de passe de la base ;
- `APP_JWT_SECRET` : longue chaîne aléatoire d'au moins 32 caractères ;
- `APP_DEV_ADMIN_PASSWORD` : mot de passe du premier compte administrateur, d'au moins 12 caractères.

> Le fichier `.env` contient des secrets : il est ignoré par Git et ne doit jamais être publié.

### 3. Démarrer
```bash
docker compose up -d --build
```

| Service | URL |
|---|---|
| Application | http://localhost:5173 |
| API | http://localhost:8080/api |
| Documentation API (Swagger) | http://localhost:8080/swagger-ui.html |

### 4. Première connexion
Au premier démarrage, si aucun administrateur n'existe, un compte est créé automatiquement :
- **Email** : valeur de `APP_DEV_ADMIN_EMAIL` (par défaut `admin@ao.local`)
- **Mot de passe** : valeur de `APP_DEV_ADMIN_PASSWORD` définie dans ton `.env`

Depuis ce compte, crée ensuite les employés, puis les appels d'offres.

### Autres modes
```bash
# Développement (base exposée en local)
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build

# Production (profil prod, contrôles de sécurité renforcés)
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

---

## Configuration

Toutes les valeurs se règlent dans `.env` (voir `.env.example`) :

| Variable | Rôle |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Connexion à la base de données |
| `APP_JWT_SECRET` | Clé de signature des jetons JWT |
| `APP_REFRESH_EXPIRATION_MS` | Durée de vie de la session (refresh token) |
| `APP_COOKIE_SECURE`, `APP_COOKIE_SAME_SITE` | Options du cookie de session (`true` en HTTPS) |
| `APP_DEV_ADMIN_EMAIL`, `APP_DEV_ADMIN_PASSWORD` | Premier compte administrateur |
| `APP_MAX_FILE_SIZE` | Taille maximale des fichiers uploadés |
| `AUTH_MAX_FAILED_ATTEMPTS`, `AUTH_LOCK_MINUTES` | Verrouillage après échecs de connexion |
| `AUTH_LOGIN_RATE_LIMIT`, `AUTH_LOGIN_RATE_WINDOW_SECONDS` | Limitation du nombre de tentatives de connexion |
| `BACKUP_RETENTION_DAYS` | Durée de conservation des sauvegardes |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Serveur SMTP pour les rappels email |
| `VITE_API_URL` | URL de l'API utilisée par le frontend |

---

## Sécurité

- Mots de passe hachés avec **BCrypt**.
- **JWT de courte durée**, gardé en mémoire côté navigateur, et refresh token dans un cookie **`HttpOnly`** (inaccessible au JavaScript).
- **Protection contre le brute-force** : verrouillage du compte après plusieurs échecs, et limitation du nombre de tentatives de connexion.
- **Contrôle d'accès** vérifié côté serveur : routes `/api/admin/**` réservées aux administrateurs, et vérification de l'affectation pour chaque AO, document, commentaire, chiffrage et soumission.
- **Upload contrôlé** : vérification du type réel des fichiers (signature PDF), taille maximale, protection contre le *path traversal*.
- **En-têtes HTTP de sécurité** : Content-Security-Policy, HSTS, X-Frame-Options, Referrer-Policy, Permissions-Policy.
- **Secrets hors du code**, dans des variables d'environnement. En production, l'application **refuse de démarrer** si un secret est absent, trop court ou trop faible.
- **Journal d'audit** des actions et des connexions.

Limites connues, à traiter avant une mise en production :
- double authentification (MFA) pour les administrateurs ;
- terminaison HTTPS/TLS à configurer sur le reverse proxy du serveur ;
- OCR des PDF scannés.

---

## Tests et intégration continue

```bash
# Backend : tests unitaires et d'intégration (base H2 en mémoire)
cd backend
mvn test

# Frontend : vérification TypeScript et build
cd frontend
npm install
npm run build
```

À chaque push, GitHub Actions lance :
1. les **tests backend** et le build du JAR ;
2. un **audit des dépendances** npm et le build du frontend ;
3. un **scan des secrets** avec Gitleaks.

---

## Structure du projet

```
appel-offres-management/
├── backend/                     API Spring Boot
│   └── src/main/
│       ├── java/com/aomanagement/
│       │   ├── config/          Sécurité, démarrage, configuration
│       │   ├── controller/      Endpoints REST
│       │   ├── service/         Logique métier, contrôle d'accès, analyse PDF, emails
│       │   ├── repository/      Accès aux données (Spring Data JPA)
│       │   ├── entity/          Modèle de données
│       │   ├── dto/             Objets échangés avec le frontend
│       │   ├── security/        Filtre JWT, utilisateur connecté
│       │   └── exception/       Gestion centralisée des erreurs
│       └── resources/
│           ├── application*.yml Configuration par profil (dev, demo, prod)
│           └── db/migration/    Scripts Flyway
├── frontend/                    Application React
│   └── src/
│       ├── pages/               Écrans (admin/, employee/, communs)
│       ├── ui/                  Composants réutilisables
│       ├── api/                 Client HTTP
│       └── state/               Authentification, thème, paramètres
├── docker-compose.yml           Lancement standard
├── docker-compose.dev.yml       Surcharge développement
├── docker-compose.prod.yml      Surcharge production
├── .env.example                 Modèle de configuration
└── .github/workflows/           Intégration continue
```

Documentation complémentaire : [sauvegarde et restauration](BACKUP_RESTORE_RUNBOOK.md), [import de données](DATA_IMPORT_GUIDE.md), [protection des données](DATA_PROTECTION.md).

---

## Auteur

Projet réalisé par **BELZIAD**.
