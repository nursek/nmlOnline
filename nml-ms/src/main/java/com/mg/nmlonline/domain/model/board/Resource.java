package com.mg.nmlonline.domain.model.board;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Représente une ressource attachée à un secteur.
 * Exemples : "joyaux", "or", "cigares", etc.
 */
@Setter
@Getter
public class Resource {
    private String type;  // "joyaux", "or", "cigares", "uranium", etc.
    private double baseValue; // valeur de base en $ (ex: 400.0, 2000.0)

    public Resource() {
    }

    public Resource(String type, double baseValue) {
        this.type = type;
        this.baseValue = baseValue;
    }

    @Override
    public String toString() {
        return type + " (" + baseValue + "$)";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource)) return false;
        Resource resource = (Resource) o;
        return Double.compare(resource.baseValue, baseValue) == 0 &&
                Objects.equals(type, resource.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, baseValue);
    }
}

