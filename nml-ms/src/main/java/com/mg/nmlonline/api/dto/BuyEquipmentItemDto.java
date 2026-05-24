package com.mg.nmlonline.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Item du panier d'équipements à acheter.
 */
@Data
@NoArgsConstructor
public class BuyEquipmentItemDto {
    private String name;
    private int quantity;
}
