package com.mg.nmlonline.domain.model.sector;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mg.nmlonline.domain.model.board.Resource;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentFactory;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.*;

@Data
@Getter
@Setter
@JsonDeserialize(using = Sector.SectorDeserializer.class)
public class Sector {
    private int number;
    private String name;
    private double income = 2000;
    private List<Unit> army = new ArrayList<>();
    private SectorStats stats = new SectorStats();

    // === NOUVELLES DONNÉES POUR LA CARTE ===
    private Integer ownerPlayerId; // null si secteur neutre/vide
    private String color; // couleur déterminée par l'owner, blanc (#ffffff) par défaut
    private Resource resource; // ressource du secteur (joyaux, or, cigares, etc.)
    private List<Integer> neighbors = new ArrayList<>(); // liste des numéros de secteurs voisins

    public Sector(int number) {
        this.number = number;
        this.name = "Secteur n°" + number;
        this.color = "#ffffff"; // blanc par défaut
    }

    public Sector(int number, String name) {
        this.number = number;
        this.name = name;
        this.color = "#ffffff"; // blanc par défaut
    }

    // === GESTION DES VOISINS ===

    public void addNeighbor(int neighborNumber) {
        if (!neighbors.contains(neighborNumber) && neighborNumber != this.number) {
            neighbors.add(neighborNumber);
        }
    }

    public void removeNeighbor(int neighborNumber) {
        neighbors.remove((Integer) neighborNumber);
    }

    public boolean isNeighbor(int sectorNumber) {
        return neighbors.contains(sectorNumber);
    }

    public List<Integer> getNeighbors() {
        return Collections.unmodifiableList(neighbors);
    }

    // === GESTION OWNER ET COULEUR ===

    public void setOwnerAndColor(Integer playerId, String colorHex) {
        this.ownerPlayerId = playerId;
        this.color = colorHex != null ? colorHex : "#ffffff";
    }

    public boolean isOwnedBy(int playerId) {
        return ownerPlayerId != null && ownerPlayerId == playerId;
    }

    public boolean isNeutral() {
        return ownerPlayerId == null;
    }

    // === GESTION DES STATISTIQUES DU SECTEUR ===

    public void recalculateMilitaryPower(){
        stats.setTotalAtk(army.stream()
                .mapToDouble(Unit::getAttack)
                .sum());
        stats.setTotalPdf(army.stream()
                .mapToDouble(Unit::getPdf)
                .sum());
        stats.setTotalPdc(army.stream()
                .mapToDouble(Unit::getPdc)
                .sum());
        stats.setTotalDef(army.stream()
                .mapToDouble(Unit::getDefense)
                .sum());
        stats.setTotalArmor(army.stream()
                .mapToDouble(Unit::getArmor)
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

    public static class SectorDeserializer extends JsonDeserializer<Sector> {
        @Override
        public Sector deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            int number = node.get("number").asInt();
            String name = node.get("name").asText();

            Sector sector = new Sector(number, name);
            sector.setIncome(node.get("income").asDouble(2000));

            JsonNode armyNode = node.get("army");
            if (armyNode != null && armyNode.isArray()) {
                List<Unit> army = new ArrayList<>();
                for (JsonNode unitNode : armyNode) {
                    Unit unit = deserializeUnit(unitNode);
                    if (unit != null) {
                        army.add(unit);
                    }
                }
                sector.setArmy(army);
            }

            JsonNode statsNode = node.get("stats");
            if (statsNode != null) {
                SectorStats stats = new SectorStats();
                stats.setTotalAtk(statsNode.get("totalAtk").asDouble(0));
                stats.setTotalPdf(statsNode.get("totalPdf").asDouble(0));
                stats.setTotalPdc(statsNode.get("totalPdc").asDouble(0));
                stats.setTotalDef(statsNode.get("totalDef").asDouble(0));
                stats.setTotalArmor(statsNode.get("totalArmor").asDouble(0));
                stats.setTotalOffensive(statsNode.get("totalOffensive").asDouble(0));
                stats.setTotalDefensive(statsNode.get("totalDefensive").asDouble(0));
                stats.setGlobalStats(statsNode.get("globalStats").asDouble(0));
                sector.setStats(stats);
            }

            return sector;
        }

        private Unit deserializeUnit(JsonNode unitNode) {
            int id = unitNode.get("id").asInt();
            String name = unitNode.get("name").asText();
            int number = unitNode.get("number").asInt(0);
            double experience = unitNode.get("experience").asDouble(0);

            JsonNode classesNode = unitNode.get("classes");
            List<UnitClass> classes = new ArrayList<>();
            if (classesNode != null && classesNode.isArray()) {
                for (JsonNode classNode : classesNode) {
                    String className = classNode.asText();
                    try {
                        classes.add(UnitClass.valueOf(className));
                    } catch (IllegalArgumentException ignored) {
                        // Classe inconnue, on l'ignore
                    }
                }
            }

            if (classes.isEmpty()) {
                return null;
            }

            Unit unit = new Unit(experience, name, classes.get(0));
            unit.setId(id);
            unit.setNumber(number);

            for (int i = 1; i < classes.size(); i++) {
                unit.addSecondClass(classes.get(i));
            }

            JsonNode equipmentsNode = unitNode.get("equipments");
            if (equipmentsNode != null && equipmentsNode.isArray()) {
                for (JsonNode eqNode : equipmentsNode) {
                    String eqName = eqNode.get("name").asText();
                    Equipment equipment = EquipmentFactory.createFromName(eqName);
                    if (equipment != null) {
                        unit.addEquipment(equipment);
                    }
                }
            }

            return unit;
        }
    }
}

