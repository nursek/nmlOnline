# AGENTS.md

Fast lane. Détails dans [README.md](README.md).
`.github/copilot-instructions.md` contient les mêmes règles pour Copilot — garder les deux synchrones.

## Commandes

```bash
# nml-ms/ (Spring Boot 3.5.6 / Java 21)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"   # JWT_SECRET + JWT_PEPPER requis
.\mvnw.cmd test                                                # H2, sans config
# nml-ui-bst-angular/ (Angular 22)
npm start / npm test / npm run lint / npm run format
```

## Commentaires

Un commentaire ne survit que s'il porte une information **absente du code**. En français,
1 ligne max. Test : en le supprimant, un lecteur perd-il quelque chose ?

Garder seulement si le commentaire :

- dit **pourquoi** (arbitrage, couplage inter-fichier, piège, limite connue) ;
- justifie un `catch` vide ou un comportement contre-intuitif ;
- donne une sortie non déductible (`« 3 x 850 = 2550 ₡ »`).

Supprimer sinon, en particulier s'il :

- reformule le nom ou le code juste en dessous ;
- est une en-tête de section (`// --- Helpers ---`, `// HTTP`) ou double un titre visible
  (`<!-- Équipements -->` au-dessus d'un `<h2>Équipements</h2>`) ;
- est une Javadoc multi-lignes, ou un `@param`/`@return` qui répète l'évident.

Seul marqueur autorisé : `// ponytail:` + plafond + voie d'upgrade
(`// ponytail: lock global, passer par-compte si le débit devient un souci`).
Avant de commit : relire chaque commentaire ajouté et en supprimer la moitié.
Réponses et commentaires en **français** ; README/docs en anglais.

## Règles dures

- **Schéma = script Flyway** `V<n>__description.sql` dans
  `nml-ms/src/main/resources/db/migration/` (actif en `prod` seulement ; jamais modifier
  une migration appliquée). Livré avec l'entité.
- Jamais de `JWT_SECRET` / `JWT_PEPPER` / identifiants DB dans le dépôt — variables
  d'environnement uniquement. Pas de console H2, pas de `@CrossOrigin` (CORS dans `CorsConfig`).
- Minimal wins : pas d'abstraction spéculative, pas d'échafaudage « pour plus tard »,
  pas de nouvelle dépendance quand quelques lignes suffisent. Supprimer > ajouter.
- Logique non triviale = **un** test qui casse si la logique casse. Pas de suite par fonction.

## Backend

`domain/model` → `domain/service` → `api/controller` → `infrastructure/repository`,
`mapper` pour DTO ↔ entité.

- **Ownership** : jamais de `playerId` du body/params — `request.getAttribute("userId")`,
  vérifié dans le service (`SecurityException` → 403).
- **Admin** : CRUD global sous `/api/admin/**` + `@PreAuthorize("hasRole('ADMIN')")`.
- `Sector.ownerId` = source unique de propriété. `Board.sectors` est une map transient
  (`@PostLoad`) — ne pas la persister. `@JsonIgnore` côté many.
- Cascade JPA : lire [`docs/jpa-pitfalls.md`](docs/jpa-pitfalls.md) avant tout
  `@OneToMany(mappedBy=…, orphanRemoval=true)` dont l'enfant porte une FK NOT NULL.
- **Tour** : `TurnService.advanceTurn()` et `TurnResolutionOrchestrator` mutent tous deux
  `Board.currentTurn` — les deux doivent invalider `TurnService.cachedTurn`.
  Session de l'orchestrateur en mémoire, JVM unique, perdue au redémarrage.
- **`BoardService.saveBoard` fusionne par numéro, ne vide jamais `sectorsList`** : supprimer
  un secteur reste une opération explicite (vider la liste cascade-delete secteurs + armées).

## Données prod

Source de vérité = la DB. `boards/board.json` et `players/*.json` sont des fixtures de démo
classpath, lues si `app.import-demo-data=true` (défaut dev/test). En prod, l'admin crée
plateau et joueurs par l'API.

## Frontend

Composants standalone, routes lazy via `loadComponent`. Pas de NgRx : état dans des
services à signaux (`signal`/`computed`, `httpResource` pour le serveur). Les composants
consomment, ils ne possèdent pas l'état global.
