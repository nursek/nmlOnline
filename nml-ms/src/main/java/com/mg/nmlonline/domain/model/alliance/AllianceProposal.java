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
@Table(name = "ALLIANCE_PROPOSALS")
@Getter
@Setter
@NoArgsConstructor
public class AllianceProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alliance_proposal_seq")
    @SequenceGenerator(name = "alliance_proposal_seq", sequenceName = "alliance_proposals_id_seq", allocationSize = 50)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProposalKind kind;

    @Column(name = "from_player_id", nullable = false)
    private Long fromPlayerId;

    @Column(name = "to_player_id", nullable = false)
    private Long toPlayerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProposalStatus status = ProposalStatus.PENDING;

    @Column(name = "created_turn", nullable = false)
    private int createdTurn;

    @Column(name = "resolved_turn")
    private Integer resolvedTurn;

    public static AllianceProposal create(ProposalKind kind, Long fromPlayerId, Long toPlayerId, int turn) {
        if (fromPlayerId.equals(toPlayerId)) {
            throw new IllegalArgumentException("Un joueur ne peut pas se proposer une alliance à lui-même.");
        }
        AllianceProposal proposal = new AllianceProposal();
        proposal.kind = kind;
        proposal.fromPlayerId = fromPlayerId;
        proposal.toPlayerId = toPlayerId;
        proposal.createdTurn = turn;
        return proposal;
    }

    public boolean isPending() {
        return status == ProposalStatus.PENDING;
    }

    public void accept(int turn) {
        resolve(ProposalStatus.ACCEPTED, turn);
    }

    public void decline(int turn) {
        resolve(ProposalStatus.DECLINED, turn);
    }

    public void withdraw(int turn) {
        resolve(ProposalStatus.WITHDRAWN, turn);
    }

    private void resolve(ProposalStatus newStatus, int turn) {
        if (!isPending()) {
            throw new IllegalStateException("La proposition est déjà résolue (" + status + ").");
        }
        this.status = newStatus;
        this.resolvedTurn = turn;
    }
}
