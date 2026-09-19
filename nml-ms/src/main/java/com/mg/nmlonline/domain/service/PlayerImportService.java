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
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import com.mg.nmlonline.infrastructure.repository.ResourceRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
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
    private final VehicleRepository vehicleRepository;

    // Cache d'Equipment pour éviter le détachement hors de la transaction d'import.
    private final Map<String, Equipment> equipmentCache = new HashMap<>();

    @Autowired
    public PlayerImportService(PlayerStatsService playerStatsService,
                               EquipmentRepository equipmentRepository,
                               ResourceRepository resourceRepository,
                               VehicleRepository vehicleRepository) {
        this.playerStatsService = playerStatsService;
        this.equipmentRepository = equipmentRepository;
        this.resourceRepository = resourceRepository;
        this.vehicleRepository = vehicleRepository;
    }


    public PlayerDTO parse(String jsonContent) throws IOException {
        return objectMapper.readValue(jsonContent, PlayerDTO.class);
    }

    /** Joueur sans équipements : ajouter via importEquipments après persistance, secteurs via importSectors. */
    public Player importPlayer(PlayerDTO dto) {
        Player player = new Player(dto.name);
        player.getStats().setMoney(dto.money);
        player.initializeStartingMoney();
        return player;
    }

    /** Le Player doit être persisté (avoir un ID) avant l'appel. */
    public void importEquipments(PlayerDTO dto, Player player) {
        importGeneralEquipments(player, dto.equipments);
    }

    /** Le Player doit être persisté (avoir un ID) avant l'appel. */
    public void importResources(PlayerDTO dto, Player player) {
        importResources(player, dto.resources);
    }

    /** Le Player doit être persisté (avoir un ID) avant l'appel. Retourne null si aucun personnage. */
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

    /** Le Player doit être persisté (avoir un ID) avant l'appel. */
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

            Sector sector = board.getSector(buildingDto.sectorNumber);
            if (sector != null) {
                building.setSector(sector);
            }

            player.getBuildings().add(building);
            buildings.add(building);
        }

        return buildings;
    }

    /** Le Player doit être persisté (avoir un ID) avant l'appel. À appeler avant importSectors pour inclure les véhicules déployés dans les stats. */
    public void importVehicles(PlayerDTO dto, Player player, Board board) {
        if (dto.vehicles == null || dto.vehicles.isEmpty()) {
            return;
        }

        for (VehicleDTO vehicleDto : dto.vehicles) {
            if (vehicleDto.type == null || vehicleDto.quantity < 1) {
                continue;
            }

            VehicleType vehicleType;
            try {
                vehicleType = VehicleType.valueOf(vehicleDto.type);
            } catch (IllegalArgumentException e) {
                logger.warn("Type de véhicule inconnu '{}' - ignoré", vehicleDto.type);
                continue;
            }

            Sector sector = vehicleDto.sectorNumber != null ? board.getSector(vehicleDto.sectorNumber) : null;
            if (vehicleDto.sectorNumber != null && sector == null) {
                logger.warn("Secteur {} non trouvé pour le véhicule {} - placé en stock", vehicleDto.sectorNumber, vehicleDto.type);
            }

            for (int i = 0; i < vehicleDto.quantity; i++) {
                Vehicle vehicle = new Vehicle(vehicleType, player.getId());
                if (sector != null) {
                    vehicle.setSector(sector);
                    sector.getVehicles().add(vehicle);
                }
                vehicleRepository.save(vehicle);
                player.getStats().setTotalVehiclesValue(
                        player.getStats().getTotalVehiclesValue() + vehicleType.getCost());
            }
        }
    }

    /** Recalcule les stats du joueur après l'import des secteurs et unités. */
    public void importSectors(PlayerDTO dto, Player player, Board board) {
        if (dto.sectors != null && !dto.sectors.isEmpty()) {
            importSectors(player, board, dto.sectors);
        }

        playerStatsService.recalculateStats(player, board);
    }

    /**
     * Equipment depuis le cache ou la BDD (jamais créé ici : provient d'equipments.csv).
     * compatibleClasses (LAZY) est initialisé avant le cache car l'Equipment sera réutilisé
     * hors session (self-invocation → @Transactional ignoré, seule la tx d'import couvre).
     */
    public Equipment getEquipmentByName(String equipmentName) {
        if (equipmentCache.containsKey(equipmentName)) {
            return equipmentCache.get(equipmentName);
        }

        if (equipmentRepository == null) {
            logger.warn("equipmentRepository est null - mode standalone non supporté");
            return null;
        }

        Optional<Equipment> existingEquipment = equipmentRepository.findByName(equipmentName);
        if (existingEquipment.isPresent()) {
            Equipment eq = existingEquipment.get();
            Hibernate.initialize(eq.getCompatibleClasses());
            equipmentCache.put(equipmentName, eq);
            return eq;
        }

        logger.warn("Équipement '{}' non trouvé en BDD (vérifier equipments.csv)", equipmentName);
        return null;
    }

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
            Optional<Resource> resourceOpt = resourceRepository.findByName(resourceDto.name);
            if (resourceOpt.isPresent()) {
                Resource resource = resourceOpt.get();
                player.addResource(resource.getName(), resourceDto.quantity);
            } else {
                logger.warn("Ressource '{}' non trouvée en BDD", resourceDto.name);
            }
        }
    }

    private void importSectors(Player player, Board board, List<SectorDTO> sectors) {
        for (SectorDTO sectorDto : sectors) {
            Sector sector = board.getSector(sectorDto.sectorNumber);
            if (sector == null) {
                logger.warn("Secteur {} non trouvé dans le Board - ignoré", sectorDto.sectorNumber);
                continue;
            }

            board.assignOwner(sectorDto.sectorNumber, player.getId(), "#ffffff");

            importUnitsToSector(player, sector, sectorDto.army);
        }
    }

    private void importUnitsToSector(Player player, Sector sector, List<UnitDTO> units) {
        if (units == null) return;
        for (UnitDTO unitDto : units) {
            Unit unit = createUnitFromDTO(player, unitDto);
            if (unit != null) {
                sector.addUnit(unit);
            }
        }
    }

    private Unit createUnitFromDTO(Player player, UnitDTO unitDto) {
        UnitType type = (unitDto.type != null && !unitDto.type.isEmpty())
                ? UnitType.valueOf(unitDto.type) : null;
        Unit unit = BoardImportService.createUnit(unitDto.classes, type, unitDto.experience);
        if (unit == null) {
            logger.warn("Impossible de créer une unité sans classe - unité ignorée");
            return null;
        }

        unit.setPlayerId(player.getId());
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayerDTO {
        public String name;
        public List<EquipmentDTO> equipments;
        public List<ResourceDTO> resources;
        public List<VehicleDTO> vehicles;
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
        public String type;
        public int sectorNumber;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EquipmentDTO {
        public String name;
        public int quantity;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class VehicleDTO {
        public String type;
        public int quantity = 1;
        public Integer sectorNumber;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ResourceDTO {
        public String name;
        public int quantity;
    }
}
