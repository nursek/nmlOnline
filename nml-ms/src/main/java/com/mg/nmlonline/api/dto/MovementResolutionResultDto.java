package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

/**
 * Compte-rendu de résolution d'un tour ; aussi renvoyé en aperçu dry-run (sans persistance).
 */
@Data
public class MovementResolutionResultDto {
    private int turn;
    private List<AdminMovementOrderDto> resolved;
    private List<AdminMovementOrderDto> blocked;
    private List<DestinationConflictDto> conflicts;
    private List<TransitCombatResultDto> transitCombats;
    private boolean hasConflicts;
    private boolean hasTransitCombats;
}