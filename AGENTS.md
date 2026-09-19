# AGENTS.md

Fast lane. Détails dans [README.md](README.md).

## Commandes

```bash
# nml-ms/ (Spring Boot 3.5.6 / Java 21)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"   # JWT_SECRET + JWT_PEPPER requis
.\mvnw.cmd clean test                                          # PostgreSQL 14 embarqué (Zonky), sans config
# nml-ui-bst-angular/ (Angular 22)
npm start / npm test / npm run lint / npm run format / npm run build
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
Réponses et commentaires en **français** ; README/docs en anglais.

## Règles dures

- **Schéma = script Flyway** `V<n>__description.sql` dans
  `nml-ms/src/main/resources/db/migration/` (actif en `prod` seulement ; jamais modifier
  une migration appliquée). Livré avec l'entité.
- Jamais de `JWT_SECRET` / `JWT_PEPPER` / identifiants DB dans le dépôt — variables
  d'environnement uniquement. Pas de console H2, pas de `@CrossOrigin` (CORS dans `CorsConfig`).
- **JWT** : access token = claim `type=access`, refresh = `type=refresh` ; ne jamais accepter l'un pour l'autre.
- **Verrous** : joueur (`findByUserIdForUpdate`) puis véhicule (`findByIdForUpdate`) — la FK
  `player_actions.player_id` pose un KEY SHARE sur `players` ; l'ordre inverse deadlock.
- **Lecture** : jamais l'état privé d'autrui — `PlayerDto` complet = propriétaire/admin,
  carte via `BoardMapper.toPublicDto` ; SVG admin = `sanitizeSvg` client + CSP `sandbox` (`/boards/**`).
- Minimal wins : pas d'abstraction spéculative, pas d'échafaudage « pour plus tard »,
  pas de nouvelle dépendance quand quelques lignes suffisent. Supprimer > ajouter.
- Logique non triviale = **un** test qui casse si la logique casse. Pas de suite par fonction.
- **Commentaires** : zéro par défaut — relire les lignes `+` de `git diff -U0` avant de rendre
  la main (voir « Commentaires »). Un `catch` vide ne survit que justifié en une ligne.
- **Règles du jeu** : toute modification de gameplay met à jour [`## Règles du jeu`](#règles-du-jeu)
  **dans le même commit** — le code et cette section ne divergent jamais.

## Règles du jeu

Source de vérité = le code ; toute évolution de gameplay met cette section à jour **dans le même commit**.

- **Tour** : ordres de mouvement simultanés, résolus en fin de tour par le MJ (hop par hop, batailles
  tranchées manuellement). Un ordre à pied = 1 secteur (2 pour la classe LEGER) ; véhicule = sa `speed`.
  Le tour s'incrémente à la finalisation. Les annonces d'alliance/trahison d'un tour ne sont publiques
  qu'au tour suivant.
- **Camps** : deux alliés fusionnent en un seul camp **si chacun n'a que l'autre comme allié présent** ;
  sinon chaque joueur est un camp séparé. Un joueur-pont (allié aux deux) ne frappe pas ses alliés
  et passe s'il ne lui reste aucun ennemi.
- **Duel** : exactement 2 camps (2v1 solidaire possible). **Impasse** : 3+ camps, ronde ordonnée
  défenseurs (par id) puis arrivants (par heure d'ordre) ; chaque camp frappe le prochain camp
  **non-allié** de la ronde, un camp sans ennemi passe. Stationnaires sans arrivée : pas de combat.
- **Capture** : un camp unique avec survivant(s) — un seul survivant → capture directe ; plusieurs
  alliés survivants → secteur neutre, le MJ attribue (immédiat). Bâtiments du camp perdant capturés
  par le joueur dominant du camp vainqueur ; QG capturé = défaite.
- **Alliances** : paires de 2 joueurs, plusieurs simultanées, **non transitives** (A-B et B-C
  n'unissent pas A et C). QG opérationnel requis pour proposer/accepter/rupture amiable ; trahison
  possible sans QG. Chat 1-à-1 entre les deux membres.
  - **Trahison** (unilatérale) : bonus `15 % + 10 % × tours d'alliance`, **sans plafond**, actif
    uniquement le tour de la trahison, en attaque comme en défense contre l'ex-allié.
  - **Rupture amiable** : proposition + validation de l'autre, effective à l'acceptation, sans bonus.
  - **Libre passage** : un allié ne bloque ni transit ni capture, on peut stationner/défendre chez
    lui ; un secteur allié n'est jamais capturé. Un bâtiment ne se déplace que dans ses propres secteurs.
  - **Après rupture/trahison** : les ex-alliés co-localisés se battent le tour de la rupture seulement.
  - **Annonces publiques** (page Rapports) : formation, rupture et trahison révélées au tour suivant.
- **Visibilité secteur** : public = nom, propriétaire/couleur, ressource, voisins, position, booléen
  « défenseurs présents ». Détail complet (revenu, stats, armée, personnages, véhicules, bâtiments) =
  propriétaire, ou allié **avec une entité sur place**. Neutre/ennemi : restreint même avec troupe.
- **Échange** : offres d'argent (hors dotation de départ) et/ou ressources ; 3 offres en attente max
  par émetteur ; expirent au tour suivant ; seul le destinataire accepte/refuse, l'émetteur annule.
- **Pas d'échange d'unités ni d'armes** entre joueurs, alliés compris.

## Backend

`domain/model` → `domain/service` → `api/controller` → `infrastructure/repository`,
`mapper` pour DTO ↔ entité.

- **Ownership** : jamais de `playerId` du body/params — `request.getAttribute("userId")`,
  vérifié dans le service (`SecurityException` → 403).
- **Admin** : CRUD global sous `/api/admin/**` + `@PreAuthorize("hasRole('ADMIN')")`.
- `Sector.ownerId` = source unique de propriété. `Board.sectorsList` = seule source persistée ;
  `BoardDto.sectors` (map) est reconstruite dans `BoardMapper`. `@JsonIgnore` côté many.
- Cascade JPA : lire [`docs/jpa-pitfalls.md`](docs/jpa-pitfalls.md) avant tout
  `@OneToMany(mappedBy=…, orphanRemoval=true)` dont l'enfant porte une FK NOT NULL.
- **Tour** : `TurnService.advanceTurn()` et `TurnResolutionOrchestrator` mutent tous deux
  `Board.currentTurn` — les deux doivent publier/invalider `TurnService.cachedTurn`
  (`publishTurn` avant commit, purge sur rollback).
  Session de l'orchestrateur en mémoire, JVM unique, perdue au redémarrage.
- **`BoardService.saveBoard` fusionne par numéro, ne vide jamais `sectorsList`** : supprimer
  un secteur reste une opération explicite (vider la liste cascade-delete secteurs + armées).
- **Véhicules** : pilote = personnage ou unité `PILOTE_DESTRUCTEUR`, passagers ≤ `VehicleType.capacity` ;
  occupants laissés dans `Sector.army`/`characters` (stats inchangées) et référencés par le véhicule ;
  le véhicule les emporte, ils sont exclus des ordres à pied. Détacher un pilote unité avant `em.remove`
  (FK `pilot_id` portée par la ligne véhicule — `CombatService.detachPilotFromVehicles`).

## Données prod

Source de vérité = la DB. `boards/board.json` et `players/*.json` sont des fixtures de démo
classpath, lues si `app.import-demo-data=true` (défaut dev/test). En prod, l'admin crée
plateau et joueurs par l'API.

## Frontend

Composants standalone, routes lazy via `loadComponent`. Pas de NgRx : état dans des
services à signaux (`signal`/`computed`, `httpResource` pour le serveur). Les composants
consomment, ils ne possèdent pas l'état global.
