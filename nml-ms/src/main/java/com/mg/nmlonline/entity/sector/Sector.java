package com.mg.nmlonline.entity.sector;

import com.mg.nmlonline.entity.unit.Unit;
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
    private double income = 2000 ;
    private List<Unit> army = new ArrayList<>();
    private SectorStats stats = new SectorStats();

    public Sector(int number) {
        this.number = number;
        this.name = "Secteur n°" + number;
    }

    public Sector(int number, String name) {
        this.number = number;
        this.name = name;
    }

    // === GESTION DES STATISTIQUES DU SECTEUR ===

    public void recalculateMilitaryPower(){
        stats.setTotalAtk(army.stream()
                .mapToDouble(Unit::getFinalAttack)
                .sum());
        stats.setTotalPdf(army.stream()
                .mapToDouble(Unit::getFinalPdf)
                .sum());
        stats.setTotalPdc(army.stream()
                .mapToDouble(Unit::getFinalPdc)
                .sum());
        stats.setTotalDef(army.stream()
                .mapToDouble(Unit::getFinalDefense)
                .sum());
        stats.setTotalArmor(army.stream()
                .mapToDouble(Unit::getFinalArmor)
                .sum());

        stats.setTotalOffensive(stats.getTotalAtk() + stats.getTotalPdf() + stats.getTotalPdc());
        stats.setTotalDefensive(stats.getTotalDef() + stats.getTotalArmor());
        stats.setGlobalStats((stats.getTotalOffensive() + stats.getTotalDefensive()) / 2);
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

    public List<Unit> getUnits() {
        return army;
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
        // Déplacer cette méthode plutôt côté Player pr garder une uniformité : larbin n°1 dans un quartier, si larbin n°1 dans un autre quartier, ce sont 2 unités différentes, donc larbin n°2.
        Map<String, Integer> typeCounters = new HashMap<>();
        for (Unit unit : army) {
            String unitType = unit.getType().name();
            int currentCount = typeCounters.getOrDefault(unitType, 0) + 1;
            typeCounters.put(unitType, currentCount);
            unit.setNumber(currentCount);
        }
    }

    /**
     * Affiche l'armée du secteur (dans la console)
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
                stats.getTotalAtk(), stats.getTotalPdf(), stats.getTotalPdc(), stats.getTotalDef(), stats.getTotalArmor()
        );
    }

    @Override
    public String toString() {
        return String.format("%s - %d unités, Revenus: %.0f$", name, getArmySize(), income);
    }
}

