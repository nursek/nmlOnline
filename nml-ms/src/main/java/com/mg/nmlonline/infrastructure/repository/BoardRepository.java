package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.board.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    Optional<Board> findByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Board b where b.id = :id")
    Optional<Board> findByIdForUpdate(@Param("id") Long id);

    // Ordre stable (id croissant) : le plateau historique reste « le premier » malgré les merges.
    @Override
    @Query("select b from Board b order by b.id asc")
    List<Board> findAll();
}
