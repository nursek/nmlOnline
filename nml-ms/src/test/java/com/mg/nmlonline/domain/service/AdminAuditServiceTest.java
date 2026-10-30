package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.AdminAuditEntryDto;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EmbeddedPostgresTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("AdminAuditService — journal des mutations admin")
class AdminAuditServiceTest {

    @Autowired
    private AdminAuditService auditService;

    @Autowired
    private AdminPlayerEditService editService;

    @Autowired
    private PlayerRepository playerRepository;

    private Player player;

    @BeforeEach
    void setUp() {
        player = playerRepository.findByName("lurio")
                .orElseThrow(() -> new IllegalStateException("Fixture lurio absente"));
    }

    @Test
    @DisplayName("Une mutation admin écrit une entrée d'audit")
    void mutationRecordsAuditEntry() {
        editService.setMoney(player.getId(), 1234.5);

        Page<AdminAuditEntryDto> history = auditService.getHistory(
                player.getId(), PageRequest.of(0, 10));

        assertEquals(1, history.getTotalElements());
        AdminAuditEntryDto entry = history.getContent().getFirst();
        assertEquals("SET_MONEY", entry.getAction());
        assertEquals(player.getId(), entry.getPlayerId());
        assertTrue(entry.getDetails().contains("1234.5"));
        assertNotNull(entry.getCreatedAt());
        assertNotNull(entry.getAdminUsername());
    }

    @Test
    @DisplayName("Une mutation refusée n'écrit aucune entrée")
    void failedMutationDoesNotRecordAuditEntry() {
        assertThrows(IllegalArgumentException.class,
                () -> editService.removeResource(player.getId(), "Caisse d'armes", 100_000));

        Page<AdminAuditEntryDto> history = auditService.getHistory(
                player.getId(), PageRequest.of(0, 10));

        assertEquals(0, history.getTotalElements());
    }

    @Test
    @DisplayName("L'historique est paginé et filtré par joueur")
    void historyIsPaginated() {
        for (int i = 0; i < 25; i++) {
            editService.setMoney(player.getId(), i);
        }

        Page<AdminAuditEntryDto> first = auditService.getHistory(
                player.getId(), PageRequest.of(0, 10));
        Page<AdminAuditEntryDto> second = auditService.getHistory(
                player.getId(), PageRequest.of(1, 10));

        assertEquals(25, first.getTotalElements());
        assertEquals(10, first.getContent().size());
        assertEquals(10, second.getContent().size());
        assertEquals(25, auditService.getHistory(null, PageRequest.of(0, 100)).getTotalElements());
    }
}
