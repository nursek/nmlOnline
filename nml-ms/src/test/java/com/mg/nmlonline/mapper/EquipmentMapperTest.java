package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.EquipmentDto;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.VehicleBonusTarget;
import com.mg.nmlonline.domain.model.player.PlayerRace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("EquipmentMapper — bonus anti-véhicules")
class EquipmentMapperTest {

    private final EquipmentMapper mapper = new EquipmentMapper();

    @Test
    @DisplayName("Un bonus négatif ou une cible inconnue est rejeté, une valeur valide est conservée")
    void rejectsNegativeBonusAndUnknownTarget() {
        EquipmentDto negative = new EquipmentDto("Arme", 100, 0, 0, 0, 0, -50, "GROUND", null, "FIREARM", null);
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(negative));

        EquipmentDto unknownTarget = new EquipmentDto("Arme", 100, 0, 0, 0, 0, 100, "SOUS_MARIN", null, "FIREARM", null);
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(unknownTarget));

        EquipmentDto valid = new EquipmentDto("Arme", 100, 50, 0, 0, 0, 100, "AERIAL", null, "FIREARM", null);
        Equipment equipment = mapper.toDomain(valid);
        assertEquals(100, equipment.getVehicleBonus());
        assertEquals(VehicleBonusTarget.AERIAL, equipment.getVehicleBonusTarget());
    }

    @Test
    @DisplayName("Les libellés par race font l'aller-retour, casse ignorée, race inconnue rejetée")
    void mapsDisplayNamesByRace() {
        EquipmentDto dto = new EquipmentDto("Arme", 100, 0, 0, 0, 0, 0, "GROUND", null, "FIREARM",
                Map.of("orks", "Truc"));
        Equipment equipment = mapper.toDomain(dto);
        assertEquals("Truc", equipment.getDisplayNames().get(PlayerRace.ORKS));
        assertEquals("Truc", mapper.toDto(equipment).getDisplayNames().get("ORKS"));

        EquipmentDto unknown = new EquipmentDto("Arme", 100, 0, 0, 0, 0, 0, "GROUND", null, "FIREARM",
                Map.of("SOUS_MARIN", "Truc"));
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(unknown));

        Map<String, String> badLabels = new HashMap<>();
        badLabels.put("ORKS", null);
        badLabels.put("NECRONS", " ");
        EquipmentDto badLabel = new EquipmentDto("Arme", 100, 0, 0, 0, 0, 0, "GROUND", null, "FIREARM",
                badLabels);
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(badLabel));
    }
}
