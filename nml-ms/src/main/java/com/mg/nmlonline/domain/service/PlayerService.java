package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BuyEquipmentItemDto;
import com.mg.nmlonline.api.dto.PlayerDto;
import com.mg.nmlonline.domain.exception.InsufficientFundsException;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import com.mg.nmlonline.mapper.PlayerMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final SectorService sectorService;
    private final EquipmentService equipmentService;
    private final VehicleRepository vehicleRepository;
    private final PlayerMapper playerMapper;
    private final BoardService boardService;

    public PlayerService(PlayerRepository playerRepository,
                          SectorService sectorService,
                          EquipmentService equipmentService,
                          VehicleRepository vehicleRepository,
                          PlayerMapper playerMapper,
                          BoardService boardService) {
        this.playerRepository = playerRepository;
        this.sectorService = sectorService;
        this.equipmentService = equipmentService;
        this.vehicleRepository = vehicleRepository;
        this.playerMapper = playerMapper;
        this.boardService = boardService;
    }

    public Page<Player> findAll(Pageable pageable) {
        return playerRepository.findAllByOrderByNameAsc(pageable);
    }

    public Optional<Player> findById(Long id) {
        return playerRepository.findById(id);
    }

    public Player findByName(String name) {
        return playerRepository.findByName(name).orElse(null);
    }

    public Player findByUserId(Long userId) {
        return playerRepository.findByUserId(userId).orElse(null);
    }

    @Transactional
    public Player save(Player player) {
        return playerRepository.save(player);
    }

    /** Achat atomique : valide tous les items (existence, quantité, fonds) avant d'appliquer. */
    @Transactional
    public Player buyEquipments(Long playerId, List<BuyEquipmentItemDto> items) {
        if (playerId == null) {
            throw new IllegalArgumentException("Player ID is required");
        }
        Player player = playerRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("No items to buy");
        }

        record ResolvedItem(Equipment equipment, int quantity) {}
        List<ResolvedItem> resolvedItems = new ArrayList<>();
        double totalCost = 0;

        for (BuyEquipmentItemDto item : items) {
            if (item == null) {
                throw new IllegalArgumentException("Cart contains a null item");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0 for: " + item.getName());
            }
            Equipment equipment = equipmentService.findByName(item.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found: " + item.getName()));
            totalCost += (double) equipment.getCost() * item.getQuantity();
            resolvedItems.add(new ResolvedItem(equipment, item.getQuantity()));
        }

        if (player.getStats().getMoney() < totalCost) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        for (ResolvedItem resolved : resolvedItems) {
            boolean success = player.buyEquipment(resolved.equipment(), resolved.quantity());
            if (!success) {
                throw new IllegalStateException("Failed to apply purchase for: " + resolved.equipment().getName());
            }
        }

        return playerRepository.save(player);
    }

    @Transactional
    public boolean delete(Long id) {
        if (!playerRepository.existsById(id)) return false;
        sectorService.removePlayerFromSectors(id);
        // Vehicle étend CombatEntity mais n'a pas de relation @ManyToOne vers Player :
        // le cascade de Player ne l'atteint pas — suppression explicite sinon FK violation.
        vehicleRepository.deleteAll(vehicleRepository.findByPlayerId(id));
        playerRepository.deleteById(id);
        return true;
    }

    // === Mapping dans la transaction (collections LAZY : equipments/resources/buildings/character) ===

    @Transactional(readOnly = true)
    public Page<PlayerDto> findAllDto(Pageable pageable) {
        Board board = boardService.getAllBoards().stream().findFirst().orElse(null);
        return playerRepository.findAllByOrderByNameAsc(pageable)
                .map(p -> playerMapper.toDtoWithSectors(p, board));
    }

    @Transactional(readOnly = true)
    public Optional<PlayerDto> findByNameDto(String name) {
        Player player = playerRepository.findByName(name).orElse(null);
        if (player == null) return Optional.empty();
        Board board = boardService.getAllBoards().stream().findFirst().orElse(null);
        return Optional.of(playerMapper.toDtoWithSectors(player, board));
    }

    @Transactional
    public PlayerDto buyEquipmentsDto(Long playerId, List<BuyEquipmentItemDto> items) {
        Player saved = buyEquipments(playerId, items);
        Board board = boardService.getAllBoards().stream().findFirst().orElse(null);
        return playerMapper.toDtoWithSectors(saved, board);
    }
}
