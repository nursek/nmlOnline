package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.EquipmentDto;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import com.mg.nmlonline.mapper.EquipmentMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentMapper equipmentMapper;

    public EquipmentService(EquipmentRepository equipmentRepository, EquipmentMapper equipmentMapper) {
        this.equipmentRepository = equipmentRepository;
        this.equipmentMapper = equipmentMapper;
    }

    public Page<Equipment> findAll(Pageable pageable) {
        return equipmentRepository.findAll(pageable);
    }

    public Optional<Equipment> findById(Long id) {
        return equipmentRepository.findById(id);
    }

    public Optional<Equipment> findByName(String name) {
        return equipmentRepository.findByName(name);
    }

    @Transactional
    public Equipment create(Equipment equipment) {
        return equipmentRepository.save(equipment);
    }

    public boolean delete(Long id) {
        if (!equipmentRepository.existsById(id)) return false;
        equipmentRepository.deleteById(id);
        return true;
    }

    // Mapping dans la transaction (compatibleClasses est LAZY).

    @Transactional(readOnly = true)
    public Page<EquipmentDto> findAllDto(Pageable pageable) {
        return equipmentRepository.findAll(pageable).map(equipmentMapper::toDto);
    }

    @Transactional(readOnly = true)
    public EquipmentDto findByIdDto(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equipment with id " + id + " not found."));
        return equipmentMapper.toDto(equipment);
    }

    @Transactional
    public EquipmentDto createDto(Equipment equipment) {
        return equipmentMapper.toDto(equipmentRepository.save(equipment));
    }
}
