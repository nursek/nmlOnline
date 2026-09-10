package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Source unique de vérité du tour courant du plateau ({@link Board#getCurrentTurn()}).
 * Muté par {@link #advanceTurn()} : résout les mouvements PENDING puis incrémente.
 */
@Service
@Transactional
public class TurnService {

    private final BoardRepository boardRepository;
    private final MovementService movementService;
    private final TurnLock turnLock;
    private final GameCharacterService characterService;

    // Cache du tour (évite un N+1) : publié avant commit, purgé sur rollback/invalidateTurnCache.
    private volatile Integer cachedTurn;

    public TurnService(BoardRepository boardRepository, MovementService movementService,
                       TurnLock turnLock, GameCharacterService characterService) {
        this.boardRepository = boardRepository;
        this.movementService = movementService;
        this.turnLock = turnLock;
        this.characterService = characterService;
    }

    /** Retourne 1 si aucun plateau n'existe encore. */
    @Transactional(readOnly = true)
    public int getCurrentTurn() {
        Integer cached = cachedTurn;
        if (cached != null) {
            return cached;
        }
        Integer turn = boardRepository.findAll().stream()
                .findFirst()
                .map(Board::getCurrentTurn)
                .orElse(null);
        if (turn == null) {
            return 1;
        }
        // Verrou : un lecteur parti avant publishTurn écraserait la nouvelle valeur.
        synchronized (this) {
            if (cachedTurn == null) {
                cachedTurn = turn;
            }
            return cachedTurn;
        }
    }

    /** À appeler quand le tour est muté hors de {@link #advanceTurn()} (ex. finalizeTurn). */
    public void invalidateTurnCache() {
        synchronized (this) {
            cachedTurn = null;
        }
    }

    /** Publie le tour cible avant commit ; le cache est purgé si la transaction échoue. */
    public void publishTurn(int turn) {
        synchronized (this) {
            cachedTurn = turn;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        invalidateTurnCache();
                    }
                }
            });
        }
    }

    /** Termine le tour : résout les mouvements PENDING puis incrémente le compteur. */
    public int advanceTurn() {
        if (!turnLock.tryAcquire()) {
            throw new IllegalStateException("Un advanceTurn ou une résolution pas-à-pas est déjà en cours");
        }
        try {
            Board board = boardRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Aucun plateau trouvé pour avancer le tour"));

            int turnEnding = board.getCurrentTurn();

            // Résolution des mouvements du tour qui se termine, AVANT l'incrément.
            movementService.resolveAllMovements(turnEnding, board);

            characterService.regenerateAllCharacters();

            int newTurn = turnEnding + 1;
            board.setCurrentTurn(newTurn);
            board = boardRepository.save(board);
            publishTurn(newTurn);
            return board.getCurrentTurn();
        } finally {
            turnLock.release();
        }
    }
}
