package com.mg.nmlonline.domain.model.sector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Entity
@Table(name = "SECTORS")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"board", "army"})
@IdClass(Sector.SectorId.class)
public class Sector {

    private static final Logger logger = LoggerFactory.getLogger(Sector.class);

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    @JsonIgnore
    private Board board;

    @Id
    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private int number;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double income = 2000.0;

    @Column(name = "owner_id")
    private Long ownerId; // Source unique d'appartenance du secteur ; null = neutre

    @Column(nullable = false)
    private String color = "#ffffff";

    @Column(name = "resource_name", nullable = true)
    private String resourceName;

    @Column(nullable = true)
    private Integer x;

    @Column(nullable = true)
    private Integer y;

    @ElementCollection
    @CollectionTable(name = "SECTOR_NEIGHBORS",
        joinColumns = {
            @JoinColumn(name = "board_id", referencedColumnName = "board_id"),
            @JoinColumn(name = "sector_number", referencedColumnName = "number")
        })
    @Column(name = "neighbor_number")
    private List<Integer> neighbors = new ArrayList<>();

    @Embedded
    private SectorStats stats = new SectorStats();

    // orphanRemoval volontairement absent : la suppression passe par em.remove explicite
    // (CombatService.simulateSectorBattle). @OnDelete(CASCADE) + Flyway V6 = ceinture DB.
    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private List<Unit> army = new ArrayList<>();

    // Relations lecture seule : le propriétaire gère ces entités (Player/Building/Character/Vehicle).
    @OneToMany(mappedBy = "sector")
    private List<Building> buildings = new ArrayList<>();

    @OneToMany(mappedBy = "sector")
    private List<GameCharacter> characters = new ArrayList<>();

    @OneToMany(mappedBy = "sector")
    private List<Vehicle> vehicles = new ArrayList<>();

    public Sector(int number) {
        this.number = number;
        this.name = "Secteur n°" + number;
        this.color = "#ffffff";
        this.resourceName = null;
    }

    public Sector(int number, String name) {
        this.number = number;
        this.name = name;
        this.color = "#ffffff";
        this.resourceName = null;
    }

    public void addNeighbor(int neighborNumber) {
        if (!neighbors.contains(neighborNumber) && neighborNumber != this.number) {
            neighbors.add(neighborNumber);
        }
    }

    public void removeNeighbor(int neighborNumber) {
        neighbors.remove((Integer) neighborNumber);
    }

    public boolean isNeighbor(int sectorNumber) {
        return neighbors.contains(sectorNumber);
    }

    public List<Integer> getNeighbors() {
        return Collections.unmodifiableList(neighbors);
    }

    public void setOwnerAndColor(Long playerId, String colorHex) {
        this.ownerId = playerId;
        this.color = colorHex != null ? colorHex : "#ffffff";
    }

    public boolean isOwnedBy(Long playerId) {
        return ownerId != null && ownerId.equals(playerId);
    }

    public boolean isNeutral() {
        return ownerId == null;
    }

    public void recalculateMilitaryPower(){
        List<CombatEntity> allEntities = getCombatEntities();

        stats.setTotalAtk(allEntities.stream()
                .mapToDouble(CombatEntity::getAttack)
                .sum());
        stats.setTotalPdf(allEntities.stream()
                .mapToDouble(CombatEntity::getPdf)
                .sum());
        stats.setTotalPdc(allEntities.stream()
                .mapToDouble(CombatEntity::getPdc)
                .sum());
        stats.setTotalDef(allEntities.stream()
                .mapToDouble(CombatEntity::getDefense)
                .sum());
        stats.setTotalArmor(allEntities.stream()
                .mapToDouble(CombatEntity::getArmor)
                .sum());

        stats.setTotalOffensive(stats.getTotalAtk() + stats.getTotalPdf() + stats.getTotalPdc());
        stats.setTotalDefensive(stats.getTotalDef() + stats.getTotalArmor());
        stats.setGlobalStats((stats.getTotalOffensive() + stats.getTotalDefensive()) / 2);
    }

    public void addUnit(Unit unit) {
        if (unit != null) {
            unit.setSector(this); // côté inverse de la relation bidirectionnelle
            army.add(unit);
            sortArmy();
            reassignUnitIds();
            recalculateMilitaryPower();
        }
    }

    public void addUnits(List<Unit> units) {
        if (units != null && !units.isEmpty()) {
            for (Unit unit : units) {
                unit.setSector(this); // côté inverse de la relation bidirectionnelle
            }
            army.addAll(units);
            sortArmy();
            reassignUnitIds();
            recalculateMilitaryPower();
        }
    }

    public boolean removeUnit(Unit unit) {
        boolean removed = army.remove(unit);
        if (removed) {
            sortArmy();
            reassignUnitIds();
            recalculateMilitaryPower();
        }
        return removed;
    }

    public boolean removeUnit(int unitId) {
        boolean removed = army.removeIf(unit -> unit.getId() == unitId);
        if (removed) {
            sortArmy();
            reassignUnitIds();
            recalculateMilitaryPower();
        }
        return removed;
    }

    public int getArmySize() {
        return army.size();
    }

    public Unit getUnitById(int id) {
        return army.stream()
                .filter(unit -> unit.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public List<Unit> getUnitsByType(String unitType) {
        return army.stream()
                .filter(unit -> unit.getType().name().equalsIgnoreCase(unitType))
                .toList();
    }

    public List<Unit> getUnits() {
        return army;
    }

    public List<CombatEntity> getCombatEntities() {
        List<CombatEntity> all = new ArrayList<>(army);
        if (buildings != null) {
            all.addAll(buildings);
        }
        if (characters != null) {
            all.addAll(characters);
        }
        if (vehicles != null) {
            all.addAll(vehicles);
        }
        return all;
    }

    public void sortArmy() {
        army.sort(Comparator
                .comparingDouble(Unit::getExperience).reversed()
                .thenComparing(Unit::getTotalDefense, Comparator.reverseOrder())
                .thenComparing(Unit::getTotalAttack, Comparator.reverseOrder())
                .thenComparing(Unit::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(System::identityHashCode)
        );
    }

    public void reassignUnitIds() {
        Map<String, Integer> typeCounters = new HashMap<>();
        for (Unit unit : army) {
            String unitType = unit.getType().name();
            int currentCount = typeCounters.getOrDefault(unitType, 0) + 1;
            typeCounters.put(unitType, currentCount);
            unit.setNumber(currentCount);
        }
    }

    @Override
    public String toString() {
        return String.format("%s - %d unités, Revenus: %.0f$", name, getArmySize(), income);
    }

    /** Clé primaire composite (board_id, number) pour @IdClass. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorId implements java.io.Serializable {
        private Long board;  // = board_id (les noms doivent matcher les @Id de Sector)
        private int number;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SectorId sectorId)) return false;
            return number == sectorId.number &&
                   java.util.Objects.equals(board, sectorId.board);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(board, number);
        }
    }
}

