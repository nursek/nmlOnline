package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.GameCharacterDto;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.service.AuthorizationService;
import com.mg.nmlonline.domain.service.GameCharacterService;
import com.mg.nmlonline.mapper.GameCharacterMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Contrôleur REST pour la gestion des personnages principaux.
 */
@RestController
@RequestMapping("/api/characters")
public class GameCharacterController {

    private final GameCharacterService characterService;
    private final GameCharacterMapper characterMapper;
    private final AuthorizationService authorizationService;

    public GameCharacterController(GameCharacterService characterService,
                                     GameCharacterMapper characterMapper,
                                     AuthorizationService authorizationService) {
        this.characterService = characterService;
        this.characterMapper = characterMapper;
        this.authorizationService = authorizationService;
    }

    /**
     * Récupère le personnage d'un joueur.
     * L'utilisateur doit être authentifié et propriétaire du joueur (ou admin).
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<GameCharacterDto> getCharacterByPlayerId(@PathVariable Long playerId,
                                                                   HttpServletRequest request) {
        Long authenticatedUserId = (Long) request.getAttribute("userId");
        if (authenticatedUserId == null) {
            return ResponseEntity.status(401).build();
        }
        if (!authorizationService.isPlayerOwner(authenticatedUserId, playerId)) {
            return ResponseEntity.status(403).build();
        }
        return characterService.getCharacter(playerId)
                .map(characterMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère un personnage par son nom.
     * L'utilisateur doit être authentifié et propriétaire du joueur associé (ou admin).
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<GameCharacterDto> getCharacterByName(@PathVariable String name,
                                                               HttpServletRequest request) {
        Long authenticatedUserId = (Long) request.getAttribute("userId");
        if (authenticatedUserId == null) {
            return ResponseEntity.status(401).build();
        }
        Optional<GameCharacter> characterOpt = characterService.getCharacterByName(name);
        if (characterOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        GameCharacter character = characterOpt.get();
        if (!authorizationService.isPlayerOwner(authenticatedUserId, character.getPlayerId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(characterMapper.toDto(character));
    }
}

