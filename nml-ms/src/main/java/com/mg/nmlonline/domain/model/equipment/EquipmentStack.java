package com.mg.nmlonline.domain.model.equipment;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
@Data
@JsonDeserialize(using = EquipmentStack.EquipmentStackDeserializer.class)
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

    public static class EquipmentStackDeserializer extends JsonDeserializer<EquipmentStack> {
        @Override
        public EquipmentStack deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            String equipmentName = node.get("equipment").get("name").asText();

            Equipment equipment = EquipmentFactory.createFromName(equipmentName);
            if (equipment == null) {
                throw new IOException("Équipement inconnu: " + equipmentName);
            }

            EquipmentStack stack = new EquipmentStack(equipment);
            stack.setQuantity(node.get("quantity").asInt(1));
            stack.setAvailable(node.get("available").asInt(1));

            return stack;
        }
    }

}