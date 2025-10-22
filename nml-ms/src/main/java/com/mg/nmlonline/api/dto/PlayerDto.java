package com.mg.nmlonline.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class PlayerDto {
    private Long id;
    private String name;
    private PlayerStatsDto stats;
    private List<EquipmentStackDto> equipments;
    private List<SectorDto> sectors;
}
