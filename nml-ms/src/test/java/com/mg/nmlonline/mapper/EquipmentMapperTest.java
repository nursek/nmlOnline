package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.EquipmentDto;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.VehicleBonusTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("EquipmentMapper — bonus anti-véhicules")
class EquipmentMapperTest {

    private final EquipmentMapper mapper = new EquipmentMapper();

    @Test
    @DisplayName("Un bonus négatif ou une cible inconnue est rejeté, une valeur valide est conservée")
    void rejectsNegativeBonusAndUnknownTarget() {
        EquipmentDto negative = new EquipmentDto("Arme", 100, 0, 0, 0, 0, -50, "GROUND", null, "FIREARM");
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(negative));

        EquipmentDto unknownTarget = new EquipmentDto("Arme", 100, 0, 0, 0, 0, 100, "SOUS_MARIN", null, "FIREARM");
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(unknownTarget));

        EquipmentDto valid = new EquipmentDto("Arme", 100, 50, 0, 0, 0, 100, "AERIAL", null, "FIREARM");
        Equipment equipment = mapper.toDomain(valid);
        assertEquals(100, equipment.getVehicleBonus());
        assertEquals(VehicleBonusTarget.AERIAL, equipment.getVehicleBonusTarget());
    }
}
