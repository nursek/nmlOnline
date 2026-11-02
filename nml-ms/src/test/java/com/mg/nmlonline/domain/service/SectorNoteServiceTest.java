package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.config.TestDataInitializer;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@EmbeddedPostgresTest
@Transactional
@DisplayName("SectorNoteService — notes privées par joueur et secteur")
class SectorNoteServiceTest {

    @Autowired
    private SectorNoteService sectorNoteService;

    @Autowired
    private BoardService boardService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("une note est trimmée, invisible pour les autres joueurs, et supprimée si vide")
    void notesArePrivateAndBlankDeletes() {
        Board board = boardService.getAllBoards().getFirst();
        int sectorNumber = board.getSector(1).getNumber();
        Long user1 = userRepository.findByUsername(TestDataInitializer.USER_1).getId();
        Long user2 = userRepository.findByUsername(TestDataInitializer.USER_2).getId();

        sectorNoteService.saveNote(user1, board.getId(), sectorNumber, "  Attention, secteur piégé  ");

        Map<Integer, String> user1Notes = sectorNoteService.getNotes(user1, board.getId());
        assertEquals("Attention, secteur piégé", user1Notes.get(sectorNumber));
        assertFalse(sectorNoteService.getNotes(user2, board.getId()).containsKey(sectorNumber));

        sectorNoteService.saveNote(user1, board.getId(), sectorNumber, "   ");

        assertFalse(sectorNoteService.getNotes(user1, board.getId()).containsKey(sectorNumber));
    }
}
