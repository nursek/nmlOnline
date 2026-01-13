package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.MoveOrderDTO;
import com.mg.nmlonline.domain.model.battle.MoveOrder;
import com.mg.nmlonline.domain.model.battle.MoveType;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Mapper pour convertir entre MoveOrderDTO (API) et MoveOrder (domaine).
 */
@Component
public class MoveOrderMapper {

    private final PlayerMapper playerMapper;
    private final PlayerRepository playerRepository;

    public MoveOrderMapper(PlayerMapper playerMapper, PlayerRepository playerRepository) {
        this.playerMapper = playerMapper;
        this.playerRepository = playerRepository;
    }

    /**
     * Convertit un MoveOrderDTO en MoveOrder du domaine.
     * Nécessite de résoudre les références (joueur, unités) à partir des IDs.
     *
     * @param dto Le DTO à convertir
     * @param unitsMap Map des unités disponibles par leur ID (pour résolution rapide)
     * @return L'ordre de déplacement du domaine
     */
    public MoveOrder toDomain(MoveOrderDTO dto, java.util.Map<Integer, Unit> unitsMap) {
        if (dto == null) return null;

        MoveOrder order = new MoveOrder();

        // Résolution du joueur
        if (dto.getPlayerId() != null) {
            playerRepository.findById(dto.getPlayerId()).ifPresent(playerEntity -> order.setPlayer(playerMapper.toDomain(playerEntity)));
        }

        // Secteurs
        order.setFromSectorId(dto.getFromSectorId());
        order.setToSectorId(dto.getToSectorId());

        // Résolution des unités
        if (dto.getUnitIds() != null && unitsMap != null) {
            List<Unit> units = dto.getUnitIds().stream()
                    .map(unitsMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            order.setUnits(units);
        } else {
            order.setUnits(new ArrayList<>());
        }

        // Type de déplacement
        if (dto.getMoveType() != null) {
            try {
                order.setMoveType(MoveType.valueOf(dto.getMoveType()));
            } catch (IllegalArgumentException e) {
                order.setMoveType(MoveType.NEUTRAL); // Valeur par défaut
            }
        }

        // Secteur intermédiaire
        order.setIntermediateSectorId(dto.getIntermediateSectorId());

        // Flags
        order.setIntercepted(dto.getIntercepted() != null ? dto.getIntercepted() : false);
        order.setInstant(dto.getInstant() != null ? dto.getInstant() : false);

        return order;
    }

    /**
     * Convertit un MoveOrder du domaine en MoveOrderDTO.
     *
     * @param order L'ordre de déplacement du domaine
     * @return Le DTO correspondant
     */
    public MoveOrderDTO toDTO(MoveOrder order) {
        if (order == null) return null;

        MoveOrderDTO dto = new MoveOrderDTO();

        // Joueur
        if (order.getPlayer() != null) {
            dto.setPlayerId(order.getPlayer().getId());
        }

        // Secteurs
        dto.setFromSectorId(order.getFromSectorId());
        dto.setToSectorId(order.getToSectorId());

        // IDs des unités
        if (order.getUnits() != null) {
            List<Integer> unitIds = order.getUnits().stream()
                    .map(Unit::getId)
                    .collect(Collectors.toList());
            dto.setUnitIds(unitIds);
        }

        // Type de déplacement
        if (order.getMoveType() != null) {
            dto.setMoveType(order.getMoveType().name());
        }

        // Secteur intermédiaire
        dto.setIntermediateSectorId(order.getIntermediateSectorId());

        // Flags
        dto.setIntercepted(order.isIntercepted());
        dto.setInstant(order.isInstant());

        return dto;
    }

    /**
     * Convertit une liste de MoveOrderDTO en liste de MoveOrder.
     */
    public List<MoveOrder> toDomainList(List<MoveOrderDTO> dtos, java.util.Map<Integer, Unit> unitsMap) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream()
                .map(dto -> toDomain(dto, unitsMap))
                .collect(Collectors.toList());
    }

    /**
     * Convertit une liste de MoveOrder en liste de MoveOrderDTO.
     */
    public List<MoveOrderDTO> toDTOList(List<MoveOrder> orders) {
        if (orders == null) return new ArrayList<>();
        return orders.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}

