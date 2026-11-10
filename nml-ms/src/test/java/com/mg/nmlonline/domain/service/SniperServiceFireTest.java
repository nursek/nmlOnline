package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.domain.model.board.Board;
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

    private FireWorld seedStandaloneFire(boolean sniperHasOwnOrder) {
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

            Unit targetUnit = new Unit(0.0, UnitClass.SNIPER);
            targetUnit.setPlayerId(target.getId());
            targetUnit.setPdf(500);
            targetSector.addUnit(targetUnit);
            em.flush();

            int turn = board.getCurrentTurn();
            SniperOrder order = sniperOrderRepository.save(SniperOrder.create(
                    shooter.getId(), turn, shooterUnit.getId(), shooterSector.getNumber(),
                    targetSector.getNumber()));
            if (sniperHasOwnOrder) {
                Sector other = targetSector.getNeighbors().stream()
                        .map(board::getSector)
                        .filter(sector -> sector != null && sector.isNeutral() && isEmpty(sector)
                                && sector.getNumber() != shooterSector.getNumber())
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Aucun secteur voisin pour l'ordre du sniper ciblé"));
                sniperOrderRepository.save(SniperOrder.create(
                        target.getId(), turn, targetUnit.getId(), targetSector.getNumber(),
                        other.getNumber()));
            }
            em.flush();
            return new FireWorld(shooter.getId(), target.getId(), order.getId(),
                    (long) shooterSector.getNumber(), (long) targetSector.getNumber());
        });
    }

    private void resolve(FireWorld world) {
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

    @Test
    @DisplayName("Le tir isolé résout l'ordre et génère un rapport de tir à distance avec contre-tir")
    void standaloneFireTriggersCounterAndWritesReport() {
        FireWorld world = seedStandaloneFire(false);

        resolve(world);

        SniperOrder order = sniperOrderRepository.findById(world.shooterOrderId()).orElseThrow();
        assertEquals(SniperOrderStatus.RESOLVED, order.getStatus());
        String payload = reportPayload(world.targetSector());
        assertNotNull(payload, "Un rapport de tir à distance doit être écrit");
        assertTrue(payload.contains("\"ranged\":true"));
        assertTrue(payload.contains("Tir à distance"));
        assertTrue(payload.contains("Contre-tir à distance"));
        assertTrue(playerRepository.findById(world.targetPlayerId()).isPresent());
        assertFalse(playerRepository.findById(world.targetPlayerId()).orElseThrow().isEliminated());
    }

    @Test
    @DisplayName("Un sniper ciblé qui a déjà un ordre ne contre-tire pas")
    void sniperWithOwnOrderDoesNotCounterFire() {
        FireWorld world = seedStandaloneFire(true);

        resolve(world);

        String payload = reportPayload(world.targetSector());
        assertNotNull(payload);
        assertFalse(payload.contains("Contre-tir à distance"),
                "Le sniper ciblé n'est pas inactif : aucun contre-tir");
    }
}
