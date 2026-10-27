package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.AdminBuildingCreateRequestDto;
import com.mg.nmlonline.api.dto.AdminCatalogDto;
import com.mg.nmlonline.api.dto.AdminItemQuantityRequestDto;
import com.mg.nmlonline.api.dto.AdminMoneyRequestDto;
import com.mg.nmlonline.api.dto.AdminPlayerEditDto;
import com.mg.nmlonline.api.dto.AdminUnitCreateRequestDto;
import com.mg.nmlonline.api.dto.AdminVehicleCreateRequestDto;
import com.mg.nmlonline.domain.service.AdminPlayerEditService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlayerEditController {

    private final AdminPlayerEditService adminPlayerEditService;

    public AdminPlayerEditController(AdminPlayerEditService adminPlayerEditService) {
        this.adminPlayerEditService = adminPlayerEditService;
    }

    @GetMapping("/players/{id}")
    public AdminPlayerEditDto getPlayer(@PathVariable Long id) {
        return adminPlayerEditService.getEdit(id);
    }

    @GetMapping("/catalog")
    public AdminCatalogDto getCatalog() {
        return adminPlayerEditService.getCatalog();
    }

    @PutMapping("/players/{id}/money")
    public ResponseEntity<Void> setMoney(@PathVariable Long id,
                                         @Valid @RequestBody AdminMoneyRequestDto request) {
        adminPlayerEditService.setMoney(id, request.getAmount());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/players/{id}/resources")
    public ResponseEntity<Void> addResource(@PathVariable Long id,
                                            @Valid @RequestBody AdminItemQuantityRequestDto request) {
        adminPlayerEditService.addResource(id, request.getName(), request.getQuantity());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/players/{id}/resources")
    public ResponseEntity<Void> removeResource(@PathVariable Long id,
                                               @RequestParam String name,
                                               @RequestParam int quantity) {
        adminPlayerEditService.removeResource(id, name, quantity);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/players/{id}/equipments")
    public ResponseEntity<Void> addEquipment(@PathVariable Long id,
                                             @Valid @RequestBody AdminItemQuantityRequestDto request) {
        adminPlayerEditService.addEquipment(id, request.getName(), request.getQuantity());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/players/{id}/equipments")
    public ResponseEntity<Void> removeEquipment(@PathVariable Long id,
                                                @RequestParam String name,
                                                @RequestParam int quantity,
                                                @RequestParam(defaultValue = "false") boolean includeEquipped) {
        adminPlayerEditService.removeEquipment(id, name, quantity, includeEquipped);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/players/{id}/units")
    public ResponseEntity<Void> addUnits(@PathVariable Long id,
                                         @Valid @RequestBody AdminUnitCreateRequestDto request) {
        adminPlayerEditService.addUnits(id, request.getType(), request.getClasses(),
                request.getQuantity(), request.getSectorNumber());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/players/{id}/units/{unitId}")
    public ResponseEntity<Void> deleteUnit(@PathVariable Long id, @PathVariable Long unitId) {
        adminPlayerEditService.deleteUnit(id, unitId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/players/{id}/vehicles")
    public ResponseEntity<Void> addVehicles(@PathVariable Long id,
                                            @Valid @RequestBody AdminVehicleCreateRequestDto request) {
        adminPlayerEditService.addVehicles(id, request.getVehicleType(), request.getQuantity(),
                request.getSectorNumber());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/players/{id}/vehicles/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id, @PathVariable Long vehicleId) {
        adminPlayerEditService.deleteVehicle(id, vehicleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/players/{id}/buildings")
    public ResponseEntity<Void> addBuilding(@PathVariable Long id,
                                            @Valid @RequestBody AdminBuildingCreateRequestDto request) {
        adminPlayerEditService.addBuilding(id, request.getBuildingType(), request.getSectorNumber());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/players/{id}/buildings/{buildingId}")
    public ResponseEntity<Void> deleteBuilding(@PathVariable Long id, @PathVariable Long buildingId) {
        adminPlayerEditService.deleteBuilding(id, buildingId);
        return ResponseEntity.noContent().build();
    }
}
