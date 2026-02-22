package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.infrastructure.repository.GameCharacterRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service pour la gestion des personnages principaux (leaders).
 */
@Service
@Transactional
public class GameCharacterService {

    private final GameCharacterRepository characterRepository;
    private final PlayerRepository playerRepository;

    public GameCharacterService(GameCharacterRepository characterRepository,
                                 PlayerRepository playerRepository) {
        this.characterRepository = characterRepository;
        this.playerRepository = playerRepository;
    }

    /**
     * Crée un personnage avec toutes les stats.
     */
    public GameCharacter createCharacter(Long playerId, String name,
                                         double baseAttack, double basePdf, double basePdc,
                                         double baseDefense, double baseArmor, double baseEvasion) {
        if (characterRepository.existsByPlayerId(playerId)) {
            throw new IllegalStateException("Le joueur a déjà un personnage principal");
        }

        if (characterRepository.existsByName(name)) {
            throw new IllegalArgumentException("Un personnage avec ce nom existe déjà");
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur non trouvé"));

        GameCharacter character = new GameCharacter(name, baseAttack, basePdf, basePdc,
                                                     baseDefense, baseArmor, baseEvasion);
        character.setPlayerId(playerId);

        character = characterRepository.save(character);

        player.setCharacter(character);
        playerRepository.save(player);

        return character;
    }

    public Optional<GameCharacter> getCharacter(Long playerId) {
        return characterRepository.findByPlayerId(playerId);
    }

    public Optional<GameCharacter> getCharacterByName(String name) {
        return characterRepository.findByName(name);
    }

}

