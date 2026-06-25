package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SellResourceBatchItemDto {

    @NotNull(message = "L'identifiant de la ressource est requis")
    private Long playerResourceId;

    @Min(value = 1, message = "La quantité à vendre doit être d'au moins 1")
    private int quantity;

    public Long getPlayerResourceId() {
        return playerResourceId;
    }

    public void setPlayerResourceId(Long playerResourceId) {
        this.playerResourceId = playerResourceId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
