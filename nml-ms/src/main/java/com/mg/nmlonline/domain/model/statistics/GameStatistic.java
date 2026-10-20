package com.mg.nmlonline.domain.model.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "GAME_STATISTICS")
@Getter
@Setter
@NoArgsConstructor
public class GameStatistic {

    @Id
    @Column(name = "stat_key", nullable = false, length = 64)
    private String statKey;

    @Column(name = "total", nullable = false)
    private long total;

    public GameStatistic(String statKey, long total) {
        this.statKey = statKey;
        this.total = total;
    }
}
