package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.sniper.SniperOrder;
import com.mg.nmlonline.domain.model.sniper.SniperOrderStatus;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.repository.BattleReportRepository;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.SniperOrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EmbeddedPostgresTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("SniperService — tir isolé, contre-tir et rapport RANGED")
class SniperServiceFireTest {

    @Autowired
    private SniperService sniperService;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private SniperOrderRepository sniperOrderRepository;

    @Autowired
    private BattleReportRepository battleReportRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager txManager;

    private record FireWorld(Long shooterPlayerId, Long targetPlayerId, Long shooterOrderId,
                             Long shooterSector, Long targetSector) {
    }

    private Sector pickEmptyNeutral(Board board) {
        return board.getAllSectors().stream()
                .filter(Sector::isNeutral)
                .filter(this::isEmpty)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aucun secteur neutre vide dans le board de démo"));
    }

    private boolean isEmpty(Sector sector) {
        return sector.getArmySize() == 0 && sector.getBuildings().isEmpty()
                && sector.getCharacters().isEmpty() && sector.getVehicles().isEmpty();
    }

    private FireWorld seedStandaloneFire(boolean targetHasOwnOrder, boolean targetOrderResolved) {
        return new TransactionTemplate(txManager).execute(status -> {
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            Sector shooterSector = pickEmptyNeutral(board);
            Sector targetSector = shooterSector.getNeighbors().stream()
                    .map(board::getSector)
                    .filter(sector -> sector != null && sector.isNeutral() && isEmpty(sector))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Aucun secteur voisin vide pour le tir"));

            Player shooter = new Player("TireurSniper");
            playerRepository.save(shooter);
            Player target = new Player("CibleSniper");
            playerRepository.save(target);
            targetSector.setOwnerAndColor(target.getId(), "#ffffff");

            Unit shooterUnit = new Unit(0.0, UnitClass.SNIPER);
            shooterUnit.setPlayerId(shooter.getId());
            shooterUnit.setPdf(500);
            shooterUnit.setArmor(1000);
            shooterSector.addUnit(shooterUnit);

            Equipment cloak = new Equipment("GiletFireTest" + System.nanoTime(), 0, 0, 0, 0, 0,
                    Set.of(UnitClass.SNIPER), EquipmentCategory.DEFENSIVE);
            cloak.setEnemyAimMalus(100);
            em.persist(cloak);
            Unit targetUnit = new Unit(0.0, UnitClass.SNIPER);
            targetUnit.setPlayerId(target.getId());
            targetUnit.addEquipment(cloak);
            targetUnit.setPdf(500);
            targetSector.addUnit(targetUnit);
            em.flush();

            int turn = board.getCurrentTurn();
            SniperOrder order = sniperOrderRepository.save(SniperOrder.create(
                    shooter.getId(), turn, shooterUnit.getId(), shooterSector.getNumber(),
                    targetSector.getNumber()));
            if (targetHasOwnOrder) {
                Sector other = targetSector.getNeighbors().stream()
                        .map(board::getSector)
                        .filter(sector -> sector != null && sector.isNeutral() && isEmpty(sector)
                                && sector.getNumber() != shooterSector.getNumber())
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Aucun secteur voisin pour l'ordre du sniper ciblé"));
                SniperOrder targetOrder = sniperOrderRepository.save(SniperOrder.create(
                        target.getId(), turn, targetUnit.getId(), targetSector.getNumber(),
                        other.getNumber()));
                if (targetOrderResolved) {
                    targetOrder.resolve();
                    sniperOrderRepository.save(targetOrder);
                }
            }
            em.flush();
            return new FireWorld(shooter.getId(), target.getId(), order.getId(),
                    (long) shooterSector.getNumber(), (long) targetSector.getNumber());
        });
    }

    private void resolve(Long targetSector) {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            sniperService.resolveRemainingFire(board.getCurrentTurn(), board, Set.of());
        });
    }

    private String reportPayload(Long targetSector) {
        return battleReportRepository.findAll().stream()
                .filter(report -> report.getSectorNumber() == targetSector)
                .map(report -> report.getPayload())
                .findFirst()
                .orElse(null);
    }

    private JsonNode reportJson(FireWorld world) {
        return reportJson(world.targetSector());
    }

    private JsonNode reportJson(Long targetSector) {
        String payload = reportPayload(targetSector);
        assertNotNull(payload, "Un rapport de tir à distance doit être écrit");
        return JsonMapper.builder().build().readTree(payload);
    }

    private boolean hasGain(JsonNode gains, Long playerId, double amount) {
        if (gains == null || !gains.isArray()) {
            return false;
        }
        for (JsonNode gain : gains) {
            if (gain.get("playerId").asLong() == playerId
                    && Math.abs(gain.get("gained").asDouble() - amount) < 0.001) {
                return true;
            }
        }
        return false;
    }

    private record WoundWorld(Long targetUnitId, Long targetSector, int turn) {
    }

    private WoundWorld seedWoundedTarget() {
        return new TransactionTemplate(txManager).execute(status -> {
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            Sector shooterSector = pickEmptyNeutral(board);
            Sector targetSector = shooterSector.getNeighbors().stream()
                    .map(board::getSector)
                    .filter(sector -> sector != null && sector.isNeutral() && isEmpty(sector))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Aucun secteur voisin vide pour le tir"));

            Player shooter = new Player("TireurBlessure");
            playerRepository.save(shooter);
            Player target = new Player("CibleBlessure");
            playerRepository.save(target);
            targetSector.setOwnerAndColor(target.getId(), "#ffffff");

            Equipment rifle = new Equipment("FusilBlessure" + System.nanoTime(), 0, 0, 0, 0, 0,
                    Set.of(UnitClass.SNIPER), EquipmentCategory.FIREARM);
            rifle.setAimMalusReduction(100);
            em.persist(rifle);
            Unit shooterUnit = new Unit(0.0, UnitClass.SNIPER);
            shooterUnit.setPlayerId(shooter.getId());
            shooterUnit.addEquipment(rifle);
            shooterUnit.setPdf(5);
            shooterSector.addUnit(shooterUnit);

            Unit targetUnit = new Unit(0.0, UnitClass.LEGER);
            targetUnit.setPlayerId(target.getId());
            targetSector.addUnit(targetUnit);
            em.flush();

            int turn = board.getCurrentTurn();
            sniperOrderRepository.save(SniperOrder.create(shooter.getId(), turn, shooterUnit.getId(),
                    shooterSector.getNumber(), targetSector.getNumber()));
            em.flush();
            return new WoundWorld(targetUnit.getId(), (long) targetSector.getNumber(), turn);
        });
    }

    private record CombinedWorld(Long bankId, Long targetSector) {
    }

    private CombinedWorld seedCombinedFire() {
        return new TransactionTemplate(txManager).execute(status -> {
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            Sector targetSector = pickEmptyNeutral(board);
            List<Sector> shooterSectors = board.getAllSectors().stream()
                    .filter(Sector::isNeutral)
                    .filter(this::isEmpty)
                    .filter(sector -> sector.getNumber() != targetSector.getNumber())
                    .limit(2)
                    .toList();
            if (shooterSectors.size() < 2) {
                throw new AssertionError("Deux secteurs neutres vides requis pour la salve combinée");
            }

            Player bankOwner = new Player("BanqueCible");
            playerRepository.save(bankOwner);
            targetSector.setOwnerAndColor(bankOwner.getId(), "#ffffff");
            Bank bank = new Bank(bankOwner.getId());
            bank.setSector(targetSector);
            em.persist(bank);
            targetSector.getBuildings().add(bank);

            List<Player> shooters = List.of(new Player("TireurCombineA"), new Player("TireurCombineB"));
            shooters.forEach(playerRepository::save);
            for (int i = 0; i < 2; i++) {
                Unit shooterUnit = new Unit(0.0, UnitClass.SNIPER);
                shooterUnit.setPlayerId(shooters.get(i).getId());
                shooterUnit.setPdf(30);
                shooterSectors.get(i).addUnit(shooterUnit);
            }
            em.flush();

            int turn = board.getCurrentTurn();
            for (int i = 0; i < 2; i++) {
                Unit shooterUnit = shooterSectors.get(i).getUnits().getFirst();
                sniperOrderRepository.save(SniperOrder.create(shooters.get(i).getId(), turn,
                        shooterUnit.getId(), shooterSectors.get(i).getNumber(), targetSector.getNumber()));
            }
            em.flush();
            return new CombinedWorld(bank.getId(), (long) targetSector.getNumber());
        });
    }

    @Test
    @DisplayName("Le tir isolé résout l'ordre, écrit un rapport et crédite le tireur survivant au contre-tir")
    void standaloneFireTriggersCounterAndWritesReport() throws Exception {
        FireWorld world = seedStandaloneFire(false, false);

        resolve(world.targetSector());

        SniperOrder order = sniperOrderRepository.findById(world.shooterOrderId()).orElseThrow();
        assertEquals(SniperOrderStatus.RESOLVED, order.getStatus());
        JsonNode report = reportJson(world);
        assertTrue(report.get("ranged").asBoolean());
        assertTrue(report.get("rangedFire").asText().contains("Tir à distance"));
        assertTrue(report.get("rangedFire").asText().contains("Contre-tir à distance"));
        assertTrue(hasGain(report.get("experienceGains"), world.shooterPlayerId(), 1.0),
                "Le tireur ciblé par le contre-tir et survivant gagne 1 Exp");
        assertTrue(hasGain(report.get("experienceGains"), world.targetPlayerId(), 1.0),
                "Le sniper ciblé survivant gagne 1 Exp");
        assertTrue(playerRepository.findById(world.targetPlayerId()).isPresent());
        assertFalse(playerRepository.findById(world.targetPlayerId()).orElseThrow().isEliminated());
    }

    @Test
    @DisplayName("Un sniper ciblé qui a déjà un ordre PENDING ne contre-tire pas et le tireur ne gagne rien")
    void sniperWithOwnOrderDoesNotCounterFire() {
        FireWorld world = seedStandaloneFire(true, false);

        resolve(world.targetSector());

        JsonNode report = reportJson(world);
        assertFalse(report.get("rangedFire").asText().contains("Contre-tir à distance"),
                "Le sniper ciblé n'est pas inactif : aucun contre-tir");
        assertFalse(hasGain(report.get("experienceGains"), world.shooterPlayerId(), 1.0),
                "Sans contre-tir subi, le tireur ne gagne rien pour son propre tir");
    }

    @Test
    @DisplayName("Un sniper ciblé dont l'ordre est déjà RESOLVED ne contre-tire pas")
    void sniperWithResolvedOrderDoesNotCounterFire() {
        FireWorld world = seedStandaloneFire(true, true);

        resolve(world.targetSector());

        String payload = reportPayload(world.targetSector());
        assertNotNull(payload);
        assertFalse(payload.contains("Contre-tir à distance"),
                "Le sniper a déjà tiré ce tour : aucun second tir");
    }

    @Test
    @DisplayName("Un tir isolé blesse la cible pour le tour courant : pas de soin dans le même finalize")
    void standaloneFireRecordsWound() {
        WoundWorld world = seedWoundedTarget();

        resolve(world.targetSector());

        Unit target = new TransactionTemplate(txManager)
                .execute(status -> em.find(Unit.class, world.targetUnitId()));
        assertNotNull(target, "La cible survivante reste en base");
        assertTrue(target.isInjured(), "La défense entamée blesse l'unité");
        assertFalse(target.shouldRecover(world.turn()),
                "La blessure est acquise au tour courant : la récupération attend le tour suivant");
    }

    @Test
    @DisplayName("Les salves de deux joueurs sur le même secteur sont combinées en une seule frappe")
    void combinedSalvoIsResolvedAtOnce() {
        CombinedWorld world = seedCombinedFire();

        resolve(world.targetSector());

        Bank bank = new TransactionTemplate(txManager)
                .execute(status -> em.find(Bank.class, world.bankId()));
        assertNotNull(bank);
        assertTrue(bank.isDestroyed(), "60 PdF combinés détruisent la Banque (50 défense)");
        JsonNode report = reportJson(world.targetSector());
        assertTrue(report.get("rangedFire").asText().contains("60 PdF"),
                "La salve est résolue avec la somme des tireurs, pas en deux frappes de 30");
    }
}
