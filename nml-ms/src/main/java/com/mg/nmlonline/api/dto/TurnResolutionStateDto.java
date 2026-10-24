package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class TurnResolutionStateDto {
    private boolean active;
    private int turnEnding;
    private int currentStep;
    private int maxSteps;
    private List<PendingConflictDto> pendingConflicts;
    private List<ResolvedBattleDto> resolvedConflicts;
    private List<PendingEliminationDto> pendingEliminations;
    private int transitCombatsCount;
    private boolean canAdvance;
    private boolean canFinalize;
    private boolean allDone;
}