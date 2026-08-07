package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour les ordres de déplacement.
 */
@Repository
public interface MovementOrderRepository extends JpaRepository<MovementOrder, Long> {

    /**
     * Tous les ordres d'un tour donné avec un statut donné.
     */
    List<MovementOrder> findByTurnAndStatus(int turn, MovementStatus status);

    /**
     * Tous les ordres d'un tour donné (tout statut confondu) — vue admin.
     */
    List<MovementOrder> findByTurn(int turn);

    /**
     * Tous les ordres PENDING d'un tour.
     */
    default List<MovementOrder> findPendingByTurn(int turn) {
        return findByTurnAndStatus(turn, MovementStatus.PENDING);
    }

    /**
     * Tous les ordres d'un joueur pour un tour donné.
     */
    List<MovementOrder> findByPlayerIdAndTurn(Long playerId, int turn);

    /**
     * Tous les ordres PENDING d'un joueur pour un tour donné.
     */
    List<MovementOrder> findByPlayerIdAndTurnAndStatus(Long playerId, int turn, MovementStatus status);

    /**
     * Tous les ordres concernant un véhicule donné.
     */
    List<MovementOrder> findByVehicleIdAndTurnAndStatus(Long vehicleId, int turn, MovementStatus status);
}
