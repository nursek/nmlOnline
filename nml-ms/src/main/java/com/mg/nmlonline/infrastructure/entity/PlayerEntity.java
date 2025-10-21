// java
package com.mg.nmlonline.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PLAYERS")
@Data
@NoArgsConstructor
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // Stats du joueur (embedded)
    @Embedded
    private PlayerStatsEmbeddable stats = new PlayerStatsEmbeddable();

    // Bonuses du joueur (embedded)
    @Embedded
    private PlayerBonusesEmbeddable bonuses = new PlayerBonusesEmbeddable();

    // Inventaire d'équipements du joueur
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EquipmentStackEntity> equipments = new ArrayList<>();

    // Secteurs contrôlés par le joueur
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SectorEntity> sectors = new ArrayList<>();
}
