package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.unit.CombatEntity;

import java.util.List;

public record PhaseResult(List<CombatEntity> casualties, List<CombatEntity> survivors, double remainingPoints) {
}
