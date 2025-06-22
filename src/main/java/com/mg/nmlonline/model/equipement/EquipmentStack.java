package com.mg.nmlonline.model.equipement;

import com.mg.nmlonline.model.equipement.Equipment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class EquipmentStack {
    private final Equipment equipment;
    private int quantity = 1;

    public void increment() {
        quantity++;
    }

    public void decrement() {
        quantity--;
    }
}