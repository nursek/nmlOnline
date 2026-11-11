package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.sniper.SniperOrder;
import com.mg.nmlonline.domain.model.sniper.SniperOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SniperOrderRepository extends JpaRepository<SniperOrder, Long> {

    List<SniperOrder> findByPlayerIdAndTurnAndStatus(Long playerId, int turn, SniperOrderStatus status);

    List<SniperOrder> findByTurnAndStatus(int turn, SniperOrderStatus status);

    boolean existsByUnitIdAndTurnAndStatus(Long unitId, int turn, SniperOrderStatus status);

    /** IDs d'unités déjà engagées dans un ordre sniper PENDING du tour — sert à refuser les doublons. */
    @Query("""
            select o.unitId from SniperOrder o
            where o.turn = :turn
              and o.status = com.mg.nmlonline.domain.model.sniper.SniperOrderStatus.PENDING
              and o.unitId in :unitIds
            """)
    List<Long> findPendingUnitIds(@Param("turn") int turn,
                                  @Param("unitIds") Collection<Long> unitIds);
}
