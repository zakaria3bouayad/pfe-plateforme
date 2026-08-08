# Plateforme de gestion des projets de fin d'etudes (PFE)

Application web centralisant la gestion des PFE : sujets, equipes, projets,
jalons, documents versionnes, messagerie et bibliotheque des rapports archives.

Projet de fin d'etudes.

## Architecture

Monolithe modulaire en couches (N-Tier), avec un service d'intelligence
artificielle externalise. Voir `docs/conception_uml_pfe_v2.pdf`.

| Couche | Technologies |
|---|---|
| Presentation | Vue.js 3, Pinia, Vue Router, Vuetify, Axios |
| Applicative | Spring Boot 4.1, Spring Security, JWT, REST |
| Persistance | PostgreSQL 16 + pgvector, Redis, MinIO, Flyway |
| Services / IA | Embeddings, similarite, resume, assistant RAG |

## Prerequis

- JDK 21 ou superieur
- Node.js 22 LTS
- Docker Desktop

## Demarrage

### 1. Infrastructure

```bash
docker compose up -d
```

Trois conteneurs demarrent : PostgreSQL (port 5433), Redis (6379), MinIO (9000/9001).

Une seule fois, dans la base `pfe_db` :

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run
```

API disponible sur http://localhost:8081

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Interface disponible sur http://localhost:5173

## Comptes de demonstration

Crees automatiquement au premier demarrage (profil non-production).

| Role | Email | Mot de passe |
|---|---|---|
| Administrateur | admin@pfe.local | Admin@2026 |

Les comptes etudiant et encadrant se creent depuis la page d'inscription.

## Avancement

- [x] Lot 0 — Environnement
- [x] Lot 1 — Entites de base et authentification JWT
- [ ] Lot 2 — Sujets, equipes, projets
- [ ] Lot 3 — Jalons et tableaux de bord
- [ ] Lot 4 — Documents versionnes
- [ ] Lot 5 — Messagerie et bibliotheque
- [ ] Lot 6 — Fonctionnalites IA
- [ ] Lot 7 — Finitions
