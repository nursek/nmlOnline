package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class UnitCatalogDto {
    private List<UnitClassDto> classes;
    private List<UnitCatalogEntryDto> entries;
}
