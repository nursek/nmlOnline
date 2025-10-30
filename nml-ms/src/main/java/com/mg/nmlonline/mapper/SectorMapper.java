package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.SectorDto;
import com.mg.nmlonline.api.dto.SectorStatsDto;
import com.mg.nmlonline.domain.model.board.Resource;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.sector.SectorStats;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.infrastructure.entity.BoardEntity;
import com.mg.nmlonline.infrastructure.entity.PlayerEntity;
import com.mg.nmlonline.infrastructure.entity.SectorEntity;
import com.mg.nmlonline.infrastructure.entity.SectorStatsEmbeddable;
import com.mg.nmlonline.infrastructure.entity.UnitEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SectorMapper {

    private final UnitMapper unitMapper;

    public SectorMapper(UnitMapper unitMapper) {
        this.unitMapper = unitMapper;
    }

    /**
     * Convertit une entité SectorEntity en objet Sector du domaine
     */
    public Sector toDomain(SectorEntity entity) {
        if (entity == null) return null;

        Sector sector = new Sector(entity.getNumber(), entity.getName());
        sector.setIncome(entity.getIncome());

        // Nouvelles propriétés pour la carte
        sector.setOwnerId(entity.getOwnerId());
        sector.setColor(entity.getColor());
        if (entity.getResource() != null) {
            Resource resource = new Resource(entity.getResource(), 0.0);
            sector.setResource(resource);
        }
        // Conversion des voisins
        if (entity.getNeighbors() != null && !entity.getNeighbors().isEmpty()) {
            for (Integer neighborNumber : entity.getNeighbors()) {
                sector.addNeighbor(neighborNumber);
            }
        }

        // Conversion des stats
        if (entity.getStats() != null) {
            SectorStats stats = new SectorStats();
            SectorStatsEmbeddable statsEmb = entity.getStats();
            stats.setTotalAtk(statsEmb.getTotalAtk());
            stats.setTotalPdf(statsEmb.getTotalPdf());
            stats.setTotalPdc(statsEmb.getTotalPdc());
            stats.setTotalDef(statsEmb.getTotalDef());
            stats.setTotalArmor(statsEmb.getTotalArmor());
            stats.setTotalOffensive(statsEmb.getTotalOffensive());
            stats.setTotalDefensive(statsEmb.getTotalDefensive());
            stats.setGlobalStats(statsEmb.getGlobalStats());
            sector.setStats(stats);
        }

        // Conversion des unités
        if (entity.getArmy() != null) {
            List<Unit> units = entity.getArmy().stream()
                    .map(unitMapper::toDomain)
                    .toList();
            sector.setArmy(units);
        }

        return sector;
    }

    /**
     * Convertit un DTO SectorDto en objet Sector du domaine
     */
    public Sector toDomain(SectorDto dto) {
        if (dto == null) return null;

        Sector sector = new Sector(dto.getNumber(), dto.getName());
        sector.setIncome(dto.getIncome());

        // Nouvelles propriétés pour la carte
        sector.setOwnerId(dto.getOwnerId());
        sector.setColor(dto.getColor());
        if (dto.getResource() != null) {
            Resource resource = new Resource(dto.getResource(), 0.0);
            sector.setResource(resource);
        }
        // Conversion des voisins
        if (dto.getNeighbors() != null && !dto.getNeighbors().isEmpty()) {
            for (Integer neighborNumber : dto.getNeighbors()) {
                sector.addNeighbor(neighborNumber);
            }
        }

        // Conversion des stats
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

        // Conversion des unités
        if (dto.getArmy() != null) {
            List<Unit> units = dto.getArmy().stream()
                    .map(unitMapper::toDomain)
                    .toList();
            sector.setArmy(units);
        }

        return sector;
    }

    /**
     * Convertit un objet Sector du domaine en entité SectorEntity (pour un joueur)
     */
    public SectorEntity toEntity(Sector sector, PlayerEntity player) {
        if (sector == null) return null;

        SectorEntity entity = new SectorEntity();
        entity.setNumber(sector.getNumber());
        entity.setName(sector.getName());
        entity.setIncome(sector.getIncome());

        // Nouvelles propriétés pour la carte
        entity.setOwnerId(sector.getOwnerId());
        entity.setColor(sector.getColor() != null ? sector.getColor() : "#ffffff");
        entity.setResource(sector.getResource() != null ? sector.getResource().getType() : null);
        if (sector.getNeighbors() != null && !sector.getNeighbors().isEmpty()) {
            entity.setNeighbors(new java.util.ArrayList<>(sector.getNeighbors()));
        }

        // Conversion des stats
        if (sector.getStats() != null) {
            SectorStatsEmbeddable statsEmb = new SectorStatsEmbeddable();
            SectorStats stats = sector.getStats();
            statsEmb.setTotalAtk(stats.getTotalAtk());
            statsEmb.setTotalPdf(stats.getTotalPdf());
            statsEmb.setTotalPdc(stats.getTotalPdc());
            statsEmb.setTotalDef(stats.getTotalDef());
            statsEmb.setTotalArmor(stats.getTotalArmor());
            statsEmb.setTotalOffensive(stats.getTotalOffensive());
            statsEmb.setTotalDefensive(stats.getTotalDefensive());
            statsEmb.setGlobalStats(stats.getGlobalStats());
            entity.setStats(statsEmb);
        }

        // Conversion des unités
        if (sector.getArmy() != null) {
            List<UnitEntity> unitEntities = sector.getArmy().stream()
                    .map(unit -> unitMapper.toEntity(unit, entity))
                    .toList();
            entity.setArmy(unitEntities);
        }

        return entity;
    }

    /**
     * Convertit un objet Sector du domaine en entité SectorEntity (pour une Board)
     */
    public SectorEntity toEntity(Sector sector, BoardEntity board) {
        if (sector == null) return null;

        SectorEntity entity = new SectorEntity();
        entity.setBoard(board);
        entity.setNumber(sector.getNumber());
        entity.setName(sector.getName());
        entity.setIncome(sector.getIncome());

        // Nouvelles propriétés pour la carte
        entity.setOwnerId(sector.getOwnerId());
        entity.setColor(sector.getColor() != null ? sector.getColor() : "#ffffff");
        entity.setResource(sector.getResource() != null ? sector.getResource().getType() : null);
        if (sector.getNeighbors() != null && !sector.getNeighbors().isEmpty()) {
            entity.setNeighbors(new java.util.ArrayList<>(sector.getNeighbors()));
        }

        // Conversion des stats
        if (sector.getStats() != null) {
            SectorStatsEmbeddable statsEmb = new SectorStatsEmbeddable();
            SectorStats stats = sector.getStats();
            statsEmb.setTotalAtk(stats.getTotalAtk());
            statsEmb.setTotalPdf(stats.getTotalPdf());
            statsEmb.setTotalPdc(stats.getTotalPdc());
            statsEmb.setTotalDef(stats.getTotalDef());
            statsEmb.setTotalArmor(stats.getTotalArmor());
            statsEmb.setTotalOffensive(stats.getTotalOffensive());
            statsEmb.setTotalDefensive(stats.getTotalDefensive());
            statsEmb.setGlobalStats(stats.getGlobalStats());
            entity.setStats(statsEmb);
        }

        // Conversion des unités
        if (sector.getArmy() != null) {
            List<UnitEntity> unitEntities = sector.getArmy().stream()
                    .map(unit -> unitMapper.toEntity(unit, entity))
                    .toList();
            entity.setArmy(unitEntities);
        }

        return entity;
    }

    /**
     * Convertit un objet Sector du domaine en DTO
     */
    public SectorDto toDto(Sector sector) {
        if (sector == null) return null;

        SectorDto dto = new SectorDto();
        dto.setNumber(sector.getNumber());
        dto.setName(sector.getName());
        dto.setIncome(sector.getIncome());

        // Nouvelles propriétés pour la carte
        dto.setOwnerId(sector.getOwnerId());
        dto.setColor(sector.getColor());
        dto.setResource(sector.getResource() != null ? sector.getResource().getType() : null);
        if (sector.getNeighbors() != null && !sector.getNeighbors().isEmpty()) {
            dto.setNeighbors(new java.util.ArrayList<>(sector.getNeighbors()));
        }

        // Conversion des stats
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

        // Conversion des unités
        if (sector.getArmy() != null) {
            dto.setArmy(sector.getArmy().stream()
                    .map(unitMapper::toDto)
                    .toList());
        }

        return dto;
    }

    /**
     * Convertit une entité SectorEntity en DTO
     */
    public SectorDto toDto(SectorEntity entity) {
        if (entity == null) return null;
        
        Sector sector = toDomain(entity);
        return toDto(sector);
    }
}

