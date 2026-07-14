# CLAUDE.md

Guidance for AI assistants (and humans) working in this repository.

## Project

BikeMatch analyzes the rear-suspension kinematics of mountain bikes: the user
marks the suspension pivots on a side photo and the backend computes the
behavior curves (leverage, axle path, pedal kickback). It is the final MVP
project of a Java bootcamp. The full brief, work plan and sprint roadmap live
in `docs/`.

## Structure (monorepo)

- `backend/`  — Spring Boot REST API (Java 21). Hand-written; the core of the project.
- `frontend/` — React + Vite (JavaScript). May be AI-generated, but must stay explainable.
- `docker/`   — docker-compose for local infrastructure (PostgreSQL 16).
- `docs/`     — project brief, work plan, sprint roadmap and the kinematics
  knowledge base (`base-conocimiento-cinematica.md`, source of truth for how
  the engine's numbers are interpreted by the AI layer).

## Running locally

```bash
# 1. Start the database (from the repo root)
docker compose -f docker/docker-compose.yml up -d

# 2. Backend (needs the database running)
cd backend && ./mvnw spring-boot:run       # http://localhost:8080

# 3. Frontend
cd frontend && npm install && npm run dev  # http://localhost:5173
```

Before running, copy `.env.example` to `.env` (repo root) and
`frontend/.env.example` to `frontend/.env`. Real secrets never go in the repo.

## Hard rules

- **Do not change the Spring Boot version** in `backend/pom.xml`: it is the
  version used by the course.
- **Add new dependencies only in the sprint that needs them** (springdoc, jjwt,
  JaCoCo…), never earlier.
- **The database schema belongs to Flyway.** Every schema change is a versioned
  migration in `backend/src/main/resources/db/migration`. Never create or alter
  tables by hand. Hibernate runs with `ddl-auto=validate`.
- **Language:** code, API, database and commit messages in English. UI text
  lives in the react-i18next translation files (English is the base language).

## Workflow

- Follow the sprint roadmap in `docs/hoja-de-ruta-sprints.md`. Work only on the
  current sprint; if something seems missing or out of scope, ask first.
- One task = one branch = one issue = one small pull request.
