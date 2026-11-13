# AGENTS.md

Fast lane. Détails dans [README.md](README.md) ; cartes SVG dans [SVG_MAP_GUIDE.md](SVG_MAP_GUIDE.md).

## Commandes

```bash
# nml-ms/ (Spring Boot 4.1.1 / Java 25) — .\mvnw.cmd sous Windows
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"        # JWT_SECRET + JWT_PEPPER requis
./mvnw clean test                                              # PostgreSQL 14 embarqué (Zonky), sans config
# Avant de rendre la main : (nml-ms/) ./mvnw clean test ; (nml-ui/) npm test + npm run lint + npm run build.
# `npm run build` est le seul contrôle des templates stricts (strictTemplates) ; la CI l'exige.
# clean obligatoire : un target/classes/db/migration périmé fait échouer Flyway (versions dupliquées).
# clean test exige le repo complet : SaleMultipliersParityTest lit ../nml-ui/src/app/core/sale-multiplier.ts.
# Test d'intégration : @EmbeddedPostgresTest obligatoire, sinon profil default (H2) — pas de parité prod.
# Pas de refresh sur @DirtiesContext : la base unique serait vidée sans réimport des fixtures.
# Cluster PG embarqué persistant : /tmp/embedded-pg garde un run précédent → Flyway échoue sur une
# migration saine ; supprimer ce dossier avant de diagnostiquer le code.
# nml-ui/ (Angular 22)
npm start / npm test / npm run lint / npm run format
npm run build                              # prod (Docker) + pipeline images (génère _opt/, gitignoré)
```

## Commentaires

Défaut : **zéro commentaire**. Un commentaire ne survit que s'il dit **pourquoi** — piège,
arbitrage, couplage inter-fichier, sortie non déductible — en français, 1 ligne max, dans
**tous** les fichiers (Java, TS, HTML, SCSS, SQL, YAML/properties, Dockerfile). Exemple gardé :
`// Sans orphanRemoval : em.remove d'une occurrence.` — exemple supprimé : `// Retire l'équipement.`

Avant de finir une tâche, relire les lignes `+` de `git diff -U0` et supprimer :
- en-têtes de section ou de fichier : `// === Tour ===`, `// --- Helpers ---`, `// Types pour X`,
  `/* Stats globales */`, `<!-- Header -->`, `# --- Sécurité ---`, `# Stage 1 — …` ;
- JSDoc/Javadoc de classe, fichier ou méthode qui redit le nom, les annotations ou les étapes
  (`/** Stocke le token. */`, `/** Ouvre le dialogue. */`) ;
- commentaire qui paraphrase la ligne suivante (`// Démarrer un nouveau refresh`) ou double un
  titre visible (`<!-- Équipements -->` au-dessus d'un `<h2>Équipements</h2>`) ;
- blocs décoratifs `/** … */`, `/* ===== … ===== */`, `# ==== … ====`.

Seul marqueur autorisé : `// ponytail:` + plafond + voie d'upgrade
(`// ponytail: lock global, passer par-compte si le débit devient un souci`).
Réponses et commentaires en **français** ; README en anglais (guide SVG en français).

## Règles dures

- **Multi-partie** : un plateau = une partie. Tout endpoint `/api/**` hors auth et `/api/instances`
  exige l'en-tête `X-Board-Id` (`BoardScopeFilter`) ; jamais de repli sur « la première partie »
  (l'absence d'en-tête n'est tolérée que si la base ne contient qu'un plateau). Tout service qui
  résout un joueur le fait par `(userId, boardId)` — jamais `findByUserId` seul.
- **Rôles** : `ADMIN` est global (compte, création de parties, catalogue) ; le MJ est **par partie**
  (`board_admins` → `ROLE_INSTANCE_ADMIN`, ajoutée à la requête seulement, jamais dans le jeton).
  Les endpoints plateforme gardent `@PreAuthorize("hasRole('ADMIN')")`.
- **Schéma = script Flyway** `V<n>__description.sql` dans
  `nml-ms/src/main/resources/db/migration/` (actif en `test` et `prod` seulement ; jamais modifier
  une migration appliquée). Livré avec l'entité.
- Jamais de `JWT_SECRET` / `JWT_PEPPER` / identifiants DB dans le dépôt — variables
  d'environnement uniquement. Pas de console H2, pas de `@CrossOrigin` (CORS dans `CorsConfig`).
- **JWT** : access token = claim `type=access`, refresh = `type=refresh` ; ne jamais accepter l'un pour l'autre.
- **Races** : ajouter une race = valeur `PlayerRace` + CHECK Flyway sur `equipment_translations.race`
  + `nml-ms/src/main/resources/equipment_translations_<race>.csv` + noms de véhicules dans
  `VehicleType.raceNames` ; vignettes dans `assets/shop/equipment/<race>/` et `assets/shop/vehicles/<race>/`.
- **Verrous** : joueur (`findByUserIdAndBoardIdForUpdate`) puis véhicule (`findByIdForUpdate`) — la FK
  `player_actions.player_id` pose un KEY SHARE sur `players` ; l'ordre inverse deadlock.
- **Lecture** : jamais l'état privé d'autrui — `PlayerDto` complet = propriétaire/admin,
  carte via `BoardMapper.toPublicDto` ; SVG admin = `sanitizeSvg` client + CSP `sandbox` (`/boards/**`).
- **Fin de partie** : `GameEndService.finishIfOver` (depuis `finalizeTurn`) écrit
  `finishedAt`/`winnerPlayerId`/`winnerAdjusted` ; `currentTurn` reste le tour final.
  `GameOverFilter` refuse les mutations hors `/api/admin/**`. `GazetteIssue` (journal du MJ)
  est distincte de `TurnJournal` (archive technique). Un brouillon (`DRAFT`) n'est jamais
  public ; `editor_notes` est admin-only. Aucune victoire d'alliance : la dernière alliance se
  brise et aucune nouvelle n'est acceptée à deux joueurs. Aucun objectif secret.
- Minimal wins : pas d'abstraction spéculative, pas d'échafaudage « pour plus tard »,
  pas de nouvelle dépendance quand quelques lignes suffisent. Supprimer > ajouter.
- Logique non triviale = **un** test qui casse si la logique casse. Pas de suite par fonction.
- **Hasard de combat** : toute chance (critique, esquive, toucher aérien…) passe par
  `RandomSource` injecté dans `Battle` par `CombatService` (`domain/model/battle/`), jamais
  `java.util.Random` ou `Math.random` dans le domaine. Tests unitaires = jets scriptés
  (`BattleTest.rolls`), intégration = `CombatTestRandomConfig` (jet 100, via
  `@EmbeddedPostgresTest`) — aucun test probabiliste.
- **Commentaires** : zéro par défaut — relire les lignes `+` de `git diff -U0` avant de rendre
  la main (voir « Commentaires »). Un `catch` vide ne survit que justifié en une ligne.
- **Règles du jeu** : elles vivent dans la page Règles
  (`nml-ui/src/app/pages/regles/`). Toute modification structurelle du gameplay
  (tour, combat, capture, alliance, échange, visibilité…) doit vérifier que cette page est
  toujours correcte et la mettre à jour **dans le même commit**.

## Backend

`domain/model` → `domain/service` → `api/controller` → `infrastructure/repository`,
`mapper` pour DTO ↔ entité.

- **Ownership** : jamais de `playerId` du body/params — `request.getAttribute("userId")`,
  vérifié dans le service (`SecurityException` → 403).
- `infrastructure/repository` est `@NullMarked` : Spring Data valide les arguments au runtime,
  un `null` sur une méthode de repository lève `IllegalArgumentException` (400) — filtrer
  les entrées (noms d'import, chemins JSON) avant l'appel.
- **Admin** : `/api/admin/**` = `hasAnyRole('ADMIN','INSTANCE_ADMIN')` (règle d'URL) ; les actions
  plateforme portent `@PreAuthorize("hasRole('ADMIN')")` au niveau méthode (elle prime sur la classe).
  `BoardScopeFilter` refuse `403` à un MJ qui vise `/api/admin/players/{id}` d'une autre partie ;
  tout endpoint admin prenant un `{boardId}` de chemin doit le comparer à `X-Board-Id`.
- `Sector.ownerId` = source unique de propriété. `Board.sectorsList` = seule source persistée ;
  `BoardDto.sectors` (map) est reconstruite dans `BoardMapper`. `@JsonIgnore` côté many.
- Cascade JPA : pas d'`orphanRemoval=true` sur une `@OneToMany` dont l'enfant porte une FK
  NOT NULL — préférer `@OnDelete(CASCADE)` + FK `ON DELETE CASCADE` (V5, V6).
- **Stockage cache/banque** : `EquipmentStack.weapon_cache_id` et `PlayerResource.bank_id`
  portent le contenu hors inventaire ; `Player.equipments`/`resources` sont filtrés
  (`@SQLRestriction`) — passer par `getAllEquipmentStacks()`/`getUsableWeaponCaches()` et clore
  les `BUY_EQUIPMENT` actifs (`discardActiveActionsReferencing`) avant tout retrait de stock.
- **Tour** : `TurnService.advanceTurn(boardId)`, `TurnService.setCurrentTurn(boardId, turn)` (navigation dev) et
  `TurnResolutionOrchestrator` mutent `Board.currentTurn` — les trois doivent publier/invalider
  `TurnService.cachedTurns` (`publishTurn(boardId, turn)` avant commit, purge sur rollback).
  Session de l'orchestrateur en mémoire, JVM unique, perdue au redémarrage.
- **Démarrage** : ordre imposé par `@Order` — `DemoDataResetter` 0, `CsvDataLoader` 1,
  `PlayerStartupImporter` 2, `TestDataInitializer` 100. Un runner sans ordre casse les imports
  (le catalogue CSV n'est pas encore chargé quand les joueurs sont lus).
- **`BoardService.saveBoard` fusionne par numéro, ne vide jamais `sectorsList`** : supprimer
  un secteur reste une opération explicite (vider la liste cascade-delete secteurs + armées).
- **Véhicules** : pilote = personnage ou unité `PILOTE_DESTRUCTEUR`, passagers ≤ `VehicleType.capacity` ;
  occupants laissés dans `Sector.army`/`characters` (stats inchangées) et référencés par le véhicule ;
  le véhicule les emporte, ils sont exclus des ordres à pied. Détacher un pilote unité avant `em.remove`
  (FK `pilot_id` portée par la ligne véhicule — `CombatService.detachFromVehicles`).

## Données prod

Source de vérité = la DB. En démo (`app.import-demo-data=true`, défaut dev/test),
`PlayerStartupImporter` importe les `DemoGame` : carte principale (`boards/board.json`,
`players/*.json`) **et** carte 6 joueurs (`boards/carte-25.json`, `players-carte-25/*.json`).
En prod, l'admin crée plateau et joueurs par l'API.

Import et suppression sont **par partie** : `importPlayer(..., boardId)` rattache la participation,
et `AdminService.deleteBoard` supprime les participations (cascades JPA) sans jamais toucher aux
comptes `credentials` — un compte peut jouer plusieurs parties.

**Import admin** : `BoardImportService` refuse les champs inconnus ; les DTO de
`PlayerImportService` les ignorent (`@JsonIgnoreProperties`). Faire évoluer ensemble les DTO
d'import (`BoardDTO`, `PlayerDTO`), l'export admin et les fixtures `boards/*.json`/`players/*.json`,
en gardant l'aller-retour export → import (`AdminExportImportTest`).

## Frontend

Composants standalone, routes lazy via `loadComponent`. Pas de NgRx : état dans des
services à signaux (`signal`/`computed`, `httpResource` pour le serveur). Les composants
consomment, ils ne possèdent pas l'état global.

- **Partie active** : `InstanceService` (choisie après login, en `sessionStorage`) ; l'intercepteur
  pose `X-Board-Id` sur `/api/**` sauf auth et `/instances`. Le rôle MJ vient de `selectedRole()`,
  jamais du jeton.
- **Guards** : `instanceGuard` avant `playerGuard`/`adminGuard` ; changer de partie recharge la page
  (tous les `httpResource` racine sont liés à la partie).
