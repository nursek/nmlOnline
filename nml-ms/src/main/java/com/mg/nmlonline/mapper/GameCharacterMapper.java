package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.GameCharacterDto;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import org.springframework.stereotype.Component;

/**
 * Mapper pour GameCharacter - conversion entre Domain et DTO.
 */
@Component
public class GameCharacterMapper {

    /**
     * Convertit un GameCharacter du domaine en DTO.
     */
    public GameCharacterDto toDto(GameCharacter character) {
        if (character == null) return null;

        GameCharacterDto dto = new GameCharacterDto();
        dto.setId(character.getId());
        dto.setPlayerId(character.getPlayerId());
        dto.setName(character.getName());

        // Stats de base
        dto.setBaseAttack(character.getBaseAttack());
        dto.setBaseDefense(character.getBaseDefense());
        dto.setBasePdf(character.getBasePdf());
        dto.setBasePdc(character.getBasePdc());
        dto.setBaseArmor(character.getBaseArmor());
        dto.setBaseEvasion(character.getBaseEvasion());

        // Localisation
        if (character.getSector() != null) {
            dto.setSectorNumber(character.getSector().getNumber());
        }

        return dto;
    }

    /**
     * Convertit un DTO en GameCharacter du domaine.
     */
    public GameCharacter toDomain(GameCharacterDto dto) {
        if (dto == null) return null;

        GameCharacter character = new GameCharacter(
            dto.getName(),
            dto.getBaseAttack() != null ? dto.getBaseAttack() : 0,
            dto.getBasePdf() != null ? dto.getBasePdf() : 0,
            dto.getBasePdc() != null ? dto.getBasePdc() : 0,
            dto.getBaseDefense() != null ? dto.getBaseDefense() : 0,
            dto.getBaseArmor() != null ? dto.getBaseArmor() : 0,
            dto.getBaseEvasion() != null ? dto.getBaseEvasion() : 0
        );

        if (dto.getId() != null) {
            character.setId(dto.getId());
        }
        character.setPlayerId(dto.getPlayerId());
        return character;
    }
}

