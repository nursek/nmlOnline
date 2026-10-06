# nml-ms — NML Online Backend

Spring Boot 3.5.6 / Java 21 backend (JWT auth, players, armies, equipment, buildings, resources, combat, movement).

Setup, env vars, profiles, dev accounts and endpoints: see the [root README](../README.md).

Schema changes (entities, columns, indexes) must ship as a Flyway script in
`src/main/resources/db/migration/` (`V<n>__description.sql`) — see
[Database Migrations](../README.md#database-migrations-flyway) in the root README.

## Classpath data fixtures

| Resource | Read by | Role |
|---|---|---|
| `equipments.csv`, `resources.csv`, `compatibility.csv` | `CsvDataLoader` (`@Order(1)`, runs before the player import) | equipment catalogue + resources, seeded at boot; `compatibility.csv` is keyed by equipment **name**, never by generated id |
| `equipment_translations_<race>.csv` | `CsvDataLoader` | per-faction equipment labels (one file per race, `name,displayName`, replayed at each boot, missing row or file = English fallback). Only listed equipment is updated: a row removed from a CSV is not purged, and equipment created through the admin API keeps its labels |
| `boards/board.json` | `PlayerStartupImporter` | demo board |
| `players/*.json` | `PlayerStartupImporter` | the 7 demo players (login created if missing, password = username) |