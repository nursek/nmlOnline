package com.mg.nmlonline.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;

@Data
public class PlayerDto {
    private Long id;
    private String name;
    private String race;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long userId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String accountUsername;
    private Integer sectorCount;
    private PlayerStatsDto stats;
    private List<EquipmentStackDto> equipments;
    private List<PlayerResourceDto> resources;
    private List<SectorDto> sectors;

    private GameCharacterDto character;

    private List<BuildingDto> buildings;

    private boolean eliminated;
    private String eliminationCause;
    private Integer eliminatedTurn;
    private Long eliminatedByPlayerId;
}
