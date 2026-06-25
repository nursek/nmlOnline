package com.mg.nmlonline.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class SellResourceBatchRequestDto {

    @NotEmpty(message = "Le panier de vente ne peut pas être vide")
    @Valid
    private List<SellResourceBatchItemDto> items;

    public List<SellResourceBatchItemDto> getItems() {
        return items;
    }

    public void setItems(List<SellResourceBatchItemDto> items) {
        this.items = items;
    }
}
