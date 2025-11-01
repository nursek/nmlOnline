// java
package com.mg.nmlonline.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    // IDs des secteurs contrôlés par le joueur (Board est la source unique de vérité)
    @ElementCollection
    @CollectionTable(name = "PLAYER_OWNED_SECTORS", joinColumns = @JoinColumn(name = "player_id"))
    @Column(name = "sector_number")
    private Set<Long> ownedSectorIds = new HashSet<>();
}
