package com.mg.nmlonline.model.unit;

import com.mg.nmlonline.model.equipement.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a unit with various attributes for combat.
 */
@Data
@NoArgsConstructor
public class Unit {
    private int id; //TODO: virer l'id normalement c'est useless.
    private String name;
    private double experience;
    private UnitType type;
    private List<UnitClass> classes;
    
    // Statistiques de base
    private int baseAttack;
    private int baseDefense;
    
    // Statistiques calculées et conservées (sans bonus du joueur)
    private double baseCalculatedPdf;
    private double baseCalculatedPdc;
    private double baseCalculatedArmor;
    private double baseCalculatedEvasion;
    
    // Statistiques finales (avec bonus du joueur appliqués)
    private double finalAttack;
    private double finalDefense;
    private double finalPdf;
    private double finalPdc;
    private double finalArmor;
    private double finalEvasion;
    
    // Équipements
    private List<Equipment> equipments;

    public Unit(double experience, String name, UnitClass primaryClass) {
        this.id = 0;
        this.name = name;
        this.experience = experience;
        this.type = UnitType.getTypeByExperience((int) experience); // Détermine le type par l'expérience
        this.classes = new ArrayList<>();
        this.classes.add(primaryClass);
        
        this.baseAttack = type.getBaseAttack();
        this.baseDefense = type.getBaseDefense();
        
        this.equipments = new ArrayList<>();
        
        recalculateBaseStats(); // Calcul initial
    }

    // Recalcule les statistiques de base (sans bonus joueur)
    public void recalculateBaseStats() {
        this.baseAttack = type.getBaseAttack();
        this.baseDefense = type.getBaseDefense();
        
        this.baseCalculatedPdf = calculateEquipmentPdf();
        this.baseCalculatedPdc = calculateEquipmentPdc();
        this.baseCalculatedArmor = calculateEquipmentArmor();
        this.baseCalculatedEvasion = calculateEquipmentEvasion();
        
        // Par défaut, pas de bonus
        this.finalAttack = baseAttack;
        this.finalDefense = baseDefense;
        this.finalPdf = baseCalculatedPdf;
        this.finalPdc = baseCalculatedPdc;
        this.finalArmor = baseCalculatedArmor;
        this.finalEvasion = baseCalculatedEvasion;
    }
    
    // Applique les bonus du joueur (appelé par Player)
    public void applyPlayerBonuses(double attackBonus, double defenseBonus, double pdfBonus, 
                                  double pdcBonus, double armorBonus, double evasionBonus) {
        this.finalAttack = baseAttack * (1.0 + attackBonus);
        this.finalDefense = baseDefense * (1.0 + defenseBonus);
        this.finalPdf = baseCalculatedPdf * (1.0 + pdfBonus);
        this.finalPdc = baseCalculatedPdc * (1.0 + pdcBonus);
        this.finalArmor = baseCalculatedArmor * (1.0 + armorBonus);
        this.finalEvasion = baseCalculatedEvasion - (evasionBonus * 10);
    }

    private double calculateEquipmentPdf() {
        double totalPdf = 0;
        
        for (Equipment equipment : equipments) {
            if (isEquipmentCompatible(equipment)) {
                totalPdf += baseAttack * (equipment.getPdfBonus() / 100.0);
            }
        }
        return totalPdf;
    }

    private double calculateEquipmentPdc() {
        double totalPdc = 0;
        
        for (Equipment equipment : equipments) {
            if (isEquipmentCompatible(equipment)) {
                totalPdc += baseAttack * (equipment.getPdcBonus() / 100.0);
            }
        }
        return totalPdc;
    }

    private double calculateEquipmentArmor() {
        double totalArmor = 0;
        
        for (Equipment equipment : equipments) {
            if (isEquipmentCompatible(equipment)) {
                totalArmor += baseDefense * (equipment.getArmBonus() / 100.0);
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
        return classes.stream().anyMatch(unitClass -> 
            equipment.getCompatibleClasses().contains(unitClass));
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
        return (type == UnitType.MALFRAT || type == UnitType.BRUTE) && classes.size() == 1 &&
               experience >= 5; // Voyou qui a évolué en malfrat
    }

    public void addSecondClass(UnitClass secondClass) {
        if (canAddSecondClass() && !classes.contains(secondClass)) {
            classes.add(secondClass);
            recalculateBaseStats(); // Recalcule avec les nouvelles compatibilités
        }
        else
            System.out.println("Impossible d'ajouter la classe : " + secondClass);
    }

    // Gestion des équipements
    public boolean canEquip(Equipment equipment) {
        EquipmentCategory category = equipment.getCategory();
        if (category == EquipmentCategory.Firearm) {
            long firearmsCount = equipments.stream()
                    .filter(e -> e.getCategory() == EquipmentCategory.Firearm)
                    .count();
            return firearmsCount < type.getMaxFirearms() &&
                    isEquipmentCompatible(equipment);
        } else if (category == EquipmentCategory.Meelee) {
            long meleeCount = equipments.stream()
                    .filter(e -> e.getCategory() == EquipmentCategory.Meelee)
                    .count();
            return meleeCount < type.getMaxMeleeWeapons() &&
                    isEquipmentCompatible(equipment);
        } else if (category == EquipmentCategory.Defensive) {
            long defensiveCount = equipments.stream()
                    .filter(e -> e.getCategory() == EquipmentCategory.Defensive)
                    .count();
            return defensiveCount < type.getMaxDefensiveEquipment() &&
                    isEquipmentCompatible(equipment);
        }
        return false;
    }

    public void equip(Equipment equipment) {
        if (canEquip(equipment)) {
            equipments.add(equipment);
            recalculateBaseStats();
        }
    }

    // Méthodes utilitaires pour le tri
    public double getTotalDefense() {
        return finalDefense + finalArmor;
    }

    public double getTotalAttack() {
        return finalAttack + finalPdf + finalPdc;
    }

    // Méthodes de formatage pour l'affichage
    private String formatStat(double value) {
        // 2 décimales, supprime les zéros inutiles
        if (value == Math.floor(value)) {
            return String.valueOf((int)value);
        } else {
            return String.format("%.2f", value).replaceAll("0+$", "").replaceAll(",$", "");
        }
    }
    
    private String formatEvasion(double value) {
        // Esquive arrondie au chiffre du dessus (plafond)
        return String.valueOf((int)Math.ceil(value));
    }

    // Méthode toString pour affichage selon le format demandé
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        // Classes
        sb.append(
                classes.stream()
                        .map(c -> "(" + c.getCode() + ")")
                        .collect(Collectors.joining(" "))
        ).append(" ");
        
        // Type et informations
        sb.append(type.name().charAt(0)).append(type.name().substring(1).toLowerCase());
        sb.append(" n°").append(id);
        sb.append(" (").append(formatStat(experience)).append(" Exp) : ");
        
        // Équipements
        equipments.forEach(f -> sb.append(f.getName()).append(". "));
        if (equipments.isEmpty()) {
            sb.append("Aucun équipement. ");
        }
        
        // Statistiques avec formatage précis
        sb.append(formatStat(finalAttack)).append(" Atk");
        if (finalPdf > 0) sb.append(" + ").append(formatStat(finalPdf)).append(" Pdf");
        if (finalPdc > 0) sb.append(" + ").append(formatStat(finalPdc)).append(" Pdc");
        sb.append(" / ").append(formatStat(finalDefense)).append(" Def");
        if (finalArmor > 0) sb.append(" + ").append(formatStat(finalArmor)).append(" Arm");
        if (finalEvasion > 0) sb.append(". Esquive : ").append(formatEvasion(finalEvasion)).append(" %");
        sb.append(".");
        
        return sb.toString();
    }
}