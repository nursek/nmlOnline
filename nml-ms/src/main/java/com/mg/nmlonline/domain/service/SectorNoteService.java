package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.sector.SectorNote;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.SectorNoteRepository;
import com.mg.nmlonline.infrastructure.repository.SectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class SectorNoteService {

    private final SectorNoteRepository sectorNoteRepository;
    private final PlayerRepository playerRepository;
    private final SectorRepository sectorRepository;

    public SectorNoteService(SectorNoteRepository sectorNoteRepository,
                             PlayerRepository playerRepository,
                             SectorRepository sectorRepository) {
        this.sectorNoteRepository = sectorNoteRepository;
        this.playerRepository = playerRepository;
        this.sectorRepository = sectorRepository;
    }

    public Map<Integer, String> getNotes(Long userId, Long boardId) {
        Player player = requirePlayer(userId, boardId);
        Map<Integer, String> notes = new HashMap<>();
        for (SectorNote sectorNote : sectorNoteRepository.findByPlayerIdAndBoardId(player.getId(), boardId)) {
            notes.put(sectorNote.getSectorNumber(), sectorNote.getNote());
        }
        return notes;
    }

    public void saveNote(Long userId, Long boardId, int sectorNumber, String note) {
        Player player = requirePlayer(userId, boardId);
        if (!sectorRepository.existsById(new Sector.SectorId(boardId, sectorNumber))) {
            throw new IllegalArgumentException("Secteur introuvable");
        }
        String cleaned = note == null ? "" : note.trim();
        if (cleaned.isEmpty()) {
            sectorNoteRepository.deleteByPlayerIdAndBoardIdAndSectorNumber(
                    player.getId(), boardId, sectorNumber);
            return;
        }
        sectorNoteRepository.save(new SectorNote(player.getId(), boardId, sectorNumber, cleaned));
    }

    private Player requirePlayer(Long userId, Long boardId) {
        if (userId == null) {
            throw new SecurityException("Utilisateur non authentifié");
        }
        return playerRepository.findByUserIdAndBoardId(userId, boardId)
                .orElseThrow(() -> new SecurityException("Aucun joueur associé à ce compte"));
    }
}
