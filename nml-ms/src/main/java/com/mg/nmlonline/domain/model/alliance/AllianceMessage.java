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

import java.time.Instant;

@Entity
@Table(name = "ALLIANCE_MESSAGES")
@Getter
@Setter
@NoArgsConstructor
public class AllianceMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alliance_message_seq")
    @SequenceGenerator(name = "alliance_message_seq", sequenceName = "alliance_messages_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "alliance_id", nullable = false)
    private Long allianceId;

    @Column(name = "sender_player_id", nullable = false)
    private Long senderPlayerId;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(nullable = false)
    private int turn;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static AllianceMessage create(Long allianceId, Long senderPlayerId, String body, int turn) {
        AllianceMessage message = new AllianceMessage();
        message.allianceId = allianceId;
        message.senderPlayerId = senderPlayerId;
        message.body = body;
        message.turn = turn;
        message.createdAt = Instant.now();
        return message;
    }
}
