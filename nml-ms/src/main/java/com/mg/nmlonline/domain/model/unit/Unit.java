package com.mg.nmlonline.domain.model.unit;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mg.nmlonline.domain.model.unit.UnitType.*;

/**
 * Represents a unit with various attributes for combat - Entité JPA
 * Hérite de CombatEntity pour partager les propriétés communes avec les bâtiments et personnages.
 */
@Entity
@DiscriminatorValue("UNIT")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Unit extends CombatEntity {

    private static final Logger logger = LoggerFactory.getLogger(Unit.class);

    // ===== CONSTANTES =====
    private static final int MIN_EXPERIENCE_FOR_SECOND_CLASS = 5;


    // ===== INFORMATIONS DE BASE =====
    // Note : nullable = true requis par l'héritage SINGLE_TABLE (colonnes partagées avec GameCharacter)
    @Column(nullable = true)
    private int number = 0; // Numéro de l'unité dans l'armée

    @Column(nullable = true)
    private double experience = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private UnitType type;

    @ElementCollection(targetClass = UnitClass.class)
    @CollectionTable(name = "UNIT_CLASSES", joinColumns = @JoinColumn(name = "unit_id"))
    @Column(name = "unit_class")
    @Enumerated(EnumType.STRING)
    private Set<UnitClass> classesSet = new HashSet<>();

    // ===== ÉTAT DE L'UNITÉ =====
    @Column(name = "is_injured", nullable = true)
    private boolean isInjured = false;


    // ===== ÉQUIPEMENTS =====
    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UnitEquipment> unitEquipments = new ArrayList<>();

    // Champ transient pour compatibilité avec l'ancien code
    @Transient
    private List<Equipment> equipments = new ArrayList<>();

    public Unit(double experience, UnitClass primaryClass) {
        this.experience = experience;
        this.type = UnitType.getTypeByExperience((int) experience);
        this.classesSet = new HashSet<>();
        this.classesSet.add(primaryClass);

        this.attack = type.getBaseAttack();
        this.defense = type.getBaseDefense();

        this.equipments = new ArrayList<>();
        this.unitEquipments = new ArrayList<>();

        recalculateBaseStats();
    }

    // Méthode de compatibilité pour l'ancien code
    public List<UnitClass> getClasses() {
        return new ArrayList<>(classesSet);
    }

    public void setClasses(List<UnitClass> classes) {
        this.classesSet = new HashSet<>(classes);
    }

    // ===== IMPLÉMENTATION DES MÉTHODES ABSTRAITES =====

    @Override
    public EntityCategory getEntityCategory() {
        return EntityCategory.INFANTRY;
    }

    @Override
    public String getDisplayName() {
        if (type == PERSONNAGE) {
            return type.name();
        }
        return type.name() + " n°" + number;
    }

    // Recalcule les statistiques de base (sans bonus joueur)
    @Override
    public void recalculateBaseStats() {
        if (type == null) return;

        double statMultiplier = isInjured ? INJURED_STAT_MULTIPLIER : 1.0;

        this.attack = type.getBaseAttack() * statMultiplier;
        this.defense = type.getBaseDefense() * statMultiplier;
        this.pdf = calculateEquipmentPdf();
        this.pdc = calculateEquipmentPdc();
        this.armor = calculateEquipmentArmor();
        this.evasion = calculateEquipmentEvasion();
    }

    /**
     * Applique les bonus du joueur sur les statistiques de l'unité.
     */
    public void applyPlayerBonuses(double attackBonus, double defenseBonus, double pdfBonus,
                                   double pdcBonus, double armorBonus, double evasionBonus) {
        this.attack *= (1.0 + attackBonus);
        this.defense *= (1.0 + defenseBonus);
        this.pdf *= (1.0 + pdfBonus);
        this.pdc *= (1.0 + pdcBonus);
        this.armor *= (1.0 + armorBonus);
        this.evasion -= (evasionBonus * 10);
    }

    /**
     * Retourne les équipements pour les calculs de stats.
     * Priorité : unitEquipments (persistant) > equipments (transient)
     */
    private List<Equipment> getEquipmentsForCalculation() {
        if (unitEquipments != null && !unitEquipments.isEmpty()) {
            return unitEquipments.stream()
                    .map(UnitEquipment::getEquipment)
                    .toList();
        }
        return equipments != null ? equipments : new ArrayList<>();
    }

    private double calculateEquipmentPdf() {
        double totalPdf = 0;
        for (Equipment equipment : getEquipmentsForCalculation()) {
            if (isEquipmentCompatible(equipment)) {
                totalPdf += attack * (equipment.getPdfBonus() / 100.0);
            }
        }
        return totalPdf;
    }

    private double calculateEquipmentPdc() {
        double totalPdc = 0;
        for (Equipment equipment : getEquipmentsForCalculation()) {
            if (isEquipmentCompatible(equipment)) {
                totalPdc += attack * (equipment.getPdcBonus() / 100.0);
            }
        }
        return totalPdc;
    }

    private double calculateEquipmentArmor() {
        double totalArmor = 0;
        for (Equipment equipment : getEquipmentsForCalculation()) {
            if (isEquipmentCompatible(equipment)) {
                totalArmor += defense * (equipment.getArmBonus() / 100.0);
            }
        }
        return totalArmor;
    }

    private double calculateEquipmentEvasion() {
        double totalEvasion = 0;
        for (Equipment equipment : getEquipmentsForCalculation()) {
            if (isEquipmentCompatible(equipment)) {
                totalEvasion += equipment.getEvasionBonus();
            }
        }
        return totalEvasion;
    }

    private boolean isEquipmentCompatible(Equipment equipment) {
        return classesSet.stream()
                .anyMatch(unitClass -> equipment.getCompatibleClasses().contains(unitClass));
    }

    // ===== GESTION DE L'EXPÉRIENCE ET DE L'ÉVOLUTION =====

    public void gainExperience(double exp) {
        this.experience += exp;
        UnitType newType = UnitType.getTypeByExperience((int) experience);
        if (newType != this.type) {
            evolve(newType);
        }
    }

    private void evolve(UnitType newType) {
        this.type = newType;
        recalculateBaseStats();
    }

    // ===== GESTION DES CLASSES =====

    public boolean isUsable() {
        return classesSet != null && !classesSet.isEmpty();
    }

    /**
     * Nombre maximum de secteurs que cette unité peut parcourir par tour.
     * Vaut le maximum parmi toutes ses classes (ex : LEGER = 2, autres = 1).
     * Une unité sans classe retourne 1 par défaut.
     */
    public int getMaxMovementHops() {
        if (classesSet == null || classesSet.isEmpty()) return 1;
        return classesSet.stream()
                .mapToInt(UnitClass::getMaxMovementHops)
                .max()
                .orElse(1);
    }

    public boolean canAddSecondClass() {
        long effectiveClassCount = classesSet.size();
        if (type == UnitType.LARBIN || type == UnitType.VOYOU) {
            return effectiveClassCount < 1;
        }
        return effectiveClassCount <= 1 && experience >= MIN_EXPERIENCE_FOR_SECOND_CLASS;
    }

    public void addSecondClass(UnitClass secondClass) {
        if (canAddSecondClass() && !classesSet.contains(secondClass)) {
            classesSet.add(secondClass);
            recalculateBaseStats();
        } else {
            logger.warn("Impossible d'ajouter la classe : {}", secondClass);
        }
    }

    // ===== GESTION DES ÉQUIPEMENTS =====

    public boolean canEquip(Equipment equipment) {
        if (!isEquipmentCompatible(equipment)) {
            return false;
        }

        EquipmentCategory category = equipment.getCategory();
        long currentCount = countEquipmentsByCategory(category);

        return switch (category) {
            case FIREARM -> currentCount < type.getMaxFirearms();
            case MELEE -> currentCount < type.getMaxMeleeWeapons();
            case DEFENSIVE -> currentCount < type.getMaxDefensiveEquipment();
        };
    }

    public boolean addEquipment(Equipment equipment) {
        if (canEquip(equipment)) {
            if (equipments == null) {
                equipments = new ArrayList<>();
            }
            if (unitEquipments == null) {
                unitEquipments = new ArrayList<>();
            }
            equipments.add(equipment);
            // Ajouter aussi dans unitEquipments pour la persistance
            unitEquipments.add(new UnitEquipment(this, equipment));
            recalculateBaseStats();
            return true;
        }
        return false;
    }

    public boolean removeEquipment(Equipment equipment) {
        if (equipment == null) return false;
        // Retrait par nom (et non par référence) : les unités chargées depuis la BDD
        // peuplent unitEquipments mais laissent la liste transient `equipments` vide,
        // donc equipments.remove(eq) échouerait. On couvre les deux sources.
        boolean removed = false;
        if (equipments != null) {
            removed = equipments.removeIf(e -> e.getName().equals(equipment.getName()));
        }
        if (unitEquipments != null) {
            removed = unitEquipments.removeIf(ue -> ue.getEquipment().getName().equals(equipment.getName())) || removed;
        }
        if (removed) {
            recalculateBaseStats();
        }
        return removed;
    }

    public List<Equipment> getEquipmentsByCategory(EquipmentCategory category) {
        return getEquipmentsForCalculation().stream()
                .filter(e -> e.getCategory() == category)
                .toList();
    }

    public long countEquipmentsByCategory(EquipmentCategory category) {
        return getEquipmentsForCalculation().stream()
                .filter(e -> e.getCategory() == category)
                .count();
    }

    // ===== MÉTHODES UTILITAIRES POUR LE COMBAT =====


    private String formatEvasion(double value) {
        return String.valueOf((int) Math.ceil(value));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Unique id: ").append(getId()).append(" - ");

        if (type == PERSONNAGE) {
            sb.append(type.name());
            sb.append(" (");
            statsBuilder(sb, attack, pdf, pdc, defense, armor, evasion);
            sb.append(").");
        } else {
            if (isInjured) {
                sb.append("[X] ");
            }
            sb.append(classesSet.stream().map(c -> "(" + c.getCode() + ")").collect(Collectors.joining(" "))).append(" ");

            sb.append(type.name());
            sb.append(" n°").append(number);
            sb.append(" (").append(formatStat(experience)).append(" Exp) : ");

            List<Equipment> eqs = getEquipmentsForCalculation();
            eqs.forEach(f -> sb.append(f.getName()).append(". "));
            if (eqs.isEmpty()) {
                sb.append("Aucun équipement. ");
            }

            statsBuilder(sb, attack, pdf, pdc, defense, armor, evasion);
            sb.append(".");
        }

        return sb.toString();
    }

    private void statsBuilder(StringBuilder sb, double attack, double pdf, double pdc,
                              double defense, double armor, double evasion) {
        sb.append(formatStat(attack)).append(" Atk");
        if (pdf > 0) sb.append(" + ").append(formatStat(pdf)).append(" Pdf");
        if (pdc > 0) sb.append(" + ").append(formatStat(pdc)).append(" Pdc");
        sb.append(" / ").append(formatStat(defense)).append(" Def");
        if (armor > 0) sb.append(" + ").append(formatStat(armor)).append(" Arm");
        if (evasion > 0) sb.append(". Esquive : ").append(formatEvasion(evasion)).append(" %");
    }

    public double getDamageReduction(String damageType) {
        return classesSet.stream()
                .mapToDouble(c -> c.getDamageReduction(damageType))
                .max()
                .orElse(0.0);
    }

    public double getBaseDefense() {
        return type.getBaseDefense();
    }

    public List<Equipment> getEquipments() {
        return getEquipmentsForCalculation();
    }

    public void setEquipments(List<Equipment> equipments) {
        this.equipments = equipments;
    }
}