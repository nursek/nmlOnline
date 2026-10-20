package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.GameStatisticsDto;
import com.mg.nmlonline.domain.model.statistics.GameStatistic;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.GameStatisticRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameStatisticsService — compteurs de pertes")
class GameStatisticsServiceTest {

    @Mock
    private GameStatisticRepository repository;

    @InjectMocks
    private GameStatisticsService service;

    @Test
    @DisplayName("incrémente par type et expose toutes les catégories de pertes")
    void incrementsAndBuildsStatistics() {
        when(repository.increment("UNIT_LARBIN")).thenReturn(1);
        when(repository.increment("CHARACTER")).thenReturn(1);
        when(repository.increment("VEHICLE_TANK")).thenReturn(1);
        when(repository.findAll()).thenReturn(List.of(
                new GameStatistic("UNIT_LARBIN", 2),
                new GameStatistic("UNIT_VOYOU", 1),
                new GameStatistic("CHARACTER", 1),
                new GameStatistic("VEHICLE_TANK", 3)));

        service.recordUnitKill(UnitType.LARBIN);
        service.recordUnitKill(UnitType.PERSONNAGE);
        service.recordCharacterKill();
        service.recordVehicleKill(VehicleType.TANK);

        verify(repository).increment("UNIT_LARBIN");
        verify(repository).increment("CHARACTER");
        verify(repository).increment("VEHICLE_TANK");
        verify(repository, never()).increment("UNIT_PERSONNAGE");

        GameStatisticsDto statistics = service.getStatistics();

        assertEquals(List.of("LARBIN", "VOYOU", "MALFRAT", "BRUTE"),
                statistics.getUnits().stream().map(GameStatisticsDto.Entry::getKey).toList());
        assertEquals(List.of(2L, 1L, 0L, 0L),
                statistics.getUnits().stream().map(GameStatisticsDto.Entry::getCount).toList());
        assertEquals("Larbins", statistics.getUnits().getFirst().getLabel());
        assertEquals(1, statistics.getCharacters());
        assertEquals(6, statistics.getVehicles().size());
        assertEquals("Tank de combat", statistics.getVehicles().get(3).getLabel());
        assertEquals(3, statistics.getVehicles().get(3).getCount());
    }

    @Test
    @DisplayName("crée le compteur absent avant de l'incrémenter")
    void createsMissingCounter() {
        when(repository.increment("UNIT_LARBIN")).thenReturn(0);

        service.recordUnitKill(UnitType.LARBIN);

        verify(repository).saveAndFlush(argThat(statistic ->
                "UNIT_LARBIN".equals(statistic.getStatKey()) && statistic.getTotal() == 1));
    }
}
