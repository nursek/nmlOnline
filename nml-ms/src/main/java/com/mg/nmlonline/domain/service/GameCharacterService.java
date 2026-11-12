package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.GameCharacterDto;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.infrastructure.repository.GameCharacterRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.mapper.GameCharacterMapper;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    public GameCharacterService(GameCharacterRepository characterRepository,
                                 PlayerRepository playerRepository,
                                 GameCharacterMapper characterMapper,
                                 EntityManager entityManager) {
        this.characterRepository = characterRepository;
        this.playerRepository = playerRepository;
        this.characterMapper = characterMapper;
        this.entityManager = entityManager;
    }

    public void createCharacter(Long playerId, String name,
                                double baseAttack, double basePdf, double basePdc,
                                double baseDefense, double baseArmor, double baseEvasion) {
        if (characterRepository.existsByPlayerId(playerId)) {
            throw new IllegalStateException("Le joueur a déjà un personnage principal");
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur non trouvé"));
        if (characterRepository.existsByBoardIdAndName(player.getBoardId(), name)) {
            throw new IllegalArgumentException("Un personnage avec ce nom existe déjà");
        }

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

    public Optional<GameCharacter> getCharacterByName(Long boardId, String name) {
        return characterRepository.findByBoardIdAndName(boardId, name);
    }

    public void placeInSector(Long playerId, Sector sector) {
        GameCharacter character = characterRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Le joueur n'a pas de personnage"));
        character.setSector(sector);
        if (sector != null && !sector.getCharacters().contains(character)) {
            sector.getCharacters().add(character);
        }
        characterRepository.save(character);
    }

    public void updateCharacter(Long playerId, String name,
                                double baseAttack, double basePdf, double basePdc,
                                double baseDefense, double baseArmor, double baseEvasion) {
        GameCharacter character = characterRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Le joueur n'a pas de personnage"));
        characterRepository.findByBoardIdAndName(playerRepository.findById(playerId)
                        .orElseThrow(() -> new IllegalArgumentException("Joueur non trouvé")).getBoardId(), name)
                .ifPresent(existing -> {
            if (!existing.getId().equals(character.getId())) {
                throw new IllegalArgumentException("Un personnage avec ce nom existe déjà");
            }
        });
        character.setName(name);
        character.setBaseAttack(baseAttack);
        character.setBasePdf(basePdf);
        character.setBasePdc(basePdc);
        character.setBaseDefense(baseDefense);
        character.setBaseArmor(baseArmor);
        character.setBaseEvasion(baseEvasion);
        character.recalculateBaseStats();
        characterRepository.save(character);
    }

    public void deleteCharacter(Long playerId) {
        GameCharacter character = characterRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Le joueur n'a pas de personnage"));
        CombatService.detachFromVehicles(entityManager, character);
        if (character.getSector() != null) {
            character.getSector().getCharacters().remove(character);
        }
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur non trouvé"));
        player.setCharacter(null);
        playerRepository.save(player);
    }

    /** Fin de tour : +50 def (plafonné baseDefense) sur chaque personnage entamé. */
    public void regenerateAllCharacters(Long boardId) {
        List<GameCharacter> damaged = characterRepository.findByBoardId(boardId).stream()
                .filter(c -> c.getDefense() < c.getBaseDefense())
                .toList();
        damaged.forEach(c -> c.regenerateDefense(GameCharacter.DEFENSE_REGEN_PER_TURN));
    }

    // Mapping dans la transaction (sector est LAZY).

    public Optional<GameCharacterDto> getCharacterDto(Long playerId) {
        return getCharacter(playerId).map(characterMapper::toDto);
    }

    public Optional<GameCharacterDto> getCharacterByNameDto(Long boardId, String name) {
        return getCharacterByName(boardId, name).map(characterMapper::toDto);
    }

}

