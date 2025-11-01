package com.mg.nmlonline.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerExportService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void savePlayerToJson(Player player, Board board, String filePath) throws IOException {
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
        // Récupérer les secteurs du joueur via le Board
        List<Sector> playerSectors = board.getSectorsByOwner(player.getId());
        for (Sector sector : playerSectors) {
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
                unitDTO.equipments = new ArrayList<>();
                // classes : récupérer noms des classes si présents
                if (unit.getClasses() != null) {
                    for (var uc : unit.getClasses()) unitDTO.classes.add(uc.name());
                }
                for (Equipment equipment : unit.getEquipments()) {
                    unitDTO.equipments.add(equipment.getName());
                }
                sectorDTO.army.add(unitDTO);
            }
            dto.sectors.add(sectorDTO);
        }

        objectMapper.writeValue(new File(filePath), dto);
    }

    // --- DTOs internes pour l'export JSON ---
    private static class PlayerDTO {
        public String name;
        public java.util.List<EquipmentDTO> equipments;
        public java.util.List<SectorDTO> sectors;
        public double money;
    }

    private static class SectorDTO {
        public int id;
        public String name;
        public double income;
        public java.util.List<UnitDTO> army;
    }

    private static class UnitDTO {
        public int id;
        public String type;
        public java.util.List<String> classes;
        public double experience;
        public java.util.List<String> equipments;
    }

    private static class EquipmentDTO {
        public String name;
        public int quantity;
    }
}
