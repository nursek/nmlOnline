package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BuyVehicleRequestDto;
import com.mg.nmlonline.api.dto.VehicleDto;
import com.mg.nmlonline.domain.exception.InsufficientFundsException;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.SectorRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import com.mg.nmlonline.mapper.VehicleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class VehicleService {

    private final PlayerRepository playerRepository;
    private final VehicleRepository vehicleRepository;
    private final SectorRepository sectorRepository;
    private final VehicleMapper vehicleMapper;

    public VehicleService(PlayerRepository playerRepository, VehicleRepository vehicleRepository,
                          SectorRepository sectorRepository, VehicleMapper vehicleMapper) {
        this.playerRepository = playerRepository;
        this.vehicleRepository = vehicleRepository;
        this.sectorRepository = sectorRepository;
        this.vehicleMapper = vehicleMapper;
    }

    public List<VehicleType> getAllVehicleTypes() {
        return Arrays.asList(VehicleType.values());
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

        Player player = playerRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new RuntimeException("Joueur introuvable pour userId : " + userId));

        List<Vehicle> created = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            Vehicle vehicle = player.buyVehicle(vehicleType);
            if (vehicle == null) {
                throw new InsufficientFundsException("Fonds insuffisants pour acheter ce véhicule (coût : " + vehicleType.getCost() + " ₡)");
            }
            created.add(vehicleRepository.save(vehicle));
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
            Vehicle vehicle = player.buyVehicle(vehicleType);
            if (vehicle == null) {
                throw new InsufficientFundsException("Fonds insuffisants pour acheter le véhicule " + vehicleType.name());
            }
            created.add(vehicleRepository.save(vehicle));
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

        Sector sector = sectorRepository.findByBoard_IdAndNumber(boardId, sectorNumber)
                .orElseThrow(() -> new RuntimeException("Secteur introuvable"));

        if (!player.getId().equals(sector.getOwnerId())) {
            throw new SecurityException("Vous ne possédez pas ce secteur");
        }

        vehicle.setSector(sector);
        return vehicleRepository.save(vehicle);
    }

    // === Mapping dans la transaction (passengers/pilot/sector sont LAZY) ===

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
