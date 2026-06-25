package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.BuyVehicleBatchRequestDto;
import com.mg.nmlonline.api.dto.BuyVehicleRequestDto;
import com.mg.nmlonline.api.dto.PlaceVehicleRequestDto;
import com.mg.nmlonline.api.dto.VehicleDto;
import com.mg.nmlonline.api.dto.VehicleTypeDto;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.service.VehicleService;
import com.mg.nmlonline.mapper.VehicleMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleMapper vehicleMapper;

    public VehicleController(VehicleService vehicleService, VehicleMapper vehicleMapper) {
        this.vehicleService = vehicleService;
        this.vehicleMapper = vehicleMapper;
    }

    /**
     * Liste tous les types de véhicules disponibles à l'achat (publique).
     */
    @GetMapping("/types")
    public List<VehicleTypeDto> getVehicleTypes() {
        return vehicleService.getAllVehicleTypes().stream()
                .map(vehicleMapper::vehicleTypeToDto)
                .toList();
    }

    /**
     * Retourne tous les véhicules du joueur authentifié (déployés et non-déployés).
     */
    @GetMapping("/my")
    public ResponseEntity<List<VehicleDto>> getMyVehicles(HttpServletRequest httpRequest) {
        Long authenticatedUserId = (Long) httpRequest.getAttribute("userId");
        if (authenticatedUserId == null) {
            return ResponseEntity.status(401).build();
        }
        List<VehicleDto> vehicles = vehicleService.getPlayerVehicles(authenticatedUserId).stream()
                .map(vehicleMapper::toDto)
                .toList();
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Achète un ou plusieurs véhicules pour le joueur authentifié.
     */
    @PostMapping("/buy")
    public ResponseEntity<List<VehicleDto>> buyVehicle(@Valid @RequestBody BuyVehicleRequestDto request,
                                                       HttpServletRequest httpRequest) {
        Long authenticatedUserId = (Long) httpRequest.getAttribute("userId");
        if (authenticatedUserId == null) {
            return ResponseEntity.status(401).build();
        }
        List<Vehicle> vehicles = vehicleService.buyVehicle(authenticatedUserId, request.getVehicleType(), request.getQuantity());
        return ResponseEntity.ok(vehicles.stream().map(vehicleMapper::toDto).toList());
    }

    /**
     * Achète un lot de véhicules de manière atomique pour le joueur authentifié.
     */
    @PostMapping("/buy-batch")
    public ResponseEntity<List<VehicleDto>> buyVehiclesBatch(@Valid @RequestBody BuyVehicleBatchRequestDto request,
                                                             HttpServletRequest httpRequest) {
        Long authenticatedUserId = (Long) httpRequest.getAttribute("userId");
        if (authenticatedUserId == null) {
            return ResponseEntity.status(401).build();
        }
        List<Vehicle> vehicles = vehicleService.buyVehiclesBatch(authenticatedUserId, request.getItems());
        return ResponseEntity.ok(vehicles.stream().map(vehicleMapper::toDto).toList());
    }

    /**
     * Déploie un véhicule sur un secteur possédé par le joueur authentifié.
     */
    @PostMapping("/{id}/place")
    public ResponseEntity<VehicleDto> placeVehicle(@PathVariable Long id,
                                                   @Valid @RequestBody PlaceVehicleRequestDto request,
                                                   HttpServletRequest httpRequest) {
        Long authenticatedUserId = (Long) httpRequest.getAttribute("userId");
        if (authenticatedUserId == null) {
            return ResponseEntity.status(401).build();
        }
        Vehicle vehicle = vehicleService.placeVehicle(id, request.getBoardId(), request.getSectorNumber(), authenticatedUserId);
        return ResponseEntity.ok(vehicleMapper.toDto(vehicle));
    }
}
