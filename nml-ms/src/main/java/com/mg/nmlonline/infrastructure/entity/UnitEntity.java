package com.mg.nmlonline.infrastructure.entity;

import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "UNITS")
@Data
@NoArgsConstructor
public class UnitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private SectorEntity sector;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int number = 0;

    @Column(nullable = false)
    private double experience = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitType type;

    @ElementCollection(targetClass = UnitClass.class)
    @CollectionTable(name = "UNIT_CLASSES", joinColumns = @JoinColumn(name = "unit_id"))
    @Column(name = "unit_class")
    @Enumerated(EnumType.STRING)
    private Set<UnitClass> classes = new HashSet<>();

    @Column(name = "is_injured", nullable = false)
    private boolean isInjured = false;

    // Statistiques de base
    @Column(nullable = false)
    private double attack;

    @Column(nullable = false)
    private double defense;

    // Statistiques calculées
    @Column(nullable = false)
    private double pdf;

    @Column(nullable = false)
    private double pdc;

    @Column(nullable = false)
    private double armor;

    @Column(nullable = false)
    private double evasion;

    // Équipements de l'unité (relation many-to-many avec Equipment)
    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UnitEquipmentEntity> equipments = new ArrayList<>();
}

