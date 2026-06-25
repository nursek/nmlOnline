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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    @JsonIgnore  // Éviter les boucles infinies lors de la sérialisation JSON
    private Player player;

    @Column(name = "resource_name", nullable = false)
    private String resourceName; // "Or", "Ivoire", "Joyaux", etc.

    @Column(nullable = false)
    private int quantity = 0; // quantité possédée

    // Relation optionnelle avec Resource pour validation et récupération du prix
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

    /**
     * Ajoute une quantité de ressources
     */
    public void addQuantity(int amount) {
        if (amount > 0) {
            if (this.quantity > Integer.MAX_VALUE - amount) {
                throw new IllegalArgumentException("Quantity overflow: cannot add " + amount + " to " + this.quantity);
            }
            this.quantity += amount;
        }
    }

    /**
     * Retire une quantité de ressources
     */
    public boolean removeQuantity(int amount) {
        if (amount > 0 && this.quantity >= amount) {
            this.quantity -= amount;
            return true;
        }
        return false;
    }

    /**
     * Vérifie si le joueur possède au moins une certaine quantité
     */
    public boolean hasQuantity(int amount) {
        return this.quantity >= amount;
    }
}
