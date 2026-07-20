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
}

