package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.SectorDto;
import com.mg.nmlonline.api.dto.SectorStatsDto;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.sector.SectorStats;
import com.mg.nmlonline.domain.model.unit.Unit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class SectorMapper {

    private final UnitMapper unitMapper;
    private final BuildingMapper buildingMapper;
    private final GameCharacterMapper gameCharacterMapper;
    private final VehicleMapper vehicleMapper;

    public SectorMapper(UnitMapper unitMapper, BuildingMapper buildingMapper,
                        GameCharacterMapper gameCharacterMapper, VehicleMapper vehicleMapper) {
        this.unitMapper = unitMapper;
        this.buildingMapper = buildingMapper;
        this.gameCharacterMapper = gameCharacterMapper;
        this.vehicleMapper = vehicleMapper;
    }

    public Sector toDomain(SectorDto dto) {
        if (dto == null) return null;

        Sector sector = new Sector(dto.getNumber(), dto.getName());
        sector.setIncome(dto.getIncome() != null ? dto.getIncome() : 2000.0);

        sector.setOwnerId(dto.getOwnerId());
        sector.setColor(dto.getColor() != null ? dto.getColor() : "#ffffff");
        if (dto.getResource() != null) {
            sector.setResourceName(dto.getResource());
        }

        sector.setX(dto.getX());
        sector.setY(dto.getY());

        if (dto.getNeighbors() != null) {
            for (Integer neighborNumber : dto.getNeighbors()) {
                sector.addNeighbor(neighborNumber);
            }
        }

        if (dto.getStats() != null) {
            SectorStats stats = new SectorStats();
            SectorStatsDto statsDto = dto.getStats();
            stats.setTotalAtk(statsDto.getTotalAtk());
            stats.setTotalPdf(statsDto.getTotalPdf());
            stats.setTotalPdc(statsDto.getTotalPdc());
            stats.setTotalDef(statsDto.getTotalDef());
            stats.setTotalArmor(statsDto.getTotalArmor());
            stats.setTotalOffensive(statsDto.getTotalOffensive());
            stats.setTotalDefensive(statsDto.getTotalDefensive());
            stats.setGlobalStats(statsDto.getGlobalStats());
            sector.setStats(stats);
        }

        if (dto.getArmy() != null) {
            List<Unit> units = dto.getArmy().stream()
                    .map(unitMapper::toDomain)
                    .toList();
            sector.setArmy(new ArrayList<>(units));
        }

        return sector;
    }

    /** Contexte de lecture : propriétaire ou allié avec une troupe sur place → détail complet, sinon restrictif. */
    public record Visibility(Long requesterPlayerId, Set<Long> allyIds) {

        public static Visibility none() {
            return new Visibility(null, Set.of());
        }

        public boolean canSeeDetail(Sector sector) {
            if (requesterPlayerId == null || sector == null) {
                return false;
            }
            if (sector.isOwnedBy(requesterPlayerId)) {
                return true;
            }
            Long ownerId = sector.getOwnerId();
            return ownerId != null && allyIds.contains(ownerId) && hasOwnEntityPresent(sector, requesterPlayerId);
        }
    }

    private static boolean hasOwnEntityPresent(Sector sector, Long playerId) {
        return sector.getUnits().stream()
                .anyMatch(unit -> playerId.equals(unit.getPlayerId()) && !unit.isDestroyed())
                || sector.getCharacters().stream()
                .anyMatch(character -> playerId.equals(character.getPlayerId()) && !character.isDestroyed());
    }

    /** Vue d'un secteur : détail complet si autorisé, sinon nom/propriétaire/ressource/voisins + présence de défenseurs. */
    public SectorDto toPublicDto(Sector sector, Visibility visibility) {
        if (sector == null) {
            return null;
        }
        if (visibility != null && visibility.canSeeDetail(sector)) {
            return toDto(sector);
        }
        return toRestrictedDto(sector);
    }

    /** Vue restreinte : aucun détail militaire (armée, personnages, véhicules, bâtiments, revenu, stats). */
    public SectorDto toRestrictedDto(Sector sector) {
        if (sector == null) return null;

        SectorDto dto = new SectorDto();
        dto.setNumber(sector.getNumber());
        dto.setName(sector.getName());
        dto.setOwnerId(sector.getOwnerId());
        dto.setBoardId(sector.getBoard() != null ? sector.getBoard().getId() : null);
        dto.setColor(sector.getColor());
        if (sector.getResourceName() != null && !sector.getResourceName().isEmpty()) {
            dto.setResource(sector.getResourceName());
        }
        dto.setNeighbors(new ArrayList<>(sector.getNeighbors()));
        dto.setX(sector.getX());
        dto.setY(sector.getY());
        dto.setHasDefenders(hasDefenders(sector));
        return dto;
    }

    private boolean hasDefenders(Sector sector) {
        return sector.getUnits().stream().anyMatch(unit -> !unit.isDestroyed())
                || sector.getCharacters().stream().anyMatch(character -> !character.isDestroyed());
    }

    public SectorDto toDto(Sector sector) {
        if (sector == null) return null;

        SectorDto dto = new SectorDto();
        dto.setNumber(sector.getNumber());
        dto.setName(sector.getName());
        dto.setIncome(sector.getIncome());

        dto.setOwnerId(sector.getOwnerId());
        dto.setBoardId(sector.getBoard() != null ? sector.getBoard().getId() : null);
        dto.setColor(sector.getColor());
        if (sector.getResourceName() != null && !sector.getResourceName().isEmpty()) {
            dto.setResource(sector.getResourceName());
        }
        dto.setNeighbors(new ArrayList<>(sector.getNeighbors()));

        dto.setX(sector.getX());
        dto.setY(sector.getY());

        if (sector.getStats() != null) {
            SectorStatsDto statsDto = new SectorStatsDto();
            SectorStats stats = sector.getStats();
            statsDto.setTotalAtk(stats.getTotalAtk());
            statsDto.setTotalPdf(stats.getTotalPdf());
            statsDto.setTotalPdc(stats.getTotalPdc());
            statsDto.setTotalDef(stats.getTotalDef());
            statsDto.setTotalArmor(stats.getTotalArmor());
            statsDto.setTotalOffensive(stats.getTotalOffensive());
            statsDto.setTotalDefensive(stats.getTotalDefensive());
            statsDto.setGlobalStats(stats.getGlobalStats());
            dto.setStats(statsDto);
        }

        if (sector.getArmy() != null) {
            dto.setArmy(sector.getArmy().stream()
                    .map(unitMapper::toDto)
                    .toList());
        }

        if (sector.getBuildings() != null && !sector.getBuildings().isEmpty()) {
            dto.setBuildings(sector.getBuildings().stream()
                    .map(buildingMapper::toDto)
                    .toList());
        }

        // Un seul personnage par secteur au max
        if (sector.getCharacters() != null && !sector.getCharacters().isEmpty()) {
            dto.setCharacter(gameCharacterMapper.toDto(sector.getCharacters().getFirst()));
        }

        if (sector.getVehicles() != null && !sector.getVehicles().isEmpty()) {
            dto.setVehicles(sector.getVehicles().stream()
                    .map(vehicleMapper::toDto)
                    .toList());
        }

        return dto;
    }
}
