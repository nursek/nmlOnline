package com.mg.nmlonline.domain.service;

import tools.jackson.databind.json.JsonMapper;
import com.mg.nmlonline.api.dto.BattleHighlightDto;
import com.mg.nmlonline.api.dto.BattleReportDetailDto;
import com.mg.nmlonline.api.dto.BattleReportDto;
import com.mg.nmlonline.domain.model.battle.BattleLogEntry;
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
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BattleReportService — sérialisation JSON et lecture par participant")
class BattleReportServiceTest {

    private static final Long BOARD_ID = 1L;

    @Mock
    private BattleReportRepository reportRepository;

    @Mock
    private PlayerRepository playerRepository;

    private BattleReportService service;

    @BeforeEach
    void setUp() {
        service = new BattleReportService(reportRepository, playerRepository, JsonMapper.builder().build());
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
                List.of(), List.of(), List.of());

        service.saveDuelReport(BOARD_ID, 7, 32, List.of(attacker), List.of(defender), result);

        ArgumentCaptor<BattleReport> captor = ArgumentCaptor.forClass(BattleReport.class);
        verify(reportRepository).save(captor.capture());
        BattleReport saved = captor.getValue();
        assertEquals(7, saved.getTurn());
        assertEquals(32, saved.getSectorNumber());
        assertEquals(attacker.getId(), saved.getWinnerPlayerId());
        assertEquals(Set.of(1L, 2L), saved.getParticipantIds());
        assertTrue(saved.getPayload().contains("VOYOU"), "Le payload JSON est sérialisé");

        when(playerRepository.findByUserIdAndBoardId(99L, BOARD_ID)).thenReturn(Optional.of(defender));
        when(reportRepository.findByParticipantId(eq(2L), any(Pageable.class))).thenReturn(List.of(saved));

        List<BattleReportDto> reports = service.getReportsForUser(99L, BOARD_ID);

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
                List.of(), List.of(), List.of());

        service.saveStandoffReport(BOARD_ID, 12, 32, List.of(premier, deuxieme, troisieme), result);

        ArgumentCaptor<BattleReport> captor = ArgumentCaptor.forClass(BattleReport.class);
        verify(reportRepository).save(captor.capture());
        BattleReport saved = captor.getValue();
        assertTrue(saved.isStandoff());
        assertEquals(12, saved.getTurn());
        assertEquals(Set.of(1L, 2L, 3L), saved.getParticipantIds());

        when(playerRepository.findByUserIdAndBoardId(99L, BOARD_ID)).thenReturn(Optional.of(deuxieme));
        when(reportRepository.findByParticipantId(eq(2L), any(Pageable.class))).thenReturn(List.of(saved));

        BattleReportDto dto = service.getReportsForUser(99L, BOARD_ID).getFirst();

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

    @Test
    @DisplayName("Le payload v2 garde forces et points clés pour l'admin, la vue joueur ne les expose pas")
    void playerViewDropsAdminDetail() {
        Player attacker = new Player("Attaquant");
        attacker.setId(1L);
        Player defender = new Player("Defenseur");
        defender.setId(2L);

        BattleReportDto.PlayerForceDto force = new BattleReportDto.PlayerForceDto();
        force.setPlayerId(1L);
        force.setPlayerName("Attaquant");
        force.setSide("ATTAQUANT");

        CombatService.SectorBattleResult result = new CombatService.SectorBattleResult(
                true, "Bataille terminée", List.of(), List.of(), List.of(), List.of(),
                attacker, 0, false, false, false,
                List.of(), List.of(),
                List.of(new BattleLogEntry("PDF", BattleLogEntry.DAMAGE, "jet secret 42")),
                List.of(force),
                List.of(highlight("CHARACTER_LOST", BattleHighlights.PUBLIC, "Personnage perdu : Defenseur"),
                        highlight("CRIT", BattleHighlights.ADMIN, "2 coups critiques")));

        service.saveDuelReport(11L, 7, 32, List.of(attacker), List.of(defender), result);

        ArgumentCaptor<BattleReport> captor = ArgumentCaptor.forClass(BattleReport.class);
        verify(reportRepository).save(captor.capture());
        BattleReportDetailDto detail = service.readPayload(captor.getValue());
        assertEquals(2, detail.getSchemaVersion());
        assertEquals("ASSAUT", detail.getKind());
        assertEquals(1, detail.getForces().size());
        assertEquals(2, detail.getHighlights().size());
        assertFalse(JsonMapper.builder().build().writeValueAsString(detail).contains("jet secret"),
                "Le déroulé n'est pas persisté dans le rapport, le journal du tour le porte");

        BattleReportDto playerView = service.toPlayerReport(detail);
        assertEquals("ASSAUT", playerView.getKind());
        assertEquals(1, playerView.getForces().size());
        assertEquals(1, playerView.getHighlights().size(), "Seuls les points clés publics sont projetés");
        assertEquals("Personnage perdu : Defenseur", playerView.getHighlights().getFirst().getMessage());

        String json = JsonMapper.builder().build().writeValueAsString(playerView);
        assertFalse(json.contains("battleLog"), "La vue joueur ne contient pas le déroulé");
        assertFalse(json.contains("jet secret"), "La vue joueur ne contient pas les messages admin");
        assertFalse(json.contains("coups critiques"), "La vue joueur ne contient pas les points clés admin");
        assertFalse(json.contains("stats"), "La vue joueur ne contient pas les statistiques");
        assertFalse(json.contains("equipments"), "La vue joueur ne contient pas les équipements");
    }

    private static BattleHighlightDto highlight(String kind, String visibility, String message) {
        BattleHighlightDto highlight = new BattleHighlightDto();
        highlight.setKind(kind);
        highlight.setPriority(1);
        highlight.setVisibility(visibility);
        highlight.setMessage(message);
        return highlight;
    }

    @Test
    @DisplayName("Le tir à distance renvoie le détail destiné au journal")
    void rangedReportReturnsDetail() {
        Player shooter = new Player("Sniper");
        shooter.setId(1L);

        BattleReportDetailDto detail = service.saveRangedReport(11L, 5, 15, List.of(shooter),
                List.of(new CombatService.CasualtyInfo(2L, "Brute", "INFANTRY", UnitType.LARBIN, 1, 1.0)),
                List.of(), "2 cible(s) touchée(s)", Map.of());

        assertNotNull(detail);
        assertEquals("TIR", detail.getKind());
        assertEquals(2, detail.getSchemaVersion());
        assertEquals(15, detail.getSectorNumber());
        assertEquals(1, detail.getCasualties().size());
    }

    @Test
    @DisplayName("Un rapport v1 archivé reste lisible et projeté sans déroulé")
    void legacyPayloadStaysReadable() {
        BattleReportDto legacy = new BattleReportDto();
        legacy.setTurn(3);
        legacy.setSectorNumber(12);
        BattleReportDto.CampDto camp = new BattleReportDto.CampDto();
        camp.setPlayerId(1L);
        camp.setPlayerName("Ancien");
        legacy.setCamps(List.of(camp));

        BattleReport report = new BattleReport();
        report.setPayload(JsonMapper.builder().build().writeValueAsString(legacy));

        BattleReportDetailDto detail = service.readPayload(report);
        assertEquals(1, detail.getSchemaVersion());
        assertEquals("ASSAUT", detail.getKind());
        assertEquals(3, detail.getTurn());
        assertEquals(12, detail.getSectorNumber());
        assertEquals("Ancien", detail.getCamps().getFirst().getPlayerName());

        BattleReportDto playerView = service.toPlayerReport(detail);
        assertEquals(3, playerView.getTurn());
        assertEquals("Ancien", playerView.getCamps().getFirst().getPlayerName());
        assertNull(playerView.getForces());
    }
}
