package com.mg.nmlonline.domain.model.movement;

import java.util.List;

/**
 * Conflit groupé d'un secteur : joueurs dans l'ordre de résolution — duel [arrivant, défenseur],
 * impasse [défenseurs puis arrivants par ordre d'envoi] (voir MovementService.resolveStep).
 */
public record SectorConflict(int sectorNumber, List<Long> participantPlayerIds) {

    public boolean isStandoff() {
        return participantPlayerIds.size() >= 3;
    }

    public Long firstParticipantPlayerId() {
        return participantPlayerIds.getFirst();
    }
}
