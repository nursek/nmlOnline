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

@Entity
@Table(name = "ALLIANCES")
@Getter
@Setter
@NoArgsConstructor
public class Alliance {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alliance_seq")
    @SequenceGenerator(name = "alliance_seq", sequenceName = "alliances_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "player_one_id", nullable = false)
    private Long playerOneId;

    @Column(name = "player_two_id", nullable = false)
    private Long playerTwoId;

    @Column(name = "created_turn", nullable = false)
    private int createdTurn;

    @Column(name = "ended_turn")
    private Integer endedTurn;

    @Column(name = "ended_by_player_id")
    private Long endedByPlayerId;

    @Column(nullable = false)
    private boolean betrayal;

    public static Alliance create(Long firstPlayerId, Long secondPlayerId, int turn) {
        if (firstPlayerId.equals(secondPlayerId)) {
            throw new IllegalArgumentException("Un joueur ne peut pas s'allier à lui-même.");
        }
        Alliance alliance = new Alliance();
        alliance.playerOneId = Math.min(firstPlayerId, secondPlayerId);
        alliance.playerTwoId = Math.max(firstPlayerId, secondPlayerId);
        alliance.createdTurn = turn;
        return alliance;
    }

    public boolean isActive() {
        return endedTurn == null;
    }

    public boolean involves(Long playerId) {
        return playerOneId.equals(playerId) || playerTwoId.equals(playerId);
    }

    public Long other(Long playerId) {
        return playerOneId.equals(playerId) ? playerTwoId : playerOneId;
    }

    public void end(int turn, Long endedByPlayerId, boolean betrayal) {
        if (!isActive()) {
            throw new IllegalStateException("L'alliance est déjà terminée.");
        }
        this.endedTurn = turn;
        this.endedByPlayerId = endedByPlayerId;
        this.betrayal = betrayal;
    }

    /** Nombre de tours de vie de l'alliance, utilisé pour le bonus de trahison. */
    public int durationTurns() {
        int end = endedTurn != null ? endedTurn : createdTurn;
        return Math.max(0, end - createdTurn);
    }
}
