package com.mg.nmlonline.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BuyVehicleBatchRequestDto {

    @NotEmpty(message = "Le panier de véhicules ne peut pas être vide")
    @Valid
    private List<BuyVehicleRequestDto> items;

    public List<BuyVehicleRequestDto> getItems() {
        return items;
    }

    public void setItems(List<BuyVehicleRequestDto> items) {
        this.items = items;
    }
}
