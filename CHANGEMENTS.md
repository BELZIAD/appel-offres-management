# Changements – audit du 23/11/2026

Tests effectués : backend lancé sur PostgreSQL 16 + 150 scénarios API (admin et employé) couvrant tous les endpoints,
puis parcours de toutes les pages de l'interface (admin et employé) dans un navigateur headless.

## Bugs corrigés

| # | Problème constaté | Correction |
|---|---|---|
| 1 | **Déconnexion forcée après 15 min** : un jeton expiré renvoyait 403 au lieu de 401, le frontend ne tentait jamais de rafraîchir la session. | `SecurityConfig` : point d'entrée 401 + gestion 403 explicite, `/error` autorisé. |
| 2 | **Sessions perdues au chargement** : deux rafraîchissements simultanés (StrictMode, plusieurs onglets) utilisaient le même jeton → le second échouait. | `client.ts` : `refreshSession()` unique, partagé et sérialisé entre onglets (Web Locks). `/auth/refresh` sans cookie renvoie 401 (au lieu d'une erreur 500). |
| 3 | **Fuite d'accès** : après réaffectation d'un AO, l'ancien responsable gardait l'accès. Retirer un membre de l'équipe ne retirait pas l'accès « legacy ». | `TenderAssignmentService.replaceTenderManager()` + nettoyage du responsable / chiffreur / déposant lors d'un retrait. |
| 4 | Les participations (groupement) d'un AO étaient lisibles par n'importe quel employé. | Vérification d'accès sur `GET /api/tenders/{id}/participations`. |
| 5 | Suppression d'un employé lié à des AO → erreur 500 avec le détail SQL. | Erreur 409 claire (« désactivez son compte »). |
| 6 | Les erreurs 500 renvoyaient le message technique (SQL, chemins). Paramètre manquant → 500. | `GlobalExceptionHandler` : 400/404/405/409 adaptés, message générique pour les 500 (détail dans les logs). |
| 7 | Un chiffrage validé restait « Validé » après modification des lignes ; un employé pouvait valider son propre chiffrage. | Toute modification repasse le chiffrage en « Révision requise » ; seule l'administration valide. |
| 8 | Import Excel du bordereau : colonnes fixes, impossible de réimporter le fichier exporté ; aucune confirmation possible pour remplacer un bordereau chiffré. | Détection automatique de la ligne d'en-tête (Désignation, Quantité, PU…), paramètre `replace=true`. |
| 9 | Confirmer un dépôt laissait l'AO au statut « En cours » (tableau de bord faux). | L'AO passe automatiquement à « Déposé ». |
| 10 | La politique de mot de passe existait mais n'était jamais appliquée. | Appliquée à la création / modification des employés. |
| 11 | Le thème « Sombre / Système » des préférences était enregistré mais jamais appliqué. | Thème sombre fonctionnel (`state/theme.ts` + `styles.css`). |
| 12 | `.gitignore` excluait `*.sql` → les migrations Flyway n'étaient pas versionnées. | Exception ajoutée pour `backend/src/main/resources/db/**`. |
| 13 | Statistiques mensuelles sans `year` → 500 ; montant estimé du chiffrage toujours vide. | Année courante par défaut ; repli sur le montant estimé de l'AO. |

## Suppressions demandées

- **Nom de l'entreprise** : valeur enregistrée effacée (migration `V10`), plus de nom par défaut « Interlink Distribution »,
  champ retiré des écrans Entreprise et Profil, plus affiché (pied de page, connexion, À propos, exports). L'e-mail de démo `contact@interlink.local` est aussi effacé.
- **Informations système** : page « Système » et endpoint `/api/admin/system` supprimés ; `/api/about` (public) n'expose plus la version, l'environnement ni la pile technique.

## Nouvelles fonctionnalités

- **Chiffrage** : bouton « Importer » (aperçu des lignes puis import, avec confirmation si le bordereau contient déjà des prix).
- **Dépôt** : formulaire « Confirmer le dépôt » (plateforme, date, heure, référence, notes, accusé PDF).
- **Équipe AO** : section « Groupement / partenaires » (notre société ou partenaire, type, part %, mandataire, contrôle du total 100 %).
- **Fiche AO** : bouton « Dupliquer » (admin, copie infos + checklist) et bouton « Agenda » (.ics avec rappels J-3 et J-1, compatible Outlook / Google Agenda).
- **Mon profil** : changement de mot de passe et gestion des sessions actives (révoquer, tout déconnecter).

## Déploiement

1. Reconstruire : `docker compose up -d --build` (la migration `V10` s'applique automatiquement au démarrage).
2. Le mot de passe de l'administrateur de développement doit respecter la politique (majuscule, minuscule, chiffre, caractère spécial).
3. Les tests d'intégration (`mvn test`) ont été mis à jour (`SprintSixIntegrationTest`) mais n'ont pas pu être relancés ici (pas d'accès Maven Central) : à exécuter de votre côté.

## Complément : logos

Le nom de l'entreprise apparaissait encore car il est dessiné dans les images du logo, du logo sombre et du favicon.
La migration `V11__remove_company_logos.sql` retire ces images des réglages ; les fichiers ont été déplacés dans
`_sauvegarde_avant_modifs_2026-11-23\uploads\branding`. Un nouveau logo peut être téléversé depuis Administration > Entreprise.
