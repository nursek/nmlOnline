package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.AdminAuditEntryDto;
import com.mg.nmlonline.domain.model.audit.AdminAuditAction;
import com.mg.nmlonline.domain.model.audit.AdminAuditLog;
import com.mg.nmlonline.infrastructure.repository.AdminAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AdminAuditService {

    private final AdminAuditLogRepository auditLogRepository;
    private final TurnService turnService;

    public AdminAuditService(AdminAuditLogRepository auditLogRepository,
                             TurnService turnService) {
        this.auditLogRepository = auditLogRepository;
        this.turnService = turnService;
    }

    @Transactional
    public void record(Long playerId, AdminAuditAction action, String details) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUsername(truncate(adminUsername(), 100));
        log.setPlayerId(playerId);
        log.setAction(action);
        log.setDetails(truncate(details, 2000));
        log.setCreatedAt(Instant.now());
        log.setTurn(turnService.getCurrentTurn());
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditEntryDto> getHistory(Long playerId, Pageable pageable) {
        Page<AdminAuditLog> page = playerId == null
                ? auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                : auditLogRepository.findByPlayerIdOrderByCreatedAtDesc(playerId, pageable);
        return page.map(this::toDto);
    }

    private AdminAuditEntryDto toDto(AdminAuditLog log) {
        AdminAuditEntryDto dto = new AdminAuditEntryDto();
        dto.setId(log.getId());
        dto.setAdminUsername(log.getAdminUsername());
        dto.setPlayerId(log.getPlayerId());
        dto.setAction(log.getAction().name());
        dto.setDetails(log.getDetails());
        dto.setCreatedAt(log.getCreatedAt());
        dto.setTurn(log.getTurn());
        return dto;
    }

    private String adminUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "system";
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }
}
