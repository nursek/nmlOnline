package com.mg.nmlonline.domain.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.EquipmentFactory;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitType;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerImportService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlayerStatsService playerStatsService;

    // Constructeur pour Spring (injection de dépendances)
    public PlayerImportService(PlayerStatsService playerStatsService) {
        this.playerStatsService = playerStatsService;
    }

    // Constructeur sans argument pour les tests/standalone
    public PlayerImportService() {
        this.playerStatsService = new PlayerStatsService();
    }

    /**
     * Importe un joueur depuis un fichier JSON.
     * Les secteurs doivent être ajoutés au Board séparément via la méthode importSectorsToBoard.
     */
    public Player importPlayerFromJson(String filePath) throws IOException {
        PlayerDTO dto = objectMapper.readValue(new File(filePath), PlayerDTO.class);
        Player player = new Player(dto.name);
        player.getStats().setMoney(dto.money);

        importGeneralEquipments(player, dto.equipments);

        return player;
    }

    /**
     * Importe les secteurs depuis un fichier JSON et les ajoute au Board.
     * Les secteurs sont assignés au joueur dans le Board.
     * Recalcule automatiquement les stats du joueur après l'import.
     */
    public void importSectorsToBoard(String filePath, Player player, Board board) throws IOException {
        PlayerDTO dto = objectMapper.readValue(new File(filePath), PlayerDTO.class);

        if (dto.sectors != null && !dto.sectors.isEmpty()) {
            importSectors(player, board, dto.sectors);
        }

        // Recalculer toutes les stats du joueur maintenant que les secteurs et unités sont chargés
        playerStatsService.recalculateStats(player, board);
    }

    private void importGeneralEquipments(Player player, List<EquipmentDTO> equipments) {
        if (equipments == null) return;
        for (EquipmentDTO equipment : equipments) {
            player.addEquipmentToStack(EquipmentFactory.createFromName(equipment.name), equipment.quantity);
        }
    }

    private void importSectors(Player player, Board board, List<SectorDTO> sectors) {
        for (SectorDTO sectorDto : sectors) {
            // Créer ou récupérer le secteur du Board
            Sector sector = board.getSector(sectorDto.id);
            if (sector == null) {
                sector = new Sector(sectorDto.id, sectorDto.name);
                sector.setIncome(sectorDto.income);
                board.addSector(sector);
            } else {
                sector.setName(sectorDto.name);
                sector.setIncome(sectorDto.income);
            }

            // Ajouter les voisins (neighbors) du secteur
            if (sectorDto.neighbors != null) {
                for (Integer neighborId : sectorDto.neighbors) {
                    sector.addNeighbor(neighborId);
                }
            }

            // Assigner le secteur au joueur
            board.assignOwner(sectorDto.id, player.getId(), "#ffffff");
            player.addOwnedSectorId((long) sectorDto.id);

            // Importer les unités
            importUnitsToSector(player, sector, sectorDto.army);
        }
    }

    private void importUnitsToSector(Player player, Sector sector, List<UnitDTO> units) {
        if (units == null) return;
        for (UnitDTO unitDto : units) {
            Unit unit = createUnitFromDTO(player, unitDto);
            if (unit != null) { // Only add unit if it was created successfully
                sector.addUnit(unit);
            }
        }
    }

    private Unit createUnitFromDTO(Player player, UnitDTO unitDto) {
        // Une unité sans classe est inutilisable
        if (unitDto.classes == null || unitDto.classes.isEmpty()) {
            System.err.println("Impossible de créer une unité sans classe - unité ignorée");
            return null;
        }

        Unit unit = new Unit(unitDto.experience, UnitClass.valueOf(unitDto.classes.get(0)));
        // Convertir le type String en UnitType
        if (unitDto.type != null && !unitDto.type.isEmpty()) {
            unit.setType(UnitType.valueOf(unitDto.type));
        }
        if (unitDto.classes.size() > 1) {
            unit.addSecondClass(UnitClass.valueOf(unitDto.classes.get(1)));
        }
        // Handle "BLESSE" class if present
        if (unitDto.isInjured) {
            unit.setInjured(true);
        }

        if (unitDto.equipments != null) {
            for (String equipmentName : unitDto.equipments) {
                if (player.isEquipmentAvailable(equipmentName) && unit.addEquipment(EquipmentFactory.createFromName(equipmentName)) && !player.decrementEquipmentAvailability(equipmentName)) {
                    System.err.println("Erreur : équipement " + equipmentName + " non disponible pour le joueur " + player.getName());
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
        public List<Integer> neighbors;
        public List<UnitDTO> army;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UnitDTO {
        public int id;
        public String type;
        public List<String> classes = new ArrayList<>();
        public double experience;
        public List<String> equipments = new ArrayList<>();
        public boolean isInjured;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EquipmentDTO {
        public String name;
        public int quantity;
    }
}
