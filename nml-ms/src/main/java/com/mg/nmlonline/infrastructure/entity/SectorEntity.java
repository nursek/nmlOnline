package com.mg.nmlonline.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SECTORS")
@Data
@NoArgsConstructor
public class SectorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = true)
    private PlayerEntity player;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double income = 2000.0;

    // === NOUVELLES PROPRIÉTÉS POUR LA CARTE ===

    @Column(name = "owner_player_id", nullable = true)
    private Integer ownerPlayerId; // null si secteur neutre

    @Column(nullable = false)
    private String color = "#ffffff"; // couleur par défaut blanc

    @Column(nullable = true)
    private String resource; // ressource du secteur (ex: "JOYAUX", "OR", "CIGARES")

    @ElementCollection
    @CollectionTable(name = "SECTOR_NEIGHBORS", joinColumns = @JoinColumn(name = "sector_id"))
    @Column(name = "neighbor_number")
    private List<Integer> neighbors = new ArrayList<>();

    // Stats du secteur (embedded)
    @Embedded
    private SectorStatsEmbeddable stats = new SectorStatsEmbeddable();

    // Unités dans ce secteur
    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UnitEntity> army = new ArrayList<>();
}

