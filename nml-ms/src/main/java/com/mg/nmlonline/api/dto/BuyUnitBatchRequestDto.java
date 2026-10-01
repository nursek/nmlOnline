package com.mg.nmlonline.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BuyUnitBatchRequestDto {

    @NotEmpty(message = "Le panier d'unités ne peut pas être vide")
    @Valid
    private List<BuyUnitRequestDto> items;

    public List<BuyUnitRequestDto> getItems() {
        return items;
    }

    public void setItems(List<BuyUnitRequestDto> items) {
        this.items = items;
    }
}
