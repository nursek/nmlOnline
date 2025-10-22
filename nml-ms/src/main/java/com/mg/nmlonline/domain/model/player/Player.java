package com.mg.nmlonline.domain.model.player;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

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

    public Equipment getEquipmentByString(String name) {
        for (EquipmentStack stack : equipments) {
            if (stack.getEquipment().getName().equalsIgnoreCase(name)) {
                return stack.getEquipment();
            }
        }
        return null;
    }

    public boolean buyEquipment(Equipment equipment, int quantity) {
        if (equipment == null || quantity <=0){
            return false;
        }
        double totalCost = (double) equipment.getCost() * quantity;
        if (stats.getMoney() >= totalCost) {
            stats.setMoney(stats.getMoney() - totalCost);
            addEquipmentToStack(equipment, quantity);
            setTotalEquipmentValue();
            calculateTotalEconomyPower();
            return true;
        }
        return false;
    }

    public void addEquipmentToStack(Equipment equipment, int number) {
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

    public void addEquipmentToStack(Equipment equipment) {
        addEquipmentToStack(equipment, 1);
    }

    public boolean isEquipmentAvailable(Equipment equipment) {
        for (EquipmentStack stack : equipments) {
            if (stack.getEquipment().equals(equipment)) {
                return stack.isAvailable();
            }
        }
        return false;
    }

    public boolean isEquipmentAvailable(String equipmentName) {
        Equipment equipment = getEquipmentByString(equipmentName);
        if (equipment == null) return false;
        return isEquipmentAvailable(equipment);
    }

    public boolean decrementEquipmentAvailability(Equipment equipment) {
        for (EquipmentStack stack : equipments) {
            if (stack.getEquipment().equals(equipment)) {
                stack.decrementAvailable();
                setTotalEquipmentValue();
                calculateTotalEconomyPower();
                return true;
            }
        }
        return false;
    }

    public boolean decrementEquipmentAvailability(String equipmentName) {
        Equipment equipment = getEquipmentByString(equipmentName);
        if (equipment == null) return false;
        return decrementEquipmentAvailability(equipment);
    }

    public void removeEquipmentFromStack(Equipment equipment) {
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

    public void refreshEquipmentAvailability() {
        // Compte le nombre d'exemplaires portés pour chaque équipement
        Map<Equipment, Integer> equippedCount = new HashMap<>();
        for (Unit unit : getAllUnits()) {
            for (Equipment eq : unit.getEquipments()) {
                equippedCount.put(eq, equippedCount.getOrDefault(eq, 0) + 1);
            }
        }
        // Met à jour l'attribut available de chaque stack
        for (EquipmentStack stack : equipments) {
            int total = stack.getQuantity();
            int used = equippedCount.getOrDefault(stack.getEquipment(), 0);
            int available = total - used;
            stack.setAvailable(available);
        }
    }


    // === GESTION DES UNITS DU JOUEUR ===

    public void reassignUnitNumbers() {
        Map<String, Integer> typeCounters = new HashMap<>();
        for (Unit unit : getAllUnits()) {
            String unitType = unit.getType().name();
            int currentCount = typeCounters.getOrDefault(unitType, 0) + 1;
            typeCounters.put(unitType, currentCount);
            unit.setNumber(currentCount);
        }
    }

    public boolean transferUnitBetweenSectors(Unit unit, int fromSectorNumber, int toSectorNumber) {
        Sector fromSector = getSectorByNumber(fromSectorNumber);
        Sector toSector = getSectorByNumber(toSectorNumber);

        if (fromSector != null && toSector != null && fromSector.removeUnit(unit)) {
            toSector.addUnit(unit);
            return true;
        }
        return false;
    }

    /**
     * Retourne la liste des équipements compatibles avec une unité donnée.
     * Un équipement est compatible si :
     * - Il correspond à une classe de l'unité
     * - Il est disponible dans l'inventaire du joueur
     * - L'unité n'a pas atteint la limite pour cette catégorie d'équipement
     *
     * @param unit L'unité pour laquelle vérifier la compatibilité
     * @return Liste des équipements compatibles disponibles
     */
    public List<Equipment> getCompatibleEquipments(Unit unit) {
        if (unit == null) {
            return new ArrayList<>();
        }

        return equipments.stream()
                .filter(stack -> stack.getAvailable() > 0) // Équipement disponible
                .map(EquipmentStack::getEquipment)
                .filter(unit::canEquip) // Compatible et limite non atteinte
                .collect(Collectors.toList());
    }

    /**
     * Retourne les équipements compatibles filtrés par catégorie.
     * Utile pour afficher uniquement les armes, ou uniquement les équipements défensifs.
     *
     * @param unit L'unité pour laquelle vérifier
     * @param category La catégorie d'équipement recherchée (FIREARM, MELEE, DEFENSIVE)
     * @return Liste des équipements compatibles de cette catégorie
     */
    public List<Equipment> getCompatibleEquipmentsByCategory(Unit unit, EquipmentCategory category) {
        if (unit == null || category == null) {
            return new ArrayList<>();
        }

        return equipments.stream()
                .filter(stack -> stack.getAvailable() > 0)
                .map(EquipmentStack::getEquipment)
                .filter(eq -> eq.getCategory() == category)
                .filter(unit::canEquip)
                .collect(Collectors.toList());
    }

    /**
     * Remplace un équipement d'une unité par un nouveau.
     * Si l'unité possède déjà un équipement de la même catégorie et atteint la limite,
     * l'ancien équipement est retiré et rendu à l'inventaire du joueur.
     *
     * @param unit L'unité dont on veut changer l'équipement
     * @param oldEquipment L'équipement à retirer (peut être null si on veut juste équiper)
     * @param newEquipment Le nouvel équipement à ajouter
     * @return true si le remplacement a réussi
     */
    public boolean replaceEquipment(Unit unit, Equipment oldEquipment, Equipment newEquipment) {
        if (unit == null || newEquipment == null) {
            return false;
        }

        // Vérifier que le nouvel équipement est disponible
        if (!isEquipmentAvailable(newEquipment)) {
            System.out.println("Équipement non disponible : " + newEquipment.getName());
            return false;
        }

        // Si un ancien équipement est spécifié, le retirer d'abord
        if (oldEquipment != null) {
            boolean removed = unit.removeEquipment(oldEquipment);
            if (removed) {
                // Rendre l'équipement à l'inventaire
                incrementEquipmentAvailability(oldEquipment);
                System.out.println("Équipement retiré : " + oldEquipment.getName());
            } else {
                System.out.println("Impossible de retirer l'équipement : " + oldEquipment.getName());
                return false;
            }
        }

        // Équiper le nouvel équipement
        boolean equipped = unit.addEquipment(newEquipment);
        if (equipped) {
            decrementEquipmentAvailability(newEquipment);
            setTotalEquipmentValue();
            System.out.println("Nouvel équipement ajouté : " + newEquipment.getName());
            return true;
        } else {
            // Si l'équipement échoue, remettre l'ancien si on l'avait retiré
            if (oldEquipment != null) {
                unit.addEquipment(oldEquipment);
                decrementEquipmentAvailability(oldEquipment);
            }
            System.out.println("Impossible d'équiper : " + newEquipment.getName());
            return false;
        }
    }

    /**
     * Remplace automatiquement un équipement de même catégorie.
     * Trouve automatiquement l'équipement de la même catégorie à remplacer.
     *
     * @param unit L'unité dont on veut changer l'équipement
     * @param newEquipment Le nouvel équipement à ajouter
     * @return true si le remplacement a réussi
     */
    public boolean replaceEquipmentByCategory(Unit unit, Equipment newEquipment) {
        if (unit == null || newEquipment == null) {
            return false;
        }

        EquipmentCategory category = newEquipment.getCategory();

        // Vérifier si l'unité a atteint la limite pour cette catégorie
        long currentCount = unit.countEquipmentsByCategory(category);
        int maxAllowed = switch (category) {
            case FIREARM -> unit.getType().getMaxFirearms();
            case MELEE -> unit.getType().getMaxMeleeWeapons();
            case DEFENSIVE -> unit.getType().getMaxDefensiveEquipment();
        };

        // Si la limite est atteinte, retirer le premier équipement de cette catégorie
        Equipment oldEquipment = null;
        if (currentCount >= maxAllowed) {
            List<Equipment> equipmentsOfCategory = unit.getEquipmentsByCategory(category);
            if (!equipmentsOfCategory.isEmpty()) {
                oldEquipment = equipmentsOfCategory.get(0);
            }
        }

        return replaceEquipment(unit, oldEquipment, newEquipment);
    }

    /**
     * Incrémente la disponibilité d'un équipement dans l'inventaire.
     * Utilisé quand un équipement est retiré d'une unité.
     *
     * @param equipment L'équipement à rendre disponible
     * @return true si l'opération a réussi
     */
    private boolean incrementEquipmentAvailability(Equipment equipment) {
        for (EquipmentStack stack : equipments) {
            if (stack.getEquipment().equals(equipment)) {
                stack.incrementAvailable();
                setTotalEquipmentValue();
                calculateTotalEconomyPower();
                return true;
            }
        }
        return false;
    }

    public boolean equipToUnit(int sectorNumber, int unitId, String equipmentName) {
        Equipment equipment = getEquipmentByString(equipmentName);
        if (equipment == null) return false;
        return equipToUnit(sectorNumber, unitId, equipment);
    }

    public boolean equipToUnit(int sectorNumber, int unitId, Equipment equipment) {
        // Trouver le secteur
        Sector sector = getSectorByNumber(sectorNumber);
        if (sector == null) return false;

        // Trouver l'unité
        Unit unit = sector.getUnitById(unitId);
        System.out.println(unit);
        if (unit == null) return false;

        // Trouver le stack correspondant à l'arme
        for (EquipmentStack stack : equipments) {
            if (stack.getEquipment().equals(equipment) && stack.getAvailable() > 0) {
                // Équiper l'unité
                unit.addEquipment(equipment);
                stack.decrementAvailable();
                setTotalEquipmentValue();
                return true;
            }
        }
        return false;
    }

    // === CALCULS ET STATISTIQUES ===

    public void updateCombatStats(){
        // Met à jour les stats de chaque secteur
        for (Sector sector : sectors) {
            sector.recalculateMilitaryPower();
        }

        double totalAtk = getAllUnits().stream()
                .mapToDouble(Unit::getAttack)
                .sum();
        double totalPdf = getAllUnits().stream()
                .mapToDouble(Unit::getPdf)
                .sum();
        double totalPdc = getAllUnits().stream()
                .mapToDouble(Unit::getPdc)
                .sum();
        double totalDef = getAllUnits().stream()
                .mapToDouble(Unit::getDefense)
                .sum();
        double totalArmor = getAllUnits().stream()
                .mapToDouble(Unit::getArmor)
                .sum();

        stats.setTotalAtk(totalAtk);
        stats.setTotalPdf(totalPdf);
        stats.setTotalPdc(totalPdc);
        stats.setTotalDef(totalDef);
        stats.setTotalArmor(totalArmor);
    }

    private void updateTotalStats() {
        double totalOffensive = sectors.stream()
                .mapToDouble(sector -> sector.getStats().getTotalOffensive())
                .sum();
        double totalDefensive = sectors.stream()
                .mapToDouble(sector -> sector.getStats().getTotalDefensive())
                .sum();

        stats.setTotalOffensivePower(totalOffensive);
        stats.setTotalDefensivePower(totalDefensive);
    }

    private void updateGlobalStats() {
        updateTotalStats();
        stats.setGlobalPower((stats.getTotalOffensivePower() + stats.getTotalDefensivePower()) / 2);
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

    public void recalculateStats() {
        updateCombatStats();
        updateGlobalStats();
        calculateTotalIncome();
        setTotalEquipmentValue();
        calculateTotalEconomyPower();
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


    /**
     * Affiche les équipements du joueur
     * Regroupe les équipements par nom et affiche le nombre de chaque type
     */
    public void displayEquipments() {
        System.out.println("=== ÉQUIPEMENTS DE " + name.toUpperCase() + " ===");
        if (equipments.isEmpty()) {
            System.out.println("Aucun équipement.");
            return;
        }
        for (EquipmentStack stack : equipments) {
            Equipment eq = stack.getEquipment();
            int quantity = stack.getQuantity();
            int available = stack.getAvailable(); // nouvel attribut
            int totalPrice = quantity * eq.getCost();
            System.out.printf("%d x %s (%d disponibles) = %,d $%n", quantity, eq.getName(), available, totalPrice);
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

    public PlayerStats getPlayerStats() {
        return stats;
    }
}
