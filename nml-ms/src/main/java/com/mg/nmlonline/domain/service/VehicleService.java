package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BuyVehicleRequestDto;
import com.mg.nmlonline.api.dto.MovementOrderDto;
import com.mg.nmlonline.api.dto.VehicleDto;
import com.mg.nmlonline.api.dto.VehicleTypeDto;
import com.mg.nmlonline.domain.exception.InsufficientFundsException;
import jakarta.persistence.EntityNotFoundException;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.player.PlayerRace;
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
import java.util.List;
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
    private final TurnLock turnLock;
    private final VehicleCrewService vehicleCrewService;
    private final VehicleMapper vehicleMapper;
    private final MovementMapper movementMapper;
    private final PlayerActionService playerActionService;

    public VehicleService(PlayerRepository playerRepository, VehicleRepository vehicleRepository,
                          SectorRepository sectorRepository, MovementOrderRepository movementOrderRepository,
                          BoardService boardService, MovementService movementService, TurnService turnService,
                          TurnLock turnLock, VehicleCrewService vehicleCrewService, VehicleMapper vehicleMapper,
                          MovementMapper movementMapper, PlayerActionService playerActionService) {
        this.playerRepository = playerRepository;
        this.vehicleRepository = vehicleRepository;
        this.sectorRepository = sectorRepository;
        this.movementOrderRepository = movementOrderRepository;
        this.boardService = boardService;
        this.movementService = movementService;
        this.turnService = turnService;
        this.turnLock = turnLock;
        this.vehicleCrewService = vehicleCrewService;
        this.vehicleMapper = vehicleMapper;
        this.movementMapper = movementMapper;
        this.playerActionService = playerActionService;
    }

    private void requireTurnOpen(Long boardId) {
        if (turnLock.isLocked(boardId)) {
            throw new IllegalStateException(
                    "La résolution du tour est en cours — réessayez après la fin du tour");
        }
    }

    public List<VehicleTypeDto> getVehicleTypesDto(Long userId, Long boardId) {
        PlayerRace race = userId == null ? null
                : playerRepository.findByUserIdAndBoardId(userId, boardId).map(Player::getRace).orElse(null);
        return Arrays.stream(VehicleType.values())
                .map(type -> vehicleMapper.vehicleTypeToDto(type, race))
                .toList();
    }

    private void requireAvailableAtCurrentTurn(VehicleType vehicleType, Long boardId) {
        int turn = turnService.getCurrentTurn(boardId);
        if (!vehicleType.isAvailableAt(turn)) {
            throw new IllegalStateException("« " + vehicleType.getDisplayName()
                    + " » est disponible à l'achat à partir du tour " + vehicleType.getAvailableFromTurn());
        }
    }

    @Transactional
    public List<Vehicle> buyVehicle(Long userId, String vehicleTypeName, int quantity, Long boardId) {
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
            throw new IllegalArgumentException("Type de véhicule invalide : " + vehicleTypeName, e);
        }
        requireAvailableAtCurrentTurn(vehicleType, boardId);

        Player player = playerRepository.findByUserIdAndBoardIdForUpdate(userId, boardId)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable pour userId : " + userId));

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
    public List<Vehicle> buyVehiclesBatch(Long userId, List<BuyVehicleRequestDto> items, Long boardId) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Le panier de véhicules est vide");
        }

        Player player = playerRepository.findByUserIdAndBoardIdForUpdate(userId, boardId)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable pour userId : " + userId));

        record OrderLine(VehicleType type, int quantity) {
        }
        // Aucune expansion avant validation : une quantité arbitraire ne doit pas allouer de listes.
        List<OrderLine> lines = new ArrayList<>();
        long totalCost = 0;
        for (BuyVehicleRequestDto item : items) {
            if (item == null) {
                throw new IllegalArgumentException("Le panier de véhicules est invalide");
            }
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
            requireAvailableAtCurrentTurn(vehicleType, boardId);
            lines.add(new OrderLine(vehicleType, item.getQuantity()));
            totalCost += (long) vehicleType.getCost() * item.getQuantity();
        }

        if (player.getStats().getMoney() < totalCost) {
            throw new InsufficientFundsException("Fonds insuffisants pour acheter ces véhicules (coût total : " + totalCost + " ₡)");
        }

        List<Vehicle> created = new ArrayList<>();
        for (OrderLine line : lines) {
            for (int i = 0; i < line.quantity(); i++) {
                double startingShare = player.startingShareOf(line.type().getCost());
                Vehicle vehicle = player.buyVehicle(line.type());
                if (vehicle == null) {
                    throw new InsufficientFundsException("Fonds insuffisants pour acheter le véhicule " + line.type().name());
                }
                Vehicle saved = vehicleRepository.save(vehicle);
                created.add(saved);
                playerActionService.recordBuyVehicle(player.getId(), saved.getId(), line.type().getCost(), startingShare);
            }
        }
        playerRepository.save(player);
        return created;
    }

    @Transactional
    public Vehicle placeVehicle(Long vehicleId, Long boardId, int sectorNumber, Long userId) {
        requireTurnOpen(boardId);
        // Même ordre de verrouillage que setCrew : joueur puis véhicule.
        Player player = playerRepository.findByUserIdAndBoardIdForUpdate(userId, boardId)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable pour userId : " + userId));

        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
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
    public VehicleDto setCrew(Long userId, Long vehicleId, Long pilotId, List<Long> passengerIds,
                             Long boardId) {
        // Verrou joueur AVANT véhicule : l'insert player_actions pose un KEY SHARE sur players.
        Player player = playerRepository.findByUserIdAndBoardIdForUpdate(userId, boardId)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable pour userId : " + userId));

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

        Long prevPilotId = vehicle.getPilot() != null ? vehicle.getPilot().getId() : null;
        List<Long> prevPassengerIds = vehicle.getPassengers().stream().map(CombatEntity::getId).toList();
        if (!vehicleCrewService.updateCrew(player, vehicle, pilotId, passengerIds,
                prevPilotId, prevPassengerIds)) {
            return vehicleMapper.toDto(vehicle, player.getRace());
        }

        vehicleRepository.save(vehicle);
        playerActionService.recordSetVehicleCrew(player.getId(), vehicleId, prevPilotId,
                prevPassengerIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        return vehicleMapper.toDto(vehicle, player.getRace());
    }

    @Transactional
    public MovementOrderDto placeVehicleOrderDto(Long userId, Long vehicleId, List<Integer> route, Long boardId) {
        requireTurnOpen(boardId);
        // Même ordre de verrouillage que setCrew : joueur puis véhicule.
        Player player = playerRepository.findByUserIdAndBoardIdForUpdate(userId, boardId)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable pour userId : " + userId));
        Board board = boardService.getBoardById(boardId)
                .orElseThrow(() -> new IllegalStateException("Aucun plateau disponible."));
        MovementOrder order = movementService.placeVehicleOrder(
                player.getId(), turnService.getCurrentTurn(boardId), vehicleId, route, board);
        return movementMapper.toDto(order);
    }

    @Transactional(readOnly = true)
    public List<VehicleDto> getPlayerVehiclesDto(Long userId, Long boardId) {
        Player player = playerRepository.findByUserIdAndBoardId(userId, boardId)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable pour userId : " + userId));
        return vehicleRepository.findByPlayerId(player.getId()).stream()
                .map(vehicle -> vehicleMapper.toDto(vehicle, player.getRace()))
                .toList();
    }

    @Transactional
    public List<VehicleDto> buyVehicleDto(Long userId, String vehicleTypeName, int quantity, Long boardId) {
        return buyVehicle(userId, vehicleTypeName, quantity, boardId).stream().map(vehicleMapper::toDto).toList();
    }

    @Transactional
    public List<VehicleDto> buyVehiclesBatchDto(Long userId, List<BuyVehicleRequestDto> items, Long boardId) {
        return buyVehiclesBatch(userId, items, boardId).stream().map(vehicleMapper::toDto).toList();
    }

    @Transactional
    public VehicleDto placeVehicleDto(Long vehicleId, Long boardId, int sectorNumber, Long userId) {
        Vehicle vehicle = placeVehicle(vehicleId, boardId, sectorNumber, userId);
        PlayerRace race = playerRepository.findByUserIdAndBoardId(userId, boardId).map(Player::getRace).orElse(null);
        return vehicleMapper.toDto(vehicle, race);
    }
}
