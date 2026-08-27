package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.EquipmentDto;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.service.EquipmentService;
import com.mg.nmlonline.mapper.EquipmentMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final EquipmentMapper equipmentMapper;

    public EquipmentController(EquipmentService equipmentService, EquipmentMapper equipmentMapper) {
        this.equipmentService = equipmentService;
        this.equipmentMapper = equipmentMapper;
    }

    @GetMapping
    public Page<EquipmentDto> findAll(Pageable pageable) {
        return equipmentService.findAllDto(pageable);
    }

    @GetMapping("/{id}")
    public EquipmentDto findById(@PathVariable("id") Long id) {
        return equipmentService.findByIdDto(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public EquipmentDto create(@RequestBody EquipmentDto dto) {
        Equipment equipment = equipmentMapper.toDomain(dto);
        return equipmentService.createDto(equipment);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        if(!equipmentService.delete(id)) {
            throw new EntityNotFoundException("Equipment with id " + id + " not found.");
        }
    }
}
