package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.action.PlayerAction;
import com.mg.nmlonline.domain.model.action.PlayerActionStatus;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerActionRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// Pas de ligne de journal : la collecte partage la transaction de l'incrément du tour, rien n'est annulable.
@Service
public class HarvestAutoCollector {

    private static final int FIRST_HARVEST_TURN = 2;

    private final PlayerActionRepository actionRepository;
    private final PlayerRepository playerRepository;
    private final BoardRepository boardRepository;
    private final EntityManager em;

    public HarvestAutoCollector(PlayerActionRepository actionRepository, PlayerRepository playerRepository,
                                BoardRepository boardRepository, EntityManager em) {
        this.actionRepository = actionRepository;
        this.playerRepository = playerRepository;
        this.boardRepository = boardRepository;
        this.em = em;
    }

    public boolean hasClaimedRevenue(int turn) {
        return boardRepository.findAll().stream()
                .findFirst()
                .map(Board::getRevenueClaimedTurn)
                .filter(claimed -> claimed == turn)
                .isPresent();
    }

    public void collectRemainingMoney(Board board, int turnEnding) {
        collectRemainingMoney(board, turnEnding, ownersBySector(board));
    }

    /** {@code sectorOwners} = propriétaires au début de la résolution (avant combats/déplacements). */
    public void collectRemainingMoney(Board board, int turnEnding, Map<Integer, Long> sectorOwners) {
        if (board == null || turnEnding < FIRST_HARVEST_TURN
                || Integer.valueOf(turnEnding).equals(board.getRevenueClaimedTurn())) {
            return;
        }
        // Aucun propriétaire au début de la résolution : le tour est quand même figé, sinon une capture en cours rouvrirait la récolte.
        if (sectorOwners == null || sectorOwners.isEmpty()) {
            board.setRevenueClaimedTurn(turnEnding);
            return;
        }

        List<Long> ownerIds = sectorOwners.values().stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        Map<Long, Player> locked = new LinkedHashMap<>();
        // Verrouillage AVANT lecture du journal : sinon une récolte en vol committerait après et le secteur serait crédité deux fois.
        for (Long ownerId : ownerIds) {
            playerRepository.findByIdForUpdate(ownerId).ifPresent(player -> locked.put(ownerId, player));
        }

        // Le board a pu être lu avant le commit d'une résolution concurrente : la marque se relit une fois les verrous joueurs acquis.
        List<Integer> claimed = em.createQuery(
                        "select b.revenueClaimedTurn from Board b where b.id = :boardId", Integer.class)
                .setParameter("boardId", board.getId())
                .getResultList();
        if (!claimed.isEmpty() && Integer.valueOf(turnEnding).equals(claimed.getFirst())) {
            return;
        }

        Set<Integer> harvestedSectors = new HashSet<>();
        for (PlayerAction action : actionRepository.findByTurnAndStatus(turnEnding, PlayerActionStatus.ACTIVE)) {
            if (action.getType().isHarvest() && action.getFromSectorNumber() != null) {
                harvestedSectors.add(action.getFromSectorNumber());
            }
        }

        Map<Long, Double> credits = new HashMap<>();
        for (Map.Entry<Integer, Long> entry : sectorOwners.entrySet()) {
            Long ownerId = entry.getValue();
            if (ownerId == null || !locked.containsKey(ownerId) || harvestedSectors.contains(entry.getKey())) {
                continue;
            }
            Sector sector = board.getSector(entry.getKey());
            if (sector != null) {
                credits.merge(ownerId, sector.getIncome(), Double::sum);
            }
        }
        credits.forEach((ownerId, amount) -> {
            Player player = locked.get(ownerId);
            player.incrementMoney(amount);
            playerRepository.save(player);
        });
        board.setRevenueClaimedTurn(turnEnding);
    }

    public static Map<Integer, Long> ownersBySector(Board board) {
        if (board == null) {
            return Map.of();
        }
        Map<Integer, Long> owners = new HashMap<>();
        for (Sector sector : board.getAllSectors()) {
            if (sector.getOwnerId() != null) {
                owners.put(sector.getNumber(), sector.getOwnerId());
            }
        }
        return owners;
    }
}
