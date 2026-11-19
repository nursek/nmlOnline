package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class AutoEquipRequestDto {
    private List<String> categories;
    private List<String> equipmentNames;
}
