package com.mg.nmlonline.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SECTORS")
@Data
@NoArgsConstructor
@IdClass(SectorEntity.SectorId.class)
public class SectorEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity board;

    @Id
    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double income = 2000.0;

    // === NOUVELLES PROPRIÉTÉS POUR LA CARTE ===

    @Column(name = "owner_id", nullable = true)
    private Long ownerId; // null si secteur neutre

    @Column(nullable = false)
    private String color = "#ffffff"; // couleur par défaut blanc

    @Column(nullable = true)
    private String resource; // ressource du secteur (ex: "JOYAUX", "OR", "CIGARES")

    @ElementCollection
    @CollectionTable(name = "SECTOR_NEIGHBORS",
        joinColumns = {
            @JoinColumn(name = "board_id", referencedColumnName = "board_id"),
            @JoinColumn(name = "sector_number", referencedColumnName = "number")
        })
    @Column(name = "neighbor_number")
    private List<Integer> neighbors = new ArrayList<>();

    // Stats du secteur (embedded)
    @Embedded
    private SectorStatsEmbeddable stats = new SectorStatsEmbeddable();

    // Unités dans ce secteur
    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UnitEntity> army = new ArrayList<>();

    /**
     * Classe pour la clé primaire composite (board_id, number)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorId implements java.io.Serializable {
        private Long board;  // Correspond au board_id (ID de BoardEntity)
        private int number;  // Numéro du secteur

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SectorId)) return false;
            SectorId sectorId = (SectorId) o;
            return number == sectorId.number &&
                   java.util.Objects.equals(board, sectorId.board);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(board, number);
        }
    }
}
