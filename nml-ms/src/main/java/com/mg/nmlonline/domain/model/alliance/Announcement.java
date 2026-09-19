package com.mg.nmlonline.domain.model.alliance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ANNOUNCEMENTS")
@Getter
@Setter
@NoArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "announcement_seq")
    @SequenceGenerator(name = "announcement_seq", sequenceName = "announcements_id_seq", allocationSize = 50)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnnouncementType type;

    @Column(name = "actor_player_id", nullable = false)
    private Long actorPlayerId;

    @Column(name = "target_player_id")
    private Long targetPlayerId;

    @Column(name = "turn_created", nullable = false)
    private int turnCreated;

    @Column(name = "visible_at_turn", nullable = false)
    private int visibleAtTurn;

    public static Announcement create(AnnouncementType type, Long actorPlayerId, Long targetPlayerId, int turn) {
        Announcement announcement = new Announcement();
        announcement.type = type;
        announcement.actorPlayerId = actorPlayerId;
        announcement.targetPlayerId = targetPlayerId;
        announcement.turnCreated = turn;
        announcement.visibleAtTurn = turn + 1;
        return announcement;
    }
}
