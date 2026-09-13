package com.mg.nmlonline.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.api.dto.BattleReportDto;
import com.mg.nmlonline.domain.model.battle.BattleReport;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.infrastructure.repository.BattleReportRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BattleReportService — sérialisation JSON et lecture par participant")
class BattleReportServiceTest {

    @Mock
    private BattleReportRepository reportRepository;

    @Mock
    private PlayerRepository playerRepository;

    private BattleReportService service;

    @BeforeEach
    void setUp() {
        service = new BattleReportService(reportRepository, playerRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("Le rapport sauvegardé est relu à l'identique par un joueur impliqué")
    void savedReportIsReadBackByParticipant() {
        Player attacker = new Player("Attaquant");
        attacker.setId(1L);
        Player defender = new Player("Defenseur");
        defender.setId(2L);

        CombatService.SectorBattleResult result = new CombatService.SectorBattleResult(
                true, "Bataille terminée", List.of(), List.of(), List.of(), List.of(),
                attacker, 1, false, true, false,
                List.of(new CombatService.CasualtyInfo(2L, "Heros", "CHARACTER", null, null, null)),
                List.of(new CombatService.ExperienceGain(1L, 10L, 3, UnitType.LARBIN, 1.0, 2.0,
                        UnitType.VOYOU, 3.0)),
                List.of());

        service.saveDuelReport(7, 32, attacker, defender, result);

        ArgumentCaptor<BattleReport> captor = ArgumentCaptor.forClass(BattleReport.class);
        verify(reportRepository).save(captor.capture());
        BattleReport saved = captor.getValue();
        assertEquals(7, saved.getTurn());
        assertEquals(32, saved.getSectorNumber());
        assertEquals(attacker.getId(), saved.getWinnerPlayerId());
        assertEquals(Set.of(1L, 2L), saved.getParticipantIds());
        assertTrue(saved.getPayload().contains("VOYOU"), "Le payload JSON est sérialisé");

        when(playerRepository.findByUserId(99L)).thenReturn(Optional.of(defender));
        when(reportRepository.findByParticipantId(2L)).thenReturn(List.of(saved));

        List<BattleReportDto> reports = service.getReportsForUser(99L);

        assertEquals(1, reports.size());
        BattleReportDto dto = reports.getFirst();
        assertEquals(7, dto.getTurn());
        assertEquals(32, dto.getSectorNumber());
        assertEquals("Attaquant", dto.getWinnerName());
        assertEquals(2, dto.getCamps().size());
        assertEquals(1, dto.getCasualties().size());
        assertEquals("Heros", dto.getCasualties().getFirst().getLabel());
        assertEquals("Defenseur", dto.getCasualties().getFirst().getPlayerName());
        assertEquals(1, dto.getExperienceGains().size());
        assertEquals("LARBIN", dto.getExperienceGains().getFirst().getTypeBefore());
        assertEquals("VOYOU", dto.getExperienceGains().getFirst().getTypeAfter());
        assertEquals("Attaquant", dto.getExperienceGains().getFirst().getPlayerName());
    }
}
