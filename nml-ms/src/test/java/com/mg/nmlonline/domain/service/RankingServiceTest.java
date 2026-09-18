package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.RankingEntryDto;
import com.mg.nmlonline.api.dto.RankingsDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankingService")
class RankingServiceTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private BoardRepository boardRepository;

    private RankingService rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new RankingService(playerRepository, boardRepository, new PlayerStatsService());
    }

    @Test
    @DisplayName("Avant le tour 2 : classements vides")
    void noRankingsBeforeTurnTwo() {
        Board board = new Board();
        board.addSector(new Sector(1, "Secteur 1"));
        board.setCurrentTurn(1);
        when(boardRepository.findAll()).thenReturn(List.of(board));

        RankingsDto rankings = rankingService.getRankings();

        assertEquals(1, rankings.getTurn());
        assertTrue(rankings.getMilitary().isEmpty());
        assertTrue(rankings.getEconomic().isEmpty());
    }

    @Test
    @DisplayName("Tour 2 : militaire trié par globalPower, économique par totalEconomyPower")
    void ranksPlayerByPower() {
        Board board = new Board();
        board.addSector(new Sector(1, "Secteur 1"));
        board.addSector(new Sector(2, "Secteur 2"));
        board.assignOwner(1, 1L, "#ff0000");
        board.assignOwner(2, 2L, "#0000ff");
        board.getSector(1).addUnit(new Unit(9.0, UnitClass.TIREUR));
        board.getSector(1).addUnit(new Unit(9.0, UnitClass.TIREUR));
        board.getSector(2).addUnit(new Unit(9.0, UnitClass.TIREUR));
        board.setCurrentTurn(2);

        Player alice = new Player("Alice");
        alice.setId(1L);
        alice.setRankingComment("Flanc gauche fragile");
        Player bob = new Player("Bob");
        bob.setId(2L);
        bob.getStats().setMoney(500_000.0);

        when(boardRepository.findAll()).thenReturn(List.of(board));
        when(playerRepository.findAll()).thenReturn(List.of(bob, alice));

        RankingsDto rankings = rankingService.getRankings();

        assertEquals(2, rankings.getTurn());
        assertEquals(List.of("Alice", "Bob"), names(rankings.getMilitary()));
        assertEquals("Flanc gauche fragile", rankings.getMilitary().getFirst().getComment());
        assertEquals(List.of("Bob", "Alice"), names(rankings.getEconomic()));
    }

    @Test
    @DisplayName("Puissances égales : départage alphabétique")
    void tiesAreBrokenByName() {
        Board board = new Board();
        board.addSector(new Sector(1, "Secteur 1"));
        board.setCurrentTurn(2);

        Player zoe = new Player("Zoe");
        zoe.setId(1L);
        Player anne = new Player("Anne");
        anne.setId(2L);

        when(boardRepository.findAll()).thenReturn(List.of(board));
        when(playerRepository.findAll()).thenReturn(List.of(zoe, anne));

        RankingsDto rankings = rankingService.getRankings();

        assertEquals(List.of("Anne", "Zoe"), names(rankings.getMilitary()));
        assertEquals(List.of("Anne", "Zoe"), names(rankings.getEconomic()));
    }

    @Test
    @DisplayName("Indice : trim, vidage en null, joueur inconnu rejeté")
    void updateRankingCommentNormalizesAndRejectsUnknownPlayer() {
        Player alice = new Player("Alice");
        alice.setId(1L);
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(alice));
        when(playerRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());

        rankingService.updateRankingComment(1L, "  Flanc gauche  ");
        assertEquals("Flanc gauche", alice.getRankingComment());

        rankingService.updateRankingComment(1L, "   ");
        assertNull(alice.getRankingComment());

        assertThrows(IllegalArgumentException.class, () -> rankingService.updateRankingComment(9L, "x"));
    }

    private static List<String> names(List<RankingEntryDto> entries) {
        return entries.stream().map(RankingEntryDto::getName).toList();
    }
}
