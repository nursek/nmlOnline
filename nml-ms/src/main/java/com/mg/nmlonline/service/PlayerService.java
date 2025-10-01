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
            Unit unit = createUnitFromDTO(player, unitDto);
            player.addUnitToSector(unit, sectorId);
        }
    }

    private Unit createUnitFromDTO(Player player, UnitDTO unitDto) {
        Unit unit = new Unit(unitDto.experience, unitDto.type, UnitClass.valueOf(unitDto.classes.get(0)));
        if (unitDto.classes.size() > 1) {
            unit.addSecondClass(UnitClass.valueOf(unitDto.classes.get(1)));
        }
        //TODO Implement better logic to add equipement with quantity and availability

        for (String equipment : unitDto.equipments) {
            if(player.isEquipmentAvailable(equipment) && unit.addEquipment(EquipmentFactory.createFromName(equipment)) && !player.decrementEquipmentAvailability(equipment)){
                        System.err.println("Erreur : équipement " + equipment + " non disponible pour le joueur " + player.getName());
            }
        }
        return unit;
    }

    public void savePlayerToJson(Player player, String filePath) throws IOException {
        PlayerDTO dto = new PlayerDTO();
        dto.name = player.getName();
        dto.money = player.getStats().getMoney();
        dto.equipments = new ArrayList<>();
        for (EquipmentStack stack : player.getEquipments()) {
            EquipmentDTO equipmentDTO = new EquipmentDTO();
            equipmentDTO.name = stack.getEquipment().getName();
            equipmentDTO.quantity = stack.getQuantity();
            dto.equipments.add(equipmentDTO);
        }
        dto.sectors = new ArrayList<>();
        for (Sector sector : player.getSectors()) {
            SectorDTO sectorDTO = new SectorDTO();
            sectorDTO.id = sector.getNumber();
            sectorDTO.name = sector.getName();
            sectorDTO.income = sector.getIncome();
            sectorDTO.army = new ArrayList<>();
            for (Unit unit : sector.getUnits()) {
                UnitDTO unitDTO = new UnitDTO();
                unitDTO.id = unit.getId();
                unitDTO.type = String.valueOf(unit.getType());
                unitDTO.experience = unit.getExperience();
                unitDTO.classes = new ArrayList<>();
//TODO               unitDTO.classes.add(unit.getClasses().toString()); // Check ici, possible bugs
//                if (unit.getSecondClass() != null) {
//                    unitDTO.classes.add(unit.getSecondClass().name());
//                }
                unitDTO.equipments = new ArrayList<>();
                for (Equipment equipment : unit.getEquipments()) {
                    unitDTO.equipments.add(equipment.getName());
                }
                sectorDTO.army.add(unitDTO);
            }
            dto.sectors.add(sectorDTO);
        }
        objectMapper.writeValue(new File(filePath), dto);
        }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlayerDTO {
        public String name;
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