package com.mg.nmlonline.domain.model.movement;

import java.util.List;

/**
 * Camps en présence, ordonnés par résolution (défenseurs puis arrivants ; voir MovementService.resolveStep).
 * Chaque camp contient 1 joueur, ou 2 alliés fusionnés (2v1 solidaire).
 */
public record SectorConflict(int sectorNumber, List<List<Long>> camps) {

    public List<Long> participantPlayerIds() {
        return camps.stream().flatMap(List::stream).toList();
    }

    public boolean isStandoff() {
        return camps.size() >= 3;
    }
}
