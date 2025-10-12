package com.mg.nmlonline.controller;

import com.mg.nmlonline.entity.equipment.EquipmentEntity;
import com.mg.nmlonline.repository.EquipmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentRepository equipmentRepository;

    public EquipmentController(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    @GetMapping
    public List<EquipmentEntity> listAll() {
        return equipmentRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentEntity> getOne(@PathVariable Long id) {
        return equipmentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EquipmentEntity> create(@RequestBody EquipmentEntity payload) {
        EquipmentEntity saved = equipmentRepository.save(payload);
        return ResponseEntity.created(URI.create("/api/equipment/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipmentEntity> update(@PathVariable Long id, @RequestBody EquipmentEntity payload) {
        if (!equipmentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        payload.setId(id);
        EquipmentEntity saved = equipmentRepository.save(payload);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!equipmentRepository.existsById(id)) return ResponseEntity.notFound().build();
        equipmentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
