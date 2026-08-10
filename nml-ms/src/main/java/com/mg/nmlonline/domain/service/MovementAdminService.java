package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.AdminMovementOrderDto;
import com.mg.nmlonline.api.dto.MovementResolutionResultDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.mapper.MovementMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Point d'entrée admin pour la consultation et la résolution des ordres de
 * déplacement d'un tour.
 *
 * <p>Trois opérations :</p>
 * <ul>
 *   <li>{@link #getOrdersForTurn} : vue de tous les ordres du tour courant
 *       (filtrable par statut), enrichie du nom du joueur.</li>
 *   <li>{@link #previewMovements} : <strong>aperçu non mutant</strong> — calcule
 *       le compte-rendu des conflits sans persister les déplacements ni marquer
 *       les ordres résolus.</li>
 *   <li>{@link #resolveMovements} : <strong>application mutante</strong> —
 *       déplace les entités, marque les ordres RESOLVED/BLOCKED, persiste.</li>
 * </ul>
 *
 * <p>{@code resolveAllMovements} mute l'état (déplace des entités, change les
 * statuts d'ordres). Pour l'aperçu, on réutilise tel quel cet algorithme dans
 * une transaction {@link TransactionDefinition#PROPAGATION_REQUIRES_NEW
 * REQUIRES_NEW} roulée en arrière explicitement après calcul du DTO : aucun
 * rollback-only silencieux ni UnexpectedRollbackException, et le diff reste
 * minimal (pas de duplication de l'algo de simulation).
 *
 * <p>ponytail: ceiling = l'aperçu ré-exécute toute la résolution (coût CPU double
 * si admin enchaîne aperçu puis apply). Upgrade path = variante pure-simulation
 * qui ne touche pas aux entités gérées (snapshot des armées en mémoire), mais
 * cela duplexerait la moitié de {@link MovementService#resolveAllMovements}.
 * Garder la version rollback tant que le volume d'ordres par tour reste raisonnable.</p>
 */
@Service
public class MovementAdminService {

    private final MovementService movementService;
    private final MovementMapper movementMapper;
    private final MovementOrderRepository orderRepository;
    private final BoardRepository boardRepository;
    private final PlayerRepository playerRepository;
    private final PlatformTransactionManager txManager;

    public MovementAdminService(MovementService movementService,
                                MovementMapper movementMapper,
                                MovementOrderRepository orderRepository,
                                BoardRepository boardRepository,
                                PlayerRepository playerRepository,
                                PlatformTransactionManager txManager) {
        this.movementService = movementService;
        this.movementMapper = movementMapper;
        this.orderRepository = orderRepository;
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
        this.txManager = txManager;
    }

    /**
     * Tous les ordres d'un tour, optionnellement filtrés par statut, avec le nom
     * du joueur résolu (lookup en lot).
     */
    @Transactional(readOnly = true)
    public List<AdminMovementOrderDto> getOrdersForTurn(int turn, MovementStatus status) {
        List<MovementOrder> orders = status != null
                ? orderRepository.findByTurnAndStatus(turn, status)
                : orderRepository.findByTurn(turn);
        Function<Long, String> names = resolveNames(collectPlayerIds(orders));
        return orders.stream()
                .map(o -> movementMapper.toAdminDto(o, names.apply(o.getPlayerId())))
                .toList();
    }

    /**
     * Aperçu (dry-run) de la résolution : calcule le compte-rendu des conflits
     * <strong>sans</strong> persister les effets (ordres laissés PENDING,
     * entités non déplacées).
     *
     * <p>Implémente le rollback via transaction programmatique REQUIRES_NEW :
     * on ouvre une transaction fraîche, on exécute {@link MovementService#resolveAllMovements}
     * (qui joint cette transaction via REQUIRED), on construit le DTO depuis les
     * entités encore vivantes, puis on rollback explicitement. Aucune trace
     * ne persiste.</p>
     */
    public MovementResolutionResultDto previewMovements(int turn) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = txManager.getTransaction(def);
        try {
            Board board = loadBoard();
            MovementResolutionResult result = movementService.resolveAllMovements(turn, board);
            Function<Long, String> names = resolveNames(collectPlayerIds(result));
            // Builder le DTO AVANT le rollback : les entités gérées sont encore
            // vivantes et leurs champs sont lisibles ; après rollback elles
            // seraient détachées/videées.
            return movementMapper.toResolutionDto(result, turn, names);
        } finally {
            txManager.rollback(status);
        }
    }

    /**
     * Applique la résolution : déplace les entités, marque les ordres
     * RESOLVED/BLOCKED et persiste. Le compte-rendu renvoyé reflète l'état
     * désormais persisté.
     */
    @Transactional
    public MovementResolutionResultDto resolveMovements(int turn) {
        Board board = loadBoard();
        MovementResolutionResult result = movementService.resolveAllMovements(turn, board);
        Function<Long, String> names = resolveNames(collectPlayerIds(result));
        return movementMapper.toResolutionDto(result, turn, names);
    }

    // ============================
    // === Helpers internes ===
    // ============================

    private Board loadBoard() {
        return boardRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun plateau trouvé pour résoudre les mouvements"));
    }

    /**
     * Collecte tous les playerIds présents dans le résultat (ordres + conflits)
     * pour résoudre les noms en un seul batch.
     */
    private Set<Long> collectPlayerIds(MovementResolutionResult result) {
        Set<Long> ids = new HashSet<>();
        result.getResolved().forEach(o -> ids.add(o.getPlayerId()));
        result.getBlocked().forEach(o -> ids.add(o.getPlayerId()));
        result.getConflicts().forEach(c -> {
            ids.add(c.attackerPlayerId());
            ids.add(c.defenderPlayerId());
        });
        return ids;
    }

    private Set<Long> collectPlayerIds(Collection<MovementOrder> orders) {
        Set<Long> ids = new HashSet<>();
        orders.forEach(o -> ids.add(o.getPlayerId()));
        return ids;
    }

    /**
     * Résout playerId → nom en un seul {@link PlayerRepository#findAllById}.
     * Les IDs inconnus ressortent null (ne fait pas échouer le rapport).
     */
    private Function<Long, String> resolveNames(Set<Long> playerIds) {
        if (playerIds.isEmpty()) {
            return id -> null;
        }
        Map<Long, String> names = playerRepository.findAllById(playerIds).stream()
                .collect(Collectors.toMap(Player::getId, Player::getName, (a, b) -> a));
        return names::get;
    }
}