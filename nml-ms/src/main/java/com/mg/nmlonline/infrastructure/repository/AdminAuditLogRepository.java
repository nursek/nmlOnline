package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.audit.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    Page<AdminAuditLog> findByBoardIdAndPlayerIdOrderByCreatedAtDesc(Long boardId, Long playerId,
                                                                       Pageable pageable);

    Page<AdminAuditLog> findByBoardIdOrderByCreatedAtDesc(Long boardId, Pageable pageable);
}
