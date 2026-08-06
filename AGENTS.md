# AGENTS.md

Details live in [README.md](README.md) — read it first. This file is only the fast lane.
`.github/copilot-instructions.md` covers the same conventions for Copilot — keep both in sync.

## Layout

- `nml-ms/` — Spring Boot 3.5.6 / Java 21 backend (Maven)
- `nml-ui-bst-angular/` — Angular 22 frontend (npm, Jest)

## Commands

```bash
# Backend (from nml-ms/)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"   # run (needs JWT_SECRET + JWT_PEPPER env)
.\mvnw.cmd test                                                # ~240 tests, H2, no config needed
.\mvnw.cmd package "-DskipTests"

# Frontend (from nml-ui-bst-angular/)
npm start / npm test / npm run lint / npm run format
```

## Hard rules

- **Schema change = Flyway script.** Add `V<n>__description.sql` in
  `nml-ms/src/main/resources/db/migration/` alongside the entity change. Flyway runs in
  `prod` profile only; H2 dev/test stay on `ddl-auto`. Never edit an applied migration.
- Never commit `JWT_SECRET` / `JWT_PEPPER` / DB credentials — env vars only.
- No H2 console, no `@CrossOrigin` on controllers (CORS is in `CorsConfig`).
- Buggy-but-observed game behavior: pin it with a characterization test and a
  `comportement actuel piné — à revoir` comment instead of silently fixing.
- Code comments and generated replies in **French**; README/docs in English.

## Working style (ponytail)

- Minimal diff wins. No speculative abstractions, no scaffolding "for later" (YAGNI).
- Deletion over addition; boring over clever.
- Use what's already there (Spring/JPA/Angular features, installed deps) — no new dependency
  when a few lines of code do the job.
- Non-trivial logic leaves ONE runnable check behind (a small test), not a test suite.
- Deliberate shortcuts get a `ponytail:` comment naming the ceiling and the upgrade path
  (example in `application-prod.properties`).

## Backend conventions

- Layers: `domain/model` (entities) → `domain/service` (business logic) → `api/controller` (REST)
  → `infrastructure/repository` (JPA) → `mapper` (DTO ↔ entity). Respect the separation.
- **Ownership**: never trust a `playerId` from body/params — take `userId` from
  `request.getAttribute("userId")` and verify ownership in the service (`SecurityException` → 403).
- **Admin**: global CRUD ops go under `/api/admin/**` with `@PreAuthorize("hasRole('ADMIN')")`,
  never in player controllers.
- Single source of sector ownership: `Sector.ownerId`.
- `Board.sectors` is a transient map populated via `@PostLoad` — don't persist it directly.
- JPA relations: `@JsonIgnore` on the many side to avoid JSON loops.

## Prod data persistence

- **Sources of truth are the DB, not JSON files.** `boards/board.json` and
  `players/*.json` are *classpath demo fixtures*. They are only read at boot when
  `app.import-demo-data=true` (dev/test default). In prod `app.import-demo-data=false`
  → `PlayerStartupImporter` returns early, nothing is imported, the admin creates the
  board and players via the API. Don't treat those JSON as prod state.
- **`BoardService.saveBoard` is non-destructive.** It merges sectors by number: it
  refreshes `income/name/resourceName/x/y/neighbors` on existing sectors and adds
  new ones, but NEVER clears `sectorsList` — that used to cascade-delete sectors +
  armies and wipe `owner_id` on every boot (the prod bug). Removing a sector is no
  longer possible through this path on purpose; it must stay an explicit op.
- **Schema change = Flyway `V<n>__*.sql`** under `db/migration/` (prod only; H2
  stays on `ddl-auto`). Never edit an applied migration. If a destructive/migration
  is impossible to express in Flyway (data repair, backfill), put a one-shot manual
  script next to it, NOT under `db/migration/` (Flyway would auto-run it). See
  `db/recovery/rebuild-sector-owners.sql` for the pattern.
- **Recovery after an owner_id wipe:** if units/buildings survived, run
  `rebuild-sector-owners.sql` (derives `owner_id` from surviving
  `combat_entities.player_id`). If they were cascade-deleted, no SQL can rebuild —
  restore a dump or re-import players from exported `players/*.json` via
  `POST /api/admin/players/import`.

## Frontend conventions

- Standalone components only; lazy routes via `loadComponent`.
- No NgRx — state lives in signal-based services (`signal`/`computed`, `httpResource` for
  server data); components consume them, they don't own global state.
