# Architecture

How NML Online is actually built. Setup and commands are in [README.md](README.md); security
assumptions in [SECURITY.md](SECURITY.md); day-to-day rules for contributors in [AGENTS.md](AGENTS.md).

## 1. System overview

One deployable unit: a Spring Boot application that serves the REST API under `/api/**` and the
compiled Angular SPA as static files. One PostgreSQL database. No message broker, no cache server,
no second service.

```
Browser (Angular 22 SPA)
   │  HTTPS — JSON over /api/**, Bearer access token, HttpOnly refresh cookie, X-Board-Id header
   ▼
Spring Boot 4 (single JVM)
   ├── static files: classpath:/static/ + file:/app/static/ (SPA build, uploaded board assets)
   ├── servlet filters → controllers → services → repositories
   └── in-memory state: turn cache, turn locks, resolution session, rate-limit counters
   ▼
PostgreSQL 14 (schema owned by Flyway)
```

The single-JVM assumption is structural, not incidental: the per-board turn-resolution sessions, the per-board
turn lock and the rate limiters live in memory. Running two instances behind a load balancer would
break turn resolution. See section 8.

## 2. Backend layout (`nml-ms`)

```
com.mg.nmlonline
├── api/controller      REST endpoints; no business logic, no entity returned outside a DTO
├── api/dto             request and response contracts
├── config              security chain, servlet filters, startup runners, exception mapping
├── domain/model        JPA entities and the pure combat engine (domain/model/battle)
├── domain/service      business rules and transaction boundaries
├── infrastructure      Spring Data repositories, CSV catalogue loader
└── mapper              entity ↔ DTO (always called inside a service transaction)
```

Dependencies point one way: `controller → service → repository`, with `mapper` used by services.
`spring.jpa.open-in-view=false` in every profile, so a lazy association touched outside a
service transaction fails immediately instead of issuing hidden queries.

### Request pipeline

Every `/api/**` request crosses these filters before reaching a controller. The first three are ordered explicitly in the security chain:

| Filter | Responsibility |
|---|---|
| `JwtAuthenticationFilter` | Validates the access token (`type=access`), sets `userId` on the request |
| `BoardScopeFilter` | Resolves the game from `X-Board-Id`, checks the caller takes part in it, grants the per-request game-master authority, confines a game master to players of that game |
| `PreparationPhaseFilter` | During turn 0, rejects everything except setup, alliances, notes and admin |
| `GameOverFilter` | After the game ends, rejects mutations outside `/api/admin/**` |
| `EliminatedPlayerFilter` | Rejects mutations from an eliminated player |
| `MutationRateLimitFilter` | 120 mutations per minute per account |

All of them match on the percent-decoded path (`RequestPaths.decoded`), the same form Spring
Security and Spring MVC route on. Errors are returned as RFC 7807 `ProblemDetail`
(`GlobalExceptionHandler`); the SPA reads the `detail` field.

## 3. Domain model

- **Account** (`credentials`): global identity, role `USER` or `ADMIN`.
- **Board**: one board is one game. It owns its sectors (`Board.sectorsList` is the only persisted
  source; the `sectors` map in the DTO is rebuilt by `BoardMapper`).
- **Player**: the participation of an account in one board, unique per `(board, user)` and
  `(board, name)`. An account can play several games.
- **Sector**: `Sector.ownerId` is the single source of territory ownership.
- **Combat entities**: units, characters, vehicles and buildings share one table
  (`combat_entities`) and are attached to a sector by `(board_id, sector_number)`.
- **Storage**: equipment and resources held in a weapon cache or a bank are the same rows as the
  inventory, flagged by `weapon_cache_id` / `bank_id` and hidden from `Player.equipments` /
  `Player.resources` by `@SQLRestriction`.
- **Player actions** (`player_actions`): the journal of what a player did this turn; it drives undo.
- **Press**: `EliminationEvent` (death order), `GazetteIssue` (the game master's newspaper) and
  `TurnJournal` (technical archive) are three distinct things.

### Roles

| Role | Scope | Source |
|---|---|---|
| `ADMIN` | Platform: accounts, game creation and deletion, catalogue | `credentials.role`, carried in the token |
| Game master (`ROLE_INSTANCE_ADMIN`) | One game | `board_admins`, added to the request by `BoardScopeFilter`, never in the token |
| Player | One game | a `players` row for `(userId, boardId)` |

A game master is never a player of the same game. Revoking a game master takes effect on the next
request because the authority is never stored in a token.

## 4. Game lifecycle

```
create board ──► turn 0 (preparation) ──► turn 1..n ──► finished
  preset/import     startGame()            resolution     GameEndService.finishIfOver
```

1. **Creation** — `BoardService.saveBoard` always creates a board at `currentTurn = 0`.
2. **Preparation (turn 0)** — the platform admin creates players, assigns starting sectors, links
   accounts and names game masters. Players place their own forces. `GameSetupService.startGame`
   locks the players (ascending id) then the board, places what was left unplaced and moves to turn 1.
3. **A turn** — players buy, equip, place and give orders. Each mutating service method takes a
   pessimistic lock on the player first (`findByUserIdAndBoardIdForUpdate`), then on the vehicle
   if one is involved. This order is mandatory: the foreign key from `player_actions` takes a
   `KEY SHARE` lock on `players`, so the reverse order deadlocks.
4. **Resolution** — `TurnResolutionOrchestrator` runs the end of turn step by step under the game
   master's control: prepare (reserve units of that game join the headquarters, rupture conflicts
   are detected), movement hops, battles, captures, finalisation. While a resolution is open the
   board's `TurnLock` is held and player mutations — purchases, placements, orders and their
   cancellation, alliance changes, undo, harvest — are refused.
5. **Combat** — `Battle` (`domain/model/battle`) is a pure engine. Every random draw goes through
   an injected `RandomSource`, so tests script the dice and no test is probabilistic.
6. **End** — `GameEndService.finishIfOver` records `finishedAt` and the winner when one player
   remains; `currentTurn` stays at the final turn and the game becomes read-only for players.

`Board.currentTurn` is mutated in three places (`TurnService.advanceTurn`,
`TurnService.setCurrentTurn`, the orchestrator). All three publish the new value to the in-memory
turn cache before commit and purge it on rollback.

## 5. Persistence

- Flyway owns the schema in `test` and `prod` (`db/migration/V<n>__*.sql`, never edited once
  applied); Hibernate only validates. The default and `dev` profiles use in-memory H2 with
  `ddl-auto=update` for a zero-setup local start.
- The `test` profile runs on a real PostgreSQL 14 started inside the JVM, so every integration test
  exercises the production engine and the production migrations.
- `default_batch_fetch_size=50` bounds the query count when a board graph is walked.
- Ids use `IDENTITY`, so Hibernate cannot batch inserts. This is a known ceiling for bulk imports.

## 6. Frontend layout (`nml-ui`)

```
src/app
├── pages/        one lazy-loaded standalone component per route
├── components/   navbar
├── shared/       dialogs, gazette template, pipes
├── services/     HTTP access (ApiService) and signal-based state
├── guards/       authGuard → instanceGuard → playerGuard / adminGuard, preparationGuard
├── models/       TypeScript mirrors of the backend DTOs
└── core/         constants, error interceptor, image helper
```

- **State** lives in root services built on `signal`, `computed` and `httpResource`. There is no
  store library. Components consume state; they do not own global state.
- **Reading a resource**: `httpResource.value()` throws when the request failed. Read it as
  `ref.hasValue() ? ref.value() : fallback`.
- **Active game**: chosen after login and kept in `sessionStorage`, so two tabs can play two games.
  The auth interceptor adds `X-Board-Id` to every `/api/**` call except authentication and
  `/api/instances`. Changing game reloads the page, because root resources are bound to the game.
- **Who may administer**: `AuthService.canAdmin` (platform admin, or game master of the selected
  game). The game-master role comes from the selected game, never from the token.
- **Token refresh**: on a 401 the interceptor refreshes once, then replays the request with the new
  token and the same game header. Concurrent 401s share one refresh request. Only a failed refresh
  logs the user out.
- **Routing**: every route added to `app.routes.ts` must also be forwarded by
  `SpaForwardController`, otherwise a page reload returns 404 in production. A test enforces this.

## 7. API contract

The backend is authoritative for every rule, price and statistic; the SPA never sends a price, a
`playerId` or an owner. The caller is identified by the token, the game by `X-Board-Id`.

Two contracts are pinned by tests because both sides must agree on a value:

- `SaleMultipliersParityTest` reads `nml-ui/src/app/core/sale-multiplier.ts` and compares it with
  the backend table.
- `SpaForwardControllerTest` compares the Angular routes with the forwarded paths.

The TypeScript models are otherwise maintained by hand against the Java DTOs. Generating them from
an OpenAPI description is the natural next step if drift becomes a recurring cost; it was not
introduced because it adds a dependency and a build step for a contract that one team owns end to end.

## 8. Build and deployment

- `Dockerfile` is a three-stage build: Node 24 builds the SPA, Maven with Temurin 25 packages the
  jar, a Temurin 25 JRE Alpine image runs it as a non-root user with the `prod` profile by default.
- Runtime data lives in two directories that should be volumes: `/app/static/boards` (uploaded
  board images) and `/app/logs`.
- `ci.yml` runs backend `clean verify` and frontend lint, tests and build on pull requests.
  `cd.yml` runs the same workflow first and publishes the image only if it passes.

### Known limits

| Limit | Consequence | Way out |
|---|---|---|
| In-memory turn lock, resolution session and rate limiters | One JVM only; an open resolution is lost on restart | Persist the session, move locks and counters to the database |
| `IDENTITY` ids | No insert batching | Sequences plus a migration |
| Hand-written TypeScript models | Contract drift is caught by review, not by the build | Generate from OpenAPI |
