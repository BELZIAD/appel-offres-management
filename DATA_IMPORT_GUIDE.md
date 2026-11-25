# Guide d'import des donnees reelles

Ce dossier est reserve a la preparation des futurs imports de donnees reelles. Ne pas placer de donnees de production dans le depot Git.

## Principes

- Importer d'abord les employes, puis les partenaires, puis les appels d'offres, puis les affectations.
- Utiliser des fichiers CSV ou Excel controles avant import.
- Ne pas importer de donnees de test en environnement `demo` ou `prod`.
- Conserver une sauvegarde PostgreSQL et une sauvegarde du dossier `uploads` avant chaque import.

## Format employees

Colonnes recommandees :

```csv
firstName,lastName,email,phone,position,department,enabled
```

Contraintes :

- `email` obligatoire et unique.
- `firstName` et `lastName` obligatoires.
- `enabled` vaut `true` ou `false`.

## Format partners

Colonnes recommandees :

```csv
name,tradeName,ice,rc,ifNumber,address,city,country,email,phone,contactPerson,active,notes
```

Contraintes :

- `name` obligatoire.
- `active` vaut `true` ou `false`.

## Format tenders

Colonnes recommandees :

```csv
reference,marketNumber,title,organization,category,publicationDate,deadline,estimatedAmount,provisionalGuarantee,priority,status,result
```

Contraintes :

- `reference`, `title`, `organization` et `deadline` obligatoires.
- `priority` : `LOW`, `NORMAL`, `HIGH`, `URGENT`.
- `status` : `NEW`, `TO_STUDY`, `IN_PROGRESS`, `PREPARING`, `READY_TO_SUBMIT`, `SUBMITTED`, `CLOSED`.
- `result` : `PENDING`, `WON`, `LOST`, `CANCELLED`.

## Format assignments

Colonnes recommandees :

```csv
tenderReference,employeeEmail,assignmentRole
```

Contraintes :

- `tenderReference` doit exister dans les appels d'offres importes.
- `employeeEmail` doit exister dans les employes importes.
- `assignmentRole` : `TENDER_MANAGER`, `PRICING_MANAGER`, `SUBMISSION_MANAGER`, `CONTRIBUTOR`.

## Procedure future

1. Creer un backup PostgreSQL.
2. Sauvegarder le dossier `uploads`.
3. Valider les fichiers source.
4. Importer en environnement de preproduction.
5. Verifier les tableaux de bord, listes, affectations et exports.
6. Importer en production uniquement apres validation metier.
