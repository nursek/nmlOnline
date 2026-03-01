package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.GameCharacterDto;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.service.GameCharacterService;
import com.mg.nmlonline.mapper.GameCharacterMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la gestion des personnages principaux.
 * Vérification d'ownership sur tous les endpoints.
 */
@RestController
@RequestMapping("/api/characters")
public class GameCharacterController {

    private final GameCharacterService characterService;
    private final GameCharacterMapper characterMapper;

    public GameCharacterController(GameCharacterService characterService,
                                    GameCharacterMapper characterMapper) {
        this.characterService = characterService;
        this.characterMapper = characterMapper;
    }

    /**
     * Récupère le personnage du joueur authentifié.
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<GameCharacterDto> getCharacterByPlayerId(@PathVariable Long playerId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        // Vérifier que le joueur demandé correspond à l'utilisateur authentifié
        if (!userId.equals(playerId)) {
            return ResponseEntity.status(403).build();
        }
        return characterService.getCharacter(playerId)
                .map(characterMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère le personnage du joueur authentifié par nom.
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<GameCharacterDto> getCharacterByName(@PathVariable String name, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return characterService.getCharacterByName(name)
                .filter(character -> userId.equals(character.getPlayerId()))
                .map(characterMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(403).build());
    }
}

