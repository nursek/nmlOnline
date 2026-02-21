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
    @GetMapping("/name/{name}")
    public ResponseEntity<GameCharacterDto> getCharacterByName(@PathVariable String name) {
        return characterService.getCharacterByName(name)
                .map(characterMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crée un nouveau personnage pour un joueur.
     */
    @PostMapping
    public ResponseEntity<GameCharacterDto> createCharacter(@RequestBody CreateCharacterRequest request) {
        try {
            GameCharacter character = characterService.createCharacter(
                    request.playerId(),
                    request.name(),
                    request.baseAttack(),
                    request.basePdf(),
                    request.basePdc(),
                    request.baseDefense(),
                    request.baseArmor(),
                    request.baseEvasion()
            );
            return ResponseEntity.ok(characterMapper.toDto(character));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Record pour la création d'un personnage.
     */
    public record CreateCharacterRequest(
            Long playerId,
            String name,
            double baseAttack,
            double basePdf,
            double basePdc,
            double baseDefense,
            double baseArmor,
            double baseEvasion
    ) {}
}

