package com.mg.nmlonline.domain.model.resource;

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
 * Représente une ressource dans l'inventaire d'un joueur - Entité JPA
 * Stocke uniquement le nom de la ressource et la quantité possédée
 * Le prix de base est récupéré depuis la table RESOURCE_TYPE
 * Exemple: 3 unités d'Or, 15 unités d'Ivoire
 */
@Entity
@Table(name = "PLAYER_RESOURCES")
@Data
@NoArgsConstructor
@JsonDeserialize(using = PlayerResource.PlayerResourceDeserializer.class)
public class PlayerResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    @JsonIgnore  // Éviter les boucles infinies lors de la sérialisation JSON
    private Player player;

    @Column(name = "resource_name", nullable = false)
    private String resourceName; // "Or", "Ivoire", "Joyaux", etc.

    @Column(nullable = false)
    private int quantity = 0; // quantité possédée

    // Relation optionnelle avec ResourceType pour validation et récupération du prix
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_name", referencedColumnName = "name",
                insertable = false, updatable = false)
    private ResourceType resourceType;

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

    public static class PlayerResourceDeserializer extends JsonDeserializer<PlayerResource> {
        @Override
        public PlayerResource deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            String type = node.get("type").asText();
            int quantity = node.has("quantity") ? node.get("quantity").asInt() : 0;

            return new PlayerResource(type, quantity);
        }
    }
}
