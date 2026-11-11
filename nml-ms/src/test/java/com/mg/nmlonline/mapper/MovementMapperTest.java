package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.MovementResolutionResultDto;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.movement.TransitCombatResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MovementMapper — combat de transit")
class MovementMapperTest {

    @Test
    @DisplayName("Le DTO conserve le nom et le type du véhicule engagé en transit")
    void transitCombatKeepsReadableVehicle() {
        MovementResolutionResult result = new MovementResolutionResult();
        result.addTransitCombat(new TransitCombatResult(7, 3L, true, "Rhino", "VTT_BLINDE", 1L));

        MovementResolutionResultDto dto = new MovementMapper().toResolutionDto(result, 4, id -> null);

        assertEquals(1, dto.getTransitCombats().size());
        var transit = dto.getTransitCombats().getFirst();
        assertEquals(7, transit.getSectorNumber());
        assertEquals("Rhino", transit.getVehicleName());
        assertEquals("VTT_BLINDE", transit.getVehicleType());
        assertEquals(1L, transit.getOwnerPlayerId());
        assertTrue(transit.isVehicleFired());
    }
}
