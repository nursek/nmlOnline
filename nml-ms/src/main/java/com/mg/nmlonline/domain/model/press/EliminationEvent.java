package com.mg.nmlonline.domain.model.press;

import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.PlayerRace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "ELIMINATION_EVENTS")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EliminationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "elimination_event_seq")
    @SequenceGenerator(name = "elimination_event_seq", sequenceName = "elimination_events_id_seq",
            allocationSize = 50)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "death_index", nullable = false)
    private int deathIndex;

    @Column(name = "victim_player_id", nullable = false)
    private Long victimPlayerId;

    @Column(name = "victim_name", nullable = false, length = 255)
    private String victimName;

    @Enumerated(EnumType.STRING)
    @Column(name = "victim_race", length = 20)
    private PlayerRace victimRace;

    @Column(name = "victim_character_name", length = 100)
    private String victimCharacterName;

    @Column(name = "eliminated_turn", nullable = false)
    private int eliminatedTurn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EliminationCause cause;

    @Column(name = "killer_player_id")
    private Long killerPlayerId;

    @Column(name = "killer_name", length = 255)
    private String killerName;

    @Column(name = "assist_player_ids", length = 500)
    private String assistPlayerIds;

    @Column(name = "battle_sector")
    private Integer battleSector;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public List<Long> assistIds() {
        if (assistPlayerIds == null || assistPlayerIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(assistPlayerIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .toList();
    }
}
