package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Source unique de vérité du tour courant du plateau.
 *
 * <p>Le tour est stocké sur {@link Board#getCurrentTurn()} et n'est muté que par
 * {@link #advanceTurn()} (bouton admin « Finir le tour »), qui résout d'abord les
 * ordres de déplacement PENDING du tour via {@link MovementService#resolveAllMovements}
 * puis incrémente le compteur.
 *
 * <p>ponytail: ceiling = déclenchement manuel par l'admin ; upgrade path = scheduler
 * automatique end-of-turn + calcul des revenus/effets de bâtiments à l'advanceTurn.
 */
@Service
@Transactional
public class TurnService {

    private final BoardRepository boardRepository;
    private final MovementService movementService;

    /**
     * Verrou anti double-clic (bouton admin « Finir le tour ») : un seul
     * advanceTurn peut tourner à la fois dans cette JVM.
     *
     * <p>ponytail: ceiling = JVM unique, guard in-process ; le verrou est relâché
     * dans {@code finally} avant le commit transactionnel, donc une fenêtre
     * (infime) reste ouverte entre deux threads en rafale. Upgrade path = lock
     * pessimiste JPA sur {@link Board} ou DistributedLock si multi-instance.
     */
    private final AtomicBoolean advancing = new AtomicBoolean(false);

    public TurnService(BoardRepository boardRepository, MovementService movementService) {
        this.boardRepository = boardRepository;
        this.movementService = movementService;
    }

    /**
     * Tour courant lu sur le 1er plateau (un seul Board attendu en production).
     *
     * @return le tour courant, ou 1 si aucun plateau n'existe encore
     */
    @Transactional(readOnly = true)
    public int getCurrentTurn() {
        return boardRepository.findAll().stream()
                .findFirst()
                .map(Board::getCurrentTurn)
                .orElse(1);
    }

    /**
     * Termine le tour courant : résout les ordres de déplacement PENDING, puis
     * incrémente le compteur. Retourne le nouveau numéro de tour.
     *
     * <p>Aucun effet de revenus/bâtiments pour l'instant (voir ponytail sur le champ
     * {@code Board.currentTurn}).
     */
    public int advanceTurn() {
        if (!advancing.compareAndSet(false, true)) {
            throw new IllegalStateException("Un advanceTurn est déjà en cours");
        }
        try {
            Board board = boardRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Aucun plateau trouvé pour avancer le tour"));

            int turnEnding = board.getCurrentTurn();

            // Résolution des mouvements du tour qui se termine, AVANT l'incrément.
            movementService.resolveAllMovements(turnEnding, board);

            board.setCurrentTurn(turnEnding + 1);
            board = boardRepository.save(board);
            return board.getCurrentTurn();
        } finally {
            advancing.set(false);
        }
    }
}