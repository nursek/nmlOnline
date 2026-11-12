package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.board.BoardAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardAdminRepository extends JpaRepository<BoardAdmin, BoardAdmin.BoardAdminId> {

    boolean existsByBoardIdAndUserId(Long boardId, Long userId);

    List<BoardAdmin> findByUserId(Long userId);

    List<BoardAdmin> findByBoardId(Long boardId);
}
