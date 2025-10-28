package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.infrastructure.entity.EquipmentEntity;
import com.mg.nmlonline.mapper.EquipmentMapper;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentMapper equipmentMapper;

    public EquipmentService(EquipmentRepository equipmentRepository, EquipmentMapper equipmentMapper) {
        this.equipmentRepository = equipmentRepository;
        this.equipmentMapper = equipmentMapper;
    }

    // --- Lecture (DTO) utilisées par les controllers ---
    public List<Equipment> findAll() {
        return equipmentRepository.findAll().stream()
                .map(equipmentMapper::toDomain)
                .toList();
    }

    public Equipment findById(Long id) {
        return equipmentRepository.findById(id)
                .map(equipmentMapper::toDomain)
                .orElse(null);
    }

    @Transactional
    public Equipment create(Equipment equipment) {
        EquipmentEntity entity = equipmentMapper.toEntity(equipment);
        EquipmentEntity saved = equipmentRepository.save(entity);
        return equipmentMapper.toDomain(saved);
    }

    public boolean delete(Long id) {
        if (!equipmentRepository.existsById(id)) return false;
        equipmentRepository.deleteById(id);
        return true;
    }
}
