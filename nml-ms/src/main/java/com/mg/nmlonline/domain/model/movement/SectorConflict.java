package com.mg.nmlonline.domain.model.movement;

import java.util.List;

/** Joueurs dans l'ordre de résolution : duel [arrivant, défenseur], impasse [défenseurs puis arrivants] (voir MovementService.resolveStep). */
public record SectorConflict(int sectorNumber, List<Long> participantPlayerIds) {

    public boolean isStandoff() {
        return participantPlayerIds.size() >= 3;
    }
}
