package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScenarioSummaryDto {
    private int turn;
    private boolean standoff;
    private ActorDto attacker;
    private ActorDto defender;
    private UnitDto attackerUnit;
    private int defendersAdded;
    private List<Integer> route;
    private Long orderId;
    private List<OrderDto> orders;
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

    @Data
    public static class OrderDto {
        private Long playerId;
        private String playerName;
        private Long unitId;
        private String unitClass;
        private int fromSector;
        private List<Integer> route;
        private Long orderId;
    }
}