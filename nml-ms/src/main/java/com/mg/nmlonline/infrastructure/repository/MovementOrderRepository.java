package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MovementOrderRepository extends JpaRepository<MovementOrder, Long> {

    List<MovementOrder> findByTurnAndStatus(int turn, MovementStatus status);

    /** IDs d'entités déjà engagées dans un ordre PENDING du tour — sert à refuser les doublons. */
    @Query("""
            select distinct e from MovementOrder o
            join o.entityIds e
            where o.turn = :turn
              and o.status = com.mg.nmlonline.domain.model.movement.MovementStatus.PENDING
              and e in :entityIds
            """)
    List<Long> findPendingEntityIds(@Param("turn") int turn,
                                    @Param("entityIds") Collection<Long> entityIds);

    List<MovementOrder> findByTurn(int turn);

    default List<MovementOrder> findPendingByTurn(int turn) {
        return findByTurnAndStatus(turn, MovementStatus.PENDING);
    }

    List<MovementOrder> findByPlayerIdAndTurn(Long playerId, int turn);

    List<MovementOrder> findByPlayerIdAndTurnAndStatus(Long playerId, int turn, MovementStatus status);

    List<MovementOrder> findByVehicleIdAndTurnAndStatus(Long vehicleId, int turn, MovementStatus status);
}
