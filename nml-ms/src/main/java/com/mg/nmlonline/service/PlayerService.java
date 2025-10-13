package com.mg.nmlonline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.dto.PlayerDto;
import com.mg.nmlonline.entity.player.PlayerEntity;
import com.mg.nmlonline.mapper.PlayerMapper;
import com.mg.nmlonline.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service d'application pour la gestion des players.
 * - Expose des DTOs pour les controllers
 * - Opérations CRUD sur PlayerEntity
 * - Orchestration import/export via PlayerImportService / PlayerExportService
 * - Sérialisation simple des champs complexes en BLOB pour la persistence
 */
@Service
public class PlayerService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;
    private final PlayerImportService playerImportService;
    private final PlayerExportService playerExportService;

    public PlayerService(PlayerRepository playerRepository,
                         PlayerMapper playerMapper,
                         PlayerImportService playerImportService,
                         PlayerExportService playerExportService) {
        this.playerRepository = playerRepository;
        this.playerMapper = playerMapper;
        this.playerImportService = playerImportService;
        this.playerExportService = playerExportService;
    }

    // --- Lecture (DTO) pour controllers ---
    public List<PlayerDto> listAllDtos() {
        return playerRepository.findAll().stream()
                .map(playerMapper::entityToDto)
                .collect(Collectors.toList());
    }

    public Optional<PlayerDto> findDtoById(Long id) {
        return playerRepository.findById(id).map(playerMapper::entityToDto);
    }

    // --- CRUD entités (utilisées par controllers si nécessaire) ---
    public List<PlayerEntity> listAllEntities() {
        return playerRepository.findAll();
    }

    public Optional<PlayerEntity> findEntityById(Long id) {
        return playerRepository.findById(id);
    }

    public PlayerEntity createEntity(PlayerEntity payload) {
        return playerRepository.save(payload);
    }

    public PlayerEntity updateEntity(Long id, PlayerEntity payload) {
        if (!playerRepository.existsById(id)) {
            return null;
        }
        payload.setId(id);
        return playerRepository.save(payload);
    }

    public boolean deleteEntity(Long id) {
        if (!playerRepository.existsById(id)) return false;
        playerRepository.deleteById(id);
        return true;
    }

    // --- Sauvegarde d'un domaine Player (utilitaire) ---
    public PlayerEntity saveDomainAsEntity(com.mg.nmlonline.entity.player.Player player) {
        PlayerEntity e = playerMapper.domainToEntity(player);
        return playerRepository.save(e);
    }

    // --- Import / Export (délégué aux services dédiés) ---
    public com.mg.nmlonline.entity.player.Player importPlayerFromJson(String filePath) throws IOException {
        return playerImportService.importPlayerFromJson(filePath);
    }

    public void savePlayerToJson(com.mg.nmlonline.entity.player.Player player, String filePath) throws IOException {
        playerExportService.savePlayerToJson(player, filePath);
    }

    // --- Création / mise à jour via PlayerDto (utilisés par controllers POST/PUT) ---
    @Transactional
    public PlayerDto createFromDto(PlayerDto dto) {
        try {
            PlayerEntity e = new PlayerEntity();
            e.setId(null);
            e.setUsername(dto.getName());
            // Sérialisation simple des objets complexes en BLOB
            e.setStats(objectMapper.writeValueAsBytes(dto.getMoney()));
            e.setEquipments(objectMapper.writeValueAsBytes(dto.getEquipments()));
            e.setSectors(objectMapper.writeValueAsBytes(dto.getSectors()));
            PlayerEntity saved = playerRepository.save(e);
            return playerMapper.entityToDto(saved);
        } catch (IOException ex) {
            throw new RuntimeException("Erreur de sérialisation lors de la création du joueur", ex);
        }
    }

    @Transactional
    public PlayerDto updateFromDto(Long id, PlayerDto dto) {
        if (!playerRepository.existsById(id)) return null;
        try {
            PlayerEntity e = new PlayerEntity();
            e.setId(id);
            e.setUsername(dto.getName());
            e.setStats(objectMapper.writeValueAsBytes(dto.getMoney()));
            e.setEquipments(objectMapper.writeValueAsBytes(dto.getEquipments()));
            e.setSectors(objectMapper.writeValueAsBytes(dto.getSectors()));
            PlayerEntity saved = playerRepository.save(e);
            return playerMapper.entityToDto(saved);
        } catch (IOException ex) {
            throw new RuntimeException("Erreur de sérialisation lors de la mise à jour du joueur", ex);
        }
    }
}
