package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.alliance.Alliance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AllianceRepository extends JpaRepository<Alliance, Long> {

    @Query("select a from Alliance a where a.endedTurn is null "
            + "and (a.playerOneId = :playerId or a.playerTwoId = :playerId)")
    List<Alliance> findActiveForPlayer(@Param("playerId") Long playerId);

    @Query("select a from Alliance a where a.endedTurn is null")
    List<Alliance> findAllActive();

    @Query("select a from Alliance a where a.endedTurn is null "
            + "and ((a.playerOneId = :first and a.playerTwoId = :second) "
            + "or (a.playerOneId = :second and a.playerTwoId = :first))")
    Optional<Alliance> findActiveBetween(@Param("first") Long first, @Param("second") Long second);

    @Query("select a from Alliance a where a.betrayal = true and a.endedTurn = :turn")
    List<Alliance> findBetrayalsAtTurn(@Param("turn") int turn);

    @Query("select a from Alliance a where a.endedTurn = :turn")
    List<Alliance> findEndedAtTurn(@Param("turn") int turn);

    /** UPDATE conditionnel : une rupture acceptée en parallèle ne peut pas être écrasée par une trahison. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Alliance a set a.endedTurn = :turn, a.endedByPlayerId = :breakerId, a.betrayal = true "
            + "where a.id = :id and a.endedTurn is null")
    int endAsBetrayal(@Param("id") Long id, @Param("turn") int turn, @Param("breakerId") Long breakerId);
}
