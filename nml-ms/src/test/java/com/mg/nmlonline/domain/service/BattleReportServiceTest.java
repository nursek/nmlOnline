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

        service.saveDuelReport(7, 32, List.of(attacker), List.of(defender), result);

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

    @Test
    @DisplayName("Le rapport d'impasse conserve les 3 camps et l'élimination de chacun")
    void standoffReportKeepsEveryCamp() {
        Player premier = new Player("Cegorach");
        premier.setId(1L);
        Player deuxieme = new Player("Imotekh");
        deuxieme.setId(2L);
        Player troisieme = new Player("Lurio");
        troisieme.setId(3L);

        CombatService.StandoffBattleResult result = new CombatService.StandoffBattleResult(
                true, "Impasse terminée", premier, 1,
                List.of(
                        new CombatService.StandoffBattleResult.PlayerOutcome(1L, 0, 1, false, false),
                        new CombatService.StandoffBattleResult.PlayerOutcome(2L, 2, 0, true, true),
                        new CombatService.StandoffBattleResult.PlayerOutcome(3L, 1, 0, false, true)),
                List.of(new CombatService.CasualtyInfo(2L, "Brute", "INFANTRY", UnitType.LARBIN, 2, 3.0)),
                List.of(),
                List.of());

        service.saveStandoffReport(12, 32, List.of(premier, deuxieme, troisieme), result);

        ArgumentCaptor<BattleReport> captor = ArgumentCaptor.forClass(BattleReport.class);
        verify(reportRepository).save(captor.capture());
        BattleReport saved = captor.getValue();
        assertTrue(saved.isStandoff());
        assertEquals(12, saved.getTurn());
        assertEquals(Set.of(1L, 2L, 3L), saved.getParticipantIds());

        when(playerRepository.findByUserId(99L)).thenReturn(Optional.of(deuxieme));
        when(reportRepository.findByParticipantId(2L)).thenReturn(List.of(saved));

        BattleReportDto dto = service.getReportsForUser(99L).getFirst();

        assertTrue(dto.isStandoff());
        assertEquals("Cegorach", dto.getWinnerName());
        assertEquals(3, dto.getCamps().size());
        assertEquals(1L, dto.getCamps().getFirst().getPlayerId());
        assertFalse(dto.getCamps().getFirst().isEliminated());
        assertTrue(dto.getCamps().get(1).isEliminated());
        assertEquals("Imotekh", dto.getCamps().get(1).getPlayerName());
        assertTrue(dto.getCamps().get(1).isCharacterLost());
        assertEquals(1, dto.getCasualties().size());
        assertEquals("Imotekh", dto.getCasualties().getFirst().getPlayerName());
    }
}
