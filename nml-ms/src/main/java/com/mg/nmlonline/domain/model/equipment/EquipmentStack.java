package com.mg.nmlonline.domain.model.equipment;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class EquipmentStack {
    private final Equipment equipment;
    private int quantity = 1;
    private int available = 1;

    public void increment() {
        quantity++;
        available++;
    }

    public void decrement() {
        if (quantity > 0) {
            quantity--;
        }
        if (available > 0) {
            available--;
        }
    }

    public boolean isAvailable() {
        return available > 0;
    }

    public void decrementAvailable() {
        if(available > 0)
            available--;
    }

}