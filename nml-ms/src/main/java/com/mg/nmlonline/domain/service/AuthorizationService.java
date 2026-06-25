package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BuildingRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;

/**
 * Service utilitaire pour vérifier qu'un utilisateur authentifié est bien
 * propriétaire d'une ressource (joueur, bâtiment, etc.).
 */
@Service
public class AuthorizationService {

    private final PlayerRepository playerRepository;
    private final BuildingRepository buildingRepository;

    public AuthorizationService(PlayerRepository playerRepository, BuildingRepository buildingRepository) {
        this.playerRepository = playerRepository;
        this.buildingRepository = buildingRepository;
    }

    /**
     * Vérifie que l'utilisateur authentifié possède le joueur identifié par {@code playerId}.
     *
     * @param authenticatedUserId l'identifiant de l'utilisateur authentifié
     * @param playerId            l'identifiant du joueur cible
     * @return true si l'utilisateur est propriétaire ou admin (non vérifié ici)
     */
    public boolean isPlayerOwner(Long authenticatedUserId, Long playerId) {
        if (authenticatedUserId == null || playerId == null) {
            return false;
        }
        Player player = playerRepository.findById(playerId).orElse(null);
        return player != null && authenticatedUserId.equals(player.getUserId());
    }

    /**
     * Vérifie que l'utilisateur authentifié possède le bâtiment identifié par {@code buildingId}.
     *
     * @param authenticatedUserId l'identifiant de l'utilisateur authentifié
     * @param buildingId          l'identifiant du bâtiment
     * @return true si le bâtiment appartient au joueur de l'utilisateur
     */
    public boolean isBuildingOwner(Long authenticatedUserId, Long buildingId) {
        if (authenticatedUserId == null || buildingId == null) {
            return false;
        }
        return buildingRepository.findById(buildingId)
                .map(building -> {
                    Player player = playerRepository.findById(building.getPlayerId()).orElse(null);
                    return player != null && authenticatedUserId.equals(player.getUserId());
                })
                .orElse(false);
    }
}
