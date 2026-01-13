package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service simplifié pour Equipment - utilise directement les classes du domaine
 */
@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    public List<Equipment> findAll() {
        return equipmentRepository.findAll();
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

    @Transactional
    public Equipment save(Equipment equipment) {
        return equipmentRepository.save(equipment);
    }

    public boolean delete(Long id) {
        if (!equipmentRepository.existsById(id)) return false;
        equipmentRepository.deleteById(id);
        return true;
    }
}
