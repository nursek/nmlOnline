# NML Online

A turn-based strategy game where players manage territories, armies, vehicles, and resources on an interactive map. Built with **Spring Boot 3.5 / Java 21** on the backend and **Angular 22** on the frontend.

## Table of Contents

- [Architecture](#architecture)
- [Quick Start (Local Development)](#quick-start-local-development)
- [Environment Variables Reference](#environment-variables-reference)
- [Spring Profiles](#spring-profiles)
- [Database Migrations (Flyway)](#database-migrations-flyway)
- [Docker Deployment](#docker-deployment)
- [CI/CD](#cicd)
- [CORS](#cors)
- [Authentication Flow](#authentication-flow)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Game Balance Configuration](#game-balance-configuration)
- [Project Structure](#project-structure)
- [Security Notes](#security-notes)
- [IDE Setup](#ide-setup)
- [Common Issues](#common-issues)
- [License](#license)

## Architecture

```
nmlOnline/
├── nml-ms/                    # Spring Boot backend
│   ├── src/main/java/          # Java 21 source
│   ├── src/main/resources/     # Configuration, Flyway migrations (db/migration)
│   ├── src/test/               # Integration & unit tests
│   ├── Dockerfile              # Multi-stage container build
│   └── pom.xml                 # Maven build
│
├── nml-ui-bst-angular/         # Angular 22 frontend
│   ├── src/app/                # Application source
│   │   ├── pages/              # Route pages (login, carte, joueur, boutique, admin…)
│   │   ├── services/           # HTTP, token & signal-based state services
│   │   ├── guards/             # Auth & admin route guards
│   │   ├── models/             # TypeScript interfaces
│   │   └── core/               # Constants, interceptors
│   ├── src/environments/       # Dev & prod config
│   └── angular.json
│
├── pom.xml                     # Parent POM
├── Dockerfile                  # Backend Docker image
└── .gitignore
```

### Backend stack

- Java 21, Spring Boot 3.5.6, Spring Data JPA, Spring Security
- H2 (dev/test) or PostgreSQL (production)
- Flyway for schema migrations (production PostgreSQL only)
- JWT authentication with HttpOnly refresh-token cookie
- Lombok, MapStruct, SpringDoc OpenAPI (Swagger)

### Frontend stack

- Angular 22, standalone components, signals, control flow (`@if`/`@for`)
- Angular Material, SCSS
- Signal-based state in services (`signal`/`computed`/`httpResource`) — no NgRx
- Jest for unit tests, ESLint + Prettier for linting/formatting

---

## Quick Start (Local Development)

### Prerequisites

- **JDK 21+** (tested with JDK 25)
- **Maven 3.9+** (or use the included `mvnw` wrapper)
- **Node.js 20+** and **npm 10+**
- **Git**

### 1. Clone & Backend

```bash
git clone https://github.com/YOUR_USER/nmlOnline.git
cd nmlOnline/nml-ms
```

The backend needs two environment variables to start:

```bash
# Linux / macOS
export JWT_SECRET="dev-only-secret-key-at-least-32-characters-long-do-not-use-in-prod"
export JWT_PEPPER="dev-only-pepper-secret-at-least-16-characters"

# Windows PowerShell
$env:JWT_SECRET="dev-only-secret-key-at-least-32-characters-long-do-not-use-in-prod"
$env:JWT_PEPPER="dev-only-pepper-secret-at-least-16-characters"
```

Then run with the `dev` profile to get seed data:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Or, on Windows:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

The backend starts on **http://localhost:8080**.

Swagger UI: **http://localhost:8080/swagger-ui.html**

### 2. Frontend

```bash
cd ../nml-ui-bst-angular
npm install
npm start      # runs on http://localhost:4200, proxies /api → localhost:8080
```

Open **http://localhost:4200** in your browser.

### 3. Dev accounts (dev profile only)

| Username | Password | Role |
|----------|----------|------|
| test | test | USER |
| a | a | USER |
| lurio | lurio | USER |
| nursek | nursek | USER |
| admin | admin | ADMIN |

---

## Environment Variables Reference

### Required (all environments)

| Variable | Min length | Description |
|----------|-----------|-------------|
| `JWT_SECRET` | 32 chars | HMAC signing key for JWT access tokens. **App will not start without it.** |
| `JWT_PEPPER` | 16 chars | Pepper prepended to passwords before BCrypt. **App will not start without it.** |

### Required (production only)

| Variable | Example | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://db:5432/nmlonline` | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | `nmlonline` | DB user |
| `DATABASE_PASSWORD` | *(secret)* | DB password |

### Optional (all environments)

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | *(none)* | `dev` for seed data, `prod` for PostgreSQL |
| `SERVER_PORT` | `8080` | HTTP port |
| `APP_COOKIE_SECURE` | `true` (default), `false` (dev) | Set `false` for local HTTP |
| `APP_CORS_ALLOWED_ORIGINS` | *(empty)* | Comma-separated extra CORS origins |
| `NML_BALANCE_HEADQUARTERS_RECONSTRUCTION_SAME_LOCATION_COST` | `75000` | HQ rebuild cost (same sector) |
| `NML_BALANCE_HEADQUARTERS_RECONSTRUCTION_OTHER_LOCATION_COST` | `150000` | HQ rebuild cost (different sector) |
| `NML_BALANCE_HEADQUARTERS_WEALTH_STORAGE_PERCENTAGE` | `0.25` | Fraction of money protected on HQ capture |
| `NML_BALANCE_HEADQUARTERS_MOVE_COOLDOWN` | `5` | Turn cooldown after HQ move |
| `NML_BALANCE_RESOURCE_SALE_MULTIPLIERS` | `1.0,3.0,6.0,...` | Sale price multipliers per quantity tier |

---

## Spring Profiles

### Default (no profile) — H2 in-memory, no seed data

```bash
JWT_SECRET=... JWT_PEPPER=... ./mvnw spring-boot:run
```

- Database: H2 in-memory, `ddl-auto=update`
- `app.cookie.secure=true` (override to `false` for local HTTP testing)
- No dev accounts created
- Swagger UI available

### `dev` — H2 in-memory + seed data

```bash
JWT_SECRET=... JWT_PEPPER=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

- Same H2 in-memory DB
- `app.cookie.secure=false` (works over HTTP)
- **Auto-creates the 5 dev accounts** listed above
- H2 console **disabled** (even in dev)

### `test` — Used by Maven Surefire

```bash
./mvnw test   # activates automatically
```

- H2 in-memory, `ddl-auto=create-drop`
- Hardcoded test secrets (in-memory only)
- No external configuration needed

### `prod` — PostgreSQL, production-ready

```bash
export JWT_SECRET="your-32+-char-production-secret"
export JWT_PEPPER="your-16+-char-production-pepper"
export DATABASE_URL="jdbc:postgresql://your-db-host:5432/nmlonline"
export DATABASE_USERNAME="nmlonline"
export DATABASE_PASSWORD="your-secure-password"
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

- `ddl-auto=validate` — schema is managed by Flyway (see below)
- `app.cookie.secure=true` — refresh cookies are `Secure`
- Actuator: `/actuator/health`, `/actuator/info`, `/actuator/metrics`
- No seed data

---

## Database Migrations (Flyway)

Schema migrations are handled by **Flyway**, enabled in the `prod` profile only
(`spring.flyway.enabled=false` by default, so H2 dev/test keep using `ddl-auto` and are completely unaffected).

- Migration scripts live in `nml-ms/src/main/resources/db/migration/`, named `V<n>__description.sql`.
- `V1__baseline.sql` is a `pg_dump --schema-only` of the production database as of 2026-07-23.
- On the **existing prod database**: `baseline-on-migrate=true` marks it as version 1 **without executing V1**,
  then applies any newer migrations on startup.
- On a **fresh/empty database** (new server, staging): V1 runs and creates the full schema.

### Adding a migration

Any schema change (new entity, new column, new index…) MUST ship as a migration script:

1. Create `V2__add_score_column_to_players.sql` (next free version number) in `db/migration/`.
2. Update the JPA entity accordingly — `ddl-auto=validate` will refuse to start if they diverge.
3. Commit both together. Migrations run automatically at application startup.

Never edit an already-applied migration file — always add a new one.

---

## Docker Deployment

The root `Dockerfile` is a multi-stage build that compiles the Angular frontend, packages the Spring Boot backend, and produces a single runtime image with static assets embedded.

### Image features

- **Non-root**: runs as the `nmlonline` user
- **Health check**: polls `/actuator/health` every 30 seconds
- **Static assets**: Angular build copied to `/app/static/`, served by Spring Boot

### Build

```bash
# Full-stack image (backend + embedded Angular UI) — build from repo root
docker build -t nml-online/backend:latest .
```

### Run

```bash
docker run -d \
  --name nml-backend \
  -p 8080:8080 \
  -e JWT_SECRET="your-32+-char-production-secret" \
  -e JWT_PEPPER="your-16+-char-production-pepper" \
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/nmlonline" \
  -e DATABASE_USERNAME="nmlonline" \
  -e DATABASE_PASSWORD="your-secure-password" \
  -e SPRING_PROFILES_ACTIVE="prod" \
  -e APP_COOKIE_SECURE="true" \
  -e APP_CORS_ALLOWED_ORIGINS="https://nml.yourdomain.com" \
  nml-online/backend:latest
```

### Serve the frontend

When using the root `Dockerfile`, the frontend is already included. If you prefer to mount it separately:

```bash
cd nml-ui-bst-angular
npm run build
# Output: dist/nml-ui-copilot-angular/browser/
```

Copy the build output to either:
- `nml-ms/src/main/resources/static/` (bundled into the JAR — rebuild required)
- `/app/static/` inside the Docker container (no rebuild — mount a volume)

The backend serves static files from both `classpath:/static/` and `file:/app/static/`.

---

## CI/CD

GitHub Actions workflows live in `.github/workflows/`.

### `ci.yml` — Tests & Lint

Runs on every pull request and on pushes to non-master branches:

- Backend: `./mvnw test` + `./mvnw package -DskipTests`
- Frontend: `npm run lint` + `npm test -- --ci --coverage` + `npm run build`

### `cd.yml` — Build & Push Docker Image

Runs on pushes to `master` and version tags (`v*`):

1. Builds a `linux/amd64` Docker image
2. Pushes to `ghcr.io/nursek/nmlonline`
3. Tags: `latest`, short commit SHA, and semver tags for releases

No manual release steps are required; merging to `master` automatically publishes a new `latest` image.

---

## CORS

Allowed origins are configured in `CorsConfig.java`. Defaults:

| Origin | Environment |
|--------|-------------|
| `http://localhost:5174` | Vite dev |
| `http://localhost:5173` | Vite dev |
| `http://localhost:3000` | Generic dev |
| `http://localhost:4200` | Angular CLI |
| `https://nml.lurio.fr` | Production |

Add more via `APP_CORS_ALLOWED_ORIGINS`:

```bash
# Single
APP_CORS_ALLOWED_ORIGINS=https://nml.example.com

# Multiple (comma-separated)
APP_CORS_ALLOWED_ORIGINS=https://nml.example.com,https://admin.example.com
```

> **Note:** The insecure `http://nml.lurio.fr` origin was removed. All production origins must be HTTPS.

---

## Authentication Flow

```
┌─────────┐  POST /api/login         ┌─────────┐
│ Browser │ ────────────────────────► │ Backend  │
│         │ ◄─────────────────────── │          │
│         │  { accessToken, id,     │          │
│         │    name, role }          │          │
│         │  Set-Cookie: refresh_token (HttpOnly, Secure, SameSite=Lax)
└─────────┘                          └─────────┘

┌─────────┐  POST /api/auth/refresh  ┌─────────┐
│ Browser │ ────────────────────────► │ Backend  │
│ (cookie  │ ◄─────────────────────── │          │
│  sent    │  { valid, token, id,    │          │
│  auto)   │    name, role }         │          │
│         │  Set-Cookie: refresh_token (rotated)
└─────────┘                          └─────────┘
```

- **Access token**: stored in `sessionStorage`, sent as `Authorization: Bearer <token>`
- **Refresh token**: stored as `HttpOnly` cookie, sent automatically by the browser
- **SessionStorage**: cleared on tab close (more secure than `localStorage` against XSS)
- **Rate limiting**: login and refresh endpoints are rate-limited (5 attempts / 1 minute block)

---

## API Endpoints

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/api/register` | Public | Create an account |
| POST | `/api/login` | Public | Login (returns JWT + sets refresh cookie) |
| POST | `/api/auth/refresh` | Cookie | Refresh access token |
| POST | `/api/auth/logout` | Cookie | Logout (clears refresh token) |
| GET | `/api/players?page=0&size=20` | Bearer | List players (paginated) |
| GET | `/api/players/{name}` | Bearer | Get player by name |
| POST | `/api/players/equipment/buy` | Bearer | Buy equipment (atomic) |
| POST | `/api/players/resources/{id}/sell` | Bearer | Sell a resource |
| POST | `/api/players/resources/sell-batch` | Bearer | Sell multiple resources (atomic) |
| GET | `/api/boards` | Public | List boards |
| GET | `/api/equipment?page=0&size=100` | Public | List equipment (paginated) |
| POST | `/api/vehicles/buy-batch` | Bearer | Buy vehicles (atomic batch) |
| GET | `/api/characters/player/{playerId}` | Bearer+Owner | Get player's character |
| GET | `/api/buildings/headquarters/{playerId}` | Bearer+Owner | Get player's HQ |
| POST | `/api/buildings/{id}/move` | Bearer+Owner | Move building to another sector |
| GET | `/api/admin/players?page=0&size=20` | Admin | List all players (paginated) |
| POST | `/api/admin/players/import` | Admin | Import player from JSON |
| GET | `/actuator/health` | Public | Health check |
| GET | `/swagger-ui.html` | Public | API documentation |

---

## Testing

### Backend

```bash
cd nml-ms

# Unit + integration tests (uses test profile automatically)
./mvnw test

# Package without tests
./mvnw package -DskipTests

# Full verification (compile + test + package)
./mvnw verify
```

214 tests across unit tests (domain models, game-logic services with mocked repositories) and integration tests (Spring context, security ownership). Game rules are pinned as characterization tests: economy (purchases, sale multipliers), units (experience thresholds, injury, equipment formulas), buildings (cooldowns, capture, vampirisation), vehicles (pilot rules, balance table), combat (deterministic phases, no-evasion scenarios) and player stats formulas. Behavior suspected to be buggy is pinned with a characterization test rather than fixed.

### Frontend

```bash
cd nml-ui-bst-angular

# Unit tests (Jest)
npm test

# Lint
npm run lint

# Format
npm run format

# Production build
npx ng build
```

Jest unit tests on services and guards (NgRx was removed in favor of signal-based services).

---

## Game Balance Configuration

Game constants are externalized in `BalanceProperties` and can be overridden without code changes:

```properties
# application.properties or environment variables

# Headquarters
nml.balance.headquarters.reconstruction-same-location-cost=75000
nml.balance.headquarters.reconstruction-other-location-cost=150000
nml.balance.headquarters.wealth-storage-percentage=0.25
nml.balance.headquarters.move-cooldown=5

# Resource sale multipliers (index = quantity - 1)
nml.balance.resource-sale-multipliers=1.0,3.0,6.0,9.0,13.0,19.5,24.5,33.0,45.0
```

---

## Project Structure

### Backend (`nml-ms/`)

```
src/main/java/com/mg/nmlonline/
├── api/
│   ├── controller/       # REST controllers
│   └── dto/              # Data transfer objects
├── config/
│   ├── properties/       # @ConfigurationProperties (BalanceProperties)
│   ├── SecurityConfig     # JWT filter, CORS, method security
│   ├── CorsConfig        # Allowed origins
│   ├── GlobalExceptionHandler  # RFC 7807 ProblemDetail
│   ├── JwtSecretValidator    # Fail-fast secret validation
│   └── DevDataInitializer    # Seed data for dev profile
├── domain/
│   ├── model/
│   │   ├── battle/       # Combat engine (seedable RNG)
│   │   ├── board/        # Board & sectors
│   │   ├── building/     # Buildings (HQ, Weapon Cache)
│   │   ├── equipment/    # Equipment & stacks
│   │   ├── movement/     # Movement service
│   │   ├── player/       # Player, resources, stats
│   │   ├── resource/     # Resource types
│   │   ├── sector/       # Sector model
│   │   ├── unit/         # Units, characters, combat entities
│   │   ├── user/         # User, auth
│   │   └── vehicle/      # Vehicles
│   └── service/          # Business logic services
├── infrastructure/
│   └── repository/       # Spring Data JPA repositories
├── mapper/               # DTO ↔ entity mappers
└── NmlOnlineApplication.java
```

### Frontend (`nml-ui-bst-angular/`)

```
src/app/
├── pages/
│   ├── login/            # Login form (NonNullableFormBuilder, signals)
│   ├── carte/            # Interactive SVG map (afterNextRender, signals)
│   ├── joueur/           # Player profile + units
│   ├── boutique/         # Equipment & vehicle shop (takeUntilDestroyed)
│   ├── admin/            # Admin panel (effects, MatDialog confirm)
│   ├── regles/           # Game rules (static)
│   └── not-found/        # 404 page
├── components/
│   └── navbar/           # Navigation bar (effect, signals)
├── shared/
│   ├── confirm-dialog/   # Material confirm dialog
│   └── purchase-success-dialog/  # Purchase feedback
├── services/
│   ├── api.service.ts          # Centralized HTTP calls
│   ├── auth.service.ts         # Auth state (signals)
│   ├── player.service.ts       # Player state (signals)
│   ├── shop.service.ts         # Shop catalogs (httpResource) + cart state
│   ├── admin.service.ts        # Admin state (httpResource)
│   ├── auth.interceptor.ts     # JWT refresh interceptor
│   ├── token.service.ts        # JWT token management (sessionStorage)
│   └── cart-storage.service.ts # Cart validation & sessionStorage
├── guards/
│   ├── auth.guard.ts     # Requires authentication
│   └── admin.guard.ts    # Requires admin role
├── models/               # Feature-split TypeScript interfaces
└── core/
    ├── constants.ts            # Timeouts, SnackBar durations, etc.
    └── http-error.interceptor.ts  # Timeout & retry
```

---

## Security Notes

- **Passwords** are hashed with BCrypt + pepper (`JWT_PEPPER` must be the same across restarts)
- **JWT access tokens** expire after 10 minutes, stored in `sessionStorage`
- **Refresh tokens** are HttpOnly cookies rotated on each use, with a grace period for duplicate requests
- **CORS** is centralized in `CorsConfig` — no `@CrossOrigin` annotations on controllers
- **Ownership checks**: `GameCharacterController` and `BuildingController` verify the authenticated user owns the requested `playerId`
- **Admin endpoints** (`/api/admin/**`) require the `ADMIN` role
- **Rate limiting**: Login endpoint limits to 5 attempts per IP+username before blocking for 1 minute
- **JWT secret validation**: App fails to start if `JWT_SECRET` < 32 chars or `JWT_PEPPER` < 16 chars
- **No H2 console** in any profile (removed from dev and test)

---

## IDE Setup

### IntelliJ IDEA

1. Install the **Lombok** plugin (`Settings → Plugins`)
2. Enable annotation processing: `Settings → Build → Compiler → Annotation Processors → Enable`
3. Set project SDK to JDK 21+
4. Set file encoding to UTF-8: `Settings → Editor → File Encodings → UTF-8`
5. Backend formatting: 4-space indent, UTF-8

### VS Code (frontend)

1. Install extensions: `angular.ng-template`, `dbaeumer.vscode-eslint`, `esbenp.prettier-vscode`, `editorconfig.editorconfig`
2. The project includes `.editorconfig` and `eslint.config.mjs`
3. Format on save is recommended

```bash
cd nml-ui-bst-angular
npm run format   # format all files
npm run lint      # check for errors
```

---

## Common Issues

### App won't start: "jwt.secret must be at least 32 characters"

Set the `JWT_SECRET` and `JWT_PEPPER` environment variables (see [Environment Variables](#environment-variables-reference)).

### Frontend can't reach backend

Make sure the backend is running on `localhost:8080`. The Angular dev server proxies `/api` requests via `angular.json` configuration.

### Login doesn't work over HTTP

Set `APP_COOKIE_SECURE=false` when running locally without HTTPS:

```bash
export APP_COOKIE_SECURE=false
```

### Dev accounts not created

You must activate the `dev` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Password hashes don't match after changing pepper

The pepper is used in both `encodePassword` and `checkPassword`. If you change `JWT_PEPPER`, all existing password hashes become invalid. Users must re-register.

---

## License

This project is private and proprietary. All rights reserved.