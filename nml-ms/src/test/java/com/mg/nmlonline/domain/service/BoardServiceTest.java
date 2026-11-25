package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BoardDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.mapper.BoardMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoardService")
class BoardServiceTest {

    @Mock
    BoardRepository boardRepository;
    @Mock
    BoardMapper boardMapper;
    @Mock
    PlayerRepository playerRepository;
    @Mock
    AllianceGraph allianceGraph;
    @InjectMocks
    BoardService boardService;

    @Test
    @DisplayName("Création : nom absent ou déjà pris refusé")
    void createBoardValidatesName() {
        BoardDto blank = new BoardDto();
        assertThrows(IllegalArgumentException.class, () -> boardService.createBoardDto(blank));

        BoardDto duplicate = new BoardDto();
        duplicate.setName("Carte Principale");
        when(boardRepository.findByName("Carte Principale")).thenReturn(Optional.of(new Board()));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> boardService.createBoardDto(duplicate));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    @DisplayName("re-import d'un board neutre sur un board possédé → owner_id et armée préservés")
    void reImportNeutralBoardPreservesOwnership() {
        Board existing = new Board();
        existing.setName("Carte Principale");
        Sector ownedSector = new Sector(13, "Quartier Lurio");
        ownedSector.setOwnerAndColor(42L, "#ff0000");
        ownedSector.addUnit(new Unit(5.0, UnitClass.TIREUR));
        existing.addSector(ownedSector);

        Board incoming = new Board();
        incoming.addSector(new Sector(13, "Quartier Lurio"));

        when(boardRepository.findByName("Carte Principale")).thenReturn(Optional.of(existing));
        when(boardRepository.save(existing)).thenReturn(existing);

        Board result = boardService.saveBoard(incoming, "Carte Principale");

        assertSame(existing, result, "doit renvoyer le board existant (upsert), pas en créer un nouveau");
        Sector sector = existing.getSector(13);
        assertEquals(42L, sector.getOwnerId(), "owner_id préservé — c'était le bug prod");
        assertEquals("#ff0000", sector.getColor());
        assertEquals(1, sector.getArmySize(), "armée préservée — la cascade supprimait les unités");
        verify(boardRepository, never()).delete(any());
    }

    @Test
    @DisplayName("re-import ajoute les nouveaux secteurs du JSON sans toucher aux existants")
    void reImportAddsNewSectors() {
        Board existing = new Board();
        existing.setName("Carte Principale");
        Sector owned = new Sector(13, "Quartier Lurio");
        owned.setOwnerAndColor(42L, "#ff0000");
        existing.addSector(owned);

        Board incoming = new Board();
        incoming.addSector(new Sector(13, "Quartier Lurio"));
        incoming.addSector(new Sector(99, "Nouveau Secteur"));

        when(boardRepository.findByName("Carte Principale")).thenReturn(Optional.of(existing));
        when(boardRepository.save(existing)).thenReturn(existing);

        boardService.saveBoard(incoming, "Carte Principale");

        assertEquals(2, existing.getSectorsList().size());
        assertEquals(42L, existing.getSector(13).getOwnerId());
    }

    @Test
    @DisplayName("premier import : nom posé, id fourni ignoré, tour 0")
    void firstImportCreatesBoardAtTurnZero() {
        Board incoming = new Board();
        incoming.setId(99L);
        incoming.addSector(new Sector(1, "Secteur 1"));

        when(boardRepository.findByName("Carte Principale")).thenReturn(Optional.empty());
        when(boardRepository.save(incoming)).thenReturn(incoming);

        Board result = boardService.saveBoard(incoming, "Carte Principale");

        assertSame(incoming, result);
        assertEquals("Carte Principale", incoming.getName());
        assertEquals(0, incoming.getCurrentTurn());
        assertNull(incoming.getId(), "un board créé ne doit jamais réutiliser un id fourni");
    }

    @Test
    @DisplayName("Assignation d'un secteur : joueur de la partie accepté, joueur d'une autre partie refusé")
    void assignOwnerIsScopedToTheBoard() {
        Board board = new Board();
        board.setId(1L);
        board.addSector(new Sector(5, "Secteur 5"));
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        Player local = new Player("Local");
        local.setId(42L);
        local.setBoardId(1L);
        Player foreign = new Player("Etranger");
        foreign.setId(43L);
        foreign.setBoardId(2L);
        when(playerRepository.findById(42L)).thenReturn(Optional.of(local));
        when(playerRepository.findById(43L)).thenReturn(Optional.of(foreign));

        assertTrue(boardService.assignOwnerToSector(1L, 5, 42L, "#00ff00"));
        assertEquals(42L, board.getSector(5).getOwnerId());

        assertFalse(boardService.assignOwnerToSector(1L, 5, 43L, "#ff0000"),
                "Un joueur d'une autre partie ne peut pas recevoir un secteur");
        assertEquals(42L, board.getSector(5).getOwnerId());

        assertFalse(boardService.assignOwnerToSector(1L, 999, 42L, "#00ff00"));
        assertFalse(boardService.assignOwnerToSector(2L, 5, 42L, "#00ff00"));
    }
}
