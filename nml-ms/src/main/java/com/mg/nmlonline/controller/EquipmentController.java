package com.mg.nmlonline.controller;

import com.mg.nmlonline.dto.EquipmentDto;
import com.mg.nmlonline.entity.equipment.EquipmentEntity;
import com.mg.nmlonline.service.EquipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public List<EquipmentDto> listAll() {
        return equipmentService.listAllDtos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentDto> getOne(@PathVariable Long id) {
        return equipmentService.findDtoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EquipmentDto> create(@RequestBody EquipmentDto payload) {
        EquipmentDto saved = equipmentService.createFromDto(payload);
        return ResponseEntity.created(URI.create("/api/equipment/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipmentDto> update(@PathVariable Long id, @RequestBody EquipmentDto payload) {
        EquipmentDto updated = equipmentService.updateFromDto(id, payload);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = equipmentService.delete(id);
        if (!deleted) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }
}
