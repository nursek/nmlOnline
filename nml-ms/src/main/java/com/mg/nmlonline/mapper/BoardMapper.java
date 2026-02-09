package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.BoardDto;
import com.mg.nmlonline.api.dto.SectorDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper simplifié pour Board - conversion uniquement entre Domain et DTO
 */
@Component
public class BoardMapper {

    private final SectorMapper sectorMapper;

    public BoardMapper(SectorMapper sectorMapper) {
        this.sectorMapper = sectorMapper;
    }

    /**
     * Convertit un DTO BoardDto en objet Board du domaine
     */
    public Board toDomain(BoardDto dto) {
        if (dto == null) return null;

        Board board = new Board();
        board.setId(dto.getId());
        board.setName(dto.getName());
        board.setMapImageUrl(dto.getMapImageUrl());
        board.setSvgOverlayUrl(dto.getSvgOverlayUrl());

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
     * Convertit un objet Board du domaine en DTO BoardDto
     */
    public BoardDto toDto(Board board) {
        if (board == null) return null;

        BoardDto dto = new BoardDto();
        dto.setId(board.getId());
        dto.setName(board.getName());
        dto.setMapImageUrl(board.getMapImageUrl());
        dto.setSvgOverlayUrl(board.getSvgOverlayUrl());

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
}
