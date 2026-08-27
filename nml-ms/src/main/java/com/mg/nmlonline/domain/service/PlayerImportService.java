package com.mg.nmlonline.domain.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.BuildingType;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.Resource;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import com.mg.nmlonline.infrastructure.repository.ResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class PlayerImportService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerImportService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlayerStatsService playerStatsService;
    private final EquipmentRepository equipmentRepository;
    private final ResourceRepository resourceRepository;

    // Cache d'Equipment pour éviter les problèmes de détachement du contexte de persistance
    private final Map<String, Equipment> equipmentCache = new HashMap<>();

    // Constructeur pour Spring (injection de dépendances)
    @Autowired
    public PlayerImportService(PlayerStatsService playerStatsService,
                               EquipmentRepository equipmentRepository,
                               ResourceRepository resourceRepository) {
        this.playerStatsService = playerStatsService;
        this.equipmentRepository = equipmentRepository;
        this.resourceRepository = resourceRepository;
    }


    /**
     * Parse le contenu JSON d'un joueur en PlayerDTO.
     * Point d'entrée unique : le DTO parsé est passé aux méthodes d'import ci-dessous.
     */
    public PlayerDTO parse(String jsonContent) throws IOException {
        return objectMapper.readValue(jsonContent, PlayerDTO.class);
    }

    /**
     * Importe un joueur depuis le DTO (sans les équipements).
     * Les équipements doivent être ajoutés via importEquipments après persistance.
     * Les secteurs doivent être ajoutés au Board via importSectors.
     */
    public Player importPlayer(PlayerDTO dto) {
        Player player = new Player(dto.name);
        player.getStats().setMoney(dto.money);
        // Note: les équipements seront ajoutés après persistance du Player
        return player;
    }

    /**
     * Importe les équipements depuis le DTO et les ajoute au Player.
     * Le Player doit être persisté (avoir un ID) avant d'appeler cette méthode.
     */
    public void importEquipments(PlayerDTO dto, Player player) {
        importGeneralEquipments(player, dto.equipments);
    }

    /**
     * Importe les ressources depuis le DTO et les ajoute au Player.
     * Le Player doit être persisté (avoir un ID) avant d'appeler cette méthode.
     */
    public void importResources(PlayerDTO dto, Player player) {
        importResources(player, dto.resources);
    }

    /**
     * Importe le personnage principal (GameCharacter) depuis le DTO et l'associe au Player et au Sector.
     * Le Player doit être persisté (avoir un ID) avant d'appeler cette méthode.
     *
     * @return le GameCharacter créé, ou null si aucun personnage n'est défini dans le JSON
     */
    public GameCharacter importCharacter(PlayerDTO dto, Player player, Board board) {
        if (dto.character == null || dto.character.name == null) {
            return null;
        }

        GameCharacter character = new GameCharacter(
                dto.character.name,
                dto.character.baseAttack,
                dto.character.basePdf,
                dto.character.basePdc,
                dto.character.baseDefense,
                dto.character.baseArmor,
                dto.character.baseEvasion
        );
        character.setPlayerId(player.getId());
        player.setCharacter(character);

        // Lier le personnage au secteur via CombatEntity.sector
        if (dto.character.sectorNumber > 0) {
            Sector sector = board.getSector(dto.character.sectorNumber);
            if (sector != null) {
                character.setSector(sector);
            } else {
                logger.warn("Secteur {} non trouvé pour le personnage {}", dto.character.sectorNumber, character.getName());
            }
        }

        return character;
    }

    /**
     * Importe les bâtiments depuis le DTO et les ajoute au Player et au Board.
     * Le Player doit être persisté (avoir un ID) avant d'appeler cette méthode.
     *
     * @return la liste des bâtiments créés
     */
    public List<Building> importBuildings(PlayerDTO dto, Player player, Board board) {
        if (dto.buildings == null || dto.buildings.isEmpty()) {
            return List.of();
        }

        List<Building> buildings = new ArrayList<>();
        for (BuildingDTO buildingDto : dto.buildings) {
            BuildingType type = BuildingType.valueOf(buildingDto.type);
            Building building = switch (type) {
                case HEADQUARTERS -> new Headquarters(player.getId());
                case WEAPON_CACHE -> new WeaponCache(player.getId());
                case BANK -> new Bank(player.getId());
            };

            // Placer le bâtiment dans le secteur du Board
            Sector sector = board.getSector(buildingDto.sectorNumber);
            if (sector != null) {
                building.setSector(sector);
            }

            player.getBuildings().add(building);
            buildings.add(building);
        }

        return buildings;
    }

    /**
     * Importe les secteurs depuis le DTO et les ajoute au Board.
     * Les secteurs sont assignés au joueur dans le Board.
     * Recalcule automatiquement les stats du joueur après l'import.
     */
    public void importSectors(PlayerDTO dto, Player player, Board board) {
        if (dto.sectors != null && !dto.sectors.isEmpty()) {
            importSectors(player, board, dto.sectors);
        }

        // Recalculer toutes les stats du joueur maintenant que les secteurs et unités sont chargés
        playerStatsService.recalculateStats(player, board);
    }

    /**
     * Récupère un Equipment depuis le cache ou la BDD.
     * Les Equipment sont pré-chargés via equipments.csv, on ne crée jamais de nouveaux Equipment ici.
     * compatibleClasses (LAZY) est initialisé avant le cache car l'Equipment sera détaché hors de
     * la tx englobante (self-invocation → @Transactional de la méthode est ignoré, seule la tx
     * de la classe/import équipments couvre).
     */
    public Equipment getEquipmentByName(String equipmentName) {
        // 1. Vérifier le cache en premier
        if (equipmentCache.containsKey(equipmentName)) {
            return equipmentCache.get(equipmentName);
        }

        if (equipmentRepository == null) {
            logger.warn("equipmentRepository est null - mode standalone non supporté");
            return null;
        }

        // 2. Chercher l'equipment existant en BDD
        Optional<Equipment> existingEquipment = equipmentRepository.findByName(equipmentName);
        if (existingEquipment.isPresent()) {
            Equipment eq = existingEquipment.get();
            // compatibleClasses est LAZY : on l'initialise maintenant (session ouverte)
            // car les Equipment sont mis en cache et réutilisés hors session (imports multi-étapes).
            Hibernate.initialize(eq.getCompatibleClasses());
            equipmentCache.put(equipmentName, eq);
            return eq;
        }

        // L'équipement n'existe pas — c'est une erreur (devrait être dans equipments.csv).
        logger.warn("Équipement '{}' non trouvé en BDD (vérifier equipments.csv)", equipmentName);
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

    private void importResources(Player player, List<ResourceDTO> resources) {
        if (resources == null) return;
        for (ResourceDTO resourceDto : resources) {
            Optional<Resource> resourceOpt = resourceRepository.findById(resourceDto.resourceId);
            if (resourceOpt.isPresent()) {
                Resource resource = resourceOpt.get();
                player.addResource(resource.getName(), resourceDto.quantity);
            } else {
                logger.warn("Ressource avec ID '{}' non trouvée en BDD", resourceDto.resourceId);
            }
        }
    }

    private void importSectors(Player player, Board board, List<SectorDTO> sectors) {
        for (SectorDTO sectorDto : sectors) {
            // Récupérer le secteur existant du Board (doit exister dans board.json)
            Sector sector = board.getSector(sectorDto.sectorNumber);
            if (sector == null) {
                logger.warn("Secteur {} non trouvé dans le Board - ignoré", sectorDto.sectorNumber);
                continue;
            }

            // Assigner le secteur au joueur (source unique de vérité : Sector.ownerId)
            board.assignOwner(sectorDto.sectorNumber, player.getId(), "#ffffff");

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
        // Convertir le type String en UnitType
        UnitType type = (unitDto.type != null && !unitDto.type.isEmpty())
                ? UnitType.valueOf(unitDto.type) : null;
        Unit unit = BoardImportService.createUnit(unitDto.classes, type, unitDto.experience);
        if (unit == null) {
            logger.warn("Impossible de créer une unité sans classe - unité ignorée");
            return null;
        }

        // Définir le playerId pour accès direct
        unit.setPlayerId(player.getId());
        // Handle "BLESSE" class if present
        if (unitDto.isInjured) {
            unit.setInjured(true);
        }


        if (unitDto.equipments != null) {
            for (String equipmentName : unitDto.equipments) {
                Equipment equipment = getEquipmentByName(equipmentName);
                if (equipment == null) {
                    logger.warn("Équipement '{}' n'existe pas en BDD (absent de equipments.csv)", equipmentName);
                } else if (!player.isEquipmentAvailable(equipmentName)) {
                    logger.warn("Équipement '{}' non disponible dans l'inventaire du joueur {}", equipmentName, player.getName());
                } else {
                    if (unit.addEquipment(equipment)) {
                        player.decrementEquipmentAvailability(equipmentName);
                    }
                }
            }
        }
        return unit;
    }

    // --- DTOs internes pour l'import JSON ---
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayerDTO {
        public String name;
        public List<EquipmentDTO> equipments;
        public List<ResourceDTO> resources;
        public List<SectorDTO> sectors;
        public double money;
        public CharacterDTO character;
        public List<BuildingDTO> buildings;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SectorDTO {
        @JsonProperty("sectorNumber")
        public int sectorNumber;
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
    static class CharacterDTO {
        public String name;
        public int sectorNumber;
        public double baseAttack;
        public double baseDefense;
        public double basePdf;
        public double basePdc;
        public double baseArmor;
        public double baseEvasion;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BuildingDTO {
        public String type; // HEADQUARTERS, WEAPON_CACHE, BANK
        public int sectorNumber;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EquipmentDTO {
        public String name;
        public int quantity;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ResourceDTO {
        public Long resourceId;
        public int quantity;
    }
}
