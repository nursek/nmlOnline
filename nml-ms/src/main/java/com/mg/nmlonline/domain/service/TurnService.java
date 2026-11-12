package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.movement.SectorCapture;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source unique de vérité du tour courant du plateau ({@link Board#getCurrentTurn()}).
 * Muté par {@link #advanceTurn()} (résolution puis incrément) et {@link #setCurrentTurn(int)}
 * (navigation manuelle dev, sans résolution).
 */
@Service
@Transactional
public class TurnService {

    private final BoardRepository boardRepository;
    private final MovementService movementService;
    private final TurnLock turnLock;
    private final GameCharacterService characterService;
    private final HarvestAutoCollector harvestAutoCollector;
    private final VehicleMaintenanceService vehicleMaintenanceService;
    private final ReserveUnitPlacer reserveUnitPlacer;
    private final UnitRecoveryService unitRecoveryService;

    // Cache du tour par plateau (évite un N+1), publié avant commit et purgé sur rollback.
    private final Map<Long, Integer> cachedTurns = new ConcurrentHashMap<>();

    public TurnService(BoardRepository boardRepository, MovementService movementService,
                       TurnLock turnLock, GameCharacterService characterService,
                       HarvestAutoCollector harvestAutoCollector,
                       VehicleMaintenanceService vehicleMaintenanceService,
                       ReserveUnitPlacer reserveUnitPlacer,
                       UnitRecoveryService unitRecoveryService) {
        this.boardRepository = boardRepository;
        this.movementService = movementService;
        this.turnLock = turnLock;
        this.characterService = characterService;
        this.harvestAutoCollector = harvestAutoCollector;
        this.vehicleMaintenanceService = vehicleMaintenanceService;
        this.reserveUnitPlacer = reserveUnitPlacer;
        this.unitRecoveryService = unitRecoveryService;
    }

    @Transactional(readOnly = true)
    public int getCurrentTurn(Long boardId) {
        Integer cached = cachedTurns.get(boardId);
        if (cached != null) {
            return cached;
        }
        int turn = boardRepository.findById(boardId).map(Board::getCurrentTurn).orElse(1);
        // putIfAbsent : un lecteur parti avant publishTurn ne doit pas écraser la nouvelle valeur.
        cachedTurns.putIfAbsent(boardId, turn);
        return cachedTurns.get(boardId);
    }

    /** À appeler quand le tour est muté hors de {@link #advanceTurn()} (ex. finalizeTurn). */
    public void invalidateTurnCache() {
        cachedTurns.clear();
    }

    /** Publie le tour cible avant commit ; le cache est purgé si la transaction échoue. */
    public void publishTurn(Long boardId, int turn) {
        cachedTurns.put(boardId, turn);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        cachedTurns.remove(boardId, turn);
                    }
                }
            });
        }
    }

    /** Termine le tour : résout les mouvements PENDING puis incrémente le compteur. */
    public int advanceTurn(Long boardId) {
        return advanceTurnAndReport(boardId).newTurn();
    }

    public record TurnAdvanceResult(int newTurn, List<SectorCapture> captures) {
    }

    public TurnAdvanceResult advanceTurnAndReport(Long boardId) {
        if (!turnLock.tryAcquire(boardId)) {
            throw new IllegalStateException("Un advanceTurn ou une résolution pas-à-pas est déjà en cours");
        }
        try {
            Board board = boardRepository.findById(boardId)
                    .orElseThrow(() -> new IllegalStateException("Plateau " + boardId + " introuvable"));
            if (board.isPreparation()) {
                throw new IllegalStateException("Partie en préparation : démarrez-la avant de résoudre un tour");
            }
            int turnEnding = board.getCurrentTurn();

            // Avant les captures : le revenu du tour revient à son propriétaire pendant le tour.
            harvestAutoCollector.collectRemainingMoney(board, turnEnding);

            // Les recrues non déployées rejoignent le QG et participent à la résolution du tour.
            reserveUnitPlacer.placeAllAtHeadquarters();

            // Résolution des mouvements du tour qui se termine, AVANT l'incrément.
            MovementResolutionResult result = movementService.resolveAllMovements(turnEnding, board);

            characterService.regenerateAllCharacters(boardId);
            vehicleMaintenanceService.repairAllVehicles(boardId);
            unitRecoveryService.recoverWoundedUnits(turnEnding, boardId);

            int newTurn = turnEnding + 1;
            board.setCurrentTurn(newTurn);
            board = boardRepository.save(board);
            publishTurn(boardId, newTurn);
            return new TurnAdvanceResult(board.getCurrentTurn(), result.getCaptures());
        } finally {
            turnLock.release(boardId);
        }
    }

    public int setCurrentTurn(Long boardId, int turn) {
        if (turn < 1) {
            throw new IllegalArgumentException("Tour invalide : " + turn);
        }
        if (!turnLock.tryAcquire(boardId)) {
            throw new IllegalStateException("Une résolution de fin de tour est déjà en cours");
        }
        try {
            Board board = boardRepository.findById(boardId)
                    .orElseThrow(() -> new IllegalStateException("Plateau " + boardId + " introuvable"));
            board.setCurrentTurn(turn);
            boardRepository.save(board);
            publishTurn(boardId, turn);
            return turn;
        } finally {
            turnLock.release(boardId);
        }
    }
}
