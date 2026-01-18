package com.mg.nmlonline.domain.model.resource;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entité JPA représentant un type de ressource disponible dans le jeu
 * Correspond à la table RESOURCE en base de données
 * Exemples : Or (1700$), Ivoire (1100$), Joyaux (2000$), etc.
 */
@Entity
@Table(name = "RESOURCE_TYPE")
@Data
@NoArgsConstructor
public class ResourceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "base_value", nullable = false)
    private double baseValue;

    public ResourceType(String name, double baseValue) {
        this.name = name;
        this.baseValue = baseValue;
    }

    @Override
    public String toString() {
        return name + " (" + baseValue + "$)";
    }
}
