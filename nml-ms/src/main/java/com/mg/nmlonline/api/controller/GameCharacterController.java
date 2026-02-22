package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.GameCharacterDto;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.service.GameCharacterService;
import com.mg.nmlonline.mapper.GameCharacterMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la gestion des personnages principaux.
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
     * Récupère le personnage d'un joueur.
     */
    //TODO : verify authentication and authorization to prevent data leak (ex: only allow access to own character or admin access)
    @GetMapping("/player/{playerId}")
    public ResponseEntity<GameCharacterDto> getCharacterByPlayerId(@PathVariable Long playerId) {
        return characterService.getCharacter(playerId)
                .map(characterMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère un personnage par son nom.
     */
    //TODO : verify authentication and authorization to prevent data leak (ex: only allow access to own character or admin access)
    @GetMapping("/name/{name}")
    public ResponseEntity<GameCharacterDto> getCharacterByName(@PathVariable String name) {
        return characterService.getCharacterByName(name)
                .map(characterMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

