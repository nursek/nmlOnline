package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class PlayerResourceDto {
    private Long id;
    private String name;
    private int quantity;
    private Double baseValue;
}
