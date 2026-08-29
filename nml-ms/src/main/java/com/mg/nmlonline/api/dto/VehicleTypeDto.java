package com.mg.nmlonline.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTypeDto {
    private String name;
    private String displayName;
    private int cost;
    private double basePdf;
    private double baseDefense;
    private int speed;
    private int capacity;
    private int resistance;
    private boolean firesInTransit;
    private boolean aerial;
}
