package com.mg.nmlonline.domain.model.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mg.nmlonline.domain.model.player.Player;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "PLAYER_RESOURCES")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "player")
public class PlayerResource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_resource_seq")
    @SequenceGenerator(name = "player_resource_seq", sequenceName = "player_resources_id_seq", allocationSize = 50)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    @JsonIgnore
    private Player player;

    @Column(name = "resource_name", nullable = false)
    private String resourceName;

    @Column(nullable = false)
    private int quantity = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", referencedColumnName = "id")
    private Resource resource;

    public PlayerResource(String resourceName) {
        this.resourceName = resourceName;
        this.quantity = 0;
    }

    public PlayerResource(String resourceName, int quantity) {
        this.resourceName = resourceName;
        this.quantity = quantity;
    }

    public void addQuantity(int amount) {
        if (amount > 0) {
            if (this.quantity > Integer.MAX_VALUE - amount) {
                throw new IllegalArgumentException("Quantity overflow: cannot add " + amount + " to " + this.quantity);
            }
            this.quantity += amount;
        }
    }

    public boolean removeQuantity(int amount) {
        if (amount > 0 && this.quantity >= amount) {
            this.quantity -= amount;
            return true;
        }
        return false;
    }

    public boolean hasQuantity(int amount) {
        return this.quantity >= amount;
    }
}
