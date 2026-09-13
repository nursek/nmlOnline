# Rapport d'analyse Sonar — nmlOnline

Date : 2026-09-13
Périmètre : `nml-ms` (157 fichiers Java main, 43 tests), `nml-ui-bst-angular/src` (89 TS, 18 HTML, 19 SCSS),
migrations SQL, Dockerfile, workflows, `tools/`.
Analyse : statique manuelle, revue ciblée sur les règles `Sonar way` (sonar-java, SonarJS/TS, Web, Docker, GitHub Actions).

## Méthode et limites

- **Aucun rapport Sonar réel n'existe** dans le dépôt ni sur la machine : pas de serveur SonarQube/SonarCloud,
  pas de `sonar-project.properties`, pas de rapport exporté. Seul SonarLint est installé dans l'IDE (findings non persistés).
- L'analyse a donc été faite à la main par grep + lecture du contexte, pas avec le moteur Sonar. Les IDs et sévérités
  sont **estimés** ; certains checks (S5976, S6418, S6505, S2699) dépendent de la version du moteur et seraient à confirmer
  sur un serveur.
- Les règles désactivées dans le SonarLint local ont été exclues du rapport :
  `java:S106, S107, S1104, S112, S1135, S1144, S1172, S1192, S125, S1481, S3776`.
  `S3776` (complexité cognitive) reste mentionné en annexe car un profil serveur le réactiverait.
- Fichiers en cours de modification (working tree non commité, travail « battle reports ») inclus dans l'analyse.
- Les findings marqués « vérifié » ont été relus directement dans le code source lors de la consolidation.

## Résumé

| Gravité | Nombre | Nature |
|---|---|---|
| Blocker/Critical | 4 | 2 hotspots sécurité, 1 secret packagé, 1 test vide |
| Major | ~25 | 13 bugs/maintenabilité backend, 9 accessibilité front, 3 infra |
| Minor | ~30 | cosmétique front, imports, catch larges, config |
| Info / Won't fix | ~20 | indicateurs, faux positifs confirmés |

Total brut : ~80 findings actionnables après déduplication, sur ~110 relevés.

---

## P0 — Sécurité / hotfix triviaux

### 1. `S2068` / `S6418` — Secrets JWT en dur packagés dans le jar (Blocker règle / Major réel)

- `nml-ms/src/main/resources/application-test.properties:13-14`
  ```properties
  jwt.secret=test-secret-key-for-ci-at-least-32-chars-long
  jwt.pepper=test-pepper-value-for-ci-tests-only
  ```
- Le fichier est sous `src/main/resources` : les clés sont **embarquées dans le jar de production**. Si le profil
  `test` est activé par erreur en prod, la clé de signature JWT est publique. `JwtSecretValidator` ne vérifie que
  la longueur, pas les valeurs connues.
- **Fix** : déplacer le fichier en `nml-ms/src/test/resources/application-test.properties` (seul le classpath de test
  en a besoin via `@EmbeddedPostgresTest`).
- Autres secrets littéraux (impact réel faible, test-only, à marquer Won't fix ou à laisser) :
  `test/config/TestDataInitializer.java:24` (`PASSWORD = "password"`),
  `domain/service/AdminServiceTest.java:52` (`"s3cr3t"`),
  `ProdBootParityTest.java:47-49`, `DemoDataRebootTest.java:68-70`.
- Connexe (pas un littéral) : `config/DevDataInitializer.java:52,55` crée les comptes dev avec
  **le nom d'utilisateur comme mot de passe** (profil `dev` uniquement).

### 2. `S4502` — CSRF ignoré sur `/api/**` (Critical hotspot, à valider)

- `nml-ms/src/main/java/com/mg/nmlonline/config/SecurityConfig.java:74`
  ```java
  http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
  ```
- L'access token est bien en header `Authorization`, mais `/api/auth/refresh` et `/api/auth/logout` s'authentifient
  par le cookie `refresh_token` (`AuthController.java:146,231`) : ce sont des POST porteurs de cookie.
- Mitigation existante : cookie `SameSite=Lax` (`AuthController.java:249`) + pas d'ACAO pour une origine attaquante.
  Risque pratique faible mais le hotspot se déclenche.
- **Fix possible** : restreindre l'ignore hors `/api/auth/**` et protéger ces deux endpoints (CSRF token), ou passer le
  cookie refresh en `SameSite=Strict`, ou documenter le hotspot « Safe » en revue.

### 3. `typescript:S6268` — `bypassSecurityTrustHtml` sur le SVG admin (Blocker hotspot, à valider)

- `nml-ui-bst-angular/src/app/pages/carte/carte.component.ts:112`
  ```ts
  return sanitized ? this.sanitizer.bypassSecurityTrustHtml(sanitized) : null;
  ```
- Défenses en place : `sanitizeSvg()` (whitelist éléments/attributs, schémas `javascript:`/`data:` retirés, point fixe
  borné), contrôle same-origin (`isSameOriginAssetUrl`), CSP `sandbox` sur `/boards/**`
  (`BoardAssetHeadersFilter`), upload admin uniquement.
- **Action** : revue sécurité puis marquage « Safe » documenté, ou remplacement par une lib éprouvée (DOMPurify).
  Pas de fix code évident sans changer d'approche.

### 4. `S5145` — Log injection dans le filtre JWT (Major)

- `nml-ms/src/main/java/com/mg/nmlonline/config/JwtAuthenticationFilter.java:70` (vérifié)
  ```java
  logger.debug("JWT validation failed: " + e.getMessage());
  ```
- Message JJWT dérivé du token client (`Authorization`), concaténé sans placeholder → CRLF possible dans les logs.
- **Fix** : `logger.debug("JWT validation failed: {}", e.getClass().getSimpleName())` (ne pas logger le message brut),
  ou retirer les caractères de contrôle.

---

## P1 — Bugs de correction

### 5. `S1699` — Constructeur appelant une méthode overridable (Major ×2)

- `nml-ms/src/main/java/com/mg/nmlonline/domain/model/unit/Unit.java:75` (vérifié) — constructeur → `recalculateBaseStats()`.
- `nml-ms/src/main/java/com/mg/nmlonline/domain/model/vehicle/Vehicle.java:42` — idem.
- Méthodes `public`, classes non `final`. Aucune sous-classe aujourd'hui → bug latent.
- **Fix** : rendre `recalculateBaseStats()` `final` (compatible avec l'`@Override` d'interface) ou classes `final`.

### 6. `S2589` — Branches mortes (Major ×3, vérifié)

- `domain/service/PlayerImportService.java:199-202` : `if (equipmentRepository == null)` impossible
  (champ `private final` injecté par constructeur). Branche + log à supprimer.
- `mapper/BoardMapper.java:52` et `:76` : `if (board.getAllSectors() != null)` toujours vrai
  (`Board.getAllSectors()` retourne `Collections.emptyList()` ou une liste non nulle). Les `if` peuvent sauter.

### 7. `S1166` — Cause perdue au rethrow (Major ×2, vérifié)

- `domain/service/VehicleService.java:55-56` et `:96-97`
  ```java
  } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Type de véhicule invalide : " + vehicleTypeName);
  ```
- **Fix** : `throw new IllegalArgumentException("Type de véhicule invalide : " + vehicleTypeName, e);`

### 8. `S2095` — InputStream non fermé (Major, test)

- `nml-ms/src/test/java/com/mg/nmlonline/api/controller/AdminControllerTransactionBoundaryTest.java:71`
  `new ClassPathResource(...).getInputStream().readAllBytes()` sans try-with-resources.
- **Fix** : try-with-resources.

### 9. `S2699` — Test sans assertion (Blocker règle / probable)

- `nml-ms/src/test/java/com/mg/nmlonline/NmlOnlineApplicationTests.java:10` — `contextLoads()` vide.
- La classe porte `@EmbeddedPostgresTest` (méta-annotation) : l'exemption Sonar pour `contextLoads` + `@SpringBootTest`
  ne s'applique pas si le check ne suit pas les méta-annotations (à confirmer selon version).
- **Fix** : supprimer la classe (le contexte est déjà booté par les autres tests `@EmbeddedPostgresTest` et
  `ProdBootParityTest`), ou ajouter une assertion.

### 10. `typescript:S2589` — Condition toujours fausse (Major, vérifié)

- `nml-ui-bst-angular/src/app/pages/admin/admin.component.ts:90`
  ```ts
  return t === null || t === undefined ? '—' : `Tour ${t}`;
  ```
- `currentTurn` est `Signal<number | null>` (`admin.service.ts:49,58`) → `t === undefined` est toujours faux.
  (Compile sans erreur tsc, vérifié.)
- **Fix** : `return t ?? '—';` (ou `t === null ? '—' : ...`).

### 11. Hors Sonar — `combat.log` perdu en conteneur (bug prod réel)

- `Dockerfile:37` (`WORKDIR /app`) + `logback-spring.xml:13` (`${LOG_DIR}/combat.log`, défaut `logs`).
- `/app` appartient à root ; `USER nmlonline` (ligne 43) ne peut pas créer `/app/logs`. Logback échoue silencieusement
  à créer le fichier → aucun log de combat en production, même avec le volume `-v nml-logs:/app/logs`.
- **Fix** : `RUN mkdir -p /app/logs && chown nmlonline:nmlonline /app/logs` avant `USER`, ou `LOG_DIR=/tmp` (mais perdu
  au restart), ou créer/chown le point de montage dans l'entrypoint.

---

## P2 — Maintenabilité mécanique

### 12. `S6204` — `collect(Collectors.toList())` → `.toList()` (Major ×10, Java 21)

`mapper/BuildingMapper.java:76,90` — `domain/model/battle/Battle.java:411,492` —
`domain/service/CombatService.java:124,127,130,133` — `domain/service/TurnResolutionOrchestrator.java:221,224`.

### 13. `S2139` — Log + rethrow de la même exception (Major ×3)

`domain/service/JwtService.java:114,117,120` : `logger.debug/warn(... e.getMessage()); throw e;`
→ garder le log **ou** le rethrow, pas les deux.

### 14. `S1128` — Imports inutiles/doublons (Minor ×12)

- Inutilisés : `api/controller/BuildingController.java:4-7` (`Bank`, `Building`, `Headquarters`, `WeaponCache`),
  `test/config/TestDataInitializer.java:3`, `test/domain/service/BoardServiceSaveBoardTest.java:13`.
- Doublons (2e occurrence) : `BuildingRulesTest.java:16`, `PlayerEconomyTest.java:17`, `UnitTest.java:13`,
  `VehicleTest.java:12`, `AdminExportImportTest.java:17`, `BoardServiceSaveBoardTest.java:19`.

### 15. `S1874` — API dépréciée (Minor)

`test/domain/service/TurnResolutionOrchestratorStepsTest.java:56` : `@SpyBean` (Spring Boot 3.4+ déprécié)
→ `org.springframework.test.context.bean.override.mockito.MockitoSpyBean`.

### 16. `S138` / `S6541` — Méthodes trop longues / Brain Methods (Major ×3)

| Méthode | Fichier:ligne | Lignes | Cyclo~ | Prof~ |
|---|---|---:|---:|---:|
| `classicCombatConfiguration` | `domain/model/battle/Battle.java:151` | 189 | 22 | 3 |
| `exportPlayer` | `domain/service/AdminService.java:131` | 105 | 21 | 7 |
| `resolveStep` | `domain/service/MovementService.java:261` | 97 | 21 | 7 |

Candidats secondaires : `CombatService.simulateSectorBattle:57` (88 l.), `simulateSectorStandoff:150` (84 l.),
`PlayerStartupImporter.importIfPresent:130` (79 l.), `AuthController.refresh:139` (74 l.),
`Battle.classicPhaseConfiguration:50` (62 l.), `BoardImportService.importBoard:44` (58 l.), `SectorMapper.toDto:93` (58 l.).
Voir annexe A pour le top complet.

### 17. `S2221` / `S135` — Catch trop larges et boucles multi-sauts (Minor)

- `S2221` catch `Exception` : `config/JwtAuthenticationFilter.java:69`, `domain/service/JwtService.java:123`,
  `config/PlayerStartupImporter.java:124,205`, `infrastructure/loader/CsvDataLoader.java:60`,
  `domain/service/MovementService.java:620`.
- `S135` > 1 `break`/`continue` par boucle : `config/BoardAssetHeadersFilter.java:37`, `domain/model/board/Board.java:185`,
  `MovementService.java:284,326`, `PlayerImportService.java:149`.

### 18. `S1200` — Classes couplées > 20 types (Major ×7)

Par nombre d'imports (borne basse) : `PlayerImportService` 33, `CombatService` 28, `AuthController` 28,
`PlayerActionService` 26, `AdminController` 26, `MovementAdminService` 24, `AdminService` 22.
Refactor structurel à étaler ; `PlayerImportService` en tête.

### 19. `S5976` — Tests quasi identiques à paramétrer (Major probable ×2)

- `test/.../SecurityOwnershipTest.java:61-91` : 4 tests identiques au littéral près (`unauthenticated_*_returns401`).
- `test/.../AdminControllerTransactionBoundaryTest.java:39-59` : 3 tests identiques.

---

## P3 — Accessibilité frontend

### 20. `S6819` — `role="button"` sur élément non natif (Major ×6)

- `components/navbar/navbar.component.ts:125` (`<div class="mobile-drawer-backdrop" role="button">`)
- `pages/carte/carte.component.html:71` (`<div class="svg-map-wrapper" role="button">`)
- `pages/carte/carte.component.html:210` (`<mat-chip role="button">`)
- `pages/joueur/joueur.component.html:397` (`<div class="force-line clickable" role="button">`)
- `pages/joueur/unit-detail-dialog.component.html:28` et `:129` (`<div class="udd-slot ..." role="button">`)

→ préférer `<button>` natif. (Les `role="status"` des conteneurs de chargement sont des faux positifs connus, non comptés.)

### 21. `S1082` — Clic sans équivalent clavier (Major ×3)

- `components/navbar/navbar.component.ts:122-128` : backdrop `(click)` sans `keydown/keyup`.
- `pages/joueur/unit-detail-dialog.component.html:25-31` et `:126-132` : slot cliquable sans handler clavier.
- Non concernés (déjà conformes) : `carte.component.html:75,208`, `joueur.component.html:399`,
  `character-portrait.component.html:9-10` (`keydown.enter` présent).

---

## P4 — Cosmétique front + config/infra

### 22. Frontend cosmétique (Minor)

- `S6606` `|| 0` → `?? 0` : `admin.component.ts:262,272`, `boutique.component.ts:272,276`.
  (Les `string || défaut` sont volontairement exclus — FP reconnus par Sonar.)
- `S7755` `.at(-1)` : `core/sale-multiplier.ts:10`, `pages/carte/carte.component.ts:382`,
  `pages/joueur/movement.helpers.ts:64`.
- `S7781` `replaceAll` : `core/svg-sanitize.ts:63`, `core/slug.ts:4,6,7`.
- `S4325` casts `as` inutiles : `services/admin.service.ts:39`, `pages/carte/carte.component.ts:145`,
  `shared/board-import-dialog/board-import-dialog.component.ts:121`.
- `S3358` ternaire imbriqué : `core/stats.ts:24`
  (`i === 0 ? '' : i === off.length && off.length > 0 ? ' / ' : ' + '` — le `off.length > 0` est redondant).

### 23. `githubactions:S7637` — Actions non épinglées à un SHA (Major)

`.github/workflows/cd.yml:31,34,42,52` : `docker/setup-buildx-action@v3`, `docker/login-action@v3`,
`docker/metadata-action@v5`, `docker/build-push-action@v6`. Le job possède `packages: write` → épingler par SHA complet.

### 24. Dockerfile

- `docker:S6505` (id à confirmer) : `Dockerfile:7` (`npm ci` sans `--ignore-scripts`) et `:32`
  (`apk add --no-cache curl` sans `--no-scripts`). Hotspot supply-chain, acceptable si documenté.
- `docker:S6470` : `Dockerfile:9,21` (COPY récursifs, mitigé par `.dockerignore`).
- Points conformes : `USER nmlonline:nmlonline` (l.43), aucun secret en `ARG/ENV`, tags versionnés, HEALTHCHECK localhost.

### 25. Config / build

- `nml-ms/pom.xml:88-93` : pilote **H2 en scope `runtime`** → embarqué dans le jar de prod alors que la prod est
  PostgreSQL. Remonter en `test` (ou `provided`).
- `nml-ui-bst-angular/package.json:9` : `jest --passWithNoTests` → la CI passe avec zéro test exécuté.
- `nml-ui-bst-angular/eslint.config.mjs:40-79` : règles Angular/TS surchargées en `'warn'` → `ng lint` ne peut pas
  échouer sur ces règles.
- `db/migration/V1__baseline.sql:17` : `SET row_security = off;` (artefact pg_dump).
  `V1__baseline.sql:143-151` : table `credentials` avec `username`/`password`/`role` nullables, sans `UNIQUE`.
  **V1 ne doit pas être modifiée** → ajouter une migration si l'on veut contraindre.
- `application.properties:10` : `ddl-auto=update` sur le profil par défaut (H2 mémoire, dev). Acceptable mais aucun
  garde-fou n'empêche un démarrage sans profil `prod` de muter un schéma.
- `tools/svg-neighbor-detector.js:169-218` : `S3776` `minPointDistance` (~40, 4 boucles imbriquées) ;
  `S2228` 33 `console.*` (accepté pour un CLI) ; `:354,416` `async main()` sans `await` ; `:80-85` `parseInt` → `NaN`
  silencieux si option sans valeur.
- Info : `tools/package.json` déclare `svgdom` et `@svgdotjs/svg.js` jamais importés ; `ts-node` déclaré sans config ;
  Node 26 en CI (`ci.yml:55`) vs Node 24 dans l'image ; tags d'images non digest-pinnés ; métadonnées `pom.xml:14-26`
  vides ; `payload varchar(100000)` (V10:12) ; `cancel-in-progress: true` sur le CD master.

---

## Faux positifs / Won't fix recommandés

- `S2077` `DemoDataResetter.java:65` : `"TRUNCATE TABLE " + table` — `table` provient exclusivement
  d'`INFORMATION_SCHEMA.TABLES` (requête statique), jamais d'une entrée externe. Hotspot à marquer Safe.
- `S2245` `Battle.java:39` : `new Random()` — moteur de combat seedable pour tests déterministes, aucun usage crypto.
- `S3516` ×8 : retours constants intentionnels (`CombatEntity.java:69`, `UnitClass.java:49,53,57,61`,
  `TurnResolutionScenarioSeeder.java:63`, `SpaForwardController.java:26`, `PlayerActionServiceTest.java:296`).
- `S6268`, `S4502` : à marquer Safe si la revue confirme les mitigations.
- Info sans action : `S4660` (CSS, id à confirmer : `regles.component.scss:156/184` et `:240/254`), 36 `!important`
  (surtout `boutique.component.scss`), effects écrivant un signal (`admin.component.ts:104-117`,
  `turn-resolution.component.ts:62-97`, `unit-detail-dialog.component.ts:79-82`), code mort front
  (`ordres.component.ts:56 trackOrder`, `carte.component.ts:589 getInitials`, `movement-state.service.ts:27`,
  `turn-resolution.service.ts:136`, `player-actions.service.ts:20`, `shop.service.ts:181`), `S1200` structurel.

---

## Annexe A — Top méthodes longues (données S138/S6541/S3776)

| # | Méthode | Fichier:ligne | Phys | NLOC | Cyclo~ | Prof~ |
|---|---------|---------------|-----:|-----:|-------:|------:|
| 1 | `classicCombatConfiguration` | `Battle.java:151` | 189 | 137 | 22 | 3 |
| 2 | `exportPlayer` | `AdminService.java:131` | 105 | 93 | 21 | 7 |
| 3 | `resolveStep` | `MovementService.java:261` | 97 | 77 | 21 | 7 |
| 4 | `simulateSectorBattle` | `CombatService.java:57` | 88 | 70 | 11 | 2 |
| 5 | `simulateSectorStandoff` | `CombatService.java:150` | 84 | 72 | 13 | 3 |
| 6 | `importIfPresent` | `PlayerStartupImporter.java:130` | 79 | 61 | 8 | 5 |
| 7 | `refresh` | `AuthController.java:139` | 74 | 59 | 14 | 5 |
| 8 | `classicPhaseConfiguration` | `Battle.java:50` | 62 | 57 | 13 | 3 |
| 9 | `importBoard` | `BoardImportService.java:44` | 58 | 47 | 16 | 6 |
| 10 | `toDto` | `SectorMapper.java:93` | 58 | 48 | 12 | 2 |

Mesures approximatives (comptage d'accolades / points de décision) ; Phys = lignes physiques, NLOC ≈ lignes de code.

## Annexe B — Règles vérifiées sans violation

Aucune occurrence confirmée pour : `S1698` (== objet), `S2583`, `S2447`, `S1854`, `S3923`, `S1871`, `S1862`,
`S1751`, `S1656`, `S1117`, `S3518`, `S1143`, `S3626`, `S2142`, `S108`/`S1166` (catch vide), `S4143`, `S4144`
(intra-classe), `S3981`, `S3984`, `S2225`, `S2674`/`S2201`, `S6201`/`S6202`, `S1858`, `S2637`, `S1155`, `S4449`.
Tests : `S1607`, `S2925`, `S5863`, `S5785`, `S2701`, `S3415`. Front : `S6479` (tous les `@for` ont un `track`),
`S6582`, `S1854`, `S1128`, `S1874`, `S4138`, `S4624`, `S2245`, `S5148`, `S5254`/`S1079`, `S6822`, `S5256`,
`S6299` (règle Vue, non applicable). Sécurité back : XXE, désérialisation Java, `Runtime.exec`/`ProcessBuilder`,
TLS/TrustManager, open redirect, fichiers temporaires, permissions : aucun cas.

## Annexe C — Configuration SonarLint locale (contexte)

`%APPDATA%\JetBrains\WebStorm2026.2\options\sonarlint.xml` désactive globalement :
`java:S106, S107, S1104, S112, S1135, S1144, S1172, S1192, S125, S1481, S3776`.
Aucun serveur SonarQube/SonarCloud connecté, aucun binding projet.
