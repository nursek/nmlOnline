package com.mg.nmlonline.api.dto;

/** Une partie visible au sélecteur : le joueur y participe, y officie comme MJ, ou est admin plateforme. */
public record InstanceDto(Long id, String name, int currentTurn, long playerCount,
                          Long playerId, String playerName, boolean eliminated, String role) {
}
