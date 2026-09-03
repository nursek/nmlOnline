package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // Cache du tour courant : évite un N+1 (SELECT boards + tx par bâtiment mappé).
    // Invalidé par advanceTurn / invalidateTurnCache.
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
        if (turn != null) {
            cachedTurn = turn;
            return turn;
        }
        return 1;
    }

    /** À appeler quand le tour est muté hors de {@link #advanceTurn()} (ex. finalizeTurn). */
    public void invalidateTurnCache() {
        cachedTurn = null;
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

            board.setCurrentTurn(turnEnding + 1);
            board = boardRepository.save(board);
            // Invalidation (pas mise à jour) : si rollback après ce point, la prochaine lecture relit la DB.
            cachedTurn = null;
            return board.getCurrentTurn();
        } finally {
            turnLock.release();
        }
    }
}
