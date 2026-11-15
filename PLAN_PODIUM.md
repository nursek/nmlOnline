# Plan d'implémentation — fin de partie : Podium et Brigade des Morts (v2)

> **v2 — révision** : le journal n'est plus généré automatiquement. Le système fournit un
> **template** et un **éditeur** ; le MJ/Admin écrit les articles et publie quand il veut.
> Le plan est allégé d'autant : plus de banque de titres/citations/corps, plus de générateur de
> contenu. La météo, le volume, le numéro, le faux lien et la date sont générés et persistés ; la
> météo est tirée uniformément, comme prévu.
>
> Autres changements v2 :
> - **BETA 6 joueurs**, chacun pour soi : pas d'objectif secret, pas de victoire d'alliance. Le
>   panneau « Objectif secret / empereur » est retiré de la page Règles.
> - Quand il ne reste que deux joueurs et qu'ils sont alliés, **l'alliance se brise
>   automatiquement** : ils doivent s'affronter.
> - **Cause au podium** : `MAIN_CHARACTER_KILLED` = mort définitive du lore ;
>   `HEADQUARTERS_CAPTURED` = défaite (QG conquis), retour possible dans une édition future.
>   La gazette n'a pas besoin de l'exposer automatiquement, le MJ l'écrit s'il le souhaite.
> - **Champion posthume validé**, avec possibilité pour le MJ de désigner un autre champion
>   (audit + affichage « désigné par le MJ »).
> - Nom du journal : **Brigade des Morts**.
> - L'objectif secret et la victoire d'alliance sont retirés du périmètre et des règles.
>
> **Base d'implémentation** : branche `feat/game-end-podium-gazette`, issue de `origin/master`
> (`a7f7086`) après merge de `fix/qg-reconstruction-7500` (QG à 7 500). `master` ne contient pas
> encore `feat/game-setup-turn-0` : `PreparationPhaseFilter` et `GameSetupService` n'existent pas
> sur cette base, le `GameOverFilter` est donc autonome. La migration reste **V40** pour laisser
> **V39** au tour 0 ; l'ordre de merge attendu est tour 0 (V39) puis fin de partie (V40).

## Sommaire

1. [Cadre et périmètre](#1-cadre-et-périmètre)
2. [Acquis et points d'ancrage](#2-acquis-et-points-dancrage)
3. [Décisions intégrées](#3-décisions-intégrées)
4. [Fin de partie et dernière alliance](#4-fin-de-partie-et-dernière-alliance)
5. [Podium et champion](#5-podium-et-champion)
6. [Gazette : Brigade des Morts](#6-gazette--brigade-des-morts)
7. [Modèle de données](#7-modèle-de-données)
8. [Backend et API](#8-backend-et-api)
9. [Frontend](#9-frontend)
10. [Page Règles](#10-page-règles)
11. [Tests](#11-tests)
12. [Découpage en lots](#12-découpage-en-lots)
13. [Pièges et invariants](#13-pièges-et-invariants)
14. [Hors périmètre](#14-hors-périmètre)
15. [Documentation et checklist](#15-documentation-et-checklist)
- [Annexe A — Modèle d'article fourni au MJ](#annexe-a--modèle-darticle-fourni-au-mj)
- [Annexe B — Contrats API](#annexe-b--contrats-api)
- [Annexe C — Points tranchés](#annexe-c--points-tranchés)

---

## 1. Cadre et périmètre

### 1.1 Format BETA

- Cible **6 joueurs**, chacun pour soi.
- Les **alliances sont tactiques**, jamais une victoire partagée : le podium ne couronne qu'un
  joueur.
- Aucun **objectif secret**, aucun **empereur**, aucune condition de victoire cachée.
- La partie se joue jusqu'au dernier survivant ; le code reste **N-agnostique** (aucun verrou à
  6 dans les services). Le preset 6 joueurs sert aux tests et à la démo.
- Une alliance peut exister tant qu'il reste au moins trois joueurs. Dès qu'il n'en reste que
  deux et qu'ils sont alliés, le pacte se brise automatiquement au tour suivant (§4.4).

### 1.2 Livré

- Détection automatique de la fin de partie à `finalizeTurn` : `joueurs vivants <= 1`.
- `Board.finishedAt`, `Board.winnerPlayerId`, `Board.winnerAdjusted` ; mutations joueurs en 409.
- Un historique d'éliminations : ordre de mort, tueur, assists, cause, personnage.
- Le **Podium** : champion (survivant ou posthume, ajustable par le MJ), ordre complet, kills et
  assistances par joueur, cause lore (mort définitive / défaite réversible).
- La **Brigade des Morts** : un brouillon d'édition est ouvert à chaque tour avec au moins une
  élimination ; le MJ écrit les articles (1 à 3 unes + brèves), prévisualise, publie ; les joueurs
  lisent l'archive des éditions publiées.
- Le **template** de journal (masthead, une, brèves), l'**éditeur** et le **panneau de faits du
  tour** qui aide le MJ à rédiger (y compris des informations privées qu'il choisit de révéler).
- La mise à jour de la page **Règles** : retrait de l'objectif secret, BETA 6 joueurs, rupture de
  la dernière alliance, causes lore, journal rédigé par le MJ.
- Les tests backend/`@EmbeddedPostgresTest` et les specs Angular.

### 1.3 Non livré

- Pas de génération automatique de titres, citations ou corps d'articles : c'est le MJ.
- Pas d'objectif secret / empereur (retiré des règles et du périmètre).
- Pas de victoire d'alliance (retirée) ; pas de partage de la première place.
- Pas d'effet de gameplay de la météo : décor persisté.
- Pas d'upload d'image ni de médiathèque : on choisit le portrait d'un joueur existant, ou rien.
- Pas de clôture manuelle de partie par le MJ (seule la désignation du champion posthume est
  ouverte).
- Pas de mécanique de « comeback » dans la partie courante : la distinction mort définitive /
  défaite réversible est une information de **lore** affichée au podium.
- Pas de nouvelle dépendance Maven/npm.

### 1.4 Interprétation des points ouverts

- « Classements militaires remplacés par le Podium » : les classements restent pendant la partie ;
  l'Accueil bascule sur le Podium **à la fin**. `/api/rankings` reste inchangé.
- « Information révélable si le joueur est éliminé » : le système ne filtre pas le texte du MJ.
  Les **faits du tour** (notes d'aide) peuvent contenir des informations privées et ne sont
  visibles qu'en admin ; la gazette publiée contient exactement ce que le MJ a écrit. Le podium,
  lui, expose automatiquement la cause d'élimination et sa portée lore.
- « QG détruit » est couvert par la cause existante `HEADQUARTERS_CAPTURED` (le QG peut être
  détruit puis capturé ; un QG seulement détruit sans capture n'élimine pas).

---

## 2. Acquis et points d'ancrage

| Élément existant | Fichier | Réutilisation |
|---|---|---|
| Application de l'élimination | `domain/service/PlayerEliminationService.java:68` | Appel dans l'ordre de mort ; logique territoriale inchangée. |
| Flag / cause | `domain/model/player/Player.java:111` (`eliminate`) | `EliminationCause` sert au podium et aux notes du MJ. |
| Fin de tour pas-à-pas | `domain/service/TurnResolutionOrchestrator.java:334` | Point unique de la fin de partie et de la création du brouillon de gazette. |
| Tueur désigné | `TurnResolutionOrchestrator.java:301` | Le MJ choisit déjà le tueur ; l'historique le relit. |
| Bataille en session | `TurnResolutionOrchestrator.java:701` (`ResolvedBattle`) | Ajouter `conflictId` + `camps` pour l'ordre de mort et les assists. |
| Journal technique | `TurnJournal`, V35/V38 | Modèle de payload JSON pour la gazette, **concept distinct**. |
| Annonces | `Announcement`, `AnnouncementType`, V16 | Ajout `FINAL_DUEL` (CHECK à étendre) ; source des faits du MJ. |
| Visibilité publique joueur | `mapper/PlayerMapper.java:104` | `eliminated`, cause, tour, tueur et `sectorCount` sont déjà publics. |
| Filtres de phase | `PreparationPhaseFilter`, `EliminatedPlayerFilter` | Modèle du `GameOverFilter` (409 problem+json). |
| Classements | `RankingService`, `RankingsPanelComponent` | Inchangés ; bascule d'affichage en fin de partie. |
| Portraits | `shared/character-portrait/character-portrait.component.ts:30` | `assets/<nom>/characters/portrait.png` + repli sigil. |
| Jetons / polices | `nml-ui/src/styles.scss:237` | Thème, `readonly-mode`, print, `prefers-reduced-motion`. |
| Audit admin | `AdminAuditService`, `AdminAuditAction` | Ajouter les actions de publication et de champion. |
| 404 | `GlobalExceptionHandler.java:76` | Édition inconnue → 404. |

---

## 3. Décisions intégrées

| # | Décision |
|---|---|
| D1 | Fin automatique à `finalizeTurn` quand `joueurs vivants <= 1` et `joueurs >= 2`. Pas de clôture manuelle. |
| D2 | `finishedAt` + `winnerPlayerId` + `winnerAdjusted` ; `currentTurn` reste le tour final (pas d'incrément). |
| D3 | Podium = champion + éliminés dans l'ordre inverse des éliminations. `place = N - indexDeMort` (0-based). |
| D4 | 0 survivant ⇒ champion posthume = dernier éliminé. Le MJ peut désigner un autre éliminé (`winnerAdjusted`). |
| D5 | Ordre de mort d'un même tour : `conflictId` (hop), puis ordre des victimes dans la bataille, puis id pour les éliminations hors bataille ; première occurrence. |
| D6 | Kill = tueur désigné (ou capturant du QG). Assist = autre joueur du camp du tueur dans la bataille. Pas de tueur ⇒ 0 assist. |
| D7 | **La gazette est écrite par le MJ/Admin**. Le système ouvre un brouillon, fournit le template et les faits du tour ; la publication est manuelle. |
| D8 | Le système ne génère aucun article. Il persiste la météo, le volume, le numéro, le faux lien, la date, les slots pré-remplis et les notes d'aide. |
| D9 | La météo est tirée **uniformément** parmi six conditions et plages, puis persistée dans l'édition. Les autres éléments du masthead sont déterministes. |
| D10 | Journal **Brigade des Morts**, faux lien en texte (jamais un `<a>`), volume = tour, numéro d'édition, date d'édition réelle. |
| D11 | Les **notes d'aide** (faits du tour, y compris privés) sont admin-only et ne sont jamais dans le DTO public. Le MJ révèle ce qu'il veut dans le texte. |
| D12 | Le **podium** expose la cause : `MAIN_CHARACTER_KILLED` = mort définitive ; `HEADQUARTERS_CAPTURED` = défaite, retour possible. |
| D13 | Pas de victoire d'alliance, pas d'objectif secret. Si deux alliés sont les derniers, l'alliance se brise automatiquement et aucune nouvelle alliance n'est acceptée à deux joueurs. |
| D14 | Après la fin : mutations joueurs 409 ; `/api/admin/**` reste ouvert ; UI en lecture seule (sauf boutons de consultation). |
| D15 | Une édition par tour, au plus 3 unes + 4 brèves ; le MJ peut ajouter, retirer et ordonner les articles. |
| D16 | Les articles sont du texte brut (`\n\n` = paragraphes) ; aucun HTML, aucun `innerHTML` côté frontend. |
| D17 | Un brouillon non publié est invisible pour les joueurs ; une édition publiée peut être corrigée par le MJ (audit + `updatedAt`), sans changer `publishedAt`. |
| D18 | Zéro nouvelle dépendance ; pas d'IA, pas de service de génération de texte. |

### Points tranchés (v2)

| Point | Décision |
|---|---|
| Champion posthume | Oui, dernier éliminé par défaut ; ajustable par le MJ. |
| Gazette | Rédigée et publiée par le MJ ; le système fournit template + faits. |
| Nom | **Brigade des Morts**. |
| Objectif secret / empereur | Supprimé du périmètre et de la page Règles. |
| Victoire d'alliance | Impossible ; rupture automatique de la dernière alliance. |
| 6 joueurs | Cible BETA ; le code reste N-agnostique. |

---

## 4. Fin de partie et dernière alliance

### 4.1 Déclenchement

Point unique : `TurnResolutionOrchestrator.finalizeTurn(boardId)`.

Ordre cible :

1. validations existantes ;
2. collecte des éliminés du tour et **ordre de mort** ;
3. construction en mémoire des `EliminationEvent` (index de mort) ;
4. `GameEndService.finishIfOver` (vainqueur, posthume) ;
5. **dernière alliance** : si la partie continue et qu'il reste 2 alliés, rupture + annonce ;
6. application des éliminations (`PlayerEliminationService.apply`, ordre de mort) ;
7. sauvegarde des événements, du board ; création du **brouillon de gazette** (si éliminations) ;
8. journal technique du tour, cache du tour, libération du verrou.

La création du brouillon peut échouer sans bloquer le jeu ? Non : elle est dans la transaction de
finalisation. C'est un produit éditorial important ; un échec doit être visible et corrigé, pas
masqué. Les notes sont construites à partir des `ResolvedBattleDto` déjà calculés, sans accès
externe.

### 4.2 Ordre de mort

1. Conflits résolus triés par `conflictId` (ordre des hops, indépendant des clics du MJ).
2. Dans chaque bataille, ordre des victimes du record (camps aplatis).
3. Première occurrence seulement (une victime peut apparaître dans deux batailles).
4. Éliminés hors bataille : id joueur croissant.

`deathIndex = countByBoardId(boardId) + position`. La cause finale est toujours
`victim.getEliminationCause()` (QG prioritaire sur personnage).

### 4.3 Places

`N = décès + survivants`.

```text
place(événement d'index de mort i, 0-based) = N - i
champion vivant (s'il existe)               = 1
```

| Exemple | N | Décès | Places | Champion |
|---|---:|---|---|---|
| 6 joueurs, 1 mort par tour | 6 | A..E | 6,5,4,3,2 | F = 1 |
| 4 joueurs, 2 morts puis 1 mort | 4 | A,B,C | 4,3,2 | D = 1 |
| 4 joueurs, les 2 derniers meurent ensemble | 4 | A,B,C,D | 4,3,2,1 | D posthume |
| 3 joueurs, tous meurent ensemble | 3 | A,B,C | 3,2,1 | C posthume |

Le champion ajusté par le MJ (D4) est mis à la place 1 ; les autres morts gardent l'ordre
inverse, le champion retiré de la liste. `winnerAdjusted` indique la désignation.

### 4.4 Dernière alliance (D13)

À la fin d'un tour où la partie n'est pas terminée :

- si `joueurs vivants == 2` et qu'une alliance active relie les deux :
  - `Alliance.end(turn, null, false)` (pas de bonus de trahison) ;
  - annonce `AnnouncementType.FINAL_DUEL`, acteur/target = les deux joueurs, créée au tour,
    visible au tour suivant ;
  - aucune re-création possible : `AllianceService.propose` et `accept` refusent une nouvelle
    alliance quand `joueurs vivants <= 2` ;
  - les deux joueurs peuvent se battre au tour suivant.
- si les deux ne sont pas alliés : rien à faire, la partie continue normalement.

La V16 a un CHECK sur `announcements.type` : la migration V40 le remplace pour autoriser
`FINAL_DUEL`. L'UI d'annonces ajoute le libellé « La dernière alliance se brise — il ne peut
rester qu'un survivant. »

Le MJ voit l'annonce dans les faits du tour et peut la raconter dans la gazette.

### 4.5 Effets sur le board et pseudo-code

- `finishedAt = Instant.now()`, `winnerPlayerId`, `winnerAdjusted = false` si la fin est naturelle.
- `currentTurn` reste `turnEnding` ; `TurnFinalizeResultDto.newTurn = turnEnding` si terminé.
- `TurnService.publishTurn(boardId, currentTurn)` dans les deux branches.

```java
// finalizeTurn, après movementService.finalizeResolution et les captures
List<Player> players = playerRepository.findByBoardId(s.boardId);
List<Player> eliminated = players.stream()
        .filter(p -> Objects.equals(p.getEliminatedTurn(), s.turnEnding)).toList();

List<Long> deathOrder = deathOrder(s, eliminated);
List<EliminationEvent> events = eliminationHistoryService.build(board, s.turnEnding, deathOrder, s);
GameOutcome outcome = gameEndService.finishIfOver(board, players, events);

if (!outcome.finished()) {
    finalDuelService.breakLastAllianceIfNeeded(board, players, s.turnEnding);
}

for (Player victim : ordered(deathOrder, players)) {
    playerEliminationService.apply(board, victim, killerOf(s, victim), s.turnEnding);
}
characterService.regenerateAllCharacters(s.boardId);
vehicleMaintenanceService.repairAllVehicles(s.boardId);
unitRecoveryService.recoverWoundedUnits(s.turnEnding, s.boardId);

eliminationHistoryService.save(events);
if (!outcome.finished()) {
    board.setCurrentTurn(s.turnEnding + 1);
}
boardRepository.save(board);

if (!events.isEmpty()) {
    List<ResolvedBattleDto> battleDtos = s.resolvedConflicts.stream()
            .map(rb -> toBattleDto(rb, resolveNamesForSession(s))).toList();
    GazetteDraft draft = gazetteDraftFactory.build(
            board, s.turnEnding, events, outcome, result.getCaptures(), players, battleDtos);
    gazetteService.createDraft(s.boardId, s.turnEnding, draft);
}
turnService.publishTurn(s.boardId, board.getCurrentTurn());
```

Notes :

- `finishIfOver` mute `finishedAt`/`winnerPlayerId` ; le `save` est unique.
- `breakLastAllianceIfNeeded` avant le brouillon pour que les faits du tour contiennent la
  rupture ; la factory interroge les `Announcement` du tour.
- Le brouillon est créé **après** `apply` ; les snapshots de l'événement (nom, personnage, cause)
  suffisent, les `ResolvedBattleDto`/rapports de combat sont déjà calculés/persistés.
- `eliminated` est trié par l'ordre de mort, pas par nom.
- `result.getCaptures()` alimente les brèves de capture proposées au MJ ; la factory ne reçoit
  jamais un `BattleReportDetailDto` pour le contenu public, seulement les `ResolvedBattleDto`
  pour les notes admin.
- `GazetteDraft` = `(frontPage, briefs, editorNotes, finalEdition)` ; le **numéro d'édition** et
  le **masthead** (météo, date, faux lien) sont ajoutés par `GazetteService.createDraft`, qui
  calcule `max(issue_number) + 1` et appelle `GazetteMastheadService`.

### 4.6 Abandon soft

`abort` laisse des flags sans événement. Le podium normalise avec des « fantômes » (joueur éliminé
sans événement inséré en fin de tour, assists à 0). La création du brouillon d'un tour abandonné
n'a pas lieu ; le MJ ne le verra pas. Limite existante, documentée, non corrigée ici.

---

## 5. Podium et champion

### 5.1 DTO

```java
@Data
public class GamePodiumDto {
    private boolean finished;
    private Integer finalTurn;
    private String finishedAt;
    private int totalPlayers;
    private int activePlayers;
    private boolean posthumous;
    private boolean championAdjusted;   // désignation MJ
    private Integer lastGazetteTurn;    // dernière édition PUBLIÉE
    private PlaceDto champion;
    private List<PlaceDto> places;

    @Data
    public static class PlaceDto {
        private int place;
        private Long playerId;
        private String playerName;
        private String race;
        private String characterName;
        private Integer eliminatedTurn;
        private String eliminationCause;   // MAIN_CHARACTER_KILLED | HEADQUARTERS_CAPTURED
        private boolean definitive;        // true = mort définitive du lore
        private Long killerPlayerId;
        private String killerName;
        private int kills;
        private int assists;
        private boolean alive;
        private List<String> victims;
        private List<String> assistedVictims;
    }
}
```

- `definitive = "MAIN_CHARACTER_KILLED".equals(cause)`.
- `championAdjusted` vient du board.
- `lastGazetteTurn` ne renvoie que des éditions publiées ; les brouillons sont dans l'admin.
- Les places incluent tous les joueurs, le champion en premier.

### 5.2 Calcul

- Partie en cours : `finished=false`, places vides, `activePlayers`/`totalPlayers` renseignés.
- Partie terminée : `deaths` = événements (+ fantômes) triés par index de mort ; `alive` = 0 ou 1.
- `champion` :
  - 1 survivant : le survivant ;
  - 0 survivant : le `board.winnerPlayerId` (naturel = dernier mort, ou ajusté par le MJ).
- `places` : champion = 1 ; les autres morts dans l'ordre inverse de mort = 2..N ; le champion est
  retiré de la liste des morts. Aucun doublon.
- `kills` : `count(events.killerPlayerId == player)`.
- `assists` : présence dans `assistPlayerIds`.
- `victims`/`assistedVictims` : noms des victimes issues des événements.
- `characterName` du survivant : live ; pour un mort : snapshot de l'événement.

### 5.3 Ajustement du champion par le MJ

- `PUT /api/admin/game/winner`, body `{ "playerId": 5 }`.
- Préconditions : board terminé, `activePlayers == 0` (posthume), joueur du board et éliminé.
- Effet : `winnerPlayerId = playerId` ; `winnerAdjusted = (playerId != dernierMort)` ; audit
  `GAME_WINNER_OVERRIDE` (ancien/nouveau).
- Pour revenir au champion naturel : renvoyer le dernier mort ; `winnerAdjusted` repasse à
  `false`.
- Le statut session expose `winnerAdjusted` : la bannière peut afficher « champion désigné par le
  MJ ».
- UI : sur `/podium`, bloc admin visible si `posthumous` : select des éliminés + bouton
  « Désigner le champion » + confirmation ; rechargement du podium.
- Le MJ peut aussi laisser le champion naturel ; aucune action requise.
- Le brouillon de l'édition finale est créé avec le champion **naturel**. Si le MJ ajuste ensuite,
  le podium fait foi et le MJ écrit le champion qu'il veut dans l'édition : le brouillon n'est pas
  régénéré.

### 5.4 UI Podium

- Accueil : `<app-podium-panel>` remplace les classements quand `gameFinished`.
- `/podium` : champion, marches or/argent/bronze, classement complet, palmarès kills/assists,
  dernière édition publiée, actions carte/journal/accueil.
- Cause lore : chaque ligne du classement affiche un badge :
  - « Mort définitive » (`definitive=true`) — le personnage quitte le lore ;
  - « Défaite — retour possible » (`HEADQUARTERS_CAPTURED`) — le personnage peut revenir dans une
    édition future.
- Champion posthume : mention « Vainqueur posthume » ; si `championAdjusted`, « Désigné par le
  MJ ».
- Tableau : Place, Joueur, Race, Éliminé au tour, Cause, Tueur, Kills, Assists. Ligne du champion
  surlignée.
- Palmarès par joueur : « A éliminé : … », « A assisté : … ».
- Accessibilité : `caption`, `scope`, libellés de médailles, pas de couleur seule.

### 5.5 Kills et assists

- Kill = tueur désigné (MJ) ou capturant du QG.
- Assist = joueur du camp du tueur dans la bataille, hors tueur et hors victime.
- Pas de tueur ⇒ 0 assist.
- Le tueur éliminé dans le même tour conserve son kill.
- Pour la gazette, ces données sont dans les **faits du tour** admin-only ; le MJ peut les
  révéler. Le podium les publie automatiquement après la fin.

---

## 6. Gazette : Brigade des Morts

### 6.1 Principe

- **Le MJ écrit et publie.** Le système ne produit aucun titre, aucune citation, aucun corps.
- Le système :
  - ouvre un **brouillon** d'édition à chaque tour avec au moins une élimination ;
  - génère et persiste le **masthead** (nom, volume, numéro, date, faux lien, météo) ;
  - pré-remplit 1 à 3 **slots de une** (un par éliminé, dans l'ordre des meilleures places) et des
    slots de brèves pour les éliminés au-delà de 3 ;
  - fournit un panneau **Faits du tour** (admin-only) avec tout le matériau utile, y compris des
    informations privées ;
  - laisse le MJ écrire, prévisualiser, enregistrer et publier ; il peut corriger une édition
    publiée.
- Les joueurs ne voient que les éditions **publiées**.

### 6.2 Cycle de vie

```text
finalizeTurn (≥1 élimination)
        │
        ▼
GazetteService.createDraft
  ├─ masthead (Brigade des Morts, vol/numéro, date, faux lien, météo)
  ├─ frontPage : 1..3 slots "ÉLIMINATION — <victime>" (portrait pré-sélectionné)
  ├─ briefs : 0..4 slots pour les éliminés supplémentaires
  └─ editorNotes : faits du tour (admin-only)
        │
        ▼
MJ : /admin/journal → éditeur
  ├─ écrit les articles, choisit les portraits, prévisualise
  ├─ Enregistrer le brouillon (PUT)
  └─ Publier (POST, validation)
        │
        ▼
Joueurs : /journal (publiées) + /journal/:turn
```

- Un brouillon non publié n'apparaît jamais côté joueur.
- `createDraft` est idempotent sur `(board, turn)` : si une édition existe déjà, elle est retournée
  et n'est pas écrasée.
- Une édition publiée peut être corrigée (`PUT` + audit, `updatedAt`) ; `publishedAt` ne change
  pas. Le bouton « Publier » devient « Enregistrer les corrections ».
- Un brouillon peut être supprimé (`DELETE`), une édition publiée non.
- Le podium ne dépend pas de la publication : la fin de partie est immédiate ; la gazette est
  éditoriale.

### 6.3 Masthead (généré et persisté)

| Champ | Valeur |
|---|---|
| `journalName` | **Brigade des Morts** |
| `subtitle` | « Organe des conflits de {boardName} » |
| `volume` | numéro du tour |
| `issueNumber` | `max(issue_number) + 1` au moment du brouillon |
| `fakeUrl` | `www.brigade-des-morts.nml/vol-{turn}/n-{issue}/{slug}` (slug uniforme, persisté) |
| `editionDate` | instant de la finalisation du tour (persisté) |
| `dateLabel` | date de `editionDate` en français, zone Europe/Paris |
| `boardName` | nom du plateau |
| `weather` | tirage uniforme, persisté |

- Le faux lien est affiché en **texte** (`<span>`), jamais un `<a>`.
- La date de **publication** (`publishedAt`) est distincte de la date d'édition : le MJ peut
  publier plus tard ; le masthead garde la date du tour.
- Le MJ ne modifie pas le masthead ; il peut changer le contenu des articles.
- La météo est tirée une fois, avec `Random(seed)` où
  `seed = boardId * 1_000_003 + turn` : condition uniforme parmi six, puis valeurs dans les
  plages. Elle est persistée telle quelle ; la publication ne la régénère pas.
- Le **slug du faux lien** est aussi tiré uniformément dans une petite liste
  (`le-front`, `les-cendres`, `la-une`, `le-carnet`, `l-edition`) et persisté.
- Le volume, le numéro et la date sont **dérivés** du tour et persistés (pas de hasard). Seuls la
  météo et le slug du lien sont tirés ; tout est figé à la création du brouillon.
- Le numéro d'édition est réservé à la création du brouillon. Supprimer un brouillon non publié
  laisse un trou dans la numérotation publique (les joueurs ne voient que les éditions
  publiées) ; c'est accepté pour la BETA.

| Condition | Icône | Température | Précipitations | Humidité | Vent |
|---|---|---:|---:|---:|---:|
| Ciel de plomb | `cloud` | 8–18 °C | 2–8 mm | 50–75 % | 5–20 km/h |
| Brouillard de cendres | `foggy` | 4–14 °C | 0–3 mm | 70–95 % | 0–10 km/h |
| Averse acide | `water_drop` | 12–24 °C | 12–30 mm | 75–95 % | 15–35 km/h |
| Vent de sable | `air` | 25–42 °C | 0–1 mm | 10–30 % | 40–80 km/h |
| Nuit claire | `nights_stay` | 5–20 °C | 0 mm | 30–55 % | 0–15 km/h |
| Orage magnétique | `thunderstorm` | 15–30 °C | 10–25 mm | 60–90 % | 30–70 km/h |

Direction du vent : 16 points (`N` à `NNO`). Aucun effet de gameplay.

### 6.4 Template d'article

Chaque article (une ou brève) a exactement ces champs :

| Champ | Rôle | Limite |
|---|---|---|
| `kind` | ÉLIMINATION, CAPTURE, ALLIANCE, TRAHISON, DÉFAITE, AUTRE | liste |
| `section` | LA UNE, LES AUTRES FAITS, CHRONIQUE | liste |
| `headline` | titre, affiché en gras/majuscules CSS | 120 |
| `quote` | citation entre guillemets, optionnelle | 220 |
| `body` | corps, paragraphes séparés par `\n\n` | 4000 |
| `author` | nom de l'auteur | 80 |
| `photoPlayerName` | portrait d'un joueur, optionnel | nom existant |
| `photoCaption` | légende de l'image | 160 |

Mise en page :

- 1 à 3 **unes** : la première grande (image + titre + citation + corps en 2 colonnes), les
  suivantes en secondes unes (titre + citation + corps).
- 0 à 4 **brèves** en colonnes, sans image.
- Le titre est stocké en casse naturelle, affiché en majuscules via CSS.
- Le template affiche `ÉDITION SPÉCIALE — DOUBLE ÉLIMINATION` / `TRIPLE ÉLIMINATION` selon le
  nombre de unes, et `DERNIÈRE ÉDITION` si `finalEdition` ; pas de champ séparé à saisir.
- Aucun HTML ; le corps est découpé sur `\n\n` et interpolé.
- Un article vide n'est pas rendu ; la publication exige au moins une une complète (titre, corps,
  auteur).

### 6.5 Pré-remplissage et faits du tour

Le brouillon est **pré-populé**, pas rédigé :

- `frontPage` : un article par éliminé, dans l'ordre des meilleures places (le dernier éliminé du
  tour en tête), au plus 3 :
  - `headline = "ÉLIMINATION — " + victime` (placeholder) ;
  - `quote`, `body`, `author` vides ;
  - `kind = ELIMINATION`, `section = LA UNE` ;
  - `photoPlayerName = victime` (le portrait est pré-sélectionné).
- `briefs` : les éliminés au-delà de 3, avec un titre placeholder « AUSSI — <victime> ».
- `editorNotes` (admin-only, jamais dans le DTO public) :
  - éliminations : victime, tour, cause, tueur, assists, personnage, secteur de bataille ;
  - batailles : secteur, vainqueur/perdant, pertes, personnage perdu, QG capturé, extraits du
    journal de combat ;
  - captures : secteur, nouveau propriétaire, capture en mouvement ;
  - annonces : formation, rupture, trahison, **FINAL_DUEL** ;
  - joueurs : vivants/éliminés, secteurs contrôlés ;
  - issue : terminée, champion, posthume, ajusté.
- Le panneau « Faits du tour » de l'éditeur rend ces notes ; le MJ copie/adapte ce qu'il veut.
  Ces notes peuvent révéler des informations privées (unités, pertes, position) : c'est
  volontaire, le MJ décide de ce qu'il publie.

### 6.6 Visibilité

| Contenu | Joueur | MJ/Admin |
|---|---|---|
| Masthead généré | oui (édition publiée) | oui |
| Articles du MJ | oui (publiés) | oui |
| Brouillon | non (404) | oui |
| Faits du tour / notes | non | oui |
| Journal technique `TurnJournal` | non (admin) | oui |
| Cause d'élimination au podium | oui | oui |

- `GET /api/game/gazette` et `/gazette/{turn}` ne renvoient que `PUBLISHED`.
- Les endpoints admin sont sous `/api/admin/game/gazette`, protégés par
  `hasAnyRole('ADMIN','INSTANCE_ADMIN')` + `X-Board-Id`.
- Aucun fait du tour n'est publié automatiquement ; même le nom des éliminés n'apparaît dans
  l'édition que si le MJ l'écrit (les placeholders sont des suggestions à remplacer).

### 6.7 Éditeur MJ

Route : `/admin/journal` (liste) et `/admin/journal/:turn` (éditeur).

- **Liste** : brouillons et éditions publiées, tour, statut (`BROUILLON`/`PUBLIÉE`),
  date, « Rédiger » / « Modifier » / « Publier ».
- **Éditeur** :
  - colonne gauche : formulaire
    - masthead en lecture seule (aperçu) ;
    - cartes d'articles de une (max 3) : type, section, titre, citation, corps, auteur, portrait
      (select des joueurs), légende ; boutons monter/descendre/supprimer ;
    - brèves (max 4) ;
    - boutons « Ajouter une une », « Ajouter une brève » ;
    - compteurs de caractères et validation.
  - colonne droite : **aperçu live** du journal avec le même template que la page publique.
  - panneau « Faits du tour » (repliable) : notes admin-only ; bouton « Copier » par bloc.
  - panneau « Modèle » : un exemple statique d'article (titre, citation, image, corps, auteur) et
    les consignes de mise en forme. C'est le **template** fourni au MJ ; il n'est pas généré.
  - boutons : « Enregistrer » (PUT), « Publier » (POST, désactivé tant qu'aucune une complète),
    « Supprimer le brouillon » (si DRAFT), « Retour ».
- **Publication** : validation serveur et client :
  - au moins une une avec `headline`, `body`, `author` non vides ;
  - max 3 unes, max 4 brèves ;
  - limites de longueur ;
  - les articles vides sont ignorés/filtrés.
- **Après publication** : « Enregistrer les corrections » ; `updatedAt` ; audit
  `GAZETTE_UPDATED` ; la page publique affiche « Édition mise à jour le … » si
  `updatedAt > publishedAt`.
- **Accessibilité** : labels, `aria-describedby` pour les compteurs, focus, erreurs `role="alert"`,
  responsive (aperçu sous le formulaire en mobile).

### 6.8 Page publique

- `/journal` : liste des éditions **publiées** (tour décroissant), badge « Dernière édition »,
  date, titre principal ; lien vers l'édition.
- `/journal/:turn` : masthead complet (météo, nom, volume, numéro, faux lien, date), 1 à 3 unes,
  brèves, pied de page. Si l'édition est introuvable ou non publiée : message « Édition non
  publiée / introuvable ».
- Le MJ voit un lien « Modifier » vers `/admin/journal/{turn}`.
- Le bouton « Imprimer » est exempté du `readonly-mode` (comme « Réessayer »).
- Design : template papier sombre/clair selon le thème ; polices et filets existants ; pas de
  `mat-card` pour le journal.

### 6.9 Templates et composants partagés

- `GazetteTemplateComponent` (ou `JournalIssueLayoutComponent`) : reçoit un `GazetteIssue` et
  rend le masthead + les articles. Utilisé par la page publique **et** l'aperçu de l'éditeur.
- `GazetteArticleFormComponent` : un article éditable (champs + compteurs).
- `GazetteNotesPanelComponent` : les faits du tour.
- `PlayerPhotoComponent` : portrait + repli (voir §9).
- `journal.helpers.ts` : découpage des paragraphes, libellés de statut, validation des longueurs,
  formatage de la date.

---

## 7. Modèle de données

### 7.1 Board

```java
@Column(name = "finished_at")
private Instant finishedAt;

@Column(name = "winner_player_id")
private Long winnerPlayerId;

@Column(name = "winner_adjusted", nullable = false)
private boolean winnerAdjusted = false;

public boolean isFinished() { return finishedAt != null; }
```

### 7.2 Migration `V40__game_end_and_gazette.sql`

```sql
ALTER TABLE public.boards ADD COLUMN finished_at timestamp(6) with time zone;
ALTER TABLE public.boards ADD COLUMN winner_player_id bigint;
ALTER TABLE public.boards ADD COLUMN winner_adjusted boolean NOT NULL DEFAULT false;

CREATE SEQUENCE public.elimination_events_id_seq START WITH 1;
ALTER SEQUENCE public.elimination_events_id_seq INCREMENT BY 50;

CREATE TABLE public.elimination_events (
    id bigint NOT NULL DEFAULT nextval('public.elimination_events_id_seq'),
    board_id bigint NOT NULL,
    death_index integer NOT NULL,
    victim_player_id bigint NOT NULL,
    victim_name character varying(255) NOT NULL,
    victim_race character varying(20),
    victim_character_name character varying(100),
    eliminated_turn integer NOT NULL,
    cause character varying(40) NOT NULL,
    killer_player_id bigint,
    killer_name character varying(255),
    assist_player_ids character varying(500),
    battle_sector integer,
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT elimination_events_pkey PRIMARY KEY (id),
    CONSTRAINT elimination_events_cause_check
        CHECK (cause IN ('HEADQUARTERS_CAPTURED', 'MAIN_CHARACTER_KILLED')),
    CONSTRAINT elimination_events_board_fk
        FOREIGN KEY (board_id) REFERENCES public.boards (id) ON DELETE CASCADE
);
ALTER SEQUENCE public.elimination_events_id_seq OWNED BY public.elimination_events.id;
CREATE INDEX elimination_events_board_order_idx
    ON public.elimination_events (board_id, death_index);
CREATE UNIQUE INDEX elimination_events_board_victim_uk
    ON public.elimination_events (board_id, victim_player_id);
CREATE UNIQUE INDEX elimination_events_board_death_uk
    ON public.elimination_events (board_id, death_index);

CREATE SEQUENCE public.gazette_issues_id_seq START WITH 1;
ALTER SEQUENCE public.gazette_issues_id_seq INCREMENT BY 50;

CREATE TABLE public.gazette_issues (
    id bigint NOT NULL DEFAULT nextval('public.gazette_issues_id_seq'),
    board_id bigint NOT NULL,
    turn integer NOT NULL,
    issue_number integer NOT NULL,
    status character varying(20) NOT NULL,
    main_headline character varying(255),
    payload text NOT NULL,
    editor_notes text,
    final_edition boolean NOT NULL DEFAULT false,
    created_at timestamp(6) with time zone NOT NULL,
    published_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    published_by_user_id bigint,
    CONSTRAINT gazette_issues_pkey PRIMARY KEY (id),
    CONSTRAINT gazette_issues_status_check
        CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT gazette_issues_board_fk
        FOREIGN KEY (board_id) REFERENCES public.boards (id) ON DELETE CASCADE
);
ALTER SEQUENCE public.gazette_issues_id_seq OWNED BY public.gazette_issues.id;
CREATE UNIQUE INDEX gazette_issues_board_turn_uk
    ON public.gazette_issues (board_id, turn);
CREATE UNIQUE INDEX gazette_issues_board_issue_uk
    ON public.gazette_issues (board_id, issue_number);

ALTER TABLE public.announcements DROP CONSTRAINT announcements_type_check;
ALTER TABLE public.announcements
    ADD CONSTRAINT announcements_type_check
    CHECK (type IN ('ALLIANCE_FORMED', 'ALLIANCE_BROKEN', 'BETRAYAL', 'FINAL_DUEL'));
```

- Pas de FK vers `players` pour l'historique : snapshot d'archive.
- Les tables sont entraînées par `ON DELETE CASCADE` du board (Postgres) ; la purge H2 tronque
  toutes les tables.
- `status` : `DRAFT` / `PUBLISHED` ; `main_headline` nullable (brouillon sans titre).
- `updated_at` non nul (création = mise à jour initiale) ; `published_at` nullable.

### 7.3 Entités et dépôts

- `EliminationEvent` : champs de la table §7.2 ; `assistIds()` CSV trié ; `cause` enum STRING.
- `GazetteIssue` : champs de la table ; `GazetteStatus status` enum STRING ; `payload` JSON du
  `GazetteIssueDto` public ; `editorNotes` JSON du `GazetteEditorNotesDto` (admin-only) ;
  `mainHeadline` nullable.
- `GazetteStatus` : `DRAFT`, `PUBLISHED`.
- `EliminationEventRepository` :
  - `findByBoardIdOrderByDeathIndexAsc`
  - `countByBoardId`
  - `findFirstByBoardIdOrderByDeathIndexDesc`
  - `deleteByBoardId` (dev)
- `GazetteIssueRepository` :
  - `findByBoardIdAndTurn`
  - `findByBoardIdOrderByTurnDesc` (admin)
  - `findByBoardIdAndStatusOrderByTurnDesc` (public)
  - `findFirstByBoardIdAndStatusOrderByTurnDesc`
  - `findMaxIssueNumberByBoardId` (`@Query` ; `null` si aucune édition)
  - `countByBoardId`
  - `deleteByBoardId` (dev)
- `AnnouncementRepository` : `findByBoardIdAndTurnCreatedOrderByIdAsc`.
- Longueurs à mapper (`@Column(length=...)`) pour `ddl-auto=validate` : noms 255, race 20, cause
  40, assists 500, headline 255.

### 7.4 Invariants

1. Un événement par joueur et par board.
2. `death_index` unique et croissant par board.
3. Une édition par tour et un numéro par board.
4. Une édition `PUBLISHED` a `published_at` non nul et au moins une une complète.
5. Une édition `DRAFT` n'est jamais renvoyée par les endpoints publics.
6. `editor_notes` n'est jamais dans le payload public.
7. `winnerAdjusted` ne peut être vrai que si `winnerPlayerId` diffère du dernier mort.
8. `FINAL_DUEL` n'est créé que si deux joueurs vivants alliés subsistent à la fin d'un tour.

---

## 8. Backend et API

### 8.1 Composants

| Fichier | Rôle |
|---|---|
| `domain/model/press/EliminationEvent.java` | Snapshot d'élimination. |
| `domain/model/press/GazetteIssue.java` | Édition (brouillon/publiée). |
| `domain/model/press/GazetteStatus.java` | `DRAFT` / `PUBLISHED`. |
| `domain/model/alliance/AnnouncementType.java` | Ajout `FINAL_DUEL`. |
| `domain/service/EliminationHistoryService.java` | Ordre de mort, index, snapshots, assists. |
| `domain/service/GameEndService.java` | `finishIfOver`, `getState`, `adjustWinner`. |
| `domain/service/FinalDuelService.java` | Rupture automatique de la dernière alliance. |
| `domain/service/GamePodiumService.java` | Places, kills/assists, causes lore, champion ajusté. |
| `domain/service/GazetteService.java` | `createDraft`, `update`, `publish`, `deleteDraft`, listes/lecture. |
| `domain/service/GazetteMastheadService.java` | Nom, volume, numéro, date, faux lien, météo. |
| `domain/service/GazetteDraftFactory.java` | Slots pré-remplis + notes du tour (`GazetteDraft`). |
| `domain/service/GameHistoryResetService.java` | Nettoyage dev (événements + éditions + board). |
| `infrastructure/repository/EliminationEventRepository.java` | Accès. |
| `infrastructure/repository/GazetteIssueRepository.java` | Accès. |
| `api/dto/GamePodiumDto.java` | Podium. |
| `api/dto/GazetteIssueDto.java` | Édition publique. |
| `api/dto/GazetteAdminIssueDto.java` | Édition + statut + notes + dates d'admin. |
| `api/dto/GazetteIssueSummaryDto.java` | Résumé de liste. |
| `api/dto/GazetteContentRequestDto.java` | Corps d'enregistrement/publication (unes + brèves). |
| `api/dto/GazetteEditorNotesDto.java` | Faits du tour (admin-only). |
| `api/dto/GameWinnerRequestDto.java` | `{ "playerId": ... }`. |
| `api/controller/GameController.java` | Public `/api/game/podium`, `/api/game/gazette`. |
| `api/controller/AdminGameController.java` | Admin gazette + champion. |
| `config/GameOverFilter.java` | 409 mutations après la fin. |

`GazetteDraft` (record) : `frontPage`, `briefs`, `editorNotes`, `finalEdition`.
`GazetteService.createDraft(boardId, turn, draft)` :
`issueNumber = (max(issue_number) ou 0) + 1`, appelle `GazetteMastheadService`
(météo/date/lien), construit le `GazetteIssueDto` public, sérialise, persiste en `DRAFT` et
renvoie l'édition admin.

### 8.2 Fichiers modifiés

| Fichier | Changement |
|---|---|
| `domain/model/board/Board.java` | Champs de fin + `isFinished()`. |
| `domain/service/TurnResolutionOrchestrator.java` | Finalisation réordonnée, fin de partie, dernière alliance, création du brouillon, `ResolvedBattle` + camps/conflit. |
| `domain/service/TurnService.java` | Refus après la fin (`advanceTurnAndReport`, `setCurrentTurn`). |
| `domain/service/AllianceService.java` | Refus d'alliance quand `vivants <= 2`. |
| `domain/service/AllianceService`/`PlayerStatus` | Libellé `FINAL_DUEL`. |
| `api/controller/PlayerController.java` | `/me/status` enrichi (fin, champion, ajusté). |
| `api/dto/PlayerStatusDto.java` | `participant`, `gameFinished`, `finalTurn`, `winner*`, `winnerAdjusted`. |
| `api/dto/TurnFinalizeResultDto.java` | `gameFinished`, `winner*`, `gazetteDraftTurn`. |
| `api/dto/TurnJournalDto.java` | Mêmes champs de fin + `gazetteDraftTurn`. |
| `api/dto/AnnouncementDto` / mapper | Nouveau type. |
| `infrastructure/repository/AnnouncementRepository.java` | Finder par tour. |
| `domain/model/audit/AdminAuditAction.java` | `GAZETTE_PUBLISHED`, `GAZETTE_UPDATED`, `GAZETTE_DRAFT_DELETED`, `GAME_WINNER_OVERRIDE`. |
| `domain/service/TurnResolutionScenarioSeeder.java` | Reset des événements/éditions du joueur d'arène. |
| `api/controller/DevSeederController.java` | Optionnel : scénario fin de partie + brouillon. |
| `AGENTS.md`, `README.md` | Règles et endpoints. |
| `pages/regles/regles.component.html` | Retrait de l'objectif secret ; BETA ; dernière alliance ; causes ; gazette MJ. |

### 8.3 API

#### Public

| Méthode | URL | Réponse |
|---|---|---|
| GET | `/api/game/podium` | `GamePodiumDto` (200 même en cours). |
| GET | `/api/game/gazette` | `List<GazetteIssueSummaryDto>` des éditions **publiées**, tour décroissant. |
| GET | `/api/game/gazette/{turn}` | `GazetteIssueDto` publiée ; 404 si brouillon/absente. |

#### Admin (ADMIN/INSTANCE_ADMIN + X-Board-Id)

| Méthode | URL | Réponse |
|---|---|---|
| GET | `/api/admin/game/gazette` | Liste brouillons + publiées (résumé + statut). |
| GET | `/api/admin/game/gazette/{turn}` | `GazetteAdminIssueDto` (édition + notes + statut). |
| PUT | `/api/admin/game/gazette/{turn}` | Enregistre les articles (brouillon ou correction publiée). |
| POST | `/api/admin/game/gazette/{turn}/publish` | Valide et publie ; 400 si aucune une complète. |
| DELETE | `/api/admin/game/gazette/{turn}` | Supprime un brouillon ; 409 si publiée. |
| PUT | `/api/admin/game/winner` | Désigne le champion posthume ; 400 si non posthume. |

- Auth : `Bearer` + `X-Board-Id`.
- `turn < 1` → 400 ; édition inconnue → 404 ; édition publiée supprimée → 409.
- La publication est journalisée (`GAZETTE_PUBLISHED`) ; l'ajustement du champion l'est aussi.
- `GazetteIssueSummaryDto` : `turn`, `issueNumber`, `status`, `mainHeadline`, `publishedAt`,
  `updatedAt`, `finalEdition`. Côté public, `status` vaut toujours `PUBLISHED`.

#### DTO publics (extraits)

```json
{
  "finished": true, "finalTurn": 7, "finishedAt": "2026-11-13T20:14:00Z",
  "totalPlayers": 6, "activePlayers": 1, "posthumous": false, "championAdjusted": false,
  "lastGazetteTurn": 7,
  "champion": { "place": 1, "playerName": "nursek", "alive": true, "kills": 2, "assists": 1 },
  "places": [
    { "place": 1, "playerName": "nursek", "alive": true, "definitive": false },
    { "place": 2, "playerName": "imotekh", "alive": false, "eliminationCause": "HEADQUARTERS_CAPTURED",
      "definitive": false, "killerName": "nursek", "kills": 1, "assists": 0 }
  ]
}
```

```json
{
  "schemaVersion": 1, "turn": 7, "issueNumber": 3, "publishedAt": "2026-11-13T21:10:00Z",
  "updatedAt": "2026-11-13T21:10:00Z", "finalEdition": true,
  "masthead": {
    "journalName": "Brigade des Morts", "volume": 7, "dateLabel": "13 novembre 2026",
    "fakeUrl": "www.brigade-des-morts.nml/vol-7/n-3/le-front",
    "boardName": "Carte Principale",
    "weather": { "condition": "Brouillard de cendres", "icon": "foggy",
                 "temperature": 9, "precipitation": 2, "humidity": 86,
                 "wind": 8, "windDirection": "NNE" }
  },
  "frontPage": [
    { "kind": "ELIMINATION", "section": "LA UNE",
      "headline": "Les mercenaires croisent le fer",
      "quote": "« Il est tombé les armes à la main… mais avec quelles armes ? »",
      "body": "…\n\n…\n\n…", "author": "Vexa-9",
      "photoPlayerName": "lurio", "photoCaption": "Portrait d'archives" }
  ],
  "briefs": []
}
```

### 8.4 Erreurs

| Cas | Statut |
|---|---|
| Brouillon demandé côté joueur | 404 |
| Publication sans une complète | 400 + détail |
| Plus de 3 unes / 4 brèves / longueur dépassée | 400 |
| Correction d'une édition publiée | 200 (audit, `updatedAt`) |
| Suppression d'une édition publiée | 409 |
| Champion ajusté sur partie non posthume | 400 |
| Mutation après la fin | 409 (`GameOverFilter`) |
| Résolution démarrée après la fin | 400 (`IllegalStateException`) |

---

## 9. Frontend

### 9.1 Modèles, routes, services

Modèles :

- `models/podium.model.ts` : `GamePodium`, `PodiumPlace`.
- `models/gazette.model.ts` : `GazetteIssue`, `GazetteIssueSummary`, `GazetteAdminIssue`,
  `GazetteArticle`, `GazetteMasthead`, `GazetteWeather`, `GazetteEditorNotes`.
- `models/player.model.ts` : `PlayerStatus` étendu (fin de partie, champion, ajusté).
- `models/turn-resolution.model.ts` : `TurnFinalizeResult` étendu (`gazetteDraftTurn`).
- `models/turn-journal.model.ts` : `TurnExport` étendu.

Routes :

```ts
{ path: 'podium',       loadComponent: () => import('./pages/podium/podium.component').then(m => m.PodiumComponent), canActivate: [authGuard, instanceGuard] },
{ path: 'journal',      loadComponent: () => import('./pages/journal/journal-list.component').then(m => m.JournalListComponent), canActivate: [authGuard, instanceGuard] },
{ path: 'journal/:turn',loadComponent: () => import('./pages/journal/journal-issue.component').then(m => m.JournalIssueComponent), canActivate: [authGuard, instanceGuard] },
{ path: 'admin/journal',      loadComponent: () => import('./pages/admin-journal/admin-journal-list.component').then(m => m.AdminJournalListComponent), canActivate: [authGuard, instanceGuard, adminGuard] },
{ path: 'admin/journal/:turn',loadComponent: () => import('./pages/admin-journal/admin-journal-editor.component').then(m => m.AdminJournalEditorComponent), canActivate: [authGuard, instanceGuard, adminGuard] },
```

- `adminGuard` couvre le MJ (`ROLE_INSTANCE_ADMIN`) et le `ADMIN` (voir guard existant).
- L'éditeur utilise des **Reactive Forms** (`FormArray` pour les articles) : c'est le seul
  formulaire dynamique du lot ; le reste reste à signaux.
- `ApiService` ajoute :
  - `getPodium()`, `getGazetteIssues()`, `getGazetteIssue(turn)` (ou `httpResource` direct) ;
  - `adminGetGazetteIssues()`, `adminGetGazetteIssue(turn)`, `adminSaveGazette(turn, body)`,
    `adminPublishGazette(turn, body)`, `adminDeleteGazetteDraft(turn)`,
    `adminAdjustWinner(playerId)`.

**Fichiers existants modifiés :**

| Fichier | Changement |
|---|---|
| `app.routes.ts` | Routes `/podium`, `/journal`, `/journal/:turn`, `/admin/journal`, `/admin/journal/:turn`. |
| `app.ts` | Bannière fin de partie (prioritaire sur « éliminé »), `locked()`, lien podium ; `RouterLink`. |
| `components/navbar/navbar.component.ts` | « Journal » côté joueur ; « Rédaction journal » côté MJ/admin ; « Podium » conditionnel. |
| `services/player-status.service.ts` | `gameFinished`, `winner`, `winnerAdjusted`, `locked`. |
| `models/alliance.model.ts` | Type `FINAL_DUEL`. |
| `pages/accueil/accueil.component.ts/html` | Bascule classements ↔ podium ; libellé `FINAL_DUEL` ; « Joueurs en vie » filtré. |
| `pages/turn-management/turn-management.component.ts/html` | Carte « Partie terminée » + « Brouillon de gazette à rédiger » ; état verrouillé. |
| `pages/regles/regles.component.html` | Retrait de `#empereur` ; BETA ; dernière alliance ; causes ; Brigade des Morts. |
| `models/index.ts` | Exports podium/gazette. |
| `styles.scss` | `readonly-allow` ; `@media print`. |
| `ApiService` | Méthodes publiques/admin listées ci-dessus. |
| Specs | `player-status`, `navbar`, `accueil`, `turn-management`, `turn-export`. |

### 9.2 Composants

| Composant | Rôle |
|---|---|
| `pages/accueil/podium-panel` | Panneau Accueil quand `gameFinished`. |
| `pages/podium/podium` | Champion, marches, classement complet, causes lore, palmarès, dernière édition, bloc admin de désignation du champion. |
| `pages/journal/journal-list` | Archive publiée. |
| `pages/journal/journal-issue` | Édition publiée + `GazetteTemplate`. |
| `shared/gazette/gazette-template` | Masthead + unes + brèves ; input `GazetteIssue` ; utilisé par la page publique et l'aperçu éditeur. |
| `pages/admin-journal/admin-journal-list` | Liste brouillons/publiées. |
| `pages/admin-journal/admin-journal-editor` | Formulaire + aperçu live + faits du tour + modèle + actions. |
| `shared/gazette/gazette-article-form` | Un article éditable + compteurs. |
| `shared/gazette/gazette-notes-panel` | Faits du tour (admin). |
| `shared/player-photo` | Portrait + repli sigil (journal/podium). |
| `guards/game-over.guard` | Optionnel : `/boutique`, `/ordres` → `/podium`. |

### 9.3 Éditeur en détail

- **Entrée** : `GET /api/admin/game/gazette/{turn}` → `GazetteAdminIssue`.
- **Formulaire** :
  - `headline` (requis pour une une), `quote` (optionnel), `body` (requis), `author` (requis pour
    une une), `photoPlayerName` (select des joueurs), `photoCaption`, `kind`, `section`.
  - `frontPage` : 0 à 3 articles ; `briefs` : 0 à 4.
  - Boutons : ajouter/supprimer/monter/descendre.
  - Compteurs et messages d'erreur sous les champs ; le bouton Publier est désactivé si aucune
    une complète.
- **Aperçu** : le composant `GazetteTemplate` rend l'état courant (masthead généré + articles) ;
  les articles vides ne sont pas rendus.
- **Faits du tour** : sections repliables, chaque bloc a un bouton « Copier » ; les notes sont
  admin-only.
- **Modèle** : panneau « Comment écrire une une » avec un exemple statique (titre, citation,
  image, corps, auteur) et les limites.
- **Actions** :
  - `Enregistrer` (PUT) → brouillon ; pour une édition publiée, « Enregistrer les corrections ».
  - `Publier` (POST) → validation serveur ; snackbar ; redirection vers la liste.
  - `Supprimer` → confirmation ; DRAFT uniquement.
- **Avertissements** : brouillon non publié, correction d'une édition déjà lue, publication de
  l'édition finale.

### 9.4 Page publique

- `GazetteTemplate` : masthead (Brigade des Morts, vol/numéro, date, faux lien en `span`, météo),
  unes, brèves, pied de page (heure de publication, « édition archivée »).
- Le MJ voit « Modifier » (lien `/admin/journal/{turn}`) ; le joueur ne voit pas les brouillons.
- Impression (`window.print()`) + `@media print` ; bouton exempté du `readonly-mode`.
- 404/erreur : message clair + retour archive.

### 9.5 Design et accessibilité

- Réutiliser les jetons `styles.scss` : `--paper`, `--surface`, `--ink`, `--line-strong`,
  `--accent`, `--font-display`, `--font-mono`, `--font-sans`.
- Masthead : nom en `clamp(2rem, 5.5vw, 3.6rem)`, filet double, météo en mono.
- Une : grille 38/62 (photo/texte), titre en display, citation en italique, corps en 2 colonnes,
  lettrine. Brèves en 3 colonnes.
- Responsive : 900 px masthead empilé ; 720 px une en pile, corps 1 colonne.
- `readonly-mode` : exemption `button.readonly-allow` pour « Imprimer » / « Réessayer ».
- `PlayerPhotoComponent` : `assets/<joueur.toLowerCase()>/characters/portrait.png` via
  `optimizedImage` ; valider `^[a-z0-9_-]+$` avant l'URL (pas de chemins exotiques) ; repli
  sigil + initiale + « PHOTO D'ARCHIVES » ; `alt` utile, `loading="lazy"`, ratio fixe.
- A11y : `<h1>` = Brigade des Morts, unes `<h2>`, brèves `<h3>` ; image `alt` ; faux lien
  `role="note"` ; tableaux avec `caption`/`scope` ; pas de couleur seule ; focus visible.
- Aucun `innerHTML` ; le corps est découpé en paragraphes et interpolé.

---

## 10. Page Règles

Fichier `nml-ui/src/app/pages/regles/regles.component.html`.

### 10.1 Retraits

- **Supprimer entièrement le panneau `#empereur`** (« Objectif secret »), son bloc `.draft` et
  toute mention de l'objectif secret / de l'empereur.
- Retirer le badge « Podium à venir » du panneau `#victoire`.
- Vérifier les liens/ancres vers `#empereur` (aucun lien de nav attendu) et les textes qui
  promettent une victoire d'alliance.

### 10.2 Panneau `#victoire` — texte cible

> **BETA 6 joueurs** — Chacun joue pour soi. Les alliances sont tactiques : elles ne peuvent pas
> gagner la partie. Le podium ne couronne qu'un joueur.
>
> **Fin de partie** — Dès qu'il ne reste qu'un joueur en vie, la partie se termine à la fin du
> tour. S'il ne reste aucun survivant, le dernier éliminé est **vainqueur posthume** ; le MJ peut
> désigner un autre champion parmi les éliminés.
>
> **Dernière alliance** — S'il ne reste que deux joueurs alliés, l'alliance se brise
> automatiquement au tour suivant : ils doivent s'affronter. Aucune nouvelle alliance n'est
> possible à deux joueurs.
>
> **Ordre du podium** — Vainqueur puis éliminés dans l'ordre inverse des éliminations. Les
> éliminations d'un même tour sont départagées par l'ordre des conflits puis l'ordre des victimes
> dans la bataille.
>
> **Kills et assistances** — Un kill est une élimination signée par le tueur désigné (ou le
> capturant du QG). Une assistance est accordée aux alliés du tueur présents dans la bataille.
> Sans tueur, personne n'est crédité. Les assists ne sont nommés qu'au podium.
>
> **Mort définitive ou défaite** — Un personnage principal tué est **définitivement éliminé du
> lore**. Un joueur dont le QG est conquis est **battu** : il perd la partie, mais son personnage
> peut revenir dans une édition future. Le podium affiche cette distinction.
>
> **La Brigade des Morts** — À chaque tour avec au moins une élimination, un brouillon de journal
> est ouvert. Le MJ le rédige et le publie ; il peut y glisser des indices, des rumeurs ou des
> révélations. Les autres joueurs ne voient que les éditions publiées. Les pertes, effectifs et
> positions ne sont pas publiés automatiquement : c'est le MJ qui décide ce qu'il raconte.
> La météo, le faux lien, le volume et la date sont décoratifs et figés à la création du
> brouillon.
>
> **Après la fin** — Actions désactivées ; consultation de la carte, des rapports, du podium et
> des journaux ; le MJ conserve ses outils.

### 10.3 Bloc podium

Reprendre le bloc `.podium` existant avec :

- 1ʳᵉ : « Le dernier survivant — ou le dernier éliminé, vainqueur posthume, éventuellement désigné
  par le MJ. »
- 2ᵉ : « L'avant-dernier éliminé. »
- 3ᵉ : « Le joueur éliminé juste avant. »

### 10.4 Section alliances

Ajouter dans le panneau Alliances : « Aucune victoire partagée. Si deux alliés sont les derniers
survivants, le pacte se brise automatiquement et ils doivent s'affronter. Aucune nouvelle
alliance n'est possible à deux joueurs. » Mettre à jour la ligne « Annonces » pour inclure la
rupture de la dernière alliance (`FINAL_DUEL`).

---

## 11. Tests

### 11.1 Backend — unitaires

| Test | Vérifie |
|---|---|
| `EliminationHistoryServiceTest` | Ordre de mort, index, première occurrence, cause QG prioritaire, snapshots, assists par camp. |
| `GameEndServiceTest` | 1/0 survivant, plateau 1, idempotence, `winnerAdjusted`, ajustement refusé si non posthume. |
| `FinalDuelServiceTest` | 2 alliés → rupture + `FINAL_DUEL` ; 2 non-alliés → rien ; 3 vivants → rien ; partie finie → rien ; refuse une nouvelle alliance à 2. |
| `GamePodiumServiceTest` | Places `N - i`, posthume, champion ajusté, causes `definitive`, fantômes, partie en cours. |
| `GazetteMastheadServiceTest` | Nom Brigade des Morts, volume/numéro, faux lien + slug, date Europe/Paris, météo dans les plages, tirage uniforme, persistance. |
| `GazetteDraftFactoryTest` | 1–3 slots de une, brèves au-delà, portraits pré-sélectionnés, notes admin-only, placeholders. |
| `GazetteServiceTest` | Création, enregistrement, validation, publication, correction publiée, suppression brouillon, numéro, 404 brouillon public. |
| `GameOverFilterTest` | 409 mutations, GET/admin ok, autre board ok. |

### 11.2 Backend — intégration `@EmbeddedPostgresTest`

- `GameEndApiTest` (6 joueurs si le seeding le permet, sinon 4 ; la logique est N-agnostique) :
  - 2 éliminations dans un tour → 2 événements, 1 brouillon à 2 unes ;
  - publication admin → visible côté joueur ; brouillon → 404 joueur ;
  - dernière élimination → board terminé, `currentTurn` inchangé, podium complet ;
  - 0 survivant → posthume ; ajustement du champion → podium réordonné + audit ;
  - fin de partie → mutation 409 ; GET carte 200 ; admin 200 ;
  - dernière alliance : 2 alliés → `FINAL_DUEL`, plus d'alliance active ;
  - isolation board A/B ;
  - correction d'une édition publiée ; suppression d'un brouillon.
- `TurnResolutionScenariosTest` : le tour complet existant passe ; `TurnJournalDto` enrichi.
- `DemoTurnsSeederTest` : la démo tourne ; une élimination crée un brouillon.

### 11.3 Frontend — specs

- `podium.component.spec.ts` : ordre, posthume, `championAdjusted`, badges « Mort définitive » /
  « Défaite — retour possible », palmarès.
- `admin-journal-editor.component.spec.ts` : ajout/suppression d'articles, validation avant
  publication, compteurs, preview mise à jour, notes admin.
- `journal-issue.component.spec.ts` : masthead, unes, brèves, faux lien en `span`, corps sans
  `innerHTML`, 404.
- `journal-list.component.spec.ts` : publiées seulement, tri, état vide.
- `player-status.service.spec.ts` : `gameFinished`, `locked`, champion.
- `navbar.component.spec.ts` : Journal toujours, Podium conditionnel, entrée admin.
- `turn-management` : carte « Brouillon de gazette à rédiger ».

### 11.4 Commandes

```bash
# nml-ms/
./mvnw clean test

# nml-ui/
npm test
npm run lint
npm run build
```

- `clean` obligatoire (Flyway).
- Un test d'intégration = `@EmbeddedPostgresTest`.
- `npm run build` = strictTemplates.

---

## 12. Découpage en lots

| Lot | Contenu | Livrable |
|---|---|---|
| 1 | Socle fin de partie : V40 board, `finishedAt`/`winnerPlayerId`/`winnerAdjusted`, `GameOverFilter`, statut session, `TurnService`/`startSession` refusent après la fin, UI lecture seule/bannière. | Un board marqué terminé bloque les mutations. |
| 2 | Historique/ordre/places/posthume, `GameEndService`, `FinalDuelService`, podium backend, ajustement du champion, `AnnouncementType.FINAL_DUEL`, page Règles (retrait objectif secret + fin de partie + dernière alliance + causes). | Éliminer les joueurs produit un podium, un champion ajustable et une dernière alliance brisée. |
| 3 | Podium UI : panneau Accueil, page `/podium`, badges de cause, bloc admin de désignation, nav. | Le podium remplace les classements à la fin. |
| 4 | Gazette backend : entité/repo, masthead, draft factory, notes, save/publish/delete, endpoints public/admin, audit, tests. | Un tour éliminatoire crée un brouillon ; le MJ peut le publier via l'API. |
| 5 | Gazette UI : template partagé, page publique, archive, éditeur admin (form + preview + faits + modèle), nav, turn-management. | Le MJ rédige et publie ; les joueurs lisent. |
| 6 | Finition : README/AGENTS, page Règles (gazette MJ), scénario dev, tests de bout en bout, vérification manuelle. | CI au vert et démonstration. |

La page Règles est mise à jour dans les lots 2 et 6, jamais laissée fausse.

---

## 13. Pièges et invariants

1. **Un seul point d'écriture** de la fin : `finalizeTurn`.
2. **Ordre de mort indépendant des clics** : `conflictId`, pas `s.resolvedConflicts`.
3. **Snapshot avant nettoyage** : nom, race, personnage, cause, tueur, assists avant
   `PlayerEliminationService.apply`.
4. **Pas d'incrément après la fin** : `currentTurn` reste le tour final.
5. **Dernière alliance** : rompre avant la création du brouillon pour que les notes la
   contiennent ; refuser une nouvelle alliance à deux joueurs ; le CHECK `announcements` doit
   accepter `FINAL_DUEL`.
6. **Champion ajusté** : seulement en posthume ; `winnerAdjusted` recalculé par rapport au dernier
   mort ; audit ; le podium retire le champion de la liste des morts.
7. **Cause lore** : `definitive` = `MAIN_CHARACTER_KILLED` ; ne pas inventer un comeback mécanique.
8. **Brouillon invisible** : tous les endpoints publics filtrent `PUBLISHED` ; test 404.
9. **Notes admin-only** : `editorNotes` jamais dans le DTO public ; test de sérialisation.
10. **Aucun article généré** : les placeholders sont des suggestions ; le serveur ne fabrique
    pas de titre/citation/corps.
11. **Contenu du MJ = sa responsabilité** : le système ne filtre pas les révélations ; il protège
    seulement les notes d'aide.
12. **Masthead figé** : météo/date/lien/volume persistés à la création ; la correction ne les
    régénère pas.
13. **Publication** : au moins une une complète ; max 3 unes + 4 brèves ; longueurs ; 400 sinon.
14. **Correction publiée** : `publishedAt` inchangé, `updatedAt` mis à jour, audit ; pas de
    republication.
15. **Suppression** : brouillon seulement ; publiée → 409.
16. **Faux lien** : texte, pas de `href`, pas de `innerHTML`.
17. **Météo décorative** : aucune formule de combat.
18. **`readonly-mode`** : exempte `Imprimer`/`Réessayer` ; sinon la page journal est inutilisable
    en fin de partie.
19. **Multi-board** : toutes les requêtes filtrent `board_id`.
20. **Dev** : les scénarios d'arène doivent supprimer leurs événements/éditions ; sinon le podium
    et l'archive de démo contiennent des fantômes.
21. **BETA 6** : aucun hardcode `6` dans les services ; le preset 6 joueurs est un choix de test.
22. **Objectif secret** : ne pas le réintroduire dans les règles ; aucun code existant.

---

## 14. Hors périmètre

| Élément | Raison / voie future |
|---|---|
| Objectif secret / empereur | Retiré du périmètre et des règles. |
| Victoire d'alliance | Impossible par conception ; refusée et cassée. |
| Génération automatique d'articles | Le MJ écrit ; pas d'IA. |
| Upload d'images / médiathèque | BETA : portraits existants ou aucun. |
| Mécanique de comeback inter-éditions | Lore seulement ; chantier futur. |
| Clôture manuelle de partie | Hors périmètre ; seule la désignation du champion posthume existe. |
| Effets de la météo | Décor. |
| Publication automatique | Le MJ publie. |
| PDF natif | Impression navigateur. |
| Multilingue | Français. |
| Notification push | Nav + archive + bannière. |

---

## 15. Documentation et checklist

### 15.1 `AGENTS.md`

Ajouter :

> - **Fin de partie** : `GameEndService.finishIfOver` (depuis `finalizeTurn`) écrit
>   `finishedAt`/`winnerPlayerId`/`winnerAdjusted` ; `currentTurn` reste le tour final.
>   `GameOverFilter` refuse les mutations hors `/api/admin/**`. `GazetteIssue` (journal du MJ)
>   est distincte de `TurnJournal` (archive technique). Un brouillon (`DRAFT`) n'est jamais
>   public ; `editor_notes` est admin-only. Aucune victoire d'alliance : la dernière alliance se
>   brise et aucune nouvelle n'est acceptée à deux joueurs. Aucun objectif secret.

### 15.2 `README.md` (anglais)

- Endpoints publics/admin, migration V40.
- Format BETA 6 joueurs, pas d'objectif secret, pas de victoire d'alliance.
- Journal rédigé par le MJ : brouillon + publication.
- `domain/model/press`, `pages/podium`, `pages/journal`, `pages/admin-journal`.
- Tests.

### 15.3 Checklist

- [ ] V40 : board, événements, gazette, CHECK `FINAL_DUEL` ; `clean test` / validate prod.
- [ ] Fin de partie + `GameOverFilter` + statut + UI bannière.
- [ ] Ordre de mort, places, posthume, `winnerAdjusted`, audit.
- [ ] Dernière alliance + refus d'alliance à 2 + libellé UI.
- [ ] Podium (causes lore, palmarès, panneau Accueil, page, désignation MJ).
- [ ] Gazettes : brouillon auto, météo/lien/date persistés, notes admin-only, liste/édition.
- [ ] Éditeur : validation, preview, publication, correction, suppression brouillon, audit.
- [ ] Page publique : publiées seulement, template, impression, a11y.
- [ ] Page Règles : `#empereur` supprimé, BETA, dernière alliance, causes, gazette MJ.
- [ ] `AGENTS.md` / `README.md`.
- [ ] `./mvnw clean test`, `npm test`, `npm run lint`, `npm run build`.
- [ ] Manuel : 6 joueurs, double élimination, brouillon, publication, lecture, podium posthume,
      ajustement MJ, dernière alliance, 409.

---

## Annexe A — Modèle d'article fourni au MJ

Panneau « Comment écrire une une » de l'éditeur (statique, non généré).

```text
┌──────────────────────────────────────────────────────────────┐
│ [portrait]  LES MERCENAIRES CROISENT LE FER                 │
│             « Il est tombé les armes à la main…              │
│               mais avec quelles armes ? »                   │
│             Par Vexa-9, correspondante de guerre             │
│                                                              │
│             Premier paragraphe : les faits.                  │
│             Deuxième paragraphe : les conséquences.          │
│             Troisième paragraphe : la citation / l'ambiance. │
└──────────────────────────────────────────────────────────────┘
```

Consignes affichées :

- Titre court, concret, en casse naturelle (l'éditeur l'affiche en majuscules).
- Une citation entre guillemets, pas un résumé.
- Une image : portrait d'un joueur, légende courte.
- 2 à 4 paragraphes ; une idée par paragraphe.
- Signer : nom et rôle.
- Les notes « Faits du tour » sont là pour aider ; ne pas tout recopier.
- Peut révéler une information privée (unité, flotte, perte) : c'est le choix éditorial du MJ.

## Annexe B — Contrats API

Voir §8.3. Les corps d'enregistrement/publication :

```json
{
  "frontPage": [
    { "kind": "ELIMINATION", "section": "LA UNE", "headline": "…", "quote": "…",
      "body": "…\n\n…", "author": "Vexa-9", "photoPlayerName": "lurio",
      "photoCaption": "Portrait d'archives" }
  ],
  "briefs": [
    { "kind": "CAPTURE", "section": "LES AUTRES FAITS", "headline": "…", "quote": "",
      "body": "…", "author": "Iolande Krast", "photoPlayerName": null,
      "photoCaption": null }
  ]
}
```

Le masthead n'est pas dans le corps : il est généré et figé.

## Annexe C — Points tranchés

| Point | Décision |
|---|---|
| Champion posthume | Oui, dernier éliminé ; ajustable par le MJ. |
| Gazette | Rédigée et publiée par le MJ ; template + faits fournis. |
| Nom | Brigade des Morts. |
| Objectif secret | Retiré. |
| Victoire d'alliance | Impossible ; dernière alliance brisée. |
| 6 joueurs | Cible BETA, code N-agnostique. |
| Cause au podium | Oui (mort définitive / défaite réversible). |
| Cause dans la gazette | Non automatique ; le MJ l'écrit s'il veut. |
| Brouillon non publié | Invisible joueur. |
| Correction d'une édition publiée | Autorisée, auditée, `publishedAt` inchangé. |

*Fin du plan v2. Prochain jalon : lot 1 (socle de fin de partie), indépendant de l'éditeur de
journal.*
