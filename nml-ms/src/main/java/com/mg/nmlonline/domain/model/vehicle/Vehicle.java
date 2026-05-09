package com.mg.nmlonline.domain.model.vehicle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.EntityCategory;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.Unit;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Entité véhicule — conteneur combattant qui embarque des unités.
 *
 * Un véhicule nécessite un pilote (classe PILOTE_DESTRUCTEUR).
 * Selon le type, il peut transporter des passagers et/ou avoir un tireur de tourelle.
 * En transit, seul le véhicule combat (pas les passagers).
 */
@Entity
@DiscriminatorValue("VEHICLE")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Vehicle extends CombatEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private VehicleType vehicleType;

    // === PILOTE (obligatoire) ===
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pilot_id")
    @JsonIgnore
    private CombatEntity pilot;

    // === PASSAGERS EMBARQUÉS ===
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    @JsonIgnore
    private List<CombatEntity> passengers = new ArrayList<>();

    // === CONSTRUCTEUR ===

    public Vehicle(VehicleType vehicleType, Long playerId) {
        this.vehicleType = vehicleType;
        this.playerId = playerId;
        recalculateBaseStats();
    }

    // === IMPLÉMENTATION ABSTRAITE ===

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

    // === GESTION DU PILOTE ===

    /**
     * Vérifie si le véhicule a un pilote valide (classe PILOTE_DESTRUCTEUR).
     */
    public boolean hasPilot() {
        return pilot != null && !pilot.isDestroyed();
    }

    /**
     * Assigne un pilote au véhicule.
     * Le pilote doit avoir la classe PILOTE_DESTRUCTEUR.
     */
    public boolean assignPilot(CombatEntity entity) {
        if (entity == null) return false;

        if (entity instanceof Unit unit && !unit.getClassesSet().contains(UnitClass.PILOTE_DESTRUCTEUR)) {
                return false;
        }

        // Les personnages peuvent piloter sans restriction de classe.

        this.pilot = entity;
        return true;
    }

    /**
     * Retire le pilote du véhicule.
     */
    public CombatEntity removePilot() {
        CombatEntity removed = this.pilot;
        this.pilot = null;
        return removed;
    }

    // === GESTION DES PASSAGERS ===

    /**
     * Embarque une entité dans le véhicule.
     */
    public boolean embark(CombatEntity entity) {
        if (entity == null || entity.isDestroyed()) return false;
        if (getPassengerCount() >= getCapacity()) return false;
        passengers.add(entity);
        return true;
    }

    /**
     * Débarque une entité du véhicule.
     */
    public boolean disembark(CombatEntity entity) {
        return passengers.remove(entity);
    }

    /**
     * Débarque tous les passagers.
     */
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

    // === MOBILITÉ ===

    /**
     * Retourne la vitesse de déplacement (nombre de secteurs par tour).
     */
    public int getSpeed() {
        return vehicleType != null ? vehicleType.getSpeed() : 0;
    }

    /**
     * Vérifie si le véhicule peut se déplacer (nécessite un pilote vivant).
     */
    public boolean cantMove() {
        return !hasPilot() || isDestroyed();
    }

    // === CARACTÉRISTIQUES SPÉCIALES ===

    /**
     * Véhicule aérien (hélicoptère, avion).
     */
    public boolean isAerial() {
        return vehicleType != null && vehicleType.isAerial();
    }

    /**
     * Le véhicule tire sur les ennemis lors du transit (ex : VTT blindé).
     */
    public boolean firesInTransit() {
        return vehicleType != null && vehicleType.isFiresInTransit();
    }

    /**
     * Résistance aux dégâts en pourcentage (Tank = 50%).
     */
    public double getResistancePercent() {
        return vehicleType != null ? vehicleType.getResistance() / 100.0 : 0;
    }

    /**
     * L'avion de transport n'intervient pas en combat au sol.
     */
    public boolean participatesInGroundCombat() {
        return vehicleType != VehicleType.AVION_TRANSPORT;
    }

    /**
     * Vérifie si le véhicule est opérationnel (pilote + tourelle si nécessaire).
     */
    public boolean isOperational() {
        return (hasPilot() && !isDestroyed());
    }

    /**
     * Retourne toutes les entités à bord (pilote + tourelle + passagers).
     */
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
