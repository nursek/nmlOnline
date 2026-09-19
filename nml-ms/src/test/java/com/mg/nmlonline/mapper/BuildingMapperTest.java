package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.BuildingDto;
import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.service.TurnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuildingMapper — parts de fortune")
class BuildingMapperTest {

    @Mock
    EquipmentMapper equipmentMapper;

    @Mock
    TurnService turnService;

    private BuildingMapper buildingMapper;

    @BeforeEach
    void setUp() {
        buildingMapper = new BuildingMapper(equipmentMapper, turnService);
    }

    @Test
    @DisplayName("Banque 75 % et QG 25 % de la fortune du propriétaire")
    void shouldSplitOwnerWealthBetweenBankAndHeadquarters() {
        when(turnService.getCurrentTurn()).thenReturn(3);

        BuildingDto bank = buildingMapper.toDto(new Bank(1L), 10000.0);
        BuildingDto hq = buildingMapper.toDto(new Headquarters(1L), 10000.0);

        assertEquals(7500.0, bank.getStoredMoney());
        assertEquals(2500.0, hq.getStoredWealth());
    }

    @Test
    @DisplayName("Sans propriétaire : aucune part de fortune calculée")
    void shouldNotComputeWealthWithoutOwner() {
        when(turnService.getCurrentTurn()).thenReturn(3);

        BuildingDto bank = buildingMapper.toDto(new Bank(1L));
        BuildingDto hq = buildingMapper.toDto(new Headquarters(1L));

        assertEquals(0.0, bank.getStoredMoney());
        assertNull(hq.getStoredWealth());
    }
}
