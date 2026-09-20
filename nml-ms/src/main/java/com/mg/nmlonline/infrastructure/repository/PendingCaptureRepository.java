package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.alliance.PendingCapture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PendingCaptureRepository extends JpaRepository<PendingCapture, Long> {

    List<PendingCapture> findByResolvedFalseOrderByIdAsc();

    boolean existsByBoardIdAndSectorNumberAndTurnAndResolvedFalse(Long boardId, int sectorNumber, int turn);
}
