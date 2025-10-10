package com.mg.nmlonline.entity.battle;

import com.mg.nmlonline.entity.unit.Unit;

import java.util.List;

public record PhaseResult(List<Unit> casualties, List<Unit> survivors, double remainingPoints) {
}