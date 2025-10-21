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
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double income = 2000.0;

    // Stats du secteur (embedded)
    @Embedded
    private SectorStatsEmbeddable stats = new SectorStatsEmbeddable();

    // Unités dans ce secteur
    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UnitEntity> army = new ArrayList<>();
}

