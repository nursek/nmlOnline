package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BuyVehicleRequestDto;
import com.mg.nmlonline.api.dto.MovementOrderDto;
import com.mg.nmlonline.api.dto.VehicleDto;
import com.mg.nmlonline.domain.exception.InsufficientFundsException;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.SectorRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import com.mg.nmlonline.mapper.MovementMapper;
import com.mg.nmlonline.mapper.VehicleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final PlayerRepository playerRepository;
    private final VehicleRepository vehicleRepository;
    private final SectorRepository sectorRepository;
    private final MovementOrderRepository movementOrderRepository;
    private final BoardService boardService;
    private final MovementService movementService;
    private final TurnService turnService;
    private final VehicleCrewService vehicleCrewService;
    private final VehicleMapper vehicleMapper;
    private final MovementMapper movementMapper;
    private final PlayerActionService playerActionService;

    public VehicleService(PlayerRepository playerRepository, VehicleRepository vehicleRepository,
                          SectorRepository sectorRepository, MovementOrderRepository movementOrderRepository,
                          BoardService boardService, MovementService movementService, TurnService turnService,
                          VehicleCrewService vehicleCrewService, VehicleMapper vehicleMapper,
                          MovementMapper movementMapper, PlayerActionService playerActionService) {
        this.playerRepository = playerRepository;
        this.vehicleRepository = vehicleRepository;
        this.sectorRepository = sectorRepository;
        this.movementOrderRepository = movementOrderRepository;
        this.boardService = boardService;
        this.movementService = movementService;
        this.turnService = turnService;
        this.vehicleCrewService = vehicleCrewService;
        this.vehicleMapper = vehicleMapper;
        this.movementMapper = movementMapper;
        this.playerActionService = playerActionService;
    }

    public List<VehicleType> getAllVehicleTypes() {
        return Arrays.asList(VehicleType.values());
    }

    private void requireAvailableAtCurrentTurn(VehicleType vehicleType) {
        int turn = turnService.getCurrentTurn();
        if (!vehicleType.isAvailableAt(turn)) {
            throw new IllegalStateException("« " + vehicleType.getDisplayName()
                    + " » est disponible à l'achat à partir du tour " + vehicleType.getAvailableFromTurn());
        }
    }

    @Transactional
    public List<Vehicle> buyVehicle(Long userId, String vehicleTypeName, int quantity) {
        if (vehicleTypeName == null || vehicleTypeName.isBlank()) {
            throw new IllegalArgumentException("Le type de véhicule est requis");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("La quantité doit être au moins 1");
        }
        VehicleType vehicleType;
        try {
            vehicleType = VehicleType.valueOf(vehicleTypeName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type de véhicule invalide : " + vehicleTypeName);
        }
        requireAvailableAtCurrentTurn(vehicleType);

        Player player = playerRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new RuntimeException("Joueur introuvable pour userId : " + userId));

        List<Vehicle> created = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            double startingShare = player.startingShareOf(vehicleType.getCost());
            Vehicle vehicle = player.buyVehicle(vehicleType);
            if (vehicle == null) {
                throw new InsufficientFundsException("Fonds insuffisants pour acheter ce véhicule (coût : " + vehicleType.getCost() + " ₡)");
            }
            Vehicle saved = vehicleRepository.save(vehicle);
            created.add(saved);
            playerActionService.recordBuyVehicle(player.getId(), saved.getId(), vehicleType.getCost(), startingShare);
        }
        playerRepository.save(player);
        return created;
    }

    @Transactional
    public List<Vehicle> buyVehiclesBatch(Long userId, List<BuyVehicleRequestDto> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Le panier de véhicules est vide");
        }

        Player player = playerRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new RuntimeException("Joueur introuvable pour userId : " + userId));

        List<VehicleType> toCreate = new ArrayList<>();
        for (BuyVehicleRequestDto item : items) {
            if (item.getVehicleType() == null || item.getVehicleType().isBlank()) {
                throw new IllegalArgumentException("Le type de véhicule est requis");
            }
            if (item.getQuantity() < 1) {
                throw new IllegalArgumentException("La quantité doit être au moins 1");
            }
            VehicleType vehicleType;
            try {
                vehicleType = VehicleType.valueOf(item.getVehicleType());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Type de véhicule invalide : " + item.getVehicleType());
            }
            requireAvailableAtCurrentTurn(vehicleType);
            for (int i = 0; i < item.getQuantity(); i++) {
                toCreate.add(vehicleType);
            }
        }

        long totalCost = toCreate.stream().mapToLong(VehicleType::getCost).sum();
        if (player.getStats().getMoney() < totalCost) {
            throw new InsufficientFundsException("Fonds insuffisants pour acheter ces véhicules (coût total : " + totalCost + " ₡)");
        }

        List<Vehicle> created = new ArrayList<>();
        for (VehicleType vehicleType : toCreate) {
            double startingShare = player.startingShareOf(vehicleType.getCost());
            Vehicle vehicle = player.buyVehicle(vehicleType);
            if (vehicle == null) {
                throw new InsufficientFundsException("Fonds insuffisants pour acheter le véhicule " + vehicleType.name());
            }
            Vehicle saved = vehicleRepository.save(vehicle);
            created.add(saved);
            playerActionService.recordBuyVehicle(player.getId(), saved.getId(), vehicleType.getCost(), startingShare);
        }
        playerRepository.save(player);
        return created;
    }

    public List<Vehicle> getPlayerVehicles(Long userId) {
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Joueur introuvable pour userId : " + userId));
        return vehicleRepository.findByPlayerId(player.getId());
    }

    @Transactional
    public Vehicle placeVehicle(Long vehicleId, Long boardId, int sectorNumber, Long userId) {
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Joueur introuvable pour userId : " + userId));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Véhicule introuvable : " + vehicleId));

        if (!player.getId().equals(vehicle.getPlayerId())) {
            throw new SecurityException("Ce véhicule ne vous appartient pas");
        }

        if (vehicle.getSector() != null) {
            throw new IllegalStateException("Le véhicule est déjà déployé.");
        }
        if (vehicle.isDestroyed()) {
            throw new IllegalStateException("Le véhicule est détruit.");
        }

        Sector sector = sectorRepository.findByBoard_IdAndNumber(boardId, sectorNumber)
                .orElseThrow(() -> new RuntimeException("Secteur introuvable"));

        if (!player.getId().equals(sector.getOwnerId())) {
            throw new SecurityException("Vous ne possédez pas ce secteur");
        }

        vehicle.setSector(sector);
        Vehicle saved = vehicleRepository.save(vehicle);
        playerActionService.recordPlaceVehicle(player.getId(), saved.getId(), boardId, sectorNumber);
        return saved;
    }

    @Transactional
    public VehicleDto setCrew(Long userId, Long vehicleId, Long pilotId, List<Long> passengerIds) {
        // Verrou joueur AVANT véhicule : l'insert player_actions pose un KEY SHARE sur players.
        Player player = playerRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new RuntimeException("Joueur introuvable pour userId : " + userId));

        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new RuntimeException("Véhicule introuvable : " + vehicleId));

        if (!player.getId().equals(vehicle.getPlayerId())) {
            throw new SecurityException("Ce véhicule ne vous appartient pas");
        }
        if (vehicle.getSector() == null) {
            throw new IllegalStateException("Le véhicule n'est pas déployé.");
        }
        if (vehicle.isDestroyed()) {
            throw new IllegalStateException("Le véhicule est détruit.");
        }

        List<Long> passengers = passengerIds != null ? passengerIds : List.of();
        if (passengers.stream().distinct().count() != passengers.size()) {
            throw new IllegalArgumentException("Un même passager est présent deux fois.");
        }
        if (pilotId != null && passengers.contains(pilotId)) {
            throw new IllegalArgumentException("Le pilote ne peut pas être passager.");
        }
        if (passengers.size() > vehicle.getCapacity()) {
            throw new IllegalArgumentException(
                    "Capacité du véhicule dépassée (" + vehicle.getCapacity() + " passagers).");
        }

        Long prevPilotId = vehicle.getPilot() != null ? vehicle.getPilot().getId() : null;
        List<Long> prevPassengerIds = vehicle.getPassengers().stream().map(CombatEntity::getId).toList();
        if (Objects.equals(prevPilotId, pilotId)
                && new HashSet<>(prevPassengerIds).equals(new HashSet<>(passengers))) {
            return vehicleMapper.toDto(vehicle);
        }

        List<Long> targeted = new ArrayList<>(passengers);
        if (pilotId != null) {
            targeted.add(pilotId);
        }
        validateCrewCandidates(player, vehicle, targeted, prevPilotId, prevPassengerIds);

        vehicleCrewService.applyCrew(vehicle, pilotId, passengers);
        vehicleRepository.save(vehicle);
        playerActionService.recordSetVehicleCrew(player.getId(), vehicleId, prevPilotId,
                prevPassengerIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        return vehicleMapper.toDto(vehicle);
    }

    @Transactional
    public MovementOrderDto placeVehicleOrderDto(Long userId, Long vehicleId, List<Integer> route) {
        // Même ordre de verrouillage que setCrew : joueur puis véhicule.
        Player player = playerRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new RuntimeException("Joueur introuvable pour userId : " + userId));
        Board board = boardService.getAllBoards().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun plateau disponible."));
        MovementOrder order = movementService.placeVehicleOrder(
                player.getId(), turnService.getCurrentTurn(), vehicleId, route, board);
        return movementMapper.toDto(order);
    }

    private void validateCrewCandidates(Player player, Vehicle vehicle, List<Long> ids,
                                        Long prevPilotId, List<Long> prevPassengerIds) {
        if (ids.isEmpty()) {
            return;
        }
        List<Long> alreadyOrdered = movementOrderRepository
                .findPendingEntityIds(turnService.getCurrentTurn(), ids);
        if (!alreadyOrdered.isEmpty()) {
            throw new IllegalArgumentException(
                    "Entité(s) déjà engagée(s) dans un ordre en attente : " + alreadyOrdered + ".");
        }
        Set<Long> currentCrew = new HashSet<>(prevPassengerIds);
        if (prevPilotId != null) {
            currentCrew.add(prevPilotId);
        }
        Sector sector = vehicle.getSector();
        for (Long id : ids) {
            CombatEntity entity = vehicleCrewService.findEntity(id);
            if (!player.getId().equals(entity.getPlayerId())) {
                throw new SecurityException("L'entité #" + id + " n'appartient pas au joueur.");
            }
            if (entity.isDestroyed()) {
                throw new IllegalStateException("L'entité #" + id + " est détruite.");
            }
            if (!currentCrew.contains(id)) {
                if (vehicleRepository.findByPilot_Id(id)
                        .filter(other -> !Objects.equals(other.getId(), vehicle.getId()))
                        .isPresent()) {
                    throw new IllegalStateException("L'entité #" + id + " pilote déjà un autre véhicule.");
                }
                if (vehicleRepository.findByPassengers_Id(id)
                        .filter(other -> !Objects.equals(other.getId(), vehicle.getId()))
                        .isPresent()) {
                    throw new IllegalStateException("L'entité #" + id + " est déjà dans un autre véhicule.");
                }
            }
            boolean inVehicleSector = entity.getSector() != null
                    && entity.getSector().getNumber() == sector.getNumber()
                    && entity.getSector().getBoard() != null
                    && Objects.equals(entity.getSector().getBoard().getId(), sector.getBoard().getId());
            if (!inVehicleSector) {
                throw new IllegalArgumentException(
                        "L'entité #" + id + " n'est pas dans le secteur " + sector.getNumber() + ".");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<VehicleDto> getPlayerVehiclesDto(Long userId) {
        return getPlayerVehicles(userId).stream().map(vehicleMapper::toDto).toList();
    }

    @Transactional
    public List<VehicleDto> buyVehicleDto(Long userId, String vehicleTypeName, int quantity) {
        return buyVehicle(userId, vehicleTypeName, quantity).stream().map(vehicleMapper::toDto).toList();
    }

    @Transactional
    public List<VehicleDto> buyVehiclesBatchDto(Long userId, List<BuyVehicleRequestDto> items) {
        return buyVehiclesBatch(userId, items).stream().map(vehicleMapper::toDto).toList();
    }

    @Transactional
    public VehicleDto placeVehicleDto(Long vehicleId, Long boardId, int sectorNumber, Long userId) {
        return vehicleMapper.toDto(placeVehicle(vehicleId, boardId, sectorNumber, userId));
    }
}
