package com.mg.nmlonline.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.entity.equipment.EquipmentFactory;
import com.mg.nmlonline.entity.player.Player;
import com.mg.nmlonline.entity.sector.Sector;
import com.mg.nmlonline.entity.unit.Unit;
import com.mg.nmlonline.entity.unit.UnitClass;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerImportService {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
            if (!player.addUnitToSector(unit, sectorId)) {
                // comportement conservé : si l'ajout échoue, on quitte
                return;
            }
        }
    }

    private Unit createUnitFromDTO(Player player, UnitDTO unitDto) {
        Unit unit = new Unit(unitDto.experience, unitDto.type, UnitClass.valueOf(unitDto.classes.get(0)));
        if (unitDto.classes != null && unitDto.classes.size() > 1) {
            unit.addSecondClass(UnitClass.valueOf(unitDto.classes.get(1)));
        }

        if (unitDto.equipments != null) {
            for (String equipmentName : unitDto.equipments) {
                if (player.isEquipmentAvailable(equipmentName)) {
                    if (unit.addEquipment(EquipmentFactory.createFromName(equipmentName))) {
                        if (!player.decrementEquipmentAvailability(equipmentName)) {
                            System.err.println("Erreur : équipement " + equipmentName + " non disponible pour le joueur " + player.getName());
                        }
                    }
                }
            }
        }
        return unit;
    }

    // --- DTOs internes pour l'import JSON ---
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
        public List<String> classes = new ArrayList<>();
        public double experience;
        public List<String> equipments = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EquipmentDTO {
        public String name;
        public int quantity;
    }
}
