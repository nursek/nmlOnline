package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovementOrderRepository extends JpaRepository<MovementOrder, Long> {

    List<MovementOrder> findByTurnAndStatus(int turn, MovementStatus status);

    List<MovementOrder> findByTurn(int turn);

    default List<MovementOrder> findPendingByTurn(int turn) {
        return findByTurnAndStatus(turn, MovementStatus.PENDING);
    }

    List<MovementOrder> findByPlayerIdAndTurn(Long playerId, int turn);

    List<MovementOrder> findByPlayerIdAndTurnAndStatus(Long playerId, int turn, MovementStatus status);

    List<MovementOrder> findByVehicleIdAndTurnAndStatus(Long vehicleId, int turn, MovementStatus status);
}
