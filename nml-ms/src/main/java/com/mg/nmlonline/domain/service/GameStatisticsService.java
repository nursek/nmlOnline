package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.GameStatisticsDto;
import com.mg.nmlonline.domain.model.statistics.GameStatistic;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.GameStatisticRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GameStatisticsService {

    private static final String CHARACTER_KEY = "CHARACTER";

    private final GameStatisticRepository repository;

    public GameStatisticsService(GameStatisticRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordUnitKill(UnitType type) {
        if (type == null || type == UnitType.PERSONNAGE) {
            return;
        }
        increment("UNIT_" + type.name());
    }

    @Transactional
    public void recordCharacterKill() {
        increment(CHARACTER_KEY);
    }

    @Transactional
    public void recordVehicleKill(VehicleType type) {
        if (type == null) {
            return;
        }
        increment("VEHICLE_" + type.name());
    }

    @Transactional(readOnly = true)
    public GameStatisticsDto getStatistics() {
        Map<String, Long> counts = repository.findAll().stream()
                .collect(Collectors.toMap(GameStatistic::getStatKey, GameStatistic::getTotal));
        GameStatisticsDto dto = new GameStatisticsDto();
        dto.setUnits(Arrays.stream(UnitType.values())
                .filter(type -> type != UnitType.PERSONNAGE)
                .map(type -> entry(type.name(), "UNIT_" + type.name(), unitLabel(type), counts))
                .toList());
        dto.setCharacters(count(CHARACTER_KEY, counts));
        dto.setVehicles(Arrays.stream(VehicleType.values())
                .map(type -> entry(type.name(), "VEHICLE_" + type.name(),
                        type.getDisplayName(), counts))
                .toList());
        return dto;
    }

    private void increment(String key) {
        if (repository.increment(key) == 0) {
            repository.saveAndFlush(new GameStatistic(key, 1));
        }
    }

    private static GameStatisticsDto.Entry entry(String key, String statKey, String label,
                                                 Map<String, Long> counts) {
        return new GameStatisticsDto.Entry(key, label, count(statKey, counts));
    }

    private static long count(String key, Map<String, Long> counts) {
        return counts.getOrDefault(key, 0L);
    }

    private static String unitLabel(UnitType type) {
        return switch (type) {
            case LARBIN -> "Larbins";
            case VOYOU -> "Voyous";
            case MALFRAT -> "Malfrats";
            case BRUTE -> "Brutes";
            case PERSONNAGE -> "Personnages";
        };
    }
}
