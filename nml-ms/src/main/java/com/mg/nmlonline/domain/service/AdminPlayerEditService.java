package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.AdminCatalogDto;
import com.mg.nmlonline.api.dto.AdminCharacterRequestDto;
import com.mg.nmlonline.api.dto.AdminPlayerEditDto;
import com.mg.nmlonline.api.dto.AdminUnitUpdateRequestDto;
import com.mg.nmlonline.api.dto.ResourceCatalogDto;
import com.mg.nmlonline.api.dto.UnitDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.BuildingType;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.Resource;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitEquipment;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.BuildingRepository;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.ResourceRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import com.mg.nmlonline.mapper.PlayerMapper;
import com.mg.nmlonline.mapper.UnitMapper;
import com.mg.nmlonline.mapper.VehicleMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class AdminPlayerEditService {

    private static final Logger logger = LoggerFactory.getLogger(AdminPlayerEditService.class);

    private static final int MAX_STACK_QUANTITY = 1000;
    private static final int MAX_ENTITY_QUANTITY = 100;

    private final PlayerRepository playerRepository;
    private final UnitRepository unitRepository;
    private final VehicleRepository vehicleRepository;
    private final BuildingRepository buildingRepository;
    private final MovementOrderRepository movementOrderRepository;
    private final PlayerActionService playerActionService;
    private final ResourceRepository resourceRepository;
    private final EquipmentService equipmentService;
    private final BuildingService buildingService;
    private final GameCharacterService gameCharacterService;
    private final PlayerStatsService playerStatsService;
    private final BoardService boardService;
    private final PlayerMapper playerMapper;
    private final UnitMapper unitMapper;
    private final VehicleMapper vehicleMapper;
    private final TurnLock turnLock;
    private final EntityManager em;

    public AdminPlayerEditService(PlayerRepository playerRepository,
                                  UnitRepository unitRepository,
                                  VehicleRepository vehicleRepository,
                                  BuildingRepository buildingRepository,
                                  MovementOrderRepository movementOrderRepository,
                                  PlayerActionService playerActionService,
                                  ResourceRepository resourceRepository,
                                  EquipmentService equipmentService,
                                  BuildingService buildingService,
                                  GameCharacterService gameCharacterService,
                                  PlayerStatsService playerStatsService,
                                  BoardService boardService,
                                  PlayerMapper playerMapper,
                                  UnitMapper unitMapper,
                                  VehicleMapper vehicleMapper,
                                  TurnLock turnLock,
                                  EntityManager em) {
        this.playerRepository = playerRepository;
        this.unitRepository = unitRepository;
        this.vehicleRepository = vehicleRepository;
        this.buildingRepository = buildingRepository;
        this.movementOrderRepository = movementOrderRepository;
        this.playerActionService = playerActionService;
        this.resourceRepository = resourceRepository;
        this.equipmentService = equipmentService;
        this.buildingService = buildingService;
        this.gameCharacterService = gameCharacterService;
        this.playerStatsService = playerStatsService;
        this.boardService = boardService;
        this.playerMapper = playerMapper;
        this.unitMapper = unitMapper;
        this.vehicleMapper = vehicleMapper;
        this.turnLock = turnLock;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public AdminPlayerEditDto getEdit(Long playerId) {
        Player player = requirePlayer(playerId);
        Board board = activeBoard();
        AdminPlayerEditDto dto = new AdminPlayerEditDto();
        dto.setPlayer(playerMapper.toDtoWithSectors(player, board));
        dto.setUnits(unitRepository.findByPlayerId(playerId).stream()
                .map(this::toAdminUnitDto)
                .toList());
        dto.setVehicles(vehicleRepository.findByPlayerId(playerId).stream()
                .map(vehicleMapper::toDto)
                .toList());
        return dto;
    }

    @Transactional(readOnly = true)
    public AdminCatalogDto getCatalog() {
        AdminCatalogDto dto = new AdminCatalogDto();
        dto.setEquipments(equipmentService.findAllDto(Pageable.unpaged()).getContent());
        dto.setResources(resourceRepository.findAll().stream()
                .map(resource -> new ResourceCatalogDto(resource.getName(), resource.getBaseValue()))
                .toList());
        dto.setUnitTypes(Arrays.stream(UnitType.values())
                .filter(type -> type != UnitType.PERSONNAGE)
                .map(unitMapper::toUnitTypeDto)
                .toList());
        dto.setUnitClasses(Arrays.stream(UnitClass.values())
                .map(unitMapper::toUnitClassDto)
                .toList());
        dto.setVehicleTypes(Arrays.stream(VehicleType.values())
                .map(vehicleMapper::vehicleTypeToDto)
                .toList());
        return dto;
    }

    @Transactional
    public void setMoney(Long playerId, double amount) {
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Le montant doit être un nombre positif");
        }
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        player.getStats().setMoney(amount);
        player.getStats().setStartingMoneyRemaining(
                Math.min(player.getStats().getStartingMoneyRemaining(), amount));
        player.calculateTotalEconomyPower();
        playerRepository.save(player);
    }

    @Transactional
    public void updatePlayerName(Long playerId, String name) {
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        String trimmed = normalizeName(name);
        playerRepository.findByName(trimmed).ifPresent(existing -> {
            if (!existing.getId().equals(playerId)) {
                throw new IllegalArgumentException("Un joueur avec ce nom existe déjà");
            }
        });
        player.setName(trimmed);
        playerRepository.save(player);
    }

    @Transactional
    public void updateCharacter(Long playerId, AdminCharacterRequestDto request) {
        requireTurnOpen();
        lockPlayer(playerId);
        validateCharacter(request);
        gameCharacterService.updateCharacter(playerId, request.getName().trim(),
                request.getBaseAttack(), request.getBasePdf(), request.getBasePdc(),
                request.getBaseDefense(), request.getBaseArmor(), request.getBaseEvasion());
    }

    @Transactional
    public void createCharacter(Long playerId, AdminCharacterRequestDto request) {
        requireTurnOpen();
        lockPlayer(playerId);
        validateCharacter(request);
        gameCharacterService.createCharacter(playerId, request.getName().trim(),
                request.getBaseAttack(), request.getBasePdf(), request.getBasePdc(),
                request.getBaseDefense(), request.getBaseArmor(), request.getBaseEvasion());
    }

    @Transactional
    public void deleteCharacter(Long playerId) {
        requireTurnOpen();
        lockPlayer(playerId);
        gameCharacterService.deleteCharacter(playerId);
    }

    @Transactional
    public void addResource(Long playerId, String name, int quantity) {
        requireQuantity(quantity, MAX_STACK_QUANTITY);
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        Resource resource = resourceRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Ressource inconnue : " + name));
        player.addResource(resource.getName(), quantity);
        playerRepository.save(player);
    }

    @Transactional
    public void removeResource(Long playerId, String name, int quantity) {
        requireQuantity(quantity, MAX_STACK_QUANTITY);
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        if (!player.removeResource(name, quantity)) {
            throw new IllegalArgumentException("Quantité insuffisante pour « " + name
                    + " » (possédé : " + player.getResourceQuantity(name) + ")");
        }
        playerRepository.save(player);
    }

    @Transactional
    public void addEquipment(Long playerId, String name, int quantity) {
        requireQuantity(quantity, MAX_STACK_QUANTITY);
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        Equipment equipment = equipmentService.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Équipement inconnu : " + name));
        player.addEquipmentToStack(equipment, quantity);
        player.setTotalEquipmentValue();
        player.calculateTotalEconomyPower();
        playerRepository.save(player);
    }

    /** {@code includeEquipped} : retire aussi les copies portées par des unités (après confirmation UI). */
    @Transactional
    public void removeEquipment(Long playerId, String name, int quantity, boolean includeEquipped) {
        requireQuantity(quantity, MAX_STACK_QUANTITY);
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        Board board = activeBoard();
        EquipmentStack stack = player.getEquipments().stream()
                .filter(s -> s.getEquipment() != null && name.equals(s.getEquipment().getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun équipement « " + name + " » dans l'inventaire du joueur"));
        if (quantity > stack.getQuantity()) {
            throw new IllegalArgumentException("Le joueur ne possède que " + stack.getQuantity()
                    + " × « " + name + " »");
        }
        if (quantity > stack.getAvailable() && !includeEquipped) {
            throw new IllegalArgumentException((quantity - stack.getAvailable()) + " × « " + name
                    + " » sont équipés sur des unités — confirmez le retrait");
        }

        playerActionService.discardActiveActionsReferencing(
                playerId, board.getCurrentTurn(), null, null, null, name);
        int fromStock = Math.min(quantity, stack.getAvailable());
        for (int i = 0; i < fromStock; i++) {
            player.removeEquipmentFromStack(stack.getEquipment());
        }
        int fromUnits = removeEquipmentFromUnits(player, stack, quantity - fromStock);
        // Stacks issus de combats antérieurs au correctif : les copies disparues avec leur unité ne sont plus portées.
        int missing = quantity - fromStock - fromUnits;
        for (int i = 0; i < missing; i++) {
            stack.decrementEquipped();
        }
        if (missing > 0) {
            logger.warn("Retrait de {} × « {} » : {} copie(s) écrite(s) en perte (inventaire désynchronisé)",
                    quantity, name, missing);
        }
        if (stack.getQuantity() <= 0) {
            player.getEquipments().remove(stack);
        }
        playerStatsService.recalculateStats(player, board);
        playerRepository.save(player);
        boardService.save(board);
    }

    @Transactional
    public void addUnits(Long playerId, String typeName, List<String> classNames, int quantity,
                         Integer sectorNumber) {
        requireQuantity(quantity, MAX_ENTITY_QUANTITY);
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        Board board = activeBoard();
        UnitType type = parseUnitType(typeName);
        List<UnitClass> classes = parseUnitClasses(classNames);
        Sector sector = resolveOwnedSector(player, board, sectorNumber, false);
        for (int i = 0; i < quantity; i++) {
            Unit unit = new Unit(type.getMinExp(), classes.getFirst());
            if (classes.size() > 1) {
                unit.setClasses(classes);
            }
            unit.setPlayerId(playerId);
            unit.recalculateBaseStats();
            unitRepository.save(unit);
            if (sector != null) {
                sector.addUnit(unit);
            }
        }
        playerStatsService.recalculateStats(player, board);
        boardService.save(board);
        playerRepository.save(player);
    }

    @Transactional
    public void deleteUnit(Long playerId, Long unitId) {
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        Board board = activeBoard();
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("Unité introuvable : " + unitId));
        if (!playerId.equals(unit.getPlayerId())) {
            throw new IllegalArgumentException("L'unité " + unitId + " n'appartient pas à ce joueur");
        }
        playerActionService.discardActiveActionsReferencing(
                playerId, board.getCurrentTurn(), unitId, null, null, null);
        removeUnitFromPendingOrders(playerId, unitId);
        for (UnitEquipment row : new ArrayList<>(unit.getUnitEquipments())) {
            player.incrementEquipmentAvailability(row.getEquipment());
        }
        CombatService.detachFromVehicles(em, unit);
        Sector sector = unit.getSector();
        if (sector != null) {
            sector.removeUnit(unit);
        }
        em.remove(unit);
        playerStatsService.recalculateStats(player, board);
        boardService.save(board);
        playerRepository.save(player);
    }

    @Transactional
    public void updateUnit(Long playerId, Long unitId, AdminUnitUpdateRequestDto request) {
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        Board board = activeBoard();
        Unit unit = requireUnitOfPlayer(playerId, unitId);
        boolean classOrExperienceChanged = false;

        if (request.getExperience() != null) {
            double target = request.getExperience();
            if (!Double.isFinite(target) || target < 0) {
                throw new IllegalArgumentException("L'expérience doit être un nombre positif");
            }
            double delta = target - unit.getExperience();
            if (delta != 0) {
                unit.gainExperience(delta);
            }
            classOrExperienceChanged = true;
        }
        if (request.getClasses() != null) {
            List<UnitClass> classes = parseUnitClasses(request.getClasses());
            if (classes.size() == 2 && !unit.canHaveTwoClasses()) {
                throw new IllegalArgumentException(
                        "Cette unité ne peut pas avoir deux classes (type ou expérience insuffisante)");
            }
            autoUnequipIncompatible(player, unit, classes);
            unit.setClasses(classes);
            unit.recalculateBaseStats();
            classOrExperienceChanged = true;
        }
        if (classOrExperienceChanged
                && unit.getClasses().size() == 2 && !unit.canHaveTwoClasses()) {
            throw new IllegalArgumentException(
                    "Une unité à deux classes doit avoir au moins 5 d'expérience et ne peut pas être LARBIN/VOYOU");
        }
        if (request.getInjured() != null) {
            if (request.getInjured()) {
                unit.setInjured(true);
                unit.setInjuredAtTurn(board.getCurrentTurn());
                unit.setHealingDelayed(false);
                unit.recalculateBaseStats();
            } else {
                unit.recover();
            }
        }
        saveUnitAndStats(player, board, unit);
    }

    @Transactional
    public void moveUnit(Long playerId, Long unitId, Integer sectorNumber) {
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        Board board = activeBoard();
        Unit unit = requireUnitOfPlayer(playerId, unitId);
        if (unitIsInVehicle(unitId)) {
            throw new IllegalArgumentException(
                    "Détachez l'unité de son véhicule avant de la déplacer");
        }
        Sector current = unit.getSector();
        Integer currentNumber = current == null ? null : current.getNumber();
        if (Objects.equals(currentNumber, sectorNumber)) {
            return;
        }
        Sector target = sectorNumber == null
                ? null
                : resolveOwnedSector(player, board, sectorNumber, true);
        removeUnitFromPendingOrders(playerId, unitId);
        if (current != null) {
            current.removeUnit(unit);
        }
        if (target == null) {
            unit.setSector(null);
            unit.setNumber(0);
        } else {
            target.addUnit(unit);
        }
        saveUnitAndStats(player, board, unit);
    }

    @Transactional
    public void equipUnit(Long playerId, Long unitId, String equipmentName) {
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        Board board = activeBoard();
        Unit unit = requireUnitOfPlayer(playerId, unitId);
        Equipment equipment = equipmentService.findByName(equipmentName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Équipement inconnu : " + equipmentName));
        EquipmentStack stack = player.getEquipments().stream()
                .filter(candidate -> candidate.getEquipment() != null
                        && equipmentName.equals(candidate.getEquipment().getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Le joueur ne possède pas « " + equipmentName + " »"));
        if (stack.getAvailable() <= 0) {
            throw new IllegalArgumentException(
                    "Aucune copie disponible de « " + equipmentName + " »");
        }
        if (!unit.canEquip(equipment)) {
            throw new IllegalArgumentException(
                    "L'unité ne peut pas équiper « " + equipmentName + " »");
        }
        player.decrementEquipmentAvailability(equipment);
        if (!unit.addEquipment(equipment)) {
            throw new IllegalArgumentException(
                    "L'unité ne peut pas équiper « " + equipmentName + " »");
        }
        saveUnitAndStats(player, board, unit);
    }

    @Transactional
    public void unequipUnit(Long playerId, Long unitId, String equipmentName) {
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        Board board = activeBoard();
        Unit unit = requireUnitOfPlayer(playerId, unitId);
        UnitEquipment row = unit.removeOneEquipment(equipmentName);
        if (row == null) {
            throw new IllegalArgumentException(
                    "L'unité ne porte pas « " + equipmentName + " »");
        }
        em.remove(row);
        player.incrementEquipmentAvailability(row.getEquipment());
        saveUnitAndStats(player, board, unit);
    }

    @Transactional
    public void addVehicles(Long playerId, String vehicleTypeName, int quantity, Integer sectorNumber) {
        requireQuantity(quantity, MAX_ENTITY_QUANTITY);
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        Board board = activeBoard();
        VehicleType type = parseVehicleType(vehicleTypeName);
        Sector sector = resolveOwnedSector(player, board, sectorNumber, false);
        for (int i = 0; i < quantity; i++) {
            Vehicle vehicle = new Vehicle(type, playerId);
            if (sector != null) {
                vehicle.setSector(sector);
                sector.getVehicles().add(vehicle);
            }
            vehicleRepository.save(vehicle);
            player.getStats().setTotalVehiclesValue(
                    player.getStats().getTotalVehiclesValue() + type.getCost());
        }
        playerStatsService.recalculateStats(player, board);
        boardService.save(board);
        playerRepository.save(player);
    }

    @Transactional
    public void deleteVehicle(Long playerId, Long vehicleId) {
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        Board board = activeBoard();
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("Véhicule introuvable : " + vehicleId));
        if (!playerId.equals(vehicle.getPlayerId())) {
            throw new IllegalArgumentException("Le véhicule " + vehicleId + " n'appartient pas à ce joueur");
        }
        playerActionService.discardActiveActionsReferencing(
                playerId, board.getCurrentTurn(), null, vehicleId, null, null);
        cancelPendingVehicleOrders(vehicleId);
        boolean valueLost = !vehicle.isDestroyed() && vehicle.getVehicleType() != null;
        VehicleType type = vehicle.getVehicleType();
        vehicle.disembarkAll();
        Sector sector = vehicle.getSector();
        if (sector != null) {
            sector.getVehicles().remove(vehicle);
        }
        em.remove(vehicle);
        if (valueLost) {
            player.loseVehicle(type);
        }
        playerStatsService.recalculateStats(player, board);
        boardService.save(board);
        playerRepository.save(player);
    }

    @Transactional
    public void addBuilding(Long playerId, String buildingTypeName, Integer sectorNumber) {
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        Board board = activeBoard();
        BuildingType type = parseBuildingType(buildingTypeName);
        Sector sector = resolveOwnedSector(player, board, sectorNumber, true);
        if ((type == BuildingType.HEADQUARTERS || type == BuildingType.BANK)
                && buildingRepository.findByPlayerIdAndBuildingType(playerId, type).isPresent()) {
            throw new IllegalStateException("Le joueur possède déjà un bâtiment " + type.name());
        }
        Building building = buildingService.createBuilding(player, type);
        building.setSector(sector);
        sector.getBuildings().add(building);
        buildingRepository.save(building);
        playerStatsService.recalculateStats(player, board);
        boardService.save(board);
        playerRepository.save(player);
    }

    @Transactional
    public void deleteBuilding(Long playerId, Long buildingId) {
        requireTurnOpen();
        Player player = lockPlayer(playerId);
        Board board = activeBoard();
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Bâtiment introuvable : " + buildingId));
        if (!playerId.equals(building.getPlayerId())) {
            throw new IllegalArgumentException("Le bâtiment " + buildingId + " n'appartient pas à ce joueur");
        }
        playerActionService.discardActiveActionsReferencing(
                playerId, board.getCurrentTurn(), null, null, buildingId, null);
        Sector sector = building.getSector();
        if (sector != null) {
            sector.getBuildings().remove(building);
        }
        player.getBuildings().remove(building);
        playerStatsService.recalculateStats(player, board);
        boardService.save(board);
        playerRepository.save(player);
    }

    private void validateCharacter(AdminCharacterRequestDto request) {
        normalizeName(request.getName());
        double[] stats = {
                request.getBaseAttack(), request.getBaseDefense(), request.getBasePdf(),
                request.getBasePdc(), request.getBaseArmor(), request.getBaseEvasion()
        };
        for (double stat : stats) {
            if (!Double.isFinite(stat) || stat < 0) {
                throw new IllegalArgumentException(
                        "Les statistiques du personnage doivent être positives");
            }
        }
    }

    private String normalizeName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Le nom est requis");
        }
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException(
                    "Le nom est trop long (100 caractères maximum)");
        }
        return trimmed;
    }

    private Unit requireUnitOfPlayer(Long playerId, Long unitId) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("Unité introuvable : " + unitId));
        if (!playerId.equals(unit.getPlayerId())) {
            throw new IllegalArgumentException("L'unité " + unitId + " n'appartient pas à ce joueur");
        }
        return unit;
    }

    private boolean unitIsInVehicle(Long unitId) {
        return vehicleRepository.findAll().stream().anyMatch(vehicle ->
                (vehicle.getPilot() != null && unitId.equals(vehicle.getPilot().getId()))
                        || vehicle.getPassengers().stream()
                        .anyMatch(passenger -> unitId.equals(passenger.getId())));
    }

    private void autoUnequipIncompatible(Player player, Unit unit, List<UnitClass> newClasses) {
        for (UnitEquipment row : new ArrayList<>(unit.getUnitEquipments())) {
            Equipment equipment = row.getEquipment();
            if (equipment == null) {
                continue;
            }
            boolean compatible = equipment.getCompatibleClasses().stream()
                    .anyMatch(newClasses::contains);
            if (!compatible) {
                unit.removeOneEquipment(equipment.getName());
                em.remove(row);
                player.incrementEquipmentAvailability(equipment);
            }
        }
    }

    private void saveUnitAndStats(Player player, Board board, Unit unit) {
        if (unit.getSector() != null) {
            unit.getSector().recalculateMilitaryPower();
        }
        unitRepository.save(unit);
        playerStatsService.recalculateStats(player, board);
        boardService.save(board);
        playerRepository.save(player);
    }

    private void removeUnitFromPendingOrders(Long playerId, Long unitId) {
        for (MovementOrder order : movementOrderRepository.findByPlayerIdAndStatus(
                playerId, MovementStatus.PENDING)) {
            if (order.getEntityIds().remove(unitId)) {
                if (order.getEntityIds().isEmpty()) {
                    order.cancel();
                }
                movementOrderRepository.save(order);
            }
        }
    }

    private void cancelPendingVehicleOrders(Long vehicleId) {
        for (MovementOrder order : movementOrderRepository.findByVehicleIdAndStatus(
                vehicleId, MovementStatus.PENDING)) {
            order.cancel();
            movementOrderRepository.save(order);
        }
    }

    private int removeEquipmentFromUnits(Player player, EquipmentStack stack, int quantity) {
        if (quantity <= 0) {
            return 0;
        }
        String name = stack.getEquipment().getName();
        List<Unit> carriers = unitRepository.findByPlayerId(player.getId()).stream()
                .filter(unit -> unit.getUnitEquipments().stream()
                        .anyMatch(row -> name.equals(row.getEquipment().getName())))
                .sorted(Comparator.comparing(Unit::getId))
                .toList();
        int remaining = quantity;
        for (Unit unit : carriers) {
            while (remaining > 0) {
                UnitEquipment row = unit.removeOneEquipment(name);
                if (row == null) {
                    break;
                }
                em.remove(row);
                stack.decrementEquipped();
                remaining--;
            }
            if (remaining == 0) {
                break;
            }
        }
        return quantity - remaining;
    }

    private UnitDto toAdminUnitDto(Unit unit) {
        UnitDto dto = unitMapper.toDto(unit);
        dto.setSectorNumber(unit.getSector() != null ? unit.getSector().getNumber() : null);
        return dto;
    }

    private Sector resolveOwnedSector(Player player, Board board, Integer sectorNumber, boolean required) {
        if (sectorNumber == null) {
            if (required) {
                throw new IllegalArgumentException("Un secteur est requis");
            }
            return null;
        }
        Sector sector = board.getSector(sectorNumber);
        if (sector == null) {
            throw new IllegalArgumentException("Secteur inexistant : " + sectorNumber);
        }
        if (!player.getId().equals(sector.getOwnerId())) {
            throw new IllegalArgumentException(
                    "Le secteur " + sectorNumber + " n'appartient pas à ce joueur");
        }
        return sector;
    }

    private UnitType parseUnitType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le type d'unité est requis");
        }
        UnitType type;
        try {
            type = UnitType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type d'unité invalide : " + value);
        }
        if (type == UnitType.PERSONNAGE) {
            throw new IllegalArgumentException("Le type PERSONNAGE est réservé aux personnages joueurs");
        }
        return type;
    }

    private List<UnitClass> parseUnitClasses(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Au moins une classe est requise");
        }
        if (values.size() > 2) {
            throw new IllegalArgumentException("Une unité a au plus deux classes");
        }
        List<UnitClass> classes = values.stream().distinct().map(this::parseUnitClass).toList();
        if (classes.size() != values.size()) {
            throw new IllegalArgumentException("Une classe est présente deux fois");
        }
        return classes;
    }

    private UnitClass parseUnitClass(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Classe invalide : " + value);
        }
        try {
            return UnitClass.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Classe invalide : " + value);
        }
    }

    private VehicleType parseVehicleType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le type de véhicule est requis");
        }
        try {
            return VehicleType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type de véhicule invalide : " + value);
        }
    }

    private BuildingType parseBuildingType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le type de bâtiment est requis");
        }
        try {
            return BuildingType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type de bâtiment invalide : " + value);
        }
    }

    private void requireQuantity(int quantity, int max) {
        if (quantity < 1 || quantity > max) {
            throw new IllegalArgumentException("La quantité doit être comprise entre 1 et " + max);
        }
    }

    private void requireTurnOpen() {
        if (turnLock.isLocked()) {
            throw new IllegalStateException(
                    "La résolution du tour est en cours — réessayez après la fin du tour");
        }
    }

    private Player requirePlayer(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable : " + playerId));
    }

    private Player lockPlayer(Long playerId) {
        return playerRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable : " + playerId));
    }

    private Board activeBoard() {
        return boardService.getAllBoards().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun plateau disponible."));
    }
}
