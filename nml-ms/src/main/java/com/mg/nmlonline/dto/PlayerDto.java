package com.mg.nmlonline.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlayerDto {
    public Long id;
    public String name;
    public double money;
    public List<EquipmentDto> equipments;
    public List<SectorDto> sectors;

    public static class EquipmentDto {
        public String name;
        public int quantity;
    }

    public static class SectorDto {
        public int number;
        public String name;
        public double income;
        public List<UnitDto> army;
    }

    public static class UnitDto {
        public int id;
        public String type;
        public double experience;
        public List<String> equipments;
    }
}
