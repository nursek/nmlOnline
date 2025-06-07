package com.mg.nmlonline.model.player;

import com.mg.nmlonline.model.equipement.Equipment;
import com.mg.nmlonline.model.unit.Unit;
import com.mg.nmlonline.model.unit.UnitClass;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe représentant un joueur avec son armée d'unités
 */
@Data
@NoArgsConstructor
public class Player {
    private String name;
    private List<Unit> army;
    private List<Equipment> equipments;
    
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
        this.equipments = new ArrayList<>();
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

    // Méthode pour ajouter un équipement à l'armée
    public void addEquipment(Equipment equipment) {
        equipments.add(equipment);
    }

    public void removeEquipment(Equipment equipment) {
        equipments.remove(equipment);
    }

    public void removeEquipment(String equipmentName) {
        equipments.removeIf(equipment -> equipment.getName().equalsIgnoreCase(equipmentName));
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
     * Trie l'armée par expérience décroissante, puis par défense totale décroissante,:
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
            if (attackBonusPercent != 0) System.out.printf("  Attaque : %+.1f%%\n", attackBonusPercent * 100);
            if (defenseBonusPercent != 0) System.out.printf("  Défense : %+.1f%%\n", defenseBonusPercent * 100);
            if (pdfBonusPercent != 0) System.out.printf("  PDF : %+.1f%%\n", pdfBonusPercent * 100);
            if (pdcBonusPercent != 0) System.out.printf("  PDC : %+.1f%%\n", pdcBonusPercent * 100);
            if (armorBonusPercent != 0) System.out.printf("  Armure : %+.1f%%\n", armorBonusPercent * 100);
            if (evasionBonusPercent != 0) System.out.printf("  Esquive : %+.1f%%\n", evasionBonusPercent * 100);
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

    // Méthode pour créer l'armée d'un joueur via un fichier texte
    public void fromFile(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(filePath));
        System.out.println("[fromFile] Lecture du fichier : " + filePath + " (" + lines.size() + " lignes)");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                System.out.println("[fromFile] Ligne vide ignorée.");
                continue;
            }

            System.out.println("[fromFile] Analyse de la ligne : " + line);

            if (line.startsWith("(")) {
                // Extraction des classes
                Matcher classMatcher = Pattern.compile("^((\\([^)]*\\)\\s*)+)").matcher(line);
                List<UnitClass> classes = new ArrayList<>();
                int lastEnd = 0;
                if (classMatcher.find()) {
                    String classesPart = classMatcher.group(1);
                    System.out.println("[fromFile] Partie classes détectée : " + classesPart);
                    Matcher singleClassMatcher = Pattern.compile("\\(([^)]+)\\)").matcher(classesPart);
                    while (singleClassMatcher.find()) {
                        String classCode = singleClassMatcher.group(1);
                        System.out.println("[fromFile] Code classe trouvé : " + classCode);
                        try {
                            classes.add(UnitClass.fromCode(classCode));
                        } catch (Exception e) {
                            System.err.println("[fromFile] Erreur conversion classe : " + classCode + " -> " + e.getMessage());
                        }
                    }
                    lastEnd = classMatcher.end();
                } else {
                    System.err.println("[fromFile] Aucune classe détectée !");
                }

                // Extraction du nom, exp et équipements
                String rest = line.substring(lastEnd).trim();
                System.out.println("[fromFile] Partie restante après classes : " + rest);
                Matcher mainMatcher = Pattern.compile("([\\w\\s\\-éèàêîôûç]+)\\s*\\((\\d+) Exp\\)\\s*:\\s*([^.]*)").matcher(rest);
                if (mainMatcher.find()) {
                    String unitName = mainMatcher.group(1).trim();
                    float exp = Float.parseFloat(mainMatcher.group(2).trim());
                    String equipmentStr = mainMatcher.group(3).trim();
                    System.out.println("[fromFile] Nom unité : " + unitName + " | Exp : " + exp + " | Equipements : " + equipmentStr);

                    // Créer l’unité selon le nom
                    Unit unit = null;
                    if (unitName.startsWith("Larbin")) {
                        System.out.println("[fromFile] Création unité Larbin");
                        unit = new Unit(exp, "Larbin", classes.get(0));
                    } else if (unitName.startsWith("Voyou")) {
                        System.out.println("[fromFile] Création unité Voyou");
                        unit = new Unit(exp, "Voyou", classes.get(0));
                    } else if (unitName.startsWith("Malfrat")) {
                        System.out.println("[fromFile] Création unité Malfrat");
                        unit = new Unit(exp, "Malfrat", classes.get(0));
                    } else if (unitName.startsWith("Brute")) {
                        System.out.println("[fromFile] Création unité brute");
                        unit = new Unit(exp, "Brute", classes.get(0));
                    } else {
                        System.err.println("[fromFile] Type d'unité inconnu : " + unitName);
                        continue;
                    }

                    // Ajouter la classe secondaire si présente
                    if (classes.size() > 1) {
                        System.out.println("[fromFile] Ajout classe secondaire : " + classes.get(1));
                        unit.addSecondClass(classes.get(1));
                    }

                    // Ajouter les équipements si besoin
//                if (!equipmentStr.equalsIgnoreCase("Aucun équipement")) {
//                    List<Equipment> equipments = parseEquipments(equipmentStr);
//                    unit.setEquipments(equipments);
//                }

                    addUnit(unit);
                    System.out.println("[fromFile] Unité ajoutée : " + unit);
                } else {
                    System.err.println("[fromFile] Impossible d'extraire nom/exp/équipements sur : " + rest);
                }
            }
            else{
                System.err.println("[fromFile] Ligne non reconnue dans le fichier: " + line);
            }
        }
    }

    public void displayEquipments() {
        System.out.println("=== ÉQUIPEMENTS DE " + name.toUpperCase() + " ===");
        if (equipments.isEmpty()) {
            System.out.println("Aucun équipement.");
            return;
        }
        for (Equipment equipment : equipments) {
            System.out.println(equipment.getName());
        }
    }

    @Override
    public String toString() {
        return String.format("Joueur: %s (%d unités)", name, getArmySize());
    }
}