package com.mg.nmlonline.domain.model.resource;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "RESOURCE")
@Getter
@Setter
@NoArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resource_seq")
    @SequenceGenerator(name = "resource_seq", sequenceName = "resource_id_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "base_value", nullable = false)
    private double baseValue;

    public Resource(String name, double baseValue) {
        this.name = name;
        this.baseValue = baseValue;
    }

    @Override
    public String toString() {
        return name + " (" + baseValue + "$)";
    }
}
