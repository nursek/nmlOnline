package com.mg.nmlonline.domain.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PlayerImportService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlayerStatsService playerStatsService;
    private final EquipmentRepository equipmentRepository;

    // Cache d'Equipment pour éviter les problèmes de détachement du contexte de persistance
    private final Map<String, Equipment> equipmentCache = new HashMap<>();

    // Constructeur pour Spring (injection de dépendances)
    @Autowired
    public PlayerImportService(PlayerStatsService playerStatsService, EquipmentRepository equipmentRepository) {
        this.playerStatsService = playerStatsService;
        this.equipmentRepository = equipmentRepository;
    }


    /**
     * Importe un joueur depuis un fichier JSON (sans les équipements).
     * Les équipements doivent être ajoutés via importEquipmentsToPlayer après persistance.
     * Les secteurs doivent être ajoutés au Board via importSectorsToBoard.
     */
    public Player importPlayerFromJson(String filePath) throws IOException {
        PlayerDTO dto = objectMapper.readValue(new File(filePath), PlayerDTO.class);
        Player player = new Player(dto.name);
        player.getStats().setMoney(dto.money);
        // Note: les équipements seront ajoutés après persistance du Player
        return player;
    }

    /**
     * Importe les équipements depuis un fichier JSON et les ajoute au Player.
     * Le Player doit être persisté (avoir un ID) avant d'appeler cette méthode.
     */
    public void importEquipmentsToPlayer(String filePath, Player player) throws IOException {
        PlayerDTO dto = objectMapper.readValue(new File(filePath), PlayerDTO.class);
        importGeneralEquipments(player, dto.equipments);
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

    /**
     * Récupère un Equipment depuis le cache ou la BDD.
     * Les Equipment sont pré-chargés via data.sql, on ne crée jamais de nouveaux Equipment ici.
     */
    public Equipment getEquipmentByName(String equipmentName) {
        // 1. Vérifier le cache en premier
        if (equipmentCache.containsKey(equipmentName)) {
            return equipmentCache.get(equipmentName);
        }

        if (equipmentRepository == null) {
            System.err.println("WARN: equipmentRepository est null - mode standalone non supporté");
            return null;
        }

        // 2. Chercher l'equipment existant en BDD
        Optional<Equipment> existingEquipment = equipmentRepository.findByName(equipmentName);
        if (existingEquipment.isPresent()) {
            Equipment eq = existingEquipment.get();
            equipmentCache.put(equipmentName, eq);
            return eq;
        }

        // L'equipment n'existe pas - c'est une erreur (devrait être dans data.sql)
        System.err.println("WARN: Équipement '" + equipmentName + "' non trouvé en BDD (vérifier data.sql)");
        return null;
    }

    /**
     * Vide le cache d'Equipment (à appeler après chaque import complet si nécessaire)
     */
    public void clearEquipmentCache() {
        equipmentCache.clear();
    }

    private void importGeneralEquipments(Player player, List<EquipmentDTO> equipments) {
        if (equipments == null) return;
        for (EquipmentDTO equipmentDto : equipments) {
            Equipment equipment = getEquipmentByName(equipmentDto.name);
            if (equipment != null) {
                player.addEquipmentToStack(equipment, equipmentDto.quantity);
            }
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
        // Définir le playerId pour accès direct
        unit.setPlayerId(player.getId());
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
                Equipment equipment = getEquipmentByName(equipmentName);
                if (equipment != null && player.isEquipmentAvailable(equipmentName)) {
                    if (unit.addEquipment(equipment)) {
                        player.decrementEquipmentAvailability(equipmentName);
                    }
                } else {
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
