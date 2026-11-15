package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BoardDto;
import com.mg.nmlonline.api.dto.BoardPresetDto;
import com.mg.nmlonline.api.dto.BoardPresetSummaryDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.mapper.BoardMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class BoardPresetService {

    private static final String PRESETS_PATH = "boards/presets.json";

    private final BoardImportService boardImportService;
    private final BoardMapper boardMapper;
    private final List<PresetDef> presets;

    public BoardPresetService(BoardImportService boardImportService, BoardMapper boardMapper,
                              JsonMapper jsonMapper) {
        this.boardImportService = boardImportService;
        this.boardMapper = boardMapper;
        this.presets = load(jsonMapper);
    }

    public List<BoardPresetSummaryDto> list() {
        return presets.stream()
                .map(preset -> new BoardPresetSummaryDto(
                        preset.key, preset.name, boardOf(preset).getSectorCount(), preset.spots.size()))
                .toList();
    }

    public BoardPresetDto get(String key) {
        PresetDef preset = presets.stream()
                .filter(candidate -> candidate.key.equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Preset inconnu : " + key));
        Board board = boardOf(preset);
        BoardDto dto = boardMapper.toDto(board);
        dto.setPresetKey(preset.key);
        return new BoardPresetDto(preset.key, preset.name, board.getSectorCount(),
                preset.spots, dto);
    }

    private Board boardOf(PresetDef preset) {
        try {
            String json = new String(new ClassPathResource(preset.board).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            return boardImportService.importBoardFromJson(json);
        } catch (IOException e) {
            throw new IllegalStateException("Preset illisible : " + preset.board, e);
        }
    }

    private List<PresetDef> load(JsonMapper jsonMapper) {
        try {
            String json = new String(new ClassPathResource(PRESETS_PATH).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            PresetDef[] loaded = jsonMapper.readValue(json, PresetDef[].class);
            return List.of(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("Catalogue de presets illisible", e);
        }
    }

    public static class PresetDef {
        public String key;
        public String name;
        public String board;
        public List<List<Integer>> spots;
    }
}
