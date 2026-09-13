package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
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

import java.util.ArrayList;
import java.util.List;

/** Dev-only, idempotent : purge les PENDING du tour et n'ajoute que les unités manquantes. */
@Service
public class TurnResolutionScenarioSeeder {

    private static final int SECTOR_ATTACKER_FROM = 41;
    private static final int SECTOR_INTERMEDIATE = 13;
    private static final int SECTOR_DEFENDER = 32;
    private static final int TARGET_DEFENDER_COUNT = 2;

    private static final int STANDOFF_ATTACKER_FROM = 43;

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
        Board board = requireBoard();
        int turn = turnService.getCurrentTurn();
        requireNoActiveSession();

        Player lurio = resolvePlayerByName("lurio", "lurio introuvable — vérifiez le seed démo");
        Player cegorach = resolvePlayerByName("cegorach", "cegorach introuvable — vérifiez le seed démo");

        Sector sDefender = requireSector(board, SECTOR_DEFENDER, "défenseur");
        int defendersAdded = ensureDefenders(sDefender, cegorach);

        // Attaquant LEGER (≥2 hops) de lurio : cherché en 41 puis en 13 (après finalize précédent),
        // à défaut ajouté en 41.
        Sector sAttackerFrom = requireSector(board, SECTOR_ATTACKER_FROM, "attaquant");
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

        deletePendingOrders(turn, List.of(lurio.getId(), cegorach.getId()));

        MovementOrder order = movementService.placeFootOrder(
                lurio.getId(), turn, List.of(attackerUnitId),
                List.of(SECTOR_ATTACKER_FROM, SECTOR_INTERMEDIATE, SECTOR_DEFENDER), board);

        ScenarioSummaryDto dto = new ScenarioSummaryDto();
        dto.setTurn(turn);
        dto.setAttacker(actor(lurio));
        dto.setDefender(actor(cegorach));
        dto.setAttackerUnit(unit(attackerUnit, fromSector));
        dto.setDefendersAdded(defendersAdded);
        dto.setRoute(List.of(SECTOR_ATTACKER_FROM, SECTOR_INTERMEDIATE, SECTOR_DEFENDER));
        dto.setOrderId(order.getId());
        dto.setMessage(addedAttacker
                ? "Scénario prêt — unité attaquante ajoutée en " + fromSector + ". Démarrez la session puis 2 hops."
                : "Scénario prêt — démarrez la session pas-à-pas, puis avancez de 2 hops et résolvez le conflit sur le secteur " + SECTOR_DEFENDER + ".");
        return dto;
    }

    /** Impasse : cegorach défend 32 ; imotekh (43) puis lurio (41) y arrivent au même hop, dans cet ordre d'envoi. */
    @Transactional
    public ScenarioSummaryDto seedStandoffScenario() {
        Board board = requireBoard();
        int turn = turnService.getCurrentTurn();
        requireNoActiveSession();

        Player lurio = resolvePlayerByName("lurio", "lurio introuvable — vérifiez le seed démo");
        Player imotekh = resolvePlayerByName("imotekh", "imotekh introuvable — vérifiez le seed démo");
        Player cegorach = resolvePlayerByName("cegorach", "cegorach introuvable — vérifiez le seed démo");

        Sector sDefender = requireSector(board, SECTOR_DEFENDER, "défenseur");
        int defendersAdded = ensureDefenders(sDefender, cegorach);

        Sector sLurioFrom = requireSector(board, SECTOR_ATTACKER_FROM, "attaquant");
        Sector sImotekhFrom = requireSector(board, STANDOFF_ATTACKER_FROM, "attaquant");

        Unit lurioUnit = ensureAttackerUnit(sLurioFrom, lurio);
        Unit imotekhUnit = ensureAttackerUnit(sImotekhFrom, imotekh);
        entityManager.flush();

        deletePendingOrders(turn, List.of(lurio.getId(), imotekh.getId(), cegorach.getId()));

        MovementOrder imotekhOrder = movementService.placeFootOrder(
                imotekh.getId(), turn, List.of(imotekhUnit.getId()),
                List.of(STANDOFF_ATTACKER_FROM, SECTOR_DEFENDER), board);
        MovementOrder lurioOrder = movementService.placeFootOrder(
                lurio.getId(), turn, List.of(lurioUnit.getId()),
                List.of(SECTOR_ATTACKER_FROM, SECTOR_DEFENDER), board);

        ScenarioSummaryDto dto = new ScenarioSummaryDto();
        dto.setTurn(turn);
        dto.setStandoff(true);
        dto.setDefender(actor(cegorach));
        dto.setDefendersAdded(defendersAdded);
        dto.setOrders(List.of(
                order(imotekhOrder, imotekh, imotekhUnit, STANDOFF_ATTACKER_FROM),
                order(lurioOrder, lurio, lurioUnit, SECTOR_ATTACKER_FROM)));
        dto.setMessage("Impasse prête — imotekh puis lurio arrivent en " + SECTOR_DEFENDER
                + " avec cegorach. Démarrez la session puis un seul hop : un unique conflit à résoudre.");
        return dto;
    }

    private ScenarioSummaryDto.ActorDto actor(Player player) {
        ScenarioSummaryDto.ActorDto actor = new ScenarioSummaryDto.ActorDto();
        actor.setId(player.getId());
        actor.setName(player.getName());
        return actor;
    }

    private ScenarioSummaryDto.UnitDto unit(Unit unit, int fromSector) {
        ScenarioSummaryDto.UnitDto dto = new ScenarioSummaryDto.UnitDto();
        dto.setId(unit.getId());
        dto.setUnitClass(unit.getClasses().stream().map(UnitClass::name).findFirst().orElse("?"));
        dto.setFromSector(fromSector);
        return dto;
    }

    private ScenarioSummaryDto.OrderDto order(MovementOrder order, Player player, Unit unit, int fromSector) {
        ScenarioSummaryDto.OrderDto dto = new ScenarioSummaryDto.OrderDto();
        dto.setPlayerId(player.getId());
        dto.setPlayerName(player.getName());
        dto.setUnitId(unit.getId());
        dto.setUnitClass(unit.getClasses().stream().map(UnitClass::name).findFirst().orElse("?"));
        dto.setFromSector(fromSector);
        dto.setRoute(new ArrayList<>(order.getRoute()));
        dto.setOrderId(order.getId());
        return dto;
    }

    private Board requireBoard() {
        return boardRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun plateau trouvé — importez d'abord un board"));
    }

    private void requireNoActiveSession() {
        if (turnLock.isLocked()) {
            throw new IllegalStateException(
                    "Une session pas-à-pas est active — finalisez ou abandonnez-la avant de re-seeder");
        }
    }

    private Sector requireSector(Board board, int number, String role) {
        Sector sector = board.getSector(number);
        if (sector == null) {
            throw new IllegalStateException("Secteur " + role + " " + number + " introuvable sur le plateau");
        }
        return sector;
    }

    private int ensureDefenders(Sector sector, Player defender) {
        int defendersAdded = 0;
        long existing = sector.getUnits().stream()
                .filter(u -> defender.getId().equals(u.getPlayerId()))
                .count();
        for (int i = 0; i < TARGET_DEFENDER_COUNT - existing; i++) {
            Unit unit = new Unit(8.0, UnitClass.TIREUR);
            unit.setPlayerId(defender.getId());
            sector.addUnit(unit);
            defendersAdded++;
        }
        entityManager.flush();
        return defendersAdded;
    }

    private Unit ensureAttackerUnit(Sector sector, Player player) {
        Unit existing = sector.getUnits().stream()
                .filter(u -> player.getId().equals(u.getPlayerId()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        Unit unit = new Unit(8.0, UnitClass.TIREUR);
        unit.setPlayerId(player.getId());
        sector.addUnit(unit);
        return unit;
    }

    private void deletePendingOrders(int turn, List<Long> playerIds) {
        List<MovementOrder> pending = movementOrderRepository.findPendingByTurn(turn).stream()
                .filter(o -> playerIds.contains(o.getPlayerId()))
                .toList();
        if (!pending.isEmpty()) {
            movementOrderRepository.deleteAll(pending);
            entityManager.flush();
        }
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
