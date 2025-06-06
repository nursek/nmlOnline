package com.mg.nmlonline.model.player;

import com.mg.nmlonline.model.unit.Unit;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Classe représentant un joueur avec son armée d'unités
 */
@Data
@NoArgsConstructor
public class Player {
    private String name;
    private List<Unit> army;
    
    // Bonus/Malus globaux du joueur en pourcentage
    private double attackBonusPercent = 0.0;
    private double defenseBonusPercent = 0.0;
    private double pdfBonusPercent = 0.0;
    private double pdcBonusPercent = 0.0;
    private double armorBonusPercent = 0.0;
    private double evasionBonusPercent = 0.0;

    public Player(String name) {
        this.name = name;
        this.army = new ArrayList<>();
    }

    // Méthodes pour gérer l'armée
    public void addUnit(Unit unit) {
        army.add(unit);
        applyBonusesToUnit(unit);
        sortAndReorderArmy();
    }

    public void removeUnit(Unit unit) {
        army.remove(unit);
    }

    public void removeUnit(int unitId) {
        army.removeIf(unit -> unit.getId() == unitId);
    }

    // Méthodes pour appliquer les bonus/malus
    public void applyAttackBonus(double percentBonus) {
        this.attackBonusPercent = percentBonus;
        updateAllUnitsBonuses();
    }
    
    public void applyDefenseBonus(double percentBonus) {
        this.defenseBonusPercent = percentBonus;
        updateAllUnitsBonuses();
    }
    
    public void applyPdfBonus(double percentBonus) {
        this.pdfBonusPercent = percentBonus;
        updateAllUnitsBonuses();
    }
    
    public void applyPdcBonus(double percentBonus) {
        this.pdcBonusPercent = percentBonus;
        updateAllUnitsBonuses();
    }
    
    public void applyArmorBonus(double percentBonus) {
        this.armorBonusPercent = percentBonus;
        updateAllUnitsBonuses();
    }
    
    public void applyEvasionBonus(double percentBonus) {
        this.evasionBonusPercent = percentBonus;
        updateAllUnitsBonuses();
    }
    
    // Méthode pour appliquer un bonus global à toutes les stats
    public void applyGlobalBonus(double percentBonus) {
        this.attackBonusPercent = percentBonus;
        this.defenseBonusPercent = percentBonus;
        this.pdfBonusPercent = percentBonus;
        this.pdcBonusPercent = percentBonus;
        this.armorBonusPercent = percentBonus;
        this.evasionBonusPercent = percentBonus;
        updateAllUnitsBonuses();
    }

    // Applique les bonus du joueur à une unité spécifique
    private void applyBonusesToUnit(Unit unit) {
        unit.applyPlayerBonuses(
            attackBonusPercent, 
            defenseBonusPercent, 
            pdfBonusPercent, 
            pdcBonusPercent, 
            armorBonusPercent, 
            evasionBonusPercent
        );
    }

    // Met à jour les bonus pour toutes les unités
    private void updateAllUnitsBonuses() {
        for (Unit unit : army) {
            applyBonusesToUnit(unit);
        }
        sortAndReorderArmy();
    }

    /**
     * Trie l'armée selon les critères :
     * 1. Défense totale (Def + Arm) décroissante
     * 2. Expérience décroissante (en cas d'égalité)
     * 3. ID croissant (en cas d'égalité parfaite)
     */
    private void sortAndReorderArmy() {
        army.sort(Comparator
                .comparingDouble(Unit::getExperience).reversed()          // 1. Exp décroissante
                .thenComparing(Unit::getTotalDefense, Comparator.reverseOrder())  // 2. Def totale décroissante
                .thenComparing(Unit::getId)                      // 3. ID original croissant
        );

        // Renumérotation automatique des IDs par type d'unité
        Map<String, Integer> typeCounters = new HashMap<>();

        for (Unit unit : army) {
            String unitType = unit.getType().name();
            int currentCount = typeCounters.getOrDefault(unitType, 0) + 1;
            typeCounters.put(unitType, currentCount);
            unit.setId(currentCount); // ID par type
        }


    }

    // Méthodes utilitaires
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

    public double getTotalArmyValue() {
        return army.stream()
            .mapToDouble(Unit::getTotalDefense)
            .sum();
    }

    // Affichage de l'armée
    public void displayArmy() {
        System.out.println("=== ARMÉE DE " + name.toUpperCase() + " ===");

        if (army.isEmpty()) {
            System.out.println("Aucune unité dans l'armée.");
            return;
        }

        if (hasAnyBonus()) {
            System.out.println("Bonus/Malus du joueur :");
            if (attackBonusPercent != 0) System.out.printf("  Attaque : %+.1f%%\n", attackBonusPercent);
            if (defenseBonusPercent != 0) System.out.printf("  Défense : %+.1f%%\n", defenseBonusPercent);
            if (pdfBonusPercent != 0) System.out.printf("  PDF : %+.1f%%\n", pdfBonusPercent);
            if (pdcBonusPercent != 0) System.out.printf("  PDC : %+.1f%%\n", pdcBonusPercent);
            if (armorBonusPercent != 0) System.out.printf("  Armure : %+.1f%%\n", armorBonusPercent);
            if (evasionBonusPercent != 0) System.out.printf("  Esquive : %+.1f%%\n", evasionBonusPercent);
            System.out.println();
        }

        for (Unit unit : army) {
            System.out.println(unit);
        }

        // Calcul des totaux
        double totalAtk = army.stream().mapToDouble(Unit::getFinalAttack).sum();
        double totalPdf = army.stream().mapToDouble(Unit::getFinalPdf).sum();
        double totalPdc = army.stream().mapToDouble(Unit::getFinalPdc).sum();
        double totalDef = army.stream().mapToDouble(Unit::getFinalDefense).sum();
        double totalArm = army.stream().mapToDouble(Unit::getFinalArmor).sum();

        System.out.printf(
                "\nTotal : %d unités => %.0f Atk + %.0f Pdf + %.0f Pdc / %.0f Def + %.0f Arm.\n",
                getArmySize(), totalAtk, totalPdf, totalPdc, totalDef, totalArm
        );
    }

    private boolean hasAnyBonus() {
        return attackBonusPercent != 0 || defenseBonusPercent != 0 ||
               pdfBonusPercent != 0 || pdcBonusPercent != 0 ||
               armorBonusPercent != 0 || evasionBonusPercent != 0;
    }

    @Override
    public String toString() {
        return String.format("Joueur: %s (%d unités)", name, getArmySize());
    }
}