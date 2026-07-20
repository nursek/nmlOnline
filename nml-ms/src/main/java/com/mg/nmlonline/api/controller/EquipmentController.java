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

import java.util.List;

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
        return equipmentService.findAll(pageable)
                .map(equipmentMapper::toDto);
    }

    @GetMapping("/{id}")
    public EquipmentDto findById(@PathVariable("id") Long id) {
        Equipment equipment = equipmentService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equipment with id " + id + " not found."));
        return equipmentMapper.toDto(equipment);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public EquipmentDto create(@RequestBody EquipmentDto dto) {
        Equipment equipment = equipmentMapper.toDomain(dto);
        Equipment created = equipmentService.create(equipment);
        return equipmentMapper.toDto(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        if(!equipmentService.delete(id)) {
            throw new EntityNotFoundException("Equipment with id " + id + " not found.");
        }
    }
}
