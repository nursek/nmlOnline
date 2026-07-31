package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Corps de requête pour retirer un équipement d'une unité et le rendre à l'inventaire du joueur.
 */
@Data
public class RemoveEquipmentRequestDto {
    @NotBlank
    private String equipmentName;
}