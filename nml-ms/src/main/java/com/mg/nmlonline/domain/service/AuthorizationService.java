package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BuildingRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final PlayerRepository playerRepository;
    private final BuildingRepository buildingRepository;

    public AuthorizationService(PlayerRepository playerRepository, BuildingRepository buildingRepository) {
        this.playerRepository = playerRepository;
        this.buildingRepository = buildingRepository;
    }

    public boolean isPlayerOwner(Long authenticatedUserId, Long playerId) {
        if (authenticatedUserId == null || playerId == null) {
            return false;
        }
        Player player = playerRepository.findById(playerId).orElse(null);
        return player != null && authenticatedUserId.equals(player.getUserId());
    }

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
