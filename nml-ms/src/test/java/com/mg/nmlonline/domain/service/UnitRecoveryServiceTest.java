package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnitRecoveryService — récupération de fin de tour")
class UnitRecoveryServiceTest {

    @Mock
    UnitRepository unitRepository;

    @InjectMocks
    UnitRecoveryService service;

    @Test
    @DisplayName("Armure intégralement restaurée sans soigner la défense")
    void restoresArmorWithoutHealingDefense() {
        Unit unit = new Unit(5, UnitClass.TIREUR);
        unit.addEquipment(new Equipment("Protection", 100, 0, 0, 25, 0,
                Set.of(UnitClass.TIREUR), EquipmentCategory.DEFENSIVE));
        unit.setArmor(3);
        when(unitRepository.findByBoardId(1L)).thenReturn(List.of(unit));

        service.recoverAtTurnEnd(10, 1L);

        assertEquals(12.5, unit.getArmor());
        assertEquals(50.0, unit.getDefense());
    }
}
