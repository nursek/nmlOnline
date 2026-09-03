package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Le ONE runnable check du plan combat v2 : phases dédiées (secondaires entre PDF/PDC, QG après ATK,
 * personnage en dernier), infanterie seule blessée, QG destructible (reconstruction 75k si non capturé),
 * personnage non soigné au combat mais régénéré +50 def en fin de tour, capture à la victoire
 * (QG marqué capturé même détruit). Déterministe : aucune évasion, budgets choisis pour épuiser exactement.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("CombatService — bataille avec bâtiments et personnages (v2)")
class CombatServiceBuildingsCharactersBattleTest {

    @Autowired
    private CombatService combatService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private TurnService turnService;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager txManager;

    private static final double CHAR_ATK = 30;
    private static final double CHAR_DEF = 30;

    private record World(Long attackerId, Long defenderId, int sectorNumber,
                         Long characterId, Long hqId, Long cacheId, Long bankId) {
    }

    private Sector pickEmptyNeutralSector(Board board) {
        return board.getAllSectors().stream()
                .filter(s -> s.isNeutral() && s.getArmySize() == 0
                        && s.getBuildings().isEmpty() && s.getCharacters().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aucun secteur neutre vide dans le board de démo"));
    }

    /**
     * Défenseur : 3 LARBINs + personnage 30/30 + QG + Cache + Banque sur un secteur neutre vide.
     */
    private World seedDefenderHolding() {
        return new TransactionTemplate(txManager).execute(status -> {
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            Sector sector = pickEmptyNeutralSector(board);

            Player attacker = new Player("AttaquantBtC");
            attacker.getStats().setMoney(1000.0);
            playerRepository.save(attacker);
            Player defender = new Player("DefenseurBtC");
            defender.getStats().setMoney(80000.0);
            playerRepository.save(defender);
            em.flush();

            GameCharacter character = new GameCharacter("HerosBtC", CHAR_ATK, 0, 0, CHAR_DEF, 0, 0);
            character.setPlayerId(defender.getId());
            character.setSector(sector);
            defender.setCharacter(character);

            Headquarters hq = new Headquarters(defender.getId());
            WeaponCache cache = new WeaponCache(defender.getId());
            Bank bank = new Bank(defender.getId());
            bank.setStoredMoney(5000.0);
            for (Building b : List.of(hq, cache, bank)) {
                b.setSector(sector);
                defender.getBuildings().add(b);
            }
            Equipment eq = new Equipment("FusilBtC", 100, 0, 0, 0, 0,
                    Set.of(UnitClass.TIREUR), EquipmentCategory.FIREARM);
            em.persist(eq);
            EquipmentStack stack = new EquipmentStack(eq);
            stack.setPlayer(defender);
            cache.getStoredEquipments().add(stack);

            for (int i = 0; i < 3; i++) {
                Unit larbin = new Unit(0.0, UnitClass.TIREUR);
                larbin.setPlayerId(defender.getId());
                sector.addUnit(larbin);
            }

            em.flush();
            return new World(attacker.getId(), defender.getId(), sector.getNumber(),
                    character.getId(), hq.getId(), cache.getId(), bank.getId());
        });
    }

    private CombatService.SectorBattleResult runBattle(World w) {
        return new TransactionTemplate(txManager).execute(status -> {
            Player attacker = playerRepository.findById(w.attackerId()).orElseThrow();
            Player defender = playerRepository.findById(w.defenderId()).orElseThrow();
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            return combatService.simulateSectorBattle(attacker, defender, board, w.sectorNumber());
        });
    }

    private void seedAttackerUnits(Long attackerId, int sectorNumber, double experience, int count) {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            Sector sector = board.getSector(sectorNumber);
            for (int i = 0; i < count; i++) {
                Unit unit = new Unit(experience, UnitClass.TIREUR);
                unit.setPlayerId(attackerId);
                sector.addUnit(unit);
            }
            em.flush();
        });
    }

    private Sector loadSector(int sectorNumber) {
        return boardRepository.findAll().stream().findFirst().orElseThrow().getSector(sectorNumber);
    }

    @Test
    @DisplayName("Victoire écrasante : unités→Banque→Cache→QG→personnage tombent, QG détruit ET capturé")
    void overwhelmingAttacker_killsEverything_destroyedHeadquartersStillCaptured() {
        World w = seedDefenderHolding();
        seedAttackerUnits(w.attackerId(), w.sectorNumber(), 8.0, 6); // 6 BRUTEs 100/100

        CombatService.SectorBattleResult r = runBattle(w);

        assertTrue(r.success());
        assertEquals(w.attackerId(), r.winner().getId());
        // Secondaries défenseurs (150) tuent B6 et entaillent B5 ; ATK (500) vide le camp défenseur.
        assertEquals(1, r.attackerCasualties().size());
        assertEquals(7, r.defenderCasualties().size(), "3 LARBINs + personnage + Banque + Cache + QG");
        assertEquals(1, r.attackerInjured().size(), "B5 (def 20 < 100) termine blessé");
        assertEquals(0, r.defenderInjured().size(), "Personnages et bâtiments ne sont jamais blessés");
        assertTrue(r.defenderCharacterLost());
        assertFalse(r.attackerCharacterLost());
        assertTrue(r.defenderHeadquartersCaptured(), "QG détruit mais capturé : flag MJ actif");
        assertEquals(1, r.capturedBuildings(), "Seul le QG est capturé (Cache/Banque détruits = gravats)");

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Player defender = playerRepository.findById(w.defenderId()).orElseThrow();
            assertNull(em.find(GameCharacter.class, w.characterId()), "La ligne du personnage est supprimée");
            assertNull(defender.getCharacter(), "Le joueur perd son personnage (fin de partie arbitrée par le MJ)");

            Headquarters hq = em.find(Headquarters.class, w.hqId());
            assertNotNull(hq, "Le QG détruit reste en BDD (jamais DELETE)");
            assertTrue(hq.isDestroyed(), "Le QG n'est plus indestructible : il tombe en bataille");
            assertFalse(hq.isOperational());
            assertEquals(0.0, hq.getAttack());
            assertEquals(0.0, hq.getDefense());
            assertTrue(hq.isCaptured(), "QG détruit + victoire attaquant ⇒ marqué capturé quand même");
            assertEquals(w.attackerId(), hq.getCapturedByPlayerId());
            assertFalse(hq.isArmyImmobilized(), "Capturé ⇒ pas d'immobilisation (défaite arbitrée par le MJ)");

            Building bank = em.find(Bank.class, w.bankId());
            assertTrue(bank.isDestroyed());
            assertFalse(bank.isCaptured(), "Un bâtiment détruit n'est pas capturé");
            assertEquals(0.0, bank.getDefense());

            Building cache = em.find(WeaponCache.class, w.cacheId());
            assertTrue(cache.isDestroyed());
            assertFalse(cache.isCaptured());

            Sector sector = loadSector(w.sectorNumber());
            assertEquals(3, sector.getBuildings().size(), "Les 3 bâtiments restent dans le secteur");
            assertTrue(sector.getCharacters().isEmpty());
            assertEquals(5, sector.getUnits().size(), "Les 5 BRUTEs survivants");
            assertEquals(w.attackerId(), sector.getUnits().getFirst().getPlayerId());
        });
    }

    @Test
    @DisplayName("Match nul : QG détruit NON capturé (reconstruction 75k), perso survit à def 10 puis tick +50")
    void drawLeavesDestroyedUncapturedHeadquarters_andCharacterRegeneratesAtTurnEnd() {
        World w = seedDefenderHolding();
        seedAttackerUnits(w.attackerId(), w.sectorNumber(), 5.0, 11); // 11 MALFRATs 50/50

        CombatService.SectorBattleResult r = runBattle(w);

        assertTrue(r.success());
        assertNull(r.winner(), "Le personnage survit : aucun vainqueur (le secteur tient)");
        // Secondaries (150) tuent M9-M11 ; ATK (400) tue jusqu'au QG mais n'atteint pas le personnage ;
        // phase personnage (30) détruit M8 (def 20) et entaille M7.
        assertEquals(4, r.attackerCasualties().size(), "M9, M10, M11 (secondaries) + M8 (phase personnage)");
        assertEquals(6, r.defenderCasualties().size(), "3 LARBINs + Banque + Cache + QG ; le personnage survit");
        assertEquals(1, r.attackerInjured().size(), "M7 (def 40 < 50) termine blessé");
        assertFalse(r.defenderCharacterLost());
        assertFalse(r.defenderHeadquartersCaptured());
        assertEquals(0, r.capturedBuildings(), "Pas de capture sans vainqueur");

        // Pas de reconstruction tant que le défenseur n'a pas payé : le QG détruit non capturé immobilise l'armée.
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Headquarters hq = em.find(Headquarters.class, w.hqId());
            assertTrue(hq.isDestroyed());
            assertFalse(hq.isCaptured());
            assertTrue(hq.isArmyImmobilized(), "Détruit et non capturé ⇒ armée immobilisée jusqu'à reconstruction");

            // Le combat ne soigne PAS la défense du personnage (régén fin de tour uniquement).
            GameCharacter character = em.find(GameCharacter.class, w.characterId());
            assertEquals(10.0, character.getDefense(), "def 30 - 20 (reliquat ATK) : conservée après bataille");
            assertEquals(CHAR_ATK, character.getAttack(), "L'offense est restaurée après bataille");

            Player defender = playerRepository.findById(w.defenderId()).orElseThrow();
            assertNotNull(defender.getCharacter());
            // Reconstruction 75 000 : le chemin existant redresse le QG.
            assertTrue(buildingService.reconstructHeadquartersSameLocation(defender.getId()));
        });

        // Fin de tour : tick +50 def plafonné à baseDefense.
        turnService.advanceTurn();

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            GameCharacter character = em.find(GameCharacter.class, w.characterId());
            assertEquals(CHAR_DEF, character.getDefense(), "min(10 + 50, 30) = 30 : régénérée en fin de tour");

            Headquarters hq = em.find(Headquarters.class, w.hqId());
            assertFalse(hq.isDestroyed(), "QG reconstruit (75 000)");
            assertTrue(hq.isOperational());
            assertEquals(100.0, hq.getAttack());
            assertEquals(200.0, hq.getDefense());
            assertFalse(hq.isArmyImmobilized());

            Player defender = playerRepository.findById(w.defenderId()).orElseThrow();
            assertEquals(5000.0, defender.getStats().getMoney(), "80 000 - 75 000 de reconstruction");
        });
    }

    @Test
    @DisplayName("Défense bâtiments seule : QG intact régénéré puis capturé, secondaires ripostent")
    void buildingsOnlyDefense_regeneratesHeadquartersAndCapturesItIntact() {
        World w = seedDefenderHolding();
        // Retirer unités et personnage : seuls QG + Cache + Banque tiennent le secteur.
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Player defender = playerRepository.findById(w.defenderId()).orElseThrow();
            Sector sector = loadSector(w.sectorNumber());
            for (Unit unit : List.copyOf(sector.getUnits())) {
                if (defender.getId().equals(unit.getPlayerId())) {
                    sector.getUnits().remove(unit);
                    em.remove(unit);
                }
            }
            GameCharacter character = em.find(GameCharacter.class, w.characterId());
            sector.getCharacters().remove(character);
            defender.setCharacter(null);
            em.flush();
        });
        seedAttackerUnits(w.attackerId(), w.sectorNumber(), 8.0, 4); // 4 BRUTEs

        CombatService.SectorBattleResult r = runBattle(w);

        assertTrue(r.success());
        assertEquals(w.attackerId(), r.winner().getId(), "Aucun combattant défenseur ⇒ victoire attaquant");
        // Secondaries (150) tuent B4 ; ATK (300) détruit Banque + Cache, entame le QG (def 50) ;
        // phase QG (100) détruit B3 et entaille B2.
        assertEquals(2, r.defenderCasualties().size(), "Banque + Cache détruits, le QG absorbe et survit");
        assertEquals(2, r.attackerCasualties().size(), "B4 (secondaries) + B3 (phase QG)");
        assertEquals(1, r.attackerInjured().size(), "B2 (def 50 < 100) blessé");
        assertTrue(r.defenderHeadquartersCaptured());
        assertEquals(1, r.capturedBuildings(), "QG intact capturé seul");
        assertFalse(r.defenderCharacterLost());

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Headquarters hq = em.find(Headquarters.class, w.hqId());
            assertFalse(hq.isDestroyed(), "Le QG a survécu (entamé à 50)");
            assertTrue(hq.isCaptured());
            assertEquals(w.attackerId(), hq.getCapturedByPlayerId());
            assertEquals(200.0, hq.getDefense(), "PV régénérés après bataille (def 50 → 200)");
            assertEquals(100.0, hq.getAttack(), "Le reassign-zéro ATK est annulé par la régénération");

            assertTrue(em.find(Bank.class, w.bankId()).isDestroyed());
            assertTrue(em.find(WeaponCache.class, w.cacheId()).isDestroyed());

            Sector sector = loadSector(w.sectorNumber());
            assertEquals(3, sector.getBuildings().size());
            assertEquals(2, sector.getUnits().size(), "B1 + B2 survivants");
        });
    }

    @Test
    @DisplayName("Cache capturé intact : l'équipement stocké est transféré au vainqueur, pas supprimé")
    void capturingIntactWeaponCacheTransfersItsStoredEquipment() {
        World w = seedDefenderHolding();
        // Ni unité ni personnage, QG hors service : seule la paire Cache/Banque tient le secteur.
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Player defender = playerRepository.findById(w.defenderId()).orElseThrow();
            Sector sector = loadSector(w.sectorNumber());
            for (Unit unit : List.copyOf(sector.getUnits())) {
                if (defender.getId().equals(unit.getPlayerId())) {
                    sector.getUnits().remove(unit);
                    em.remove(unit);
                }
            }
            GameCharacter character = em.find(GameCharacter.class, w.characterId());
            sector.getCharacters().remove(character);
            defender.setCharacter(null);
            em.find(Headquarters.class, w.hqId()).destroy();
            em.flush();
            assertEquals(1, em.find(WeaponCache.class, w.cacheId()).getStoredEquipments().size(),
                    "Le Cache est approvisionné avant la bataille");
        });
        seedAttackerUnits(w.attackerId(), w.sectorNumber(), 8.0, 2); // 2 BRUTEs

        CombatService.SectorBattleResult r = runBattle(w);

        assertTrue(r.success());
        assertEquals(w.attackerId(), r.winner().getId(), "Aucun combattant défenseur ⇒ victoire attaquant");
        assertEquals(2, r.capturedBuildings(), "QG détruit (capturé) + Cache intact");

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            WeaponCache cache = em.find(WeaponCache.class, w.cacheId());
            assertFalse(cache.isDestroyed(), "Le Cache encaisse le reliquat ATK sans tomber");
            assertTrue(cache.isCaptured());
            assertTrue(cache.getStoredEquipments().isEmpty(), "Le Cache capturé est vidé");

            Player attacker = playerRepository.findById(w.attackerId()).orElseThrow();
            assertEquals(1, attacker.getEquipments().size(),
                    "L'équipement du Cache rejoint l'inventaire du capturant (pas supprimé)");
            assertEquals("FusilBtC", attacker.getEquipments().getFirst().getEquipment().getName());
            assertEquals(1, attacker.getEquipments().getFirst().getQuantity());
        });
    }
}
