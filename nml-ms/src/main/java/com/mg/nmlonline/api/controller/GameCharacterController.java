package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.GameCharacterDto;
import com.mg.nmlonline.domain.service.AuthorizationService;
import com.mg.nmlonline.domain.service.GameCharacterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/characters")
public class GameCharacterController {

    private final GameCharacterService characterService;
    private final AuthorizationService authorizationService;

    public GameCharacterController(GameCharacterService characterService,
                                     AuthorizationService authorizationService) {
        this.characterService = characterService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<GameCharacterDto> getCharacterByPlayerId(@PathVariable Long playerId,
                                                                   HttpServletRequest request) {
        Long authenticatedUserId = (Long) request.getAttribute("userId");
        if (authenticatedUserId == null) return ResponseEntity.status(401).build();
        if (!authorizationService.isPlayerOwner(authenticatedUserId, playerId)) return ResponseEntity.status(403).build();
        return characterService.getCharacterDto(playerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<GameCharacterDto> getCharacterByName(@PathVariable String name,
                                                               HttpServletRequest request) {
        Long authenticatedUserId = (Long) request.getAttribute("userId");
        if (authenticatedUserId == null) return ResponseEntity.status(401).build();
        Optional<GameCharacterDto> dto = characterService.getCharacterByNameDto(name);
        if (dto.isEmpty()) return ResponseEntity.notFound().build();
        if (!authorizationService.isPlayerOwner(authenticatedUserId, dto.get().getPlayerId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(dto.get());
    }
}
