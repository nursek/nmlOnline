package com.mg.nmlonline.api.dto;

import lombok.Data;
import java.util.Map;

@Data
public class BoardDto {
    private Long id;
    private String name;
    private String mapImageUrl;
    private String svgOverlayUrl;
    private Map<Integer, SectorDto> sectors;
}

