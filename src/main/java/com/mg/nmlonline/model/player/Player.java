package com.mg.nmlonline.model.player;

import com.mg.nmlonline.model.equipement.Equipment;
import com.mg.nmlonline.model.equipement.EquipmentStack;
import com.mg.nmlonline.model.sector.Sector;
import com.mg.nmlonline.model.unit.Unit;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Classe représentant un joueur avec son armée d'unités
 */
@Data
@NoArgsConstructor
public class Player {
    private String name;
    private PlayerStats stats = new PlayerStats();
    private List<EquipmentStack> equipments = new ArrayList<>(); // Équipements possédés par le joueur
    private List<Sector> sectors = new ArrayList<>(); // Secteurs/Quartiers contrôlés par le joueur

    public Player(String name) {
        this.name = name;
    }

    // === GESTION DES SECTEURS DU JOUEUR ===

    public void addSector(Sector sector) {
        if (sector != null && !sectors.contains(sector)) {
            sectors.add(sector);
        }
    }

    public void removeSector(Sector sector) {
        if (sectors.remove(sector)) {
            System.out.println(sector.getName() + " has been removed");
        }
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

    public List<Sector> getSectors() {
        return Collections.unmodifiableList(sectors);
    }

    /**
     * Ajoute une unité directement à un secteur spécifique
     */
    public boolean addUnitToSector(Unit unit, int sectorNumber) {
        Sector targetSector = getSectorByNumber(sectorNumber);
        if (targetSector != null) {
            targetSector.addUnit(unit);
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
                System.out.println("Unit " + unit.getName() + " removed from sector " + sectorNumber);
            }
            return removed;
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

    public boolean buyEquipment(Equipment equipment, int quantity) {
        double totalCost = equipment.getCost() * quantity;
        if (stats.getMoney() >= totalCost) {
            stats.setMoney(stats.getMoney() - totalCost);
            addEquipment(equipment, quantity);
            setTotalEquipmentValue();
            calculateTotalEconomyPower();
            return true;
        }
        return false;
    }

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

    public void setTotalEquipmentValue() {
        double inventoryValue = equipments.stream()
                .mapToDouble(stack -> stack.getEquipment().getCost() * stack.getQuantity())
                .sum();

        double equippedValue = getAllUnits().stream()
                .flatMap(unit -> unit.getEquipments().stream())
                .mapToDouble(Equipment::getCost)
                .sum();

        stats.setTotalEquipmentValue(inventoryValue + equippedValue);
    }

    // === GESTION DES UNITS DU JOUEUR ===

    public boolean transferUnitBetweenSectors(Unit unit, int fromSectorNumber, int toSectorNumber) {
        Sector fromSector = getSectorByNumber(fromSectorNumber);
        Sector toSector = getSectorByNumber(toSectorNumber);

        if (fromSector != null && toSector != null && fromSector.removeUnit(unit)) {
            toSector.addUnit(unit);
            return true;
        }
        return false;
    }

//    public boolean equipFirearmToUnit(int sectorId, int unitId, Equipment firearm) {
//        Sector sector = getSectorById(sectorId);
//        if (sector == null) return false;
//
//        Unit unit = sector.getUnitById(unitId);
//        if (unit == null) return false;
//
//        int maxQuantity = firearm.getMaxPerUnit();
//        long currentCount = unit.getEquipments().stream()
//                .filter(e -> e.equals(firearm))
//                .count();
//
//        if (currentCount >= maxQuantity) return false;
//
//        if (getEquipmentInventory().getOrDefault(firearm, 0) > 0) {
//            unit.addEquipment(firearm);
//            getEquipmentInventory().put(firearm, getEquipmentInventory().get(firearm) - 1);
//            return true;
//        }
//        return false;
//    }

    // === CALCULS ET STATISTIQUES ===

    private void updateTotalStats() {
        double totalOffensive = sectors.stream()
                .mapToDouble(sector -> sector.getStat("offensive"))
                .sum();
        double totalDefensive = sectors.stream()
                .mapToDouble(sector -> sector.getStat("defensive"))
                .sum();

        stats.setTotalOffensivePower(totalOffensive);
        stats.setTotalDefensivePower(totalDefensive);
    }

    private void updateGlobalStats() {
        updateTotalStats();
        stats.setGlobalPower((stats.getTotalOffensivePower() + stats.getTotalDefensivePower()) /2);
    }

    private void calculateTotalIncome() {
        stats.setTotalIncome(sectors.stream()
                .mapToDouble(Sector::getIncome)
                .sum());
    }

    private void calculateTotalEconomyPower() {
        double economyPower = stats.getTotalIncome()
                + stats.getTotalEquipmentValue()
                + stats.getMoney()
                + stats.getTotalVehiclesValue();
        stats.setTotalEconomyPower(economyPower);
    }

    // === AFFICHAGE ===

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

    private static final String FORMAT_INT = "%,.0f";
    private static final String FORMAT_FLOAT = "%,.2f";

    public void displayStats() {
        updateGlobalStats();
        calculateTotalIncome();
        setTotalEquipmentValue();
        calculateTotalEconomyPower();


        System.out.printf("=== %s ===%n", name.toUpperCase());

        System.out.println("--- Statistiques Économiques ---");
        System.out.println(formatStat(stats.getMoney(), "$ "));
        System.out.println(formatStat(stats.getTotalIncome(), "revenu quotidien"));
        System.out.println(formatStat(stats.getTotalVehiclesValue(), "valeur des véhicules"));
        System.out.println(formatStat(stats.getTotalEquipmentValue(), "valeur des équipements"));
        System.out.println(formatStat(stats.getTotalEconomyPower(), "puissance économique totale"));
        System.out.println();

        System.out.println("--- Statistiques Militaires ---");
        System.out.println(formatStat(stats.getTotalOffensivePower(), "puissance offensive totale"));
        System.out.println(formatStat(stats.getTotalDefensivePower(), "puissance défensive totale"));
        System.out.println(formatStat(stats.getGlobalPower(), "puissance globale"));
        System.out.println();
    }

    // Méthode utilitaire pour formater chaque statistique
    private String formatStat(double value, String label) {
        String formatted = (value % 1 == 0) ? String.format(FORMAT_INT, value) : String.format(FORMAT_FLOAT, value);
        return formatted + " " + label;
    }
}