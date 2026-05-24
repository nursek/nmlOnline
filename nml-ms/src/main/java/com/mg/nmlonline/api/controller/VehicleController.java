package com.mg.nmlonline.api.controller;

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
import java.util.Map;

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
    public ResponseEntity<?> getMyVehicles(HttpServletRequest httpRequest) {
        Long authenticatedUserId = (Long) httpRequest.getAttribute("userId");
        if (authenticatedUserId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Non authentifié"));
        }
        try {
            List<VehicleDto> vehicles = vehicleService.getPlayerVehicles(authenticatedUserId).stream()
                    .map(vehicleMapper::toDto)
                    .toList();
            return ResponseEntity.ok(vehicles);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Achète un ou plusieurs véhicules pour le joueur authentifié.
     */
    @PostMapping("/buy")
    public ResponseEntity<?> buyVehicle(@RequestBody BuyVehicleRequestDto request,
                                        HttpServletRequest httpRequest) {
        //TODO : Le DTO de requête n’est pas validé côté controller (@Valid absent) et BuyVehicleRequestDto n’a pas de contraintes. Ajoutez @Valid ici et des annotations (@NotBlank sur vehicleType, @Min(1) sur quantity) pour obtenir des erreurs 400 cohérentes et éviter de dépendre uniquement des exceptions du service.
        Long authenticatedUserId = (Long) httpRequest.getAttribute("userId");
        if (authenticatedUserId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Non authentifié"));
        }
        try {
            List<Vehicle> vehicles = vehicleService.buyVehicle(authenticatedUserId, request.getVehicleType(), request.getQuantity());
            return ResponseEntity.ok(vehicles.stream().map(vehicleMapper::toDto).toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(402).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Déploie un véhicule sur un secteur possédé par le joueur authentifié.
     */
    @PostMapping("/{id}/place")
    public ResponseEntity<?> placeVehicle(@PathVariable Long id,
                                          @Valid @RequestBody PlaceVehicleRequestDto request,
                                          HttpServletRequest httpRequest) {
        Long authenticatedUserId = (Long) httpRequest.getAttribute("userId");
        if (authenticatedUserId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Non authentifié"));
        }
        try {
            Vehicle vehicle = vehicleService.placeVehicle(id, request.getBoardId(), request.getSectorNumber(), authenticatedUserId);
            return ResponseEntity.ok(vehicleMapper.toDto(vehicle));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }
}
