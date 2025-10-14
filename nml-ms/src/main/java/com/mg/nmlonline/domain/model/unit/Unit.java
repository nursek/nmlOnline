package com.mg.nmlonline.domain.model.unit;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.equipment.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mg.nmlonline.domain.model.unit.UnitType.*;

/**
 * Represents a unit with various attributes for combat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Unit {
    // Unique ID for each unit
    private static int nextId = 1;

    private int id;
    private String name;
    private int number = 0;
    private double experience = 0.0;
    private UnitType type;
    private List<UnitClass> classes;

    // Statistiques de base
    private double attack;
    private double defense;

    // Statistiques calculées et conservées (sans bonus du joueur)
    private double pdf;
    private double pdc;
    private double armor;
    private double evasion;

    // Équipements
    private List<Equipment> equipments;

    public Unit(double experience, String name, UnitClass primaryClass) {
        this.id = nextId++;
        this.name = name;
        this.experience = experience;
        this.type = UnitType.getTypeByExperience((int) experience); // Détermine le type par l'expérience
        this.classes = new ArrayList<>();
        this.classes.add(primaryClass);

        this.attack = type.getBaseAttack();
        this.defense = type.getBaseDefense();

        this.equipments = new ArrayList<>();

        recalculateBaseStats(); // Calcul initial
    }

    // Recalcule les statistiques de base (sans bonus joueur)
    public void recalculateBaseStats() {
        double statMultiplier = classes.stream().mapToDouble(UnitClass::getStatMultiplier).min().orElse(1.0);

        this.attack = type.getBaseAttack() * statMultiplier;
        this.defense =  type.getBaseDefense() * statMultiplier;
        this.pdf = calculateEquipmentPdf();
        this.pdc = calculateEquipmentPdc();
        this.armor = calculateEquipmentArmor();
        this.evasion = calculateEquipmentEvasion();
    }

    //TODO : pour les bonus du joueur, revoir + tard pendant système de combat
    // Applique les bonus du joueur (appelé par Player)
    public void applyPlayerBonuses(double attackBonus, double defenseBonus, double pdfBonus, double pdcBonus, double armorBonus, double evasionBonus) {
        this.attack = attack * (1.0 + attackBonus);
        this.defense = defense * (1.0 + defenseBonus);
        this.pdf = pdf * (1.0 + pdfBonus);
        this.pdc = pdc * (1.0 + pdcBonus);
        this.armor = armor * (1.0 + armorBonus);
        this.evasion = evasion - (evasionBonus * 10);
    }

    private double calculateEquipmentPdf() {
        double totalPdf = 0;

        for (Equipment equipment : equipments) {
            if (isEquipmentCompatible(equipment)) {
                totalPdf += attack * (equipment.getPdfBonus() / 100.0);
            }
        }
        return totalPdf;
    }

    private double calculateEquipmentPdc() {
        double totalPdc = 0;

        for (Equipment equipment : equipments) {
            if (isEquipmentCompatible(equipment)) {
                totalPdc += attack * (equipment.getPdcBonus() / 100.0);
            }
        }
        return totalPdc;
    }

    private double calculateEquipmentArmor() {
        double totalArmor = 0;

        for (Equipment equipment : equipments) {
            if (isEquipmentCompatible(equipment)) {
                totalArmor += defense * (equipment.getArmBonus() / 100.0);
            }
        }
        return totalArmor;
    }

    private double calculateEquipmentEvasion() {
        double totalEvasion = 0;

        for (Equipment equipment : equipments) {
            if (isEquipmentCompatible(equipment)) {
                totalEvasion += equipment.getEvasionBonus();
            }
        }
        return totalEvasion;
    }

    private boolean isEquipmentCompatible(Equipment equipment) {
        return classes.stream().anyMatch(unitClass -> equipment.getCompatibleClasses().contains(unitClass));
    }

    // Gestion de l'évolution
    public void gainExperience(double exp) {
        this.experience += exp;
        UnitType newType = UnitType.getTypeByExperience((int) experience);
        if (newType != this.type) {
            evolve(newType);
        }
    }

    private void evolve(UnitType newType) {
        this.type = newType;
        recalculateBaseStats(); // Recalcule avec les nouvelles stats de base
    }

    // Gestion des classes
    public boolean canAddSecondClass() {
        long effectiveClassCount = classes.stream().filter(c -> c != UnitClass.BLESSE).count();
        if (type == UnitType.LARBIN || type == UnitType.VOYOU) {
            return effectiveClassCount < 1;
        } else return effectiveClassCount <= 1 && experience >= 5;
    }

    public void addSecondClass(UnitClass secondClass) {
        if (secondClass == UnitClass.BLESSE) {
            System.out.println("Impossible d'ajouter la classe BLESSE comme seconde classe.");
            return;
        }
        if (canAddSecondClass() && !classes.contains(secondClass)) {
            classes.add(secondClass);
            recalculateBaseStats();
        } else {
            System.out.println("Impossible d'ajouter la classe : " + secondClass);
        }
    }

    // Gestion des équipements
    public boolean canEquip(Equipment equipment) {
        EquipmentCategory category = equipment.getCategory();
        if (category == EquipmentCategory.FIREARM) {
            long firearmsCount = equipments.stream().filter(e -> e.getCategory() == EquipmentCategory.FIREARM).count();
            return firearmsCount < type.getMaxFirearms() && isEquipmentCompatible(equipment);
        } else if (category == EquipmentCategory.MELEE) {
            long meleeCount = equipments.stream().filter(e -> e.getCategory() == EquipmentCategory.MELEE).count();
            return meleeCount < type.getMaxMeleeWeapons() && isEquipmentCompatible(equipment);
        } else if (category == EquipmentCategory.DEFENSIVE) {
            long defensiveCount = equipments.stream().filter(e -> e.getCategory() == EquipmentCategory.DEFENSIVE).count();
            return defensiveCount < type.getMaxDefensiveEquipment() && isEquipmentCompatible(equipment);
        }
        return false;
    }

    public boolean addEquipment(Equipment equipment) {
        if (canEquip(equipment)) {
            equipments.add(equipment);
            recalculateBaseStats();
            return true;
        }
        return false;
    }

    public boolean removeEquipment(Equipment equipment) {
        if (equipment == null) return false;
        boolean removed = equipments.remove(equipment);
        if (removed) {
            recalculateBaseStats();
        }
        return removed;
    }

    public double getTotalAttack() {
        return attack + pdf + pdc;
    }

    // Méthodes utilitaires pour le tri
    public double getTotalDefense() {
        return defense + armor;
    }

    // Méthodes de formatage pour l'affichage
    private String formatStat(double value) {
        // 2 décimales, supprime les zéros inutiles
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.2f", value).replaceAll("0+$", "").replaceAll(",$", "");
        }
    }

    private String formatEvasion(double value) {
        // Esquive arrondie au chiffre du dessus (plafond)
        return String.valueOf((int) Math.ceil(value));
    }

    // Méthode toString pour affichage selon le format demandé
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Unique id: ").append(id).append(" - ");

        if (type == PERSONNAGE) {
            // Exemple de ligne : Mortarion (100 Atk + 100 Pdf + 50 Pdc / 250 Def)
            sb.append(name);
            sb.append(" (");
            statsBuilder(sb, attack, pdf, pdc, defense, armor, evasion);
            sb.append(").");
        } else {
            sb.append(classes.stream().map(c -> "(" + c.getCode() + ")").collect(Collectors.joining(" "))).append(" ");

            // Type et informations
            sb.append(name);
            sb.append(" n°").append(number);
            sb.append(" (").append(formatStat(experience)).append(" Exp) : ");

            // Équipements
            equipments.forEach(f -> sb.append(f.getName()).append(". "));
            if (equipments.isEmpty()) {
                sb.append("Aucun équipement. ");
            }

            // Statistiques avec formatage précis
            statsBuilder(sb, attack, pdf, pdc, defense, armor, evasion);
            sb.append(".");
        }


        return sb.toString();
    }

    private void statsBuilder(StringBuilder sb, double attack, double pdf, double pdc, double defense, double armor, double evasion) {
        sb.append(formatStat(attack)).append(" Atk");
        if (pdf > 0) sb.append(" + ").append(formatStat(pdf)).append(" Pdf");
        if (pdc > 0) sb.append(" + ").append(formatStat(pdc)).append(" Pdc");
        sb.append(" / ").append(formatStat(defense)).append(" Def");
        if (armor > 0) sb.append(" + ").append(formatStat(armor)).append(" Arm");
        if (evasion > 0) sb.append(". Esquive : ").append(formatEvasion(evasion)).append(" %");
    }

    public double getDamageReduction(String damageType) {
        return classes.stream().mapToDouble(c -> c.getDamageReduction(damageType)).max().orElse(0);
    }

    public double getBaseDefense() {
        return this.type.getBaseDefense();
    }
}