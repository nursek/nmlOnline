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
 * Point d'entrée admin pour les ordres de déplacement d'un tour : consultation,
 * aperçu non mutant et application mutante.
 *
 * <p>L'aperçu ré-exécute {@link MovementService#resolveAllMovements} dans une
 * transaction {@link TransactionDefinition#PROPAGATION_REQUIRES_NEW
 * REQUIRES_NEW} roulée en arrière explicitement après construction du DTO —
 * évite rollback-only silencieux et duplication de l'algo de simulation.</p>
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

    /** Aperçu (dry-run) : calcule les conflits sans persister, via rollback de transaction REQUIRES_NEW. */
    public MovementResolutionResultDto previewMovements(int turn) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = txManager.getTransaction(def);
        try {
            Board board = loadBoard();
            MovementResolutionResult result = movementService.resolveAllMovements(turn, board);
            Function<Long, String> names = resolveNames(collectPlayerIds(result));
            // Construire le DTO AVANT le rollback : après, les entités seraient détachées.
            return movementMapper.toResolutionDto(result, turn, names);
        } finally {
            txManager.rollback(status);
        }
    }

    @Transactional
    public MovementResolutionResultDto resolveMovements(int turn) {
        Board board = loadBoard();
        MovementResolutionResult result = movementService.resolveAllMovements(turn, board);
        Function<Long, String> names = resolveNames(collectPlayerIds(result));
        return movementMapper.toResolutionDto(result, turn, names);
    }

    private Board loadBoard() {
        return boardRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun plateau trouvé pour résoudre les mouvements"));
    }

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

    /** IDs inconnus ressortent null (ne fait pas échouer le rapport). */
    private Function<Long, String> resolveNames(Set<Long> playerIds) {
        if (playerIds.isEmpty()) {
            return id -> null;
        }
        Map<Long, String> names = playerRepository.findAllById(playerIds).stream()
                .collect(Collectors.toMap(Player::getId, Player::getName, (a, b) -> a));
        return names::get;
    }
}