# Plan d'implémentation — règles sniper (v4)

> **v4** : la chance de toucher un **véhicule aérien** dépend du **niveau (exp) du tireur**,
> comme la règle aérienne classique. Le point restant de la v3 est donc tranché.
> **v3** intègre les arbitrages finaux : réduction d'arme **plate**, valeurs des non-unités
> validées, chaîne de salve confirmée, contre-tir limité au groupe tireur, élimination par le
> personnage principal validée, action offensive nommée **Tir de précision**.
> **v2** : le tir se résout en **jet de HIT puis jet d'Esquive** (aucun malus de dégâts), la
> salve rejoint la **phase PdF du combat**, le sniper peut viser **tout type d'entité**, et un
> sniper **inactif** ciblé peut **contre-tirer**.
> Les données d'équipement (V32 : `aim_malus_reduction`, `enemy_aim_malus`) restent inchangées.

## 1. Acquis

- classe `SNIPER` (`UnitClass`), règles de déplacement/tir/surveillance dans la page Règles ;
- modificateurs de visée sur trois équipements (Particle Shredder −10, Enmitic Exterminator −15,
  Gilet de camouflage optique +15), affichés boutique/inventaire/Règles ;
- infrastructure de tour : `MovementOrder` / `MovementService`, session de résolution
  (`TurnResolutionOrchestrator`), combats (`CombatService` → `Battle`).

Manque : ordres sniper, jet de HIT, jet d'Esquive sniper, intégration PdF multi-secteurs,
contre-tir, rapports de tir à distance, expérience associée.

## 2. Décisions intégrées

| # | Décision |
|---|---|
| D1 | **Deux jets** : jet de HIT sniper, puis, s'il touche, jet d'Esquive de la cible. Pas de malus de dégâts : la salve passe entière ou est perdue. |
| D2 | **Malus de HIT = valeur de la classe visée** (LARBIN 20, VOYOU 35, MALFRAT 50, BRUTE 65), **réduit à plat** par l'arme du tireur (soustraction de points). |
| D3 | **Portée 1** : uniquement un secteur adjacent. |
| D4 | **Intégration à `Battle`** : la salve est un renfort de la phase PdF, les deux se produisent simultanément. Les kills à distance ne capturent pas ; le rapport ne nomme pas les unités qui ont tiré (« X PdF, à distance »). |
| D5 | **Tout type d'entité ciblable** : unités, véhicules, bâtiments, QG, personnages. Un QG peut être détruit, jamais capturé par un tir ; un personnage principal tué élimine son joueur (règle existante). |
| D6 | **Esquive = valeur de classe + `enemy_aim_malus`** (points) : LARBIN + gilet = 35 % d'Esquive. |
| D7 | **Cumul par maximum** de catégorie (meilleure arme compatible / meilleur défensif compatible). |
| D8 | **Surveillance** : le sniper rejoint la phase PdF de la bataille du secteur surveillé, côté défenseur, sans être ciblable ni compter dans les pertes/vainqueur. |
| D9 | **Expérience** : +1 Exp aux unités qui esquivent et aux snipers ciblés à distance survivants ; le tireur ne gagne rien ; plafond 1 Exp/unité/tour. |
| D10 | **N'importe quel secteur voisin** est ciblable : possédé/alllié → Surveillance ; neutre/ennemi → **Tir de précision**. |
| D11 | **Contre-tir** : un sniper inactif (aucun ordre sniper, aucun combat au sol dans son secteur, pas déjà contré) ciblé par une salve riposte vers le groupe de snipers tireur, simultanément. |
| D12 | Valeurs des cibles non-unités validées : véhicule terrestre 25, bâtiment 0, personnage 35 ; véhicule aérien = chance de toucher classique selon le **niveau (exp) du tireur** (voir §3.1). |

## 3. Mécanique de tir

### 3.1 Jet de HIT

```
malusHit = clamp(valeurCible − réductionArme, 0, 100)     // réduction plate, en points
jet 1..100 ; jet <= malusHit  →  la salve rate, aucune Esquive jouée
```

`réductionArme` = valeur `aim_malus_reduction` du meilleur équipement à feu compatible (D7).
Exemple : LARBIN 20 − Particle Shredder 10 = **10 %** de rater ; LARBIN 20 − Enmitic
Exterminator 15 = **5 %**.

| Cible | Valeur de HIT |
|---|---|
| Unité LARBIN | 20 |
| Unité VOYOU | 35 |
| Unité MALFRAT | 50 |
| Unité BRUTE | 65 |
| Véhicule terrestre | 25 |
| Véhicule aérien | chance de toucher selon le niveau (exp) du tireur (voir ci-dessous) |
| Bâtiment (dont QG) | 0 |
| Personnage | 35 |

Un seul jet de HIT par salve (par unité sniper), contre la première cible visée.

**Véhicules aériens** : pas de malus fixe, la chance de toucher dépend du **niveau (exp) du
tireur**, c'est-à-dire de son type d'unité (`UnitType.getTypeByExperience`), comme la règle de
ciblage aérien classique (`CombatEntity.getAerialHitChance`, page Règles / Véhicules) :

| Niveau du tireur | Exp | Chance de toucher |
|---|---|---|
| LARBIN | 0–1 | 10 % |
| VOYOU | 2–4 | 25 % |
| MALFRAT | 5–7 | 40 % |
| BRUTE | 8+ | 65 % |

Une arme anti-aérienne porterait la chance à 75 % (aucune arme sniper compatible ne l'a
aujourd'hui). Le jet est **inversé** : pour l'aérien on touche si `jet ≤ chanceDeToucher`,
alors que le malus se lit `jet ≤ malus` (rate). La réduction d'arme du fusil sniper ne
s'applique pas à ce jet : c'est la règle classique reprise telle quelle. Le type du véhicule
aérien visé (hélicoptère / avion de transport) n'entre pas dans le calcul.

### 3.2 Jet d'Esquive

```
chanceEsquive = clamp(valeurClasseCible + enemyAimMalusCible, 0, 100)   // en points
jet 1..100 ; jet <= chanceEsquive  →  la salve est perdue
```

- `enemyAimMalusCible` = valeur `enemy_aim_malus` du meilleur défensif compatible de la cible
  (le Gilet de camouflage optique : +15).
- Exemple : LARBIN 20 + gilet 15 = **35 %** d'Esquive.
- La stat `evasion` de l'unité n'est **pas** utilisée pour les tirs sniper : elle reste la
  défense des combats classiques (le gilet garde son +10 % Esquive pour ces combats).
- Seules les unités ont une classe ; véhicules, bâtiments et personnages ont une Esquive 0
  (les véhicules n'ont pas d'esquive, les bâtiments sont fixes).

### 3.3 Dégâts

- Une salve qui touche applique **toute la PdF** de l'unité sniper, sans malus.
- Armure puis défense, surplus enchaîné sur la cible suivante (même logique que
  `Battle.performStrike`) ; une Esquive arrête la salve (comportement actuel du moteur).
- Destruction : voir §8 (pertes, blessures, équipements, véhicules, bâtiments, QG).

### 3.4 Exemples de chances de réussite

Sans équipement (HIT × Esquive) :

| Cible | Chance de toucher une salve |
|---|---|
| LARBIN | 0,80 × 0,80 = **64 %** |
| VOYOU | 0,65 × 0,65 = **42 %** |
| MALFRAT | 0,50 × 0,50 = **25 %** |
| BRUTE | 0,35 × 0,35 = **12 %** |

Avec Enmitic Exterminator (−15 HIT) : LARBIN 76 %, VOYOU 52 %, MALFRAT 32,5 %, BRUTE 17,5 %.
Avec Gilet de camouflage optique chez la cible (+15 Esquive) : LARBIN 52 %, VOYOU 32,5 %,
MALFRAT 17,5 %, BRUTE 7 %.
Le double jet rend les hauts rangs très difficiles à toucher ; c'est cohérent avec le ciblage
« du plus faible au plus puissant » (les snipers chassent d'abord les rangs bas). À surveiller
en équilibrage : le gilet est très fort en défense.

## 4. Ciblage

- Cibles : toutes les entités **hostiles** du secteur visé (unités, véhicules, bâtiments).
- Ordre de ciblage : unités d'abord, de la moins expérimentée à la plus puissante
  (`Battle.EXPOSURE_ORDER`), puis véhicules, puis bâtiments (Banque, Cache, QG), puis
  personnages. Le QG devient donc attaquable une fois les autres cibles traitées (D5).
- Le surplus de PdF enchaîne sur les cibles suivantes ; une Esquive perd la salve.
- Seules les unités de classe `SNIPER` reçoivent un ordre sniper (pas les personnages).

## 5. Contre-tir (D11)

Déclencheur : une unité sniper est **prise pour cible** par au moins une salve ennemie
(incluse dans la chaîne de ciblage, qu'elle soit touchée ou qu'elle esquive).

Conditions cumulatives pour riposter :

1. l'unité est de classe `SNIPER` ;
2. elle n'a **aucun ordre sniper** ce tour (elle n'a pas tiré à distance) ;
3. son secteur n'est pas engagé dans un **combat au sol** ce tour (elle ne se défend pas) ;
4. elle n'a pas déjà contre-tiré ce tour.

Résolution :

- le contre-tir vise **uniquement les unités qui ont effectivement tiré** (les `unit_id` des
  ordres sniper à l'origine des salves), pas les autres snipers présents dans le secteur
  d'origine ni les autres unités ; si plusieurs secteurs d'origine, le premier tir (ordre
  d'id) ;
- mêmes jets que §3 : HIT puis Esquive, PdF complète, pas de malus de dégâts ;
- **simultanéité** : les salves entrantes et les contre-tirs sont résolus sur un instantané
  des combattants ; un sniper détruit par la salve riposte quand même (comme les phases
  d'impasse) ;
- un seul contre-tir par unité et par tour ; pas de chaîne : les tireurs ont un ordre, donc ils
  ne ripostent pas au contre-tir.

## 6. Modèle de données

### 6.1 Ordres sniper

Migration `V33__sniper_orders.sql` :

```sql
CREATE SEQUENCE public.sniper_orders_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE public.sniper_orders (
    id bigint NOT NULL DEFAULT nextval('public.sniper_orders_id_seq'),
    player_id bigint NOT NULL,
    turn integer NOT NULL,
    unit_id bigint NOT NULL,
    from_sector integer NOT NULL,
    target_sector integer NOT NULL,
    status character varying(20) NOT NULL,
    status_message character varying(500),
    submitted_at timestamp with time zone NOT NULL,
    CONSTRAINT sniper_orders_pkey PRIMARY KEY (id),
    CONSTRAINT sniper_orders_status_check
        CHECK (status IN ('PENDING', 'RESOLVED', 'CANCELLED', 'BLOCKED'))
);

ALTER SEQUENCE public.sniper_orders_id_seq OWNED BY public.sniper_orders.id;

ALTER TABLE public.sniper_orders
    ADD CONSTRAINT sniper_orders_player_fk
    FOREIGN KEY (player_id) REFERENCES public.players(id) ON DELETE CASCADE;

ALTER TABLE public.sniper_orders
    ADD CONSTRAINT sniper_orders_unit_fk
    FOREIGN KEY (unit_id) REFERENCES public.combat_entities(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX sniper_orders_turn_unit_uk
    ON public.sniper_orders (turn, unit_id) WHERE status = 'PENDING';
```

- un ordre = `target_sector` (adjacent) ; pas de colonne `mode` : le rôle **Tir de précision**
  ou **Surveillance** est dérivé de la propriété du secteur cible au moment de la résolution
  (secteur possédé/alllié → Surveillance, sinon Tir de précision). Le DTO expose un libellé
  calculé pour l'UI, mais aucune décision n'est figée sur une propriété qui peut changer dans
  le tour ;
- statuts : `PENDING` | `RESOLVED` | `CANCELLED` | `BLOCKED` ; index unique partiel pour
  autoriser un nouvel ordre après annulation (même sémantique que les ordres de déplacement) ;
- entités : `SniperOrder` + `SniperOrderRepository` dans `domain/model/sniper/`.

### 6.2 Domaine

- `Unit` : `isSniper()` ; `getAimMalusReduction()` (max armes à feu compatibles) ;
  `getEnemyAimMalus()` (max défensifs compatibles).
- `SniperRules` (`domain/service/sniper/`) : constantes et fonctions
  `hitMalus(CombatEntity)`, `dodgeChance(CombatEntity)`, `isEligibleCounterFire(...)`,
  `targetPriority(...)` — un seul endroit pour les tables et les plafonds.
- `Board` : `hopDistance(from, to)` si la portée s'étend plus tard (pas nécessaire en v2).
- `BattleReport` : nouvelle colonne `kind` (`BATTLE` | `RANGED`) et méthode
  `saveRangedReport(...)` dans `BattleReportService`, pour distinguer un tir à distance d'un
  combat au sol dans la liste des rapports.

### 6.3 API

- `POST /api/units/{unitId}/sniper-orders` — body `{ targetSector }` ;
- `DELETE /api/units/sniper-orders/{id}` — annulation tant que le tour n'est pas résolu ;
- `GET /api/units/sniper-orders/current` — intégré à l'écran Ordres.

Validations (ownership via `request.getAttribute("userId")`, jamais le body) :

1. unité possédée, sur le plateau, vivante, `isSniper()` ;
2. unité pas à bord d'un véhicule ;
3. aucun ordre de déplacement actif pour cette unité (« un ordre par entité ») ;
4. aucun autre ordre sniper `PENDING` pour cette unité ce tour ;
5. secteur cible **adjacent** (`Sector.isNeighbor`) ; aucune autre restriction de propriété
   (D10) : le rôle est dérivé.

## 7. Résolution et intégration au tour

### 7.1 Secteur avec combat au sol

Au moment de `TurnResolutionOrchestrator.resolveBattle(conflictId)` :

1. collecter les ordres `PENDING` dont `target_sector` = secteur du conflit ;
2. annuler ceux dont l'unité est morte, déplacée, ou dont le **secteur d'origine** est engagé
   dans un combat ce tour (conflits déjà résolus + conflits du hop courant) : le sniper se
   défend sur place (règle « Attaque sur place ») ;
3. déterminer le camp de chaque sniper : joueur/allié dans le camp attaquant → renfort
   attaquant ; joueur/allié dans le camp défenseur (ou propriétaire du secteur) → renfort
   défenseur ; sinon → tir indépendant contre les unités hostiles du secteur ;
4. passer ces renforts (`unit`, joueur, camp, secteur d'origine) à
   `CombatService.simulateSectorBattle` → `Battle` ;
5. dans `Battle`, pendant la **phase PdF** du camp concerné, résoudre les salves sniper avec
   la mécanique §3 (HIT → Esquive → dégâts), après les frappes PdF normales du camp ;
6. les unités sniper **n'entrent pas** dans `attackerUnits` / `defenderUnits` : elles ne sont
   ni ciblables, ni comptées dans le vainqueur, ni dans les pertes, ni dans la capture ;
7. journal : acteur générique `Tir à distance` + PdF engagée, sans nom d'unité (« Tir à
   distance : 1 200 PdF — 3 cibles détruites, 2 esquives ») ;
8. contre-tirs (§5) résolus dans la même phase sur un instantané.

### 7.2 Secteur sans combat au sol

À `TurnResolutionOrchestrator.finalizeTurn`, après tous les hops et batailles :

1. les ordres sniper encore `PENDING` ciblent des secteurs sans combat ;
2. pour chaque secteur cible, résoudre une **bataille de tir à distance** : les unités du
   secteur sont la cible, aucune troupe au sol ne riposte (sauf contre-tir §5) ;
3. appliquer les pertes, produire un `BattleReport` de type `RANGED` (pas de vainqueur, pas de
   capture) ;
4. les snipers dont le secteur a été attaqué dans un hop antérieur sont annulés ; un hop
   **ultérieur** ne peut plus annuler un tir déjà résolu — limite connue de la résolution
   séquentielle par hops, à documenter dans la page Règles.

### 7.3 Annulations

- unité détruite, absente, déplacée : `CANCELLED` ;
- secteur d'origine engagé dans un combat (au moment du tir) : `CANCELLED` (« attaqué sur
  place ») ;
- secteur cible devenu allié pour un tir offensif : le rôle est réévalué en Surveillance au
  moment de la résolution (pas de tir ami) ;
- aucun hostile dans le secteur cible : `RESOLVED` sans effet.

## 8. Pertes, destructions, expérience, rapports

### 8.1 Pertes

- réutiliser le circuit de pertes actuel (`CombatService.removeCasualties` ou un helper
  partagé) : unités détruites retirées avec leurs équipements, personnages (personnage
  principal tué → élimination, règle existante), véhicules détruits (équipage débarqué),
  bâtiments marqués détruits, QG `Headquarters.destroy()` ;
- **QG détruit ≠ QG capturé** : `destroy()` n'élimine pas le joueur (l'élimination reste
  réservée à la capture, cf. `PlayerEliminationService`) ;
- un tir à distance ne capture jamais de secteur et ne change pas `Sector.ownerId` ;
- les survivants d'infanterie endommagés par un tir sniper suivent la règle de blessure
  existante (blessé en fin de tour), comme après un combat classique ;
- les contre-tirs touchent des unités d'un **autre secteur** : prévoir un helper de destruction
  par secteur pour retirer les pertes des deux côtés.

### 8.2 Expérience

- +1 Exp à chaque unité qui esquive une salve sniper (y compris les non-snipers) ;
- +1 Exp à chaque unité sniper **ciblée** à distance et survivante ;
- le tireur ne gagne rien pour son tir ;
- plafond **1 Exp par unité et par tour**, en agrégeant l'Exp déjà gagnée pendant les batailles
  (`ExperienceGain` de la session) : une unité qui a combattu au sol ne cumule pas ;
- les gains sont ajoutés au compte-rendu du tour et au `BattleReport`.

### 8.3 Rapports

- combat au sol : le rapport existant inclut les entrées de tir à distance (acteur générique) et
  les pertes correspondantes ;
- tir isolé : `BattleReport` `kind = RANGED`, participants = défenseur + tireur, payload avec
  les pertes et « X PdF, à distance », `winnerPlayerId = null`, `capturedBuildings = 0` ;
- le rapport **ne nomme pas les unités qui ont tiré** (demande D4) ; les cibles et les pertes
  sont nommées, la PdF est agrégée ;
- l'UI des rapports distingue « Combat » et « Tir à distance ».

## 9. Frontend

- fiche unité `SNIPER` : choix d'un **secteur voisin** ; libellé calculé — **Tir de précision**
  si le secteur est neutre/ennemi, **Surveillance** s'il est possédé par le joueur ou un allié ;
- écran Ordres : ordre sniper affiché dans le groupe de son secteur d'origine, annulable tant
  que le tour n'est pas résolu ;
- carte : surbrillance du secteur visé (même calque que les ordres de déplacement) ;
- rapports : nouvelle catégorie « Tir à distance » dans la liste et le dialogue de rapport ;
- page Règles : mise à jour dans le même lot que l'implémentation — séquence HIT/Esquive,
  ciblage de tous les types, contre-tir, rapports, libellés Tir de précision/Surveillance.

## 10. Tests

- `SniperRulesTest` : malus HIT (classe, arme, plancher 0), Esquive (classe + gilet, plafond
  100), valeurs des non-unités, chance aérienne selon l'exp du tireur, ciblage ;
- `SniperOrderServiceTest` : ownership, classe, adjacence, double ordre, ordre de déplacement,
  annulation, rôle dérivé selon la propriété ;
- `SniperResolutionTest` (`@EmbeddedPostgresTest`) :
  - salve qui rate (HIT), salve qui touche puis esquive, salve qui touche et blesse/détruit ;
  - soutien attaquant dans un combat au sol : la PdF sniper s'ajoute à la phase PdF et les
    pertes comptent, sans changer la capture ;
  - surveillance d'un secteur allié attaqué : renfort défenseur non ciblable ;
  - secteur sans combat : rapport `RANGED`, pas de capture, pas de vainqueur ;
  - contre-tir : sniper inactif ciblé qui riposte, simultanéité, conditions d'inéligibilité
    (ordre sniper, combat au sol, déjà contré), pas de chaîne ;
  - QG détruit par un tir : bâtiment détruit, joueur non éliminé ;
  - Exp : +1 esquive, +1 sniper ciblé, tireur à 0, plafond 1/tour ;
- test d'orchestration : tour complet avec déplacements, combat, salves sniper et contre-tir ;
- specs Angular : libellé Tir de précision/Surveillance, sélection des voisins, affichage des
  ordres.

## 11. Découpage

| Lot | Contenu | Livrable testable |
|---|---|---|
| 1 | Données d'équipement + page Règles | **fait** |
| 2 | Ordres sniper : table, entité, API, validations, UI | poser/annuler un ordre, sans effet en jeu |
| 3 | Mécanique HIT/Esquive + dégâts + intégration PdF des combats au sol + rapport | salves dans un combat, pertes, aucun impact capture |
| 4 | Tir hors combat (rapport `RANGED`) + contre-tir | tir isolé et riposte des snipers ciblés |
| 5 | Expérience, UI rapports, page Règles, équilibrage | fonctionnalité complète et documentée |

## 12. Pièges et invariants

- **Simultanéité** : snapshot des salves et des contre-tirs avant application des dégâts, sinon
  l'ordre de résolution avantage un camp.
- **Ne pas polluer le combat** : les snipers renforts ne sont pas des combattants du secteur
  pour `abortIfWiped`, le vainqueur, la capture et les rapports de pertes.
- **Pertes croisées** : un contre-tir détruit des unités dans un autre secteur ; passer par un
  helper de destruction commun (équipements, véhicules, personnages, bâtiments).
- **QG** : destruction uniquement, jamais de capture ni d'élimination par ce chemin.
- **Expérience** : ne pas réutiliser `awardExperience` tel quel (il récompense le tireur et
  compte 0,5 par esquive) ; passer par le calcul sniper et plafonner à 1/tour.
- **Locks** : résolution sous `TurnLock`, session en mémoire (JVM unique) ; ne pas ouvrir de
  transaction séparée ; les entités sniper doivent être chargées dans le même contexte.
- **Annulation « attaqué sur place »** : vérifier les conflits du hop courant et des hops
  précédents ; un hop ultérieur ne rétroagit pas (limite de la résolution séquentielle). Même
  limite pour l'éligibilité au contre-tir : un combat au sol déclenché après la salve ne
  l'annule pas rétroactivement.
- **Rapport anonyme** : ne jamais écrire le nom de l'unité sniper dans le journal, seulement
  « Tir à distance ».
- **Un ordre par entité** : symétrie mouvement ↔ sniper, y compris après annulation.

## 13. Règles validées

Toutes les règles sont tranchées : réduction d'arme plate (−10 points pour Particle Shredder,
−15 pour Enmitic Exterminator), véhicule terrestre 25, bâtiment 0, personnage 35, véhicule
aérien selon le niveau (exp) du tireur, chaîne du surplus, contre-tir limité au groupe tireur,
élimination par le personnage principal tué, nom **Tir de précision**.

Le plan est prêt pour l'implémentation (lot 2 : table `sniper_orders`, API, validations, UI).
