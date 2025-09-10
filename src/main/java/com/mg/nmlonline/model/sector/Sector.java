package com.mg.nmlonline.model.sector;

import com.mg.nmlonline.model.unit.Unit;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Data
@Getter
@Setter
public class Sector {
    private int number;
    private String name;
    private List<Unit> army = new ArrayList<>();
    private double income = 2000 ;

    private double totalAtk = 0.0;
    private double totalPdf = 0.0;
    private double totalPdc = 0.0;
    private double totalDef = 0.0;
    private double totalArmor = 0.0;

    public Sector(int number) {
        this.number = number;
        this.name = "Secteur n°" + number;
    }

    public Sector(int number, String name) {
        this.number = number;
        this.name = name;
    }

    // === GESTION DE L'ARMÉE DU SECTEUR ===

    public void addUnit(Unit unit) {
        if (unit != null) {
            army.add(unit);
            sortArmy();
            reassignUnitIds();
            recalculateMilitaryPower();
        }
    }

    public void addUnits(List<Unit> units) {
        if (units != null && !units.isEmpty()) {
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

    /**
     * Calcule la puissance militaire totale du secteur
     */

    public void calculateTotalAtk(){
        totalAtk =  army.stream()
                .mapToDouble(Unit::getFinalAttack)
                .sum();
    }

    public void calculateTotalPdf(){
        totalPdf = army.stream()
                .mapToDouble(Unit::getFinalPdf)
                .sum();
    }

    public void calculateTotalPdc(){
        totalPdc =  army.stream()
                .mapToDouble(Unit::getFinalPdc)
                .sum();
    }

    public void calculateTotalDef(){
        totalDef = army.stream()
                .mapToDouble(Unit::getFinalDefense)
                .sum();
    }

    public void calculateTotalArmor(){
        totalArmor = army.stream()
                .mapToDouble(Unit::getFinalArmor)
                .sum();
    }

    public void recalculateMilitaryPower(){
        calculateTotalAtk();
        calculateTotalPdf();
        calculateTotalPdc();
        calculateTotalDef();
        calculateTotalArmor();
    }

    public double getOffensivePower(){
        return totalAtk + totalPdf + totalPdc;
    }

    public double getDefensivePower(){
        return totalDef + totalArmor;
    }

    // === TRI ET RÉASSIGNATION DES IDS ===

    public void sortArmy() {
        army.sort(Comparator
                .comparingDouble(Unit::getExperience).reversed()
                .thenComparing(Unit::getTotalDefense, Comparator.reverseOrder())
                .thenComparing(Unit::getTotalAttack, Comparator.reverseOrder())
                .thenComparing(Unit::getId)
        );
    }

    public void reassignUnitIds() {
        Map<String, Integer> typeCounters = new HashMap<>();
        for (Unit unit : army) {
            String unitType = unit.getType().name();
            int currentCount = typeCounters.getOrDefault(unitType, 0) + 1;
            typeCounters.put(unitType, currentCount);
            unit.setId(currentCount);
        }
    }

    /**
     * Affiche l'armée du secteur
     */
    public void displayArmy() {
        System.out.printf("=== %s ===%n", name.toUpperCase());

        if (army.isEmpty()) {
            System.out.println("Aucune unité dans ce secteur.");
            return;
        }

        for (Unit unit : army) {
            System.out.println(unit);
        }

        // Calcul des totaux pour ce secteur
        recalculateMilitaryPower();

        System.out.printf(
                "Total => %.0f Atk + %.0f Pdf + %.0f Pdc / %.0f Def + %.0f Arm.%n",
                totalAtk, totalPdf, totalPdc, totalDef, totalArmor
        );
    }

    @Override
    public String toString() {
        return String.format("%s - %d unités, Revenus: %.0f$", name, getArmySize(), income);
    }
}

