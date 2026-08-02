package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Corps de requête pour équiper une unité depuis l'inventaire du joueur.
 */
@Data
public class AssignEquipmentRequestDto {
    @NotBlank
    private String equipmentName;
}