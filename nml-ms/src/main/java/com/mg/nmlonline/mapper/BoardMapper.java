package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.BoardDto;
import com.mg.nmlonline.api.dto.SectorDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.entity.BoardEntity;
import com.mg.nmlonline.infrastructure.entity.SectorEntity;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class BoardMapper {

    private final SectorMapper sectorMapper;

    public BoardMapper(SectorMapper sectorMapper) {
        this.sectorMapper = sectorMapper;
    }

    /**
     * Convertit une entité BoardEntity en objet Board du domaine
     */
    public Board toDomain(BoardEntity entity) {
        if (entity == null) return null;

        Board board = new Board();

        // Conversion de tous les secteurs
        if (entity.getSectors() != null) {
            for (SectorEntity sectorEntity : entity.getSectors()) {
                Sector sector = sectorMapper.toDomain(sectorEntity);
                if (sector != null) {
                    board.addSector(sector);
                }
            }
        }

        return board;
    }

    /**
     * Convertit un DTO BoardDto en objet Board du domaine
     */
    public Board toDomain(BoardDto dto) {
        if (dto == null) return null;

        Board board = new Board();

        // Conversion de tous les secteurs
        if (dto.getSectors() != null) {
            for (Map.Entry<Integer, SectorDto> entry : dto.getSectors().entrySet()) {
                Sector sector = sectorMapper.toDomain(entry.getValue());
                if (sector != null) {
                    board.addSector(sector);
                }
            }
        }

        return board;
    }

    /**
     * Convertit un objet Board du domaine en entité BoardEntity
     */
    public BoardEntity toEntity(Board board, String boardName) {
        if (board == null) return null;

        BoardEntity entity = new BoardEntity();
        entity.setName(boardName);

        // Conversion des secteurs
        if (board.getAllSectors() != null) {
            List<SectorEntity> sectorEntities = new ArrayList<>();
            for (Sector sector : board.getAllSectors()) {
                SectorEntity sectorEntity = sectorMapper.toEntity(sector, entity);
                if (sectorEntity != null) {
                    sectorEntities.add(sectorEntity);
                }
            }
            entity.setSectors(sectorEntities);
        }

        return entity;
    }

    /**
     * Convertit un objet Board du domaine en DTO BoardDto
     */
    public BoardDto toDto(Board board) {
        if (board == null) return null;

        BoardDto dto = new BoardDto();

        // Conversion des secteurs en Map
        if (board.getAllSectors() != null) {
            Map<Integer, SectorDto> sectorsMap = board.getAllSectors().stream()
                    .collect(Collectors.toMap(
                            Sector::getNumber,
                            sectorMapper::toDto,
                            (existing, replacement) -> existing,
                            LinkedHashMap::new
                    ));
            dto.setSectors(sectorsMap);
        }

        return dto;
    }

    /**
     * Convertit une entité BoardEntity en DTO BoardDto
     */
    public BoardDto toDto(BoardEntity entity) {
        if (entity == null) return null;

        BoardDto dto = new BoardDto();
        dto.setId(entity.getId());

        // Conversion des secteurs en Map
        if (entity.getSectors() != null) {
            Map<Integer, SectorDto> sectorsMap = entity.getSectors().stream()
                    .collect(Collectors.toMap(
                            SectorEntity::getNumber,
                            sectorMapper::toDto,
                            (existing, replacement) -> existing,
                            LinkedHashMap::new
                    ));
            dto.setSectors(sectorsMap);
        }

        return dto;
    }
}

