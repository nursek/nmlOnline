package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class PlayerDto {
    public Long id;
    public String name;
    public PlayerStatsDto stats; // remplace le champ primitif money
    public List<EquipmentDto> equipments;
    public List<SectorDto> sectors;

    @Data
    public static class EquipmentDto {
        public String name;
        public Integer quantity; // Integer pour permettre null
    }

    @Data
    public static class UnitDto {
        public Integer id;
        public String type;
        public Double experience;
        public List<String> equipments;
        public Boolean isInjured;
    }

    @Data
    public static class SectorDto {
        public Integer number;
        public String name;
        public Double income; // Double pour permettre null
        public List<UnitDto> army;
    }

    @Data
    public static class PlayerStatsDto {
        public Double money;
        public Double totalIncome;
        public Double totalVehiclesValue;
        public Double totalEquipmentValue;
        public Double totalOffensivePower;
        public Double totalDefensivePower;
        public Double globalPower;
        public Double totalEconomyPower;
        public Double totalAtk;
        public Double totalPdf;
        public Double totalPdc;
        public Double totalDef;
        public Double totalArmor;
    }
}
