package com.mg.nmlonline.domain.model.equipment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mg.nmlonline.domain.model.player.Player;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "EQUIPMENT_STACKS")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "player")
public class EquipmentStack {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "equipment_stack_seq")
    @SequenceGenerator(name = "equipment_stack_seq", sequenceName = "equipment_stacks_id_seq", allocationSize = 50)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    @JsonIgnore
    private Player player;

    // Pas de cascade : Equipment est une référence partagée du catalogue (instance managed du repository).
    @ManyToOne(fetch = FetchType.EAGER)
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

}
