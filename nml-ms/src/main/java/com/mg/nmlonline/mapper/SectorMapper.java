package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.SectorDto;
import com.mg.nmlonline.api.dto.SectorStatsDto;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.sector.SectorStats;
import com.mg.nmlonline.domain.model.unit.Unit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper simplifié pour Sector - conversion uniquement entre Domain et DTO
 */
@Component
public class SectorMapper {

    private final UnitMapper unitMapper;

    public SectorMapper(UnitMapper unitMapper) {
        this.unitMapper = unitMapper;
    }

    /**
     * Convertit un DTO SectorDto en objet Sector du domaine
     */
    public Sector toDomain(SectorDto dto) {
        if (dto == null) return null;

        Sector sector = new Sector(dto.getNumber(), dto.getName());
        sector.setIncome(dto.getIncome() != null ? dto.getIncome() : 2000.0);

        // Propriétés pour la carte
        sector.setOwnerId(dto.getOwnerId());
        sector.setColor(dto.getColor() != null ? dto.getColor() : "#ffffff");
        if (dto.getResource() != null) {
            sector.setResourceName(dto.getResource());
        }

        // Coordonnées
        sector.setX(dto.getX());
        sector.setY(dto.getY());

        // Conversion des voisins
        if (dto.getNeighbors() != null) {
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
            sector.setArmy(new ArrayList<>(units));
        }

        return sector;
    }

    /**
     * Convertit un objet Sector du domaine en DTO SectorDto
     */
    public SectorDto toDto(Sector sector) {
        if (sector == null) return null;

        SectorDto dto = new SectorDto();
        dto.setNumber(sector.getNumber());
        dto.setName(sector.getName());
        dto.setIncome(sector.getIncome());

        // Propriétés pour la carte
        dto.setOwnerId(sector.getOwnerId());
        dto.setColor(sector.getColor());
        if (sector.getResourceName() != null && !sector.getResourceName().isEmpty()) {
            dto.setResource(sector.getResourceName());
        }
        dto.setNeighbors(new ArrayList<>(sector.getNeighbors()));

        // Coordonnées
        dto.setX(sector.getX());
        dto.setY(sector.getY());

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
}
