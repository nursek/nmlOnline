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
            recalculateStats();
        }
    }

    public void removeSector(Sector sector) {
        if (sectors.remove(sector)) {
            recalculateStats();
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
    // TODO : Utilisé pour afficher "Troupes disponibles : 1 brute (10 Exp), 2 soldats (5 Exp)."
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

    public void setTotalEquipmentValue() {
        stats.setTotalEquipmentValue(equipments.stream()
                .mapToDouble(stack -> stack.getEquipment().getCost() * stack.getQuantity())
                .sum());
    }

    // === CALCULS ET STATISTIQUES ===

    private void recalculateStats() {
        stats.setTotalMilitaryPower(calculateTotalMilitaryPower());
    }

    private void calculateTotalIncome() {
        stats.setTotalIncome(sectors.stream()
                .mapToDouble(Sector::getIncome)
                .sum());
    }

    private double calculateTotalSectorsAtk(){
        return sectors.stream()
                .mapToDouble(Sector::getTotalAtk)
                .sum();
    }

    private double calculateTotalSectorsPdf(){
        return sectors.stream()
                .mapToDouble(Sector::getTotalPdf)
                .sum();
    }

    private double calculateTotalSectorsPdc(){
        return sectors.stream()
                .mapToDouble(Sector::getTotalPdc)
                .sum();
    }

    private double calculateTotalSectorsDef(){
        return sectors.stream()
                .mapToDouble(Sector::getTotalDef)
                .sum();
    }

    private double calculateTotalSectorsArmor(){
        return sectors.stream()
                .mapToDouble(Sector::getTotalArmor)
                .sum();
    }

    private double calculateTotalMilitaryPower() {
        double offensivePower = sectors.stream()
                .mapToDouble(Sector::getOffensivePower)
                .sum();
        double defensivePower = sectors.stream()
                .mapToDouble(Sector::getDefensivePower)
                .sum();

        return (offensivePower + defensivePower) / 2;
    }

    private void calculateTotalEconomyPower() {
        double economyPower = stats.getTotalIncome()
                + stats.getTotalEquipmentValue()
                + stats.getMoney()
                + stats.getTotalVehiclesValue();
        stats.setTotalEconomyPower(economyPower);
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

    private static final String FORMAT_INT = "%,.0f";
    private static final String FORMAT_FLOAT = "%,.2f";

    public void displayStats() {
        // C'est le bordel mais trql
        calculateTotalIncome();
        calculateTotalEconomyPower();
        recalculateStats();

        double totalAtk = calculateTotalSectorsAtk();
        double totalPdf = calculateTotalSectorsPdf();
        double totalPdc = calculateTotalSectorsPdc();
        double totalDef = calculateTotalSectorsDef();
        double totalArmor = calculateTotalSectorsArmor();

        List<String> offensiveStats = new ArrayList<>();
        if (totalAtk != 0) offensiveStats.add(formatStat(totalAtk, "Atk"));
        if (totalPdf != 0) offensiveStats.add(formatStat(totalPdf, "Pdf"));
        if (totalPdc != 0) offensiveStats.add(formatStat(totalPdc, "Pdc"));

        List<String> defensiveStats = new ArrayList<>();
        if (totalDef != 0) defensiveStats.add(formatStat(totalDef, "Def"));
        if (totalArmor != 0) defensiveStats.add(formatStat(totalArmor, "Arm"));

        StringBuilder sb = new StringBuilder("Puissance militaire totale -> ");
        sb.append(String.join(" + ", offensiveStats));
        if (!offensiveStats.isEmpty() && !defensiveStats.isEmpty()) sb.append(" / ");
        sb.append(String.join(" + ", defensiveStats));
        sb.append(".");

        System.out.println(sb);

        double money = stats.getMoney();
        double income = stats.getTotalIncome();
        double equipment = stats.getTotalEquipmentValue();
        double economy = stats.getTotalEconomyPower();
        double vehicles = stats.getTotalVehiclesValue();

        String moneyStr = (money % 1 == 0) ? String.format(FORMAT_INT, money) : String.format(FORMAT_FLOAT, money);
        String incomeStr = (income % 1 == 0) ? String.format(FORMAT_INT, income) : String.format(FORMAT_FLOAT, income);
        String equipmentStr = (equipment % 1 == 0) ? String.format(FORMAT_INT, equipment) : String.format(FORMAT_FLOAT, equipment);
        String economyStr = (economy % 1 == 0) ? String.format(FORMAT_INT, economy) : String.format(FORMAT_FLOAT, economy);
        String vehiclesStr = (vehicles % 1 == 0) ? String.format(FORMAT_INT, vehicles) : String.format(FORMAT_FLOAT, vehicles);

        System.out.printf("Puissance militaire globale -> %,.2f%n", stats.getTotalMilitaryPower());
        System.out.printf("Puissance économique -> %s $ (compte en banque) + %s $ (revenu quotidien) + %s $ (équipements) + %s $ (véhicules) = %s $.%n",
                moneyStr, incomeStr, equipmentStr, vehiclesStr, economyStr);
    }

    // Méthode utilitaire pour formater chaque statistique
    private String formatStat(double value, String label) {
        String formatted = (value % 1 == 0) ? String.format(FORMAT_INT, value) : String.format(FORMAT_FLOAT, value);
        return formatted + " " + label;
    }
}