package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

/**
 * Résumé d'un scénario de test pas-à-pas seedé par l'admin (dev uniquement).
 * Décrit les acteurs, la route de l'attaquant, l'ordre créé et un message
 * d'accompagnement à afficher dans l'UI admin.
 */
@Data
public class ScenarioSummaryDto {
    private int turn;
    private ActorDto attacker;
    private ActorDto defender;
    private UnitDto attackerUnit;
    private int defendersAdded;
    private List<Integer> route;
    private Long orderId;
    private String message;

    @Data
    public static class ActorDto {
        private Long id;
        private String name;
    }

    @Data
    public static class UnitDto {
        private Long id;
        private String unitClass;
        private int fromSector;
    }
}