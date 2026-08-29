package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeder dev-only d'un scénario de test pour la résolution pas-à-pas.
 *
 * <p>Scénario 2 hops : lurio (LEGER) part du secteur 41 via la route [41, 13, 32]
 * vers cegorach en 32 (2 TIREUR BRUTE 100/100 ajoutés si manquants, combat déterministe).
 * Step 1 = secteur 13 allié (pas de conflit), step 2 = secteur 32 ennemi (conflit).
 * Idempotent : nettoie les PENDING antérieurs de lurio et n'ajoute les défenseurs que s'ils manquent.</p>
 */
@Service
public class TurnResolutionScenarioSeeder {

    private static final int SECTOR_ATTACKER_FROM = 41;
    private static final int SECTOR_INTERMEDIATE = 13;
    private static final int SECTOR_DEFENDER = 32;
    private static final int TARGET_DEFENDER_COUNT = 2;

    private final BoardRepository boardRepository;
    private final PlayerRepository playerRepository;
    private final MovementOrderRepository movementOrderRepository;
    private final MovementService movementService;
    private final TurnService turnService;
    private final TurnLock turnLock;
    private final EntityManager entityManager;

    public TurnResolutionScenarioSeeder(BoardRepository boardRepository,
                                        PlayerRepository playerRepository,
                                        MovementOrderRepository movementOrderRepository,
                                        MovementService movementService,
                                        TurnService turnService,
                                        TurnLock turnLock,
                                        EntityManager entityManager) {
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
        this.movementOrderRepository = movementOrderRepository;
        this.movementService = movementService;
        this.turnService = turnService;
        this.turnLock = turnLock;
        this.entityManager = entityManager;
    }

    public boolean isAvailable() {
        return true;
    }

    @Transactional
    public ScenarioSummaryDto seedScenario() {
        if (turnLock.isLocked()) {
            throw new IllegalStateException(
                    "Une session pas-à-pas est active — finalisez ou abandonnez-la avant de re-seeder");
        }

        Board board = boardRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun plateau trouvé — importez d'abord un board"));
        int turn = turnService.getCurrentTurn();

        Player lurio = resolvePlayerByName("lurio", "lurio introuvable — vérifiez le seed démo");
        Player cegorach = resolvePlayerByName("cegorach", "cegorach introuvable — vérifiez le seed démo");

        Sector sDefender = board.getSector(SECTOR_DEFENDER);
        if (sDefender == null) {
            throw new IllegalStateException("Secteur défenseur " + SECTOR_DEFENDER + " introuvable sur le plateau");
        }

        // Défenseurs cegorach en 32 : 2 BRUTEs (exp 8 → 100/100, déterministe sans évasion).
        int defendersAdded = 0;
        long existing = sDefender.getUnits().stream()
                .filter(u -> cegorach.getId().equals(u.getPlayerId()))
                .count();
        for (int i = 0; i < TARGET_DEFENDER_COUNT - existing; i++) {
            Unit defender = new Unit(8.0, UnitClass.TIREUR);
            defender.setPlayerId(cegorach.getId());
            sDefender.addUnit(defender);
            defendersAdded++;
        }
        entityManager.flush(); // obtenir les IDs persistés

        // Attaquant LEGER (≥2 hops) de lurio : cherché en 41 puis en 13 (après finalize précédent),
        // à défaut ajouté en 41.
        Sector sAttackerFrom = board.getSector(SECTOR_ATTACKER_FROM);
        if (sAttackerFrom == null) {
            throw new IllegalStateException("Secteur attaquant " + SECTOR_ATTACKER_FROM + " introuvable");
        }
        Unit attackerUnit = pickAttacker(sAttackerFrom, lurio.getId());
        boolean addedAttacker = false;
        if (attackerUnit == null) {
            Sector sIntermediate = board.getSector(SECTOR_INTERMEDIATE);
            if (sIntermediate != null) {
                attackerUnit = pickAttacker(sIntermediate, lurio.getId());
            }
        }
        if (attackerUnit == null) {
            attackerUnit = new Unit(2.0, UnitClass.LEGER);
            attackerUnit.setPlayerId(lurio.getId());
            sAttackerFrom.addUnit(attackerUnit);
            addedAttacker = true;
            entityManager.flush();
        }

        Long attackerUnitId = attackerUnit.getId();
        int fromSector = sAttackerFrom.getNumber();

        List<MovementOrder> pendingLurio = movementOrderRepository
                .findByPlayerIdAndTurnAndStatus(lurio.getId(), turn, MovementStatus.PENDING);
        if (!pendingLurio.isEmpty()) {
            movementOrderRepository.deleteAll(pendingLurio);
            entityManager.flush();
        }

        MovementOrder order = movementService.placeFootOrder(
                lurio.getId(), turn, List.of(attackerUnitId),
                List.of(SECTOR_ATTACKER_FROM, SECTOR_INTERMEDIATE, SECTOR_DEFENDER), board);

        ScenarioSummaryDto dto = new ScenarioSummaryDto();
        dto.setTurn(turn);

        ScenarioSummaryDto.ActorDto attackerDto = new ScenarioSummaryDto.ActorDto();
        attackerDto.setId(lurio.getId());
        attackerDto.setName(lurio.getName());
        dto.setAttacker(attackerDto);

        ScenarioSummaryDto.ActorDto defenderDto = new ScenarioSummaryDto.ActorDto();
        defenderDto.setId(cegorach.getId());
        defenderDto.setName(cegorach.getName());
        dto.setDefender(defenderDto);

        ScenarioSummaryDto.UnitDto unitDto = new ScenarioSummaryDto.UnitDto();
        unitDto.setId(attackerUnitId);
        unitDto.setUnitClass(attackerUnit.getClasses().stream()
                .map(UnitClass::name).findFirst().orElse("?"));
        unitDto.setFromSector(fromSector);
        dto.setAttackerUnit(unitDto);

        dto.setDefendersAdded(defendersAdded);
        dto.setRoute(List.of(SECTOR_ATTACKER_FROM, SECTOR_INTERMEDIATE, SECTOR_DEFENDER));
        dto.setOrderId(order.getId());
        dto.setMessage(addedAttacker
                ? "Scénario prêt — unité attaquante ajoutée en " + fromSector + ". Démarrez la session puis 2 hops."
                : "Scénario prêt — démarrez la session pas-à-pas, puis avancez de 2 hops et résolvez le conflit sur le secteur " + SECTOR_DEFENDER + ".");
        return dto;
    }

    private Unit pickAttacker(Sector sector, Long playerId) {
        return sector.getUnits().stream()
                .filter(u -> playerId.equals(u.getPlayerId()))
                .filter(u -> u.getMaxMovementHops() >= 2)
                .findFirst()
                .orElse(null);
    }

    private Player resolvePlayerByName(String name, String errorMessage) {
        return playerRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException(errorMessage));
    }
}