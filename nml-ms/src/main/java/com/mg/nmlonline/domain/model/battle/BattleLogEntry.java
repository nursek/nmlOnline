package com.mg.nmlonline.domain.model.battle;

public record BattleLogEntry(String phase, String outcome, String message) {

    public static final String INFO = "INFO";
    public static final String DODGE = "DODGE";
    public static final String DAMAGE = "DAMAGE";
    public static final String DESTROYED = "DESTROYED";
    public static final String WINNER = "WINNER";
    public static final String LOSS = "LOSS";
    public static final String GAIN = "GAIN";
}
