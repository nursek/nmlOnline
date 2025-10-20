package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class EquipmentStackDto {
    private EquipmentDto equipment;
    private int quantity;
    private int available;
}