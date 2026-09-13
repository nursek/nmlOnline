package com.mg.nmlonline.domain.model.battle;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "BATTLE_REPORTS")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BattleReport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "battle_report_seq")
    @SequenceGenerator(name = "battle_report_seq", sequenceName = "battle_reports_id_seq", allocationSize = 50)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private int turn;

    @Column(name = "sector_number", nullable = false)
    private int sectorNumber;

    @Column(nullable = false)
    private boolean standoff;

    @Column(name = "winner_player_id")
    private Long winnerPlayerId;

    @Column(name = "captured_buildings", nullable = false)
    private int capturedBuildings;

    // Snapshot JSON : le rapport ne doit pas dépendre des entités mutées/supprimées après le combat.
    @Column(nullable = false, length = 100000)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @ElementCollection
    @CollectionTable(name = "BATTLE_REPORT_PLAYERS", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "player_id", nullable = false)
    private Set<Long> participantIds = new HashSet<>();
}
