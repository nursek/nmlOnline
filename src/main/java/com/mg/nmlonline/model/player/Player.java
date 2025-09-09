package com.mg.nmlonline.model.player;

import com.mg.nmlonline.model.equipement.Equipment;
import com.mg.nmlonline.model.equipement.EquipmentStack;
import com.mg.nmlonline.model.sector.Sector;
import com.mg.nmlonline.model.unit.Unit;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
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
    private double money = 0.0; // Argent du joueur
    private List<EquipmentStack> equipments = new ArrayList<>(); // Équipements possédés par le joueur
    private List<Sector> sectors = new ArrayList<>(); // Secteurs/Quartiers contrôlés par le joueur

    private double totalIncome = 0.0 ;
    private double totalMilitaryPower = 0.0; // Puissance militaire totale du joueur, calculée à partir des armées des secteurs
    private double totalEconomyPower = 0.0; // Puissance économique totale du joueur, calculée à partir des secteurs, des équipements, etc.

    public Player(String name) {
        this.name = name;
    }

    // === GESTION DES SECTEURS DU JOUEUR ===

    public void addSector(Sector sector) {
        sectors.add(sector);
        recalculateStats();
    }

    public void removeSector(Sector sector) {
        sectors.remove(sector);
        recalculateStats();
    }

    public Sector getSectorByNumber(int sectorNumber) {
        return sectors.stream()
                .filter(sector -> sector.getNumber() == sectorNumber)
                .findFirst()
                .orElse(null);
    }

    public List<Sector> getSectorsWithArmy() {
        return sectors.stream()
                .filter(sector -> sector.getArmySize() > 0)
                .toList();
    }

    /**
     * Ajoute une unité directement à un secteur spécifique
     */
    public boolean addUnitToSector(Unit unit, int sectorNumber) {
        Sector targetSector = getSectorByNumber(sectorNumber);
        if (targetSector != null) {
            targetSector.addUnit(unit);
            recalculateStats();
            return true;
        }
        return false;
    }

    /**
     * Supprime une unité d'un secteur spécifique
     */
    public boolean removeUnitFromSector(Unit unit, int sectorNumber) {
        Sector sourceSector = getSectorByNumber(sectorNumber);
        if (sourceSector != null) {
            boolean removed = sourceSector.removeUnit(unit);
            if (removed) {
                recalculateStats();
            }
            return removed;
        }
        return false;
    }

    /**
     * Transfert d'unités entre secteurs
     */
    public boolean transferUnitBetweenSectors(Unit unit, int fromSectorNumber, int toSectorNumber) {
        Sector fromSector = getSectorByNumber(fromSectorNumber);
        Sector toSector = getSectorByNumber(toSectorNumber);

        if (fromSector != null && toSector != null && fromSector.removeUnit(unit)) {
            toSector.addUnit(unit);
            recalculateStats();
            return true;
        }
        return false;
    }

    /**
     * Obtient toutes les unités du joueur (toutes dans les secteurs)
     */
    public List<Unit> getAllUnits() {
        List<Unit> allUnits = new ArrayList<>();
        for (Sector sector : sectors) {
            allUnits.addAll(sector.getArmy());
        }
        return allUnits;
    }

    /**
     * Trouve une unité par son ID dans tous les secteurs
     */
    public Unit getUnitById(int id) {
        return getAllUnits().stream()
                .filter(unit -> unit.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Trouve des unités par type dans tous les secteurs
     */
    public List<Unit> getUnitsByType(String unitType) {
        return getAllUnits().stream()
                .filter(unit -> unit.getClasses().stream()
                        .anyMatch(unitClass -> unitClass.name().equalsIgnoreCase(unitType)))
                .toList();
    }


    /**
     * Obtient le nombre total d'unités (toutes dans les secteurs)
     */
    public int getTotalArmySize() {
        return sectors.stream().mapToInt(Sector::getArmySize).sum();
    }

    // === GESTION DES EQUIPMENT DU JOUEUR ===

    public void addEquipment(Equipment equipment, int number) {
        for (EquipmentStack stack : equipments) {
            if (stack.getEquipment().equals(equipment)) {
                for (int i = 0; i < number; i++) {
                    stack.increment();
                }
                return;
            }
        }
        EquipmentStack newStack = new EquipmentStack(equipment);
        for (int i = 1; i < number; i++) {
            newStack.increment();
        }
        equipments.add(newStack);
    }

    public void addEquipment(Equipment equipment) {
        addEquipment(equipment, 1);
    }

    public void removeEquipment(Equipment equipment) {
        for (int i = 0; i < equipments.size(); i++) {
            EquipmentStack stack = equipments.get(i);
            if (stack.getEquipment().equals(equipment)) {
                if (stack.getQuantity() > 1) {
                    stack.decrement();
                } else {
                    equipments.remove(i);
                }
                return;
            }
        }
    }

    public double getTotalEquipmentValue() {
        return equipments.stream()
                .mapToDouble(stack -> stack.getEquipment().getCost() * stack.getQuantity())
                .sum();
    }

    // === CALCULS ET STATISTIQUES ===

    private void recalculateStats() {
        totalIncome = sectors.stream()
                .mapToDouble(Sector::getIncome)
                .sum();

        // TotalAtk de chaque secteur + TotalDef de chaque secteur / 2
        totalMilitaryPower = calculateTotalMilitaryPower();

        totalEconomyPower = money + totalIncome + getTotalEquipmentValue();
        //TODO : à modifier pr totalEconomyPower, prendre en compte le revenu de chaque quartier
    }

    private double calculateTotalMilitaryPower() {
        double offensivePower = sectors.stream()
                .mapToDouble(Sector::getOffensivePower)
                .sum();
        double defensivePower = sectors.stream()
                .mapToDouble(Sector::getDefensivePower)
                .sum();

        return offensivePower + defensivePower / 2;
    }

    /**
     * Affiche toutes les armées des secteurs du joueur
     */
    public void displayArmy() {
        System.out.println("=== ARMÉES DE " + name.toUpperCase() + " ===");

        List<Sector> sectorsWithArmy = getSectorsWithArmy();
        if (sectorsWithArmy.isEmpty()) {
            System.out.println("Aucune unité dans les secteurs.");
        } else {
            for (Sector sector : sectorsWithArmy) {
                sector.displayArmy();
                System.out.println();
            }
        }

        System.out.printf("TOTAL : %d unités réparties dans %d secteurs%n",
                getTotalArmySize(), sectors.size());
    }


    /** Affiche les équipements du joueur
     * Regroupe les équipements par nom et affiche le nombre de chaque type
     */
    public void displayEquipments() {
        System.out.println("=== ÉQUIPEMENTS DE " + name.toUpperCase() + " ===");
        List<Unit> allUnits = getAllUnits(); // Toutes les unités sont dans les secteurs

        if (equipments.isEmpty() && allUnits.stream().allMatch(u -> u.getEquipments().isEmpty())) {
            System.out.println("Aucun équipement.");
            return;
        }

        // Compte total par nom
        Map<String, Equipment> equipmentRef = new HashMap<>();
        Map<String, Integer> totalCount = new HashMap<>();
        Map<String, Integer> equippedCount = new HashMap<>();

        // Compte dans l'inventaire du joueur
        for (EquipmentStack stack : equipments) {
            Equipment eq = stack.getEquipment();
            totalCount.put(eq.getName(), stack.getQuantity());
            equipmentRef.putIfAbsent(eq.getName(), eq);
        }

        // Compte dans toutes les unités (dans tous les secteurs)
        for (Unit unit : allUnits) {
            for (Equipment eq : unit.getEquipments()) {
                totalCount.merge(eq.getName(), 1, Integer::sum);
                equippedCount.merge(eq.getName(), 1, Integer::sum);
                equipmentRef.putIfAbsent(eq.getName(), eq);
            }
        }

        // Affichage
        for (Map.Entry<String, Integer> entry : totalCount.entrySet()) {
            String eqName = entry.getKey();
            int total = entry.getValue();
            int equipped = equippedCount.getOrDefault(eqName, 0);
            Equipment eq = equipmentRef.get(eqName);
            int totalPrice = total * eq.getCost();
            System.out.printf("%d x %s (%d / %d équipé) = %,d $%n", total, eq.getName(), equipped, total, totalPrice);
        }
    }


}