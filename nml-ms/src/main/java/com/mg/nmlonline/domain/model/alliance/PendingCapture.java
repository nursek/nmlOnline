package com.mg.nmlonline.domain.model.alliance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "PENDING_CAPTURES")
@Getter
@Setter
@NoArgsConstructor
public class PendingCapture {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pending_capture_seq")
    @SequenceGenerator(name = "pending_capture_seq", sequenceName = "pending_captures_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "sector_number", nullable = false)
    private int sectorNumber;

    @Column(nullable = false)
    private int turn;

    @Column(name = "candidate_player_ids", nullable = false, length = 255)
    private String candidatePlayerIds;

    @Column(nullable = false)
    private boolean resolved;

    public static PendingCapture create(Long boardId, int sectorNumber, int turn, List<Long> candidates) {
        PendingCapture pending = new PendingCapture();
        pending.boardId = boardId;
        pending.sectorNumber = sectorNumber;
        pending.turn = turn;
        pending.candidatePlayerIds = candidates.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        return pending;
    }

    public List<Long> candidateIds() {
        if (candidatePlayerIds == null || candidatePlayerIds.isBlank()) {
            return List.of();
        }
        return List.of(candidatePlayerIds.split(",")).stream().map(Long::valueOf).toList();
    }
}
