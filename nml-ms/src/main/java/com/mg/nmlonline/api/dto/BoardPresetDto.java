package com.mg.nmlonline.api.dto;

import java.util.List;

public record BoardPresetDto(String key, String name, int sectorCount,
                             List<List<Integer>> spots, BoardDto board) {
}
