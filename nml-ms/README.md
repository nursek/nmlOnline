# nml-ms — NML Online Backend

Spring Boot 3.5.6 / Java 21 backend (JWT auth, players, armies, equipment, buildings, resources, combat, movement).

Setup, env vars, profiles, dev accounts and endpoints: see the [root README](../README.md).

Schema changes (entities, columns, indexes) must ship as a Flyway script in
`src/main/resources/db/migration/` (`V<n>__description.sql`) — see
[Database Migrations](../README.md#database-migrations-flyway) in the root README.
