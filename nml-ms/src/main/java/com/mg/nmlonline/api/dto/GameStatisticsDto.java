package com.mg.nmlonline.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class GameStatisticsDto {

    private List<Entry> units;
    private long characters;
    private List<Entry> vehicles;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private String key;
        private String label;
        private long count;
    }
}
