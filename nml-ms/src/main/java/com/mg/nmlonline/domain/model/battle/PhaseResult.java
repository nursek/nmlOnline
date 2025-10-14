package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.unit.Unit;

import java.util.List;

public record PhaseResult(List<Unit> casualties, List<Unit> survivors, double remainingPoints) {
}