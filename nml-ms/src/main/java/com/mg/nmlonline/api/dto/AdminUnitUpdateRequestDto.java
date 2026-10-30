package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminUnitUpdateRequestDto {
    private Double experience;
    private List<String> classes;
    private Boolean injured;
}
