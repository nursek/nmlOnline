package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.GameCharacterDto;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.infrastructure.repository.GameCharacterRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.mapper.GameCharacterMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GameCharacterService {

    private final GameCharacterRepository characterRepository;
    private final PlayerRepository playerRepository;
    private final GameCharacterMapper characterMapper;

    public GameCharacterService(GameCharacterRepository characterRepository,
                                 PlayerRepository playerRepository,
                                 GameCharacterMapper characterMapper) {
        this.characterRepository = characterRepository;
        this.playerRepository = playerRepository;
        this.characterMapper = characterMapper;
    }

    public void createCharacter(Long playerId, String name,
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

    }

    public Optional<GameCharacter> getCharacter(Long playerId) {
        return characterRepository.findByPlayerId(playerId);
    }

    public Optional<GameCharacter> getCharacterByName(String name) {
        return characterRepository.findByName(name);
    }

    /**
     * Régénération de fin de tour : +50 def (plafonné baseDefense) pour tout personnage blessé.
     */
    public void regenerateAllCharacters() {
        List<GameCharacter> damaged = characterRepository.findAll().stream()
                .filter(c -> c.getDefense() < c.getBaseDefense())
                .toList();
        damaged.forEach(c -> c.regenerateDefense(GameCharacter.DEFENSE_REGEN_PER_TURN));
    }

    // Mapping dans la transaction (sector est LAZY).

    public Optional<GameCharacterDto> getCharacterDto(Long playerId) {
        return getCharacter(playerId).map(characterMapper::toDto);
    }

    public Optional<GameCharacterDto> getCharacterByNameDto(String name) {
        return getCharacterByName(name).map(characterMapper::toDto);
    }

}

