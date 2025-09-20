package com.mg.nmlonline.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.model.player.Player;
import com.mg.nmlonline.model.sector.Sector;
import com.mg.nmlonline.model.unit.Unit;
import com.mg.nmlonline.model.unit.UnitClass;
import com.mg.nmlonline.model.equipement.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    // === JSON IMPORT METHOD ===
    public Player importPlayerFromJson(String filePath) throws IOException {
        PlayerDTO dto = objectMapper.readValue(new File(filePath), PlayerDTO.class);
        Player player = new Player(dto.name);
        player.getStats().setMoney(dto.money);

        importGeneralEquipments(player, dto.equipments);

        if (dto.sectors != null && !dto.sectors.isEmpty()) {
            importSectors(player, dto.sectors);
        } else {
            importDefaultSector(player, dto.army);
        }
        return player;
    }

    private void importGeneralEquipments(Player player, List<EquipmentDTO> equipments) {
        if (equipments == null) return;
        for (EquipmentDTO equipment : equipments) {
            player.addEquipmentToStack(EquipmentFactory.createFromName(equipment.name), equipment.quantity);
        }
    }

    private void importSectors(Player player, List<SectorDTO> sectors) {
        for (SectorDTO sectorDto : sectors) {
            Sector sector = new Sector(sectorDto.id, sectorDto.name);
            sector.setIncome(sectorDto.income);
            player.addSector(sector);
            importUnitsToSector(player, sectorDto.army, sectorDto.id);
        }
    }

    private void importUnitsToSector(Player player, List<UnitDTO> units, int sectorId) {
        if (units == null) return;
        for (UnitDTO unitDto : units) {
            Unit unit = createUnitFromDTO(unitDto);
            player.addUnitToSector(unit, sectorId);
        }
    }

    private void importDefaultSector(Player player, List<UnitDTO> units) {
        Sector defaultSector = new Sector(1);
        player.addSector(defaultSector);
        importUnitsToSector(player, units, 1);
    }

    private Unit createUnitFromDTO(UnitDTO unitDto) {
        Unit unit = new Unit(unitDto.id, unitDto.type, UnitClass.valueOf(unitDto.classes.get(0)));
        unit.gainExperience(unitDto.experience);
        if (unitDto.classes.size() > 1) {
            unit.addSecondClass(UnitClass.valueOf(unitDto.classes.get(1)));
        }
        // Implement better logic to add equipement with quantity and availability

        for (String equipment : unitDto.equipments) {
            unit.addEquipment(EquipmentFactory.createFromName(equipment));
            //TODO: handle equipement stock
        }
        return unit;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlayerDTO {
        public String name;
        public List<UnitDTO> army;
        public List<EquipmentDTO> equipments;
        public List<SectorDTO> sectors;
        public double money;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SectorDTO {
        @JsonProperty("number")
        public int id;
        public String name;
        public double income;
        public List<UnitDTO> army;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UnitDTO {
        public int id;
        public String type;
        public List<String> classes;
        public double experience;
        public List<String> equipments = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EquipmentDTO {
        public String name;
        public int quantity;
    }
}