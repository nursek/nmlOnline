package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BattleHighlightDto;
import com.mg.nmlonline.api.dto.BattleReportDto;
import com.mg.nmlonline.domain.model.battle.BattleLogEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class BattleHighlights {

    public static final String PUBLIC = "PUBLIC";
    public static final String ADMIN = "ADMIN";

    private BattleHighlights() {
    }

    public static List<BattleHighlightDto> build(List<BattleReportDto.PlayerForceDto> forces,
                                                 List<CombatService.CasualtyInfo> casualties,
                                                 List<CombatService.ExperienceGain> gains,
                                                 String winnerName,
                                                 boolean headquartersCaptured,
                                                 int capturedBuildings,
                                                 List<BattleLogEntry> log) {
        List<BattleHighlightDto> highlights = new ArrayList<>();
        Map<Long, String> names = new HashMap<>();
        forces.forEach(force -> names.put(force.getPlayerId(), force.getPlayerName()));

        add(highlights, "WINNER", 1, PUBLIC,
                winnerName == null ? "Aucun vainqueur" : "Vainqueur : " + winnerName);
        casualties.stream()
                .filter(casualty -> "CHARACTER".equals(casualty.category()))
                .map(casualty -> names.get(casualty.playerId()))
                .distinct()
                .forEach(name -> add(highlights, "CHARACTER_LOST", 1, PUBLIC, "Personnage perdu : " + name));
        if (headquartersCaptured) {
            add(highlights, "HEADQUARTERS_CAPTURED", 1, PUBLIC, "Quartier général capturé");
        }
        casualties.stream()
                .filter(casualty -> "VEHICLE".equals(casualty.category()))
                .forEach(casualty -> add(highlights, "VEHICLE_DESTROYED", 2, PUBLIC,
                        "Véhicule détruit : " + casualty.label() + owner(casualty.playerId(), names)));

        long criticals = log == null ? 0 : log.stream()
                .filter(entry -> BattleLogEntry.CRIT.equals(entry.outcome()))
                .count();
        if (criticals > 0) {
            add(highlights, "CRIT", 2, ADMIN, criticals + " coup(s) critique(s)");
        }

        gains.stream()
                .filter(gain -> gain.typeAfter() != gain.typeBefore())
                .forEach(gain -> add(highlights, "PROMOTION", 3, PUBLIC,
                        "Promotion : " + gain.typeBefore() + " n°" + gain.unitNumber() + " → " + gain.typeAfter()
                                + owner(gain.playerId(), names)));
        if (capturedBuildings > 0) {
            add(highlights, "BUILDING_CAPTURED", 3, PUBLIC,
                    capturedBuildings + " bâtiment(s) capturé(s)");
        }

        Map<Long, Long> losses = casualties.stream()
                .collect(Collectors.groupingBy(CombatService.CasualtyInfo::playerId, Collectors.counting()));
        for (BattleReportDto.PlayerForceDto force : forces) {
            long lost = losses.getOrDefault(force.getPlayerId(), 0L);
            int engaged = engaged(force);
            if (lost > 0 && engaged > 0 && lost * 2 >= engaged) {
                add(highlights, "HEAVY_LOSSES", 3, PUBLIC,
                        "Pertes lourdes : " + force.getPlayerName() + " (" + lost + "/" + engaged + ")");
            }
        }
        return highlights;
    }

    private static int engaged(BattleReportDto.PlayerForceDto force) {
        return size(force.getCharacters()) + size(force.getVehicles())
                + size(force.getInfantry()) + size(force.getBuildings());
    }

    private static int size(List<?> entities) {
        return entities == null ? 0 : entities.size();
    }

    private static String owner(Long playerId, Map<Long, String> names) {
        String name = names.get(playerId);
        return name == null ? "" : " (" + name + ")";
    }

    private static void add(List<BattleHighlightDto> highlights, String kind, int priority,
                            String visibility, String message) {
        BattleHighlightDto highlight = new BattleHighlightDto();
        highlight.setKind(kind);
        highlight.setPriority(priority);
        highlight.setVisibility(visibility);
        highlight.setMessage(message);
        highlights.add(highlight);
    }
}
