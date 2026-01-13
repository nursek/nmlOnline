package com.mg.nmlonline.domain.model.equipment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mg.nmlonline.domain.model.player.Player;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

/**
 * Représente un stack d'équipements dans l'inventaire d'un joueur - Entité JPA
 */
@Entity
@Table(name = "EQUIPMENT_STACKS")
@Data
@NoArgsConstructor
@JsonDeserialize(using = EquipmentStack.EquipmentStackDeserializer.class)
public class EquipmentStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    @JsonIgnore  // Éviter les boucles infinies lors de la sérialisation JSON
    private Player player;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(nullable = false)
    private int available = 1;

    public EquipmentStack(Equipment equipment) {
        this.equipment = equipment;
        this.quantity = 1;
        this.available = 1;
    }

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
        if (available > 0)
            available--;
    }

    public void incrementAvailable() {
        if (available < quantity)
            available++;
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