package com.mg.nmlonline.domain.model.sector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "SECTOR_NOTES")
@IdClass(SectorNote.NoteId.class)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SectorNote {

    @Id
    @Column(name = "player_id", nullable = false)
    @EqualsAndHashCode.Include
    private Long playerId;

    @Id
    @Column(name = "board_id", nullable = false)
    @EqualsAndHashCode.Include
    private Long boardId;

    @Id
    @Column(name = "sector_number", nullable = false)
    @EqualsAndHashCode.Include
    private int sectorNumber;

    @Column(nullable = false, length = 500)
    private String note;

    public SectorNote(Long playerId, Long boardId, int sectorNumber, String note) {
        this.playerId = playerId;
        this.boardId = boardId;
        this.sectorNumber = sectorNumber;
        this.note = note;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoteId implements Serializable {
        private Long playerId;
        private Long boardId;
        private int sectorNumber;
    }
}
