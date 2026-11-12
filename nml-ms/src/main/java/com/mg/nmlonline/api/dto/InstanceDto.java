package com.mg.nmlonline.api.dto;

/** `participant` distingue « mes parties » (joueur ou MJ) des parties qu'un admin plateforme peut ouvrir. */
public record InstanceDto(Long id, String name, int currentTurn, long playerCount,
                          Long playerId, String playerName, boolean eliminated, String role,
                          boolean participant) {
}
