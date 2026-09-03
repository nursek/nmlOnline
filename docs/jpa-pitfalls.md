# JPA / Hibernate pitfalls

Leçons retenues des pièges Hibernate rencontrés en production. Source : HTTP 500 sur
`POST /api/admin/turn/resolve/next-hop` (fix : `V5__unit_equipments_fk_cascade.sql`,
changement de mapping sur `Unit.unitEquipments`).

## 1. Le piège `orphanRemoval=true` + `cascade=ALL` + `@JoinColumn(nullable=false)` sur un sous-enfant

### Symptôme
 
`DataIntegrityViolationException` au **commit** de la transaction (pas avant)
→ HTTP 500 remonté par `GlobalExceptionHandler.handleGeneric` (catch-all).
La stack contient `JpaTransactionManager.doCommit` puis `ConstraintViolationException`
sur une colonne `NOT NULL` qu'Hibernate essaie d'écrire à `NULL`.

### Mécanisme

Quand on retire un enfant d'une collection `@OneToMany(mappedBy=…, cascade=ALL,
orphanRemoval=true)`, Hibernate émet une **release-FK en cascade avant DELETE** :

```sql
UPDATE <enfant> SET <fk>=NULL WHERE id=?
-- puis
DELETE FROM <enfant> WHERE id=?
```

Si cet enfant porte lui-même une sous-collection `@OneToMany(orphanRemoval=true)` dont
la feuille a `@JoinColumn(nullable=false)`, la release-FK cascade descend et tente
`UPDATE <feuille> SET <fk_parent>=NULL` qui heurte la contrainte NOT NULL.

Cas concret (résolu en Phase 2) :
- `Sector.army` `@OneToMany(mappedBy="sector", cascade=ALL, orphanRemoval=true)`
- `Unit.unitEquipments` `@OneToMany(mappedBy="unit", cascade=ALL, orphanRemoval=true)`
- `UnitEquipment.unit` `@ManyToOne @JoinColumn(name="unit_id", nullable=false)`

→ Quand `MovementService.advanceOrder` retirait une unité équipée de `fromSector.army`,
le commit émettait `UPDATE unit_equipments SET unit_id=NULL WHERE id=?` avant le DELETE,
qui heurtait `unit_id NOT NULL` → 500.

### Pattern sûr pour une `@OneToMany` bidirectionnelle avec enfant FK NOT NULL

```java
// 1. Mapping : cascade=ALL SANS orphanRemoval + @OnDelete(CASCADE)
@OneToMany(mappedBy = "unit", cascade = CascadeType.ALL)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
private List<UnitEquipment> unitEquipments = new ArrayList<>();

// 2. FK côté enfant : nullable=false inchangé
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "unit_id", nullable = false)
private Unit unit;

// 3. Flyway : FK ON DELETE CASCADE en DB (filet de sécurité)
ALTER TABLE unit_equipments
    DROP CONSTRAINT IF EXISTS fk8nq4cv16iq9am2hcwp76dpq9n;
ALTER TABLE unit_equipments
    ADD CONSTRAINT fk8nq4cv16iq9am2hcwp76dpq9n
    FOREIGN KEY (unit_id) REFERENCES combat_entities(id) ON DELETE CASCADE;
```

Quand un Unit est DELETEd (pertes de combat via `Sector.army.orphanRemoval`, suppression
d'un joueur via `SectorService.removePlayerFromSectors.clear()`), Hibernate cascade
REMOVE → `DELETE FROM unit_equipments WHERE id=?` direct (pas d'UPDATE NULL).

Pour les **retraits ciblés d'un enfant sans supprimer le parent** (ex. `UnitService.removeEquipment`,
déséquipement d'une unité non détruite), appeler explicitement `em.remove(ue)` — la
mutation collection seule n'émet plus de DELETE auto maintenant qu'`orphanRemoval` est retiré.

## 2. Pourquoi les tests `@Transactional` masquent ce piège

`@Transactional` sur une méthode de test fait que Spring **rollback** la transaction à la
fin de la méthode. L'`DataIntegrityViolationException` a lieu uniquement **au commit** —
donc le rollback la masque : le test passe quand le code casse en prod.

Pour reproduire :

```java
@EmbeddedPostgresTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RegressionTest {
    @Autowired private PlatformTransactionManager txManager;

    @Test void reproduces_commit_time_exception() {
        // Setup dans une tx qui COMMITTE (TransactionTemplate hors d'une tx existante).
        new TransactionTemplate(txManager).execute(status -> {
            // setup
            return null;
        });
        // L'appel au service @Transactional COMMITTE pour de bon (pas rollbacké par le test).
        service.someMethod();
    }
}
```

`@DirtiesContext(BEFORE_EACH_TEST_METHOD)` : recrée le contexte Spring avant chaque test,
pour que les commits ne polluent pas les autres tests. La base PostgreSQL embarquée étant
partagée par toute la suite, `PlayerStartupImporter` doit rester idempotent entre ces
recréations : un joueur déjà présent est **remplacé**, sinon l'armée est dupliquée (et les
doublons n'ont plus d'équipement, le stock du joueur ayant déjà été consommé).

## 3. OSIV=false partout

`application-test.properties` et `application-prod.properties` ont toutes deux
`spring.jpa.open-in-view=false`. Un 500 qui ne se reproduit pas en test mais apparaît en
prod ne peut donc plus venir d'un écart OSIV : suspecter les **données** de prod (états
que les fixtures de démo ne produisent pas) plutôt que le mode de session.

## 4. Checklist avant d'ajouter un `@OneToMany(mappedBy=…)`

1. Quel est le `nullable` du `@JoinColumn` côté owning (many) ?
2. Un **ancêtre** dans la chaîne de cascade porte-t-il `orphanRemoval=true` ?
3. L'enfant porte-t-il lui-même une `@OneToMany(orphanRemoval=true)` dont la feuille a
   `@JoinColumn(nullable=false)` ?
4. Est-ce qu'un path de prod appellera `.remove(child)` / `.clear()` pour **déplacer**
   (pas supprimer) un enfant ?

Si 1 = NOT NULL et 2 = OUI → préférer le pattern "DB FK `ON DELETE CASCADE` +
`@OnDelete(CASCADE)` + `cascade=ALL` sans `orphanRemoval`".

## 5. Checklist avant de RETIRER un élément d'une `@OneToMany(orphanRemoval=true)`

1. L'enfant porte-t-il une sous-collection `@OneToMany(orphanRemoval=true)` dont la
   feuille a `@JoinColumn(nullable=false)` ?
2. Si OUI → appeler `em.remove(child)` explicitement, OU refactorer le mapping vers le
   pattern sûr ci-dessus.

## 6. Audit à mener — mappings `orphanRemoval=true` non couverts par le fix Phase 2

Cartographie au moment du fix. À investiguer avant d'invoquer un path qui retire
explicitement un enfant de l'une de ces collections.

| Mapping | orphanRemoval | Sous-enfant direct / feuille | Risque 500 au retrait ? |
|---------|---------------|-----------------------------|-------------------------|
| `Player.character` (Player.java:59) | OUI | `GameCharacter` (pas de sous-collection) | Non — DELETE propre |
| `Player.buildings` (Player.java:64) | OUI | `Building.player_id` nullable=false **mais** `insertable=false, updatable=false` (Building.java:58) → protégé contre release-FK NULL | À surveiller si on retire un Building d'un Player pour le **déplacer** (transfert à un autre joueur). Les sous-collections `Bank.storedResources` / `WeaponCache.storedEquipments` sont en cascade REMOVE → vérifier leurs `@JoinColumn` (voir lignes ci-dessous) |
| `Player.equipments` (Player.java:69) | OUI | `EquipmentStack.player_id` nullable=false (EquipmentStack.java:30) | **PIÈGE POTENTIEL** si un path retire un `EquipmentStack` de `Player.equipments` pour le transférer (ex. entre joueurs via capture de `WeaponCache`). Vérifier `BuildingService` / `AdminService` |
| `Player.resources` (Player.java:74) | OUI | `PlayerResource.player_id` nullable=false (PlayerResource.java:27) | **PIÈGE POTENTIEL** identique si un path retire un `PlayerResource` de `Player.resources` pour le transférer. Vérifier `ResourceService.transfer` et capture de `Bank` |
| `Board.sectorsList` (Board.java:70) | OUI | `Sector.board_id` (IdClass, implicite NOT NULL) → `Sector.army` (corrigé) → `Unit.unitEquipments` (corrigé) | Couvert par le fix Phase 2 (cascade DELETE propre). Déjà documenté dans AGENTS.md : `BoardService.saveBoard` non-destructive — ne jamais `clear()` |
| `Sector.army` (Sector.java:86) | ~~OUI~~ → **NON** (fix Phase 3) | `Unit` → `Unit.unitEquipments` (corrigé Phase 2) | **Fixé en Phase 3** : retrait d'`orphanRemoval` (mapping + `V6__sector_army_fk_cascade.sql` FK ON DELETE CASCADE). La variante Phase 2 « équipé LAZY + MOVED + pertes » était pinnée puis fixée via `em.remove` explicite dans `CombatService.simulateSectorBattle` + `SectorService.removePlayerFromSectors` (test `TurnResolutionOrchestratorLurioCegorachTest`). |
| `Bank.storedResources` (Bank.java:50) | OUI (unidirectional `@JoinColumn`) | `PlayerResource.bank_id` nullable=true par défaut (Bank.java:51 — pas de `nullable=false`) | Non — `bank_id` nullable=true, release-FK NULL OK |
| `WeaponCache.storedEquipments` (WeaponCache.java:44) | OUI (unidirectional `@JoinColumn`) | `EquipmentStack.weapon_cache_id` nullable=true par défaut (WeaponCache.java:45 — pas de `nullable=false`) | Non — `weapon_cache_id` nullable=true, release-FK NULL OK |
| `Unit.unitEquipments` | ~~OUI~~ → **NON** (fix Phase 2) | — | **Fixé** |

### Suspects prioritaires de l'audit

1. **`Player.equipments`** : si `AdminService.exportPlayer`/`importPlayer` ou la capture
   d'une `WeaponCache` (qui transfère des `EquipmentStack` au capteur) retire un stack
   de `Player.equipments` pour l'attacher à un autre joueur → `UPDATE equipment_stacks
   SET player_id=NULL` → heurte `player_id NOT NULL` → 500.
2. **`Player.resources`** : même scénario côté `Bank` capture (le `BuildingService`
   vampirise et transfère des `PlayerResource` au capteur).

### Test de régression à prévoir pour l'audit

Pour chaque suspect, un test `@DirtiesContext(BEFORE_EACH_TEST_METHOD)` +
`TransactionTemplate` qui :
1. Crée un joueur A avec une `EquipmentStack` / `PlayerResource`.
2. Invoque le path de transfert (capture, admin import, etc.).
3. Assert : pas de `DataIntegrityViolationException` au commit + la row est bien
   attachée au nouveau propriétaire côté DB.

## 7. Référence au fix appliqué

### Phase 2 — `Unit.unitEquipments` (casse équipé DÉPLACÉ non-détruit)

- Migration : `nml-ms/src/main/resources/db/migration/V5__unit_equipments_fk_cascade.sql`
- Mapping : `nml-ms/src/main/java/com/mg/nmlonline/domain/model/unit/Unit.java:64-79`
- Service : `nml-ms/src/main/java/com/mg/nmlonline/domain/service/UnitService.java`
  (injection `EntityManager`, `em.remove(ue)` explicite dans `removeEquipment`)
- Test de régression : `TurnResolutionOrchestratorTest#resolveBattle_perteUniteEquipee_effaceRowsUnitEquipmentsViaFkCascade`

### Phase 3 — `Sector.army` (variante non couverte : équipé LAZY + MOVED + pertes)

- Migration : `nml-ms/src/main/resources/db/migration/V6__sector_army_fk_cascade.sql`
  (FK `combat_entities.board_id, sector_number → sectors.board_id, number` ON DELETE CASCADE)
- Mapping : `nml-ms/src/main/java/com/mg/nmlonline/domain/model/sector/Sector.java:85-94`
  (retrait d'`orphanRemoval=true` + `@OnDelete(CASCADE)`)
- Services :
  - `CombatService.simulateSectorBattle` : `em.remove(unit)` explicite pour chaque
    pertes (avant `sector.getUnits().remove(unit)`)
  - `SectorService.removePlayerFromSectors` : boucle `em.remove(unit)` autour de
    `sector.getArmy().clear()` pour préserver la DELETE des armées d'un joueur supprimé
  - `MovementService.advanceOrder` : inchangé — la FK du secteur est déjà pilotée
    côté owning par `entity.setSector(target)` (UPDATE combat_entities), pas par
    l'orphanRemoval
- Test de régression :
  - `TurnResolutionOrchestratorLurioCegorachTest#lurioVsCegorach_resolveBattle_surAttaquantEquipeDeplace_detruitProprementPhase3`
    (l'attaquant Lurio VOYOU équipé HK-MP7 + Tenue ultra légère chargé LAZY de DB,
    MOVED 2 hops [41 → 13 → 32], détruit au combat — cascade propre, no more 500)