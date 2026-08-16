package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

/**
 * Instantané de l'état d'une session de résolution pas-à-pas :
 * hop courant, conflits en attente de résolution manuelle, et historique
 * des batailles déjà résolues. {@code active} indique si une session tourne.
 */
@Data
public class TurnResolutionStateDto {
    private boolean active;
    private int turnEnding;
    private int currentStep;
    private int maxSteps;
    private List<PendingConflictDto> pendingConflicts;
    private List<ResolvedBattleDto> resolvedConflicts;
    private int transitCombatsCount;
    private boolean canAdvance;
    private boolean canFinalize;
    private boolean allDone;
}