package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminCatalogDto {
    private List<EquipmentDto> equipments;
    private List<ResourceCatalogDto> resources;
    private List<UnitTypeDto> unitTypes;
    private List<UnitClassDto> unitClasses;
    private List<VehicleTypeDto> vehicleTypes;
}
