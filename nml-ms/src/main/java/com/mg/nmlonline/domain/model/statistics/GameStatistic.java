package com.mg.nmlonline.domain.model.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "GAME_STATISTICS")
@IdClass(GameStatistic.StatId.class)
@Getter
@Setter
@NoArgsConstructor
public class GameStatistic {

    @Id
    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Id
    @Column(name = "stat_key", nullable = false, length = 64)
    private String statKey;

    @Column(nullable = false)
    private long total;

    public GameStatistic(Long boardId, String statKey, long total) {
        this.boardId = boardId;
        this.statKey = statKey;
        this.total = total;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class StatId implements Serializable {
        private Long boardId;
        private String statKey;
    }
}
