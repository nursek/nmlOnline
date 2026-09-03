package com.mg.nmlonline.domain.model.vehicle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.EntityCategory;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.Unit;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("VEHICLE")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Vehicle extends CombatEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private VehicleType vehicleType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pilot_id")
    @JsonIgnore
    private CombatEntity pilot;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    @JsonIgnore
    private List<CombatEntity> passengers = new ArrayList<>();

    public Vehicle(VehicleType vehicleType, Long playerId) {
        this.vehicleType = vehicleType;
        this.playerId = playerId;
        recalculateBaseStats();
    }

    @Override
    public EntityCategory getEntityCategory() {
        return EntityCategory.VEHICLE;
    }

    @Override
    public String getDisplayName() {
        return vehicleType != null ? vehicleType.getDisplayName() : "Véhicule";
    }

    @Override
    public void recalculateBaseStats() {
        if (vehicleType == null) return;

        if (isDestroyed()) {
            this.attack = 0;
            this.defense = 0;
            this.pdf = 0;
            return;
        }

        this.pdf = vehicleType.getBasePdf();
        this.defense = vehicleType.getBaseDefense();
        this.attack = 0;
        this.pdc = 0;
        this.armor = 0;
        this.evasion = 0;
    }

    @Override
    public double getBaseDefense() {
        return vehicleType != null ? vehicleType.getBaseDefense() : 0;
    }

    public boolean hasPilot() {
        return pilot != null && !pilot.isDestroyed();
    }

    public boolean assignPilot(CombatEntity entity) {
        if (entity == null) return false;

        if (entity instanceof Unit unit && !unit.getClassesSet().contains(UnitClass.PILOTE_DESTRUCTEUR)) {
                return false;
        }

        // Les personnages peuvent piloter sans restriction de classe.
        this.pilot = entity;
        return true;
    }

    public CombatEntity removePilot() {
        CombatEntity removed = this.pilot;
        this.pilot = null;
        return removed;
    }

    public boolean embark(CombatEntity entity) {
        if (entity == null || entity.isDestroyed()) return false;
        if (getPassengerCount() >= getCapacity()) return false;
        passengers.add(entity);
        return true;
    }

    public boolean disembark(CombatEntity entity) {
        return passengers.remove(entity);
    }

    public List<CombatEntity> disembarkAll() {
        List<CombatEntity> all = new ArrayList<>();
        if (pilot != null) {
            all.add(pilot);
            pilot = null;
        }
        all.addAll(passengers);
        passengers.clear();
        return all;
    }

    public int getPassengerCount() {
        return passengers.size();
    }

    public int getCapacity() {
        return vehicleType != null ? vehicleType.getCapacity() : 0;
    }

    public int getRemainingCapacity() {
        return getCapacity() - getPassengerCount();
    }

    public int getSpeed() {
        return vehicleType != null ? vehicleType.getSpeed() : 0;
    }

    public boolean cantMove() {
        return !hasPilot() || isDestroyed();
    }

    public boolean isAerial() {
        return vehicleType != null && vehicleType.isAerial();
    }

    public boolean firesInTransit() {
        return vehicleType != null && vehicleType.isFiresInTransit();
    }

    public double getResistancePercent() {
        return vehicleType != null ? vehicleType.getResistance() / 100.0 : 0;
    }

    public boolean participatesInGroundCombat() {
        return vehicleType != VehicleType.AVION_TRANSPORT;
    }

    public boolean isOperational() {
        return (hasPilot() && !isDestroyed());
    }

    public List<CombatEntity> getAllOccupants() {
        List<CombatEntity> all = new ArrayList<>();
        if (pilot != null) all.add(pilot);
        all.addAll(passengers);
        return all;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isDestroyed()) sb.append("[DÉTRUIT] ");
        sb.append(getDisplayName()).append(" (");
        buildStatsString(sb);
        sb.append(")");
        sb.append(" [Pilote: ").append(hasPilot() ? "✓" : "✗").append("]");
        sb.append(" [Passagers: ").append(getPassengerCount()).append("/").append(getCapacity()).append("]");
        if (vehicleType != null && vehicleType.getResistance() > 0) {
            sb.append(" [Rés: ").append(vehicleType.getResistance()).append("%]");
        }
        return sb.toString();
    }
}
