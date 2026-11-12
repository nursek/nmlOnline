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

    private static final Long BOARD_ID = 1L;

    @Mock
    private GameStatisticRepository repository;

    @InjectMocks
    private GameStatisticsService service;

    @Test
    @DisplayName("incrémente par type et expose toutes les catégories de pertes")
    void incrementsAndBuildsStatistics() {
        when(repository.increment(BOARD_ID, "UNIT_LARBIN")).thenReturn(1);
        when(repository.increment(BOARD_ID, "CHARACTER")).thenReturn(1);
        when(repository.increment(BOARD_ID, "VEHICLE_TANK")).thenReturn(1);
        when(repository.findByBoardId(BOARD_ID)).thenReturn(List.of(
                new GameStatistic(BOARD_ID, "UNIT_LARBIN", 2),
                new GameStatistic(BOARD_ID, "UNIT_VOYOU", 1),
                new GameStatistic(BOARD_ID, "CHARACTER", 1),
                new GameStatistic(BOARD_ID, "VEHICLE_TANK", 3)));

        service.recordUnitKill(BOARD_ID, UnitType.LARBIN);
        service.recordUnitKill(BOARD_ID, UnitType.PERSONNAGE);
        service.recordCharacterKill(BOARD_ID);
        service.recordVehicleKill(BOARD_ID, VehicleType.TANK);

        verify(repository).increment(BOARD_ID, "UNIT_LARBIN");
        verify(repository).increment(BOARD_ID, "CHARACTER");
        verify(repository).increment(BOARD_ID, "VEHICLE_TANK");
        verify(repository, never()).increment(BOARD_ID, "UNIT_PERSONNAGE");

        GameStatisticsDto statistics = service.getStatistics(BOARD_ID);

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
        when(repository.increment(BOARD_ID, "UNIT_LARBIN")).thenReturn(0);

        service.recordUnitKill(BOARD_ID, UnitType.LARBIN);

        verify(repository).saveAndFlush(argThat(statistic ->
                "UNIT_LARBIN".equals(statistic.getStatKey()) && statistic.getTotal() == 1));
    }
}
