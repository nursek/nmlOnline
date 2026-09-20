package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.action.PlayerAction;
import com.mg.nmlonline.domain.model.action.PlayerActionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerActionRepository extends JpaRepository<PlayerAction, Long> {

    List<PlayerAction> findByPlayerIdAndTurnAndStatusOrderByIdAsc(Long playerId, int turn, PlayerActionStatus status);

    List<PlayerAction> findByTurnAndStatus(int turn, PlayerActionStatus status);

    List<PlayerAction> findByPlayerIdAndTurnAndStatusAndIdGreaterThanEqualOrderByIdAsc(
            Long playerId, int turn, PlayerActionStatus status, Long id);

    Optional<PlayerAction> findByIdAndPlayerId(Long id, Long playerId);

    void deleteByPlayerId(Long playerId);
}
