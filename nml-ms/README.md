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
| `boards/board.json` | `PlayerStartupImporter` | demo board |
| `players/*.json` | `PlayerStartupImporter` | the 5 demo players |
| `humains.csv`, `nekrons.csv`, `orks.csv`, `tyrannides.csv` | **nothing yet — work in progress** | per-faction equipment catalogues (same 8-column layout as `equipments.csv`) |