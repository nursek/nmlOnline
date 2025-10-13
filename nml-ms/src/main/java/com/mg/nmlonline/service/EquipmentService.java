package com.mg.nmlonline.service;

import com.mg.nmlonline.dto.EquipmentDto;
import com.mg.nmlonline.entity.equipment.EquipmentEntity;
import com.mg.nmlonline.mapper.EquipmentMapper;
import com.mg.nmlonline.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentMapper equipmentMapper;

    public EquipmentService(EquipmentRepository equipmentRepository, EquipmentMapper equipmentMapper) {
        this.equipmentRepository = equipmentRepository;
        this.equipmentMapper = equipmentMapper;
    }

    // --- Lecture (DTO) utilisées par les controllers ---
    public List<EquipmentDto> listAllDtos() {
        return equipmentRepository.findAll().stream()
                .map(equipmentMapper::entityToDto)
                .collect(Collectors.toList());
    }

    public Optional<EquipmentDto> findDtoById(Long id) {
        return equipmentRepository.findById(id).map(equipmentMapper::entityToDto);
    }

    // --- CRUD sur les entités (restent disponibles) ---
    public List<EquipmentEntity> listAllEntities() {
        return equipmentRepository.findAll();
    }

    public Optional<EquipmentEntity> findById(Long id) {
        return equipmentRepository.findById(id);
    }

    public EquipmentEntity create(EquipmentEntity payload) {
        return equipmentRepository.save(payload);
    }

    public EquipmentEntity update(Long id, EquipmentEntity payload) {
        if (!equipmentRepository.existsById(id)) {
            return null;
        }
        payload.setId(id);
        return equipmentRepository.save(payload);
    }

    public boolean delete(Long id) {
        if (!equipmentRepository.existsById(id)) return false;
        equipmentRepository.deleteById(id);
        return true;
    }

    // --- Nouveaux helpers pour POST/PUT via DTO (controllers appellent ces méthodes) ---
    @Transactional
    public EquipmentDto createFromDto(EquipmentDto dto) {
        EquipmentEntity entity = equipmentMapper.dtoToEntity(dto);
        // s'assurer qu'on n'essaie pas de forcer un ID côté create
        entity.setId(null);
        EquipmentEntity saved = equipmentRepository.save(entity);
        return equipmentMapper.entityToDto(saved);
    }

    @Transactional
    public EquipmentDto updateFromDto(Long id, EquipmentDto dto) {
        if (!equipmentRepository.existsById(id)) return null;
        EquipmentEntity entity = equipmentMapper.dtoToEntity(dto);
        entity.setId(id);
        EquipmentEntity saved = equipmentRepository.save(entity);
        return equipmentMapper.entityToDto(saved);
    }
}
