package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class BattleHighlightDto {
    private String kind;
    private int priority;
    private String visibility;
    private String message;
}
