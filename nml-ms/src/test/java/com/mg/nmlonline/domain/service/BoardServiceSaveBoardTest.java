package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Caractérisation du safeguard {@link BoardService#saveBoard} : un re-import d'un
 * board.json neutre NE doit JAMAIS réinitialiser l'appartenance (owner_id) ni
 * l'armée des secteurs déjà présents en base. C'était le bug prod : le boot import
 * faisait getSectorsList().clear() → cascade DELETE des secteurs + armées →
 * joueurs plus assignés à leur quartier après chaque redémarrage.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BoardService.saveBoard — re-import non destructif (préserve owner_id)")
class BoardServiceSaveBoardTest {

    @Mock
    BoardRepository boardRepository;

    @InjectMocks
    BoardService boardService;

    @Test
    @DisplayName("re-import d'un board neutre sur un board existant possédé → owner_id et armée préservés")
    void reImportNeutralBoardPreservesOwnership() {
        // Board existant en base : secteur 13 appartenant au joueur 42, avec une unité.
        Board existing = new Board();
        existing.setName("Carte Principale");
        Sector ownedSector = new Sector(13, "Quartier Lurio");
        ownedSector.setOwnerAndColor(42L, "#ff0000");
        existing.addSector(ownedSector);

        // Board.json re-importé : secteur 13 NEUTRE (ownerId null), sans armée.
        Board incoming = new Board();
        Sector neutralSector = new Sector(13, "Quartier Lurio");
        incoming.addSector(neutralSector);

        when(boardRepository.findByName("Carte Principale")).thenReturn(Optional.of(existing));
        when(boardRepository.save(existing)).thenReturn(existing);

        Board result = boardService.saveBoard(incoming, "Carte Principale");

        assertSame(existing, result, "doit renvoyer le board existant (upsert), pas en créer un nouveau");
        assertEquals(1, existing.getSectorsList().size(), "le secteur existant ne doit pas être supprimé puis recréé");
        Sector s = existing.getSector(13);
        assertEquals(42L, s.getOwnerId(), "owner_id préservé — c'était le bug prod");
        assertEquals("#ff0000", s.getColor(), "couleur préservée");
        verify(boardRepository, never()).delete(any());
        verify(boardRepository).save(existing);
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
        incoming.addSector(new Sector(13, "Quartier Lurio"));   // déjà présent
        incoming.addSector(new Sector(99, "Nouveau Secteur"));   // nouveau dans le JSON

        when(boardRepository.findByName("Carte Principale")).thenReturn(Optional.of(existing));
        when(boardRepository.save(existing)).thenReturn(existing);

        boardService.saveBoard(incoming, "Carte Principale");

        assertEquals(2, existing.getSectorsList().size());
        assertNotNull(existing.getSector(99), "nouveau secteur ajouté");
        assertEquals(42L, existing.getSector(13).getOwnerId(), "secteur existant préservé");
    }

    @Test
    @DisplayName("premier import (board inexistant) → création simple, pas de fusion")
    void firstImportCreatesBoard() {
        Board incoming = new Board();
        incoming.addSector(new Sector(1, "Secteur 1"));

        when(boardRepository.findByName("Carte Principale")).thenReturn(Optional.empty());
        when(boardRepository.save(incoming)).thenReturn(incoming);

        Board result = boardService.saveBoard(incoming, "Carte Principale");

        assertSame(incoming, result);
        assertEquals("Carte Principale", incoming.getName());
    }
}