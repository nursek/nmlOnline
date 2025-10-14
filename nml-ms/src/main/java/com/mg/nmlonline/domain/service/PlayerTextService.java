package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.equipment.EquipmentFactory;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerTextService {
    // === TEXT FILE IMPORT METHOD ===
    public Player fromFile(String filePath) throws IOException {
        Player player = new Player("Placeholder"); //TODO nom du joueur en lecture

        // Création d'un secteur par défaut
        Sector defaultSector = new Sector(1);
        player.addSector(defaultSector);

        List<String> lines = Files.readAllLines(Path.of(filePath));
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("(")) {
                processUnitLine(player, line); // Unité classique
            } else {
                processCharacterLine(player, line); // Personnage spécial
            }
        }
        return player;
    }

    private void processUnitLine(Player player, String line) {
        if (line.startsWith("(")) {
            // Extraction des classes
            Matcher classMatcher = Pattern.compile("^((\\([^)]*\\)\\s*)+)").matcher(line);
            List<UnitClass> classes = new ArrayList<>();
            int lastEnd = 0;

            lastEnd = handleClasses(classMatcher, classes, lastEnd);

            // Extraction du nom, exp et équipements
            String rest = line.substring(lastEnd).trim();
            Matcher mainMatcher = Pattern.compile("([\\w\\s\\-éèàêîôûç]+)\\s*(?:n°\\d+\\s*)?\\((\\d+[.,]?\\d*) Exp\\)\\s*:\\s*(.*)").matcher(rest.replace(',', '.'));
            if (mainMatcher.find()) {
                String unitName = mainMatcher.group(1).trim();
                float exp = Float.parseFloat(mainMatcher.group(2).trim());
                String equipmentStr = mainMatcher.group(3).trim();

                Unit unit = new Unit(exp, unitName, classes.getFirst());

                addSecondClass(classes, unit);
                addEquipmentsToUnit(unit, equipmentStr);

                // Ajout au secteur par défaut au lieu de l'armée du joueur
                player.addUnitToSector(unit, 1);

            } else {
                System.err.println("[DEBUG] Format de ligne non reconnu après extraction des classes : " + rest);
            }
        }
    }

    private void processCharacterLine(Player player, String line) {
        // Exemple de ligne : Mortarion (100 Atk + 100 Pdf + 50 Pdc / 250 Def)
        Matcher m = Pattern.compile("^([\\w\\s\\-éèàêîôûç]+)\\s*\\(([^)]+)\\)").matcher(line);
        if (m.find()) {
            String charName = m.group(1).trim();
            String stats = m.group(2);

            int atk = extractStat(stats, "Atk");
            int pdf = extractStat(stats, "Pdf");
            int pdc = extractStat(stats, "Pdc");
            int def = extractStat(stats, "Def");
            int arm = extractStat(stats, "Arm");
            double esquive = extractStatDouble(stats);

            Unit personnage = new Unit(charName, UnitType.PERSONNAGE, atk, pdf, pdc, def, arm, esquive);

            // Ajout au secteur par défaut au lieu de l'armée du joueur
            player.addUnitToSector(personnage, 1);
        } else {
            System.err.println("[DEBUG] Ligne personnage non reconnue : " + line);
        }
    }

    private static int handleClasses(Matcher classMatcher, List<UnitClass> classes, int lastEnd) {
        if (classMatcher.find()) {
            Matcher singleClassMatcher = Pattern.compile("\\(([^)]+)\\)").matcher(classMatcher.group(1));
            while (singleClassMatcher.find()) {
                try {
                    UnitClass uc = UnitClass.fromCode(singleClassMatcher.group(1));
                    classes.add(uc);
                    System.out.println("[DEBUG] Classe trouvée : " + uc);
                } catch (Exception e) {
                    System.err.println("Classe inconnue : " + singleClassMatcher.group(1));
                }
            }
            lastEnd = classMatcher.end();
        }
        return lastEnd;
    }

    private static void addSecondClass(List<UnitClass> classes, Unit unit) {
        if (classes.size() > 1) {
            for (int i = 1; i < classes.size(); i++) {
                unit.addSecondClass(classes.get(i));
                System.out.println("[DEBUG] Ajout d'une seconde classe : " + classes.get(i));
            }
        }
    }

    private int extractStat(String stats, String key) {
        Matcher m = Pattern.compile("(\\d+)\\s*" + key).matcher(stats);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private double extractStatDouble(String stats) {
        Matcher m = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*" + Pattern.quote("% Esquive")).matcher(stats);
        if (m.find()) {
            return Double.parseDouble(m.group(1).replace(',', '.'));
        }
        return 0.0;
    }

    /**
     * Parse la chaîne d'équipements, ajoute chaque équipement à l'unité, et retourne la liste des noms.
     */
    private void addEquipmentsToUnit(Unit unit, String equipmentStr) {
        String[] statKeywords = {"Atk", "Pdf", "Pdc", "Def", "Arm", "Esquive"};
        for (String keyword : statKeywords) {
            int idx = equipmentStr.indexOf(keyword);
            if (idx != -1) {
                equipmentStr = equipmentStr.substring(0, idx).trim();
                break;
            }
        }

        if (!equipmentStr.equalsIgnoreCase("Aucun équipement")) {
            String[] equipmentArray = equipmentStr.split("\\.");
            for (String eq : equipmentArray) {
                String trimmed = eq.trim();
                // Ne garder que les éléments contenant au moins une lettre
                if (!trimmed.isEmpty() && trimmed.matches(".*[a-zA-ZéèàêîôûçÉÈÀÊÎÔÛÇ].*")) {
                    try {
                        unit.addEquipment(EquipmentFactory.createFromName(trimmed));
                    } catch (Exception e) {
                        System.out.println("Erreur lors de la création de l'équipement : " + e.getMessage());
                    }
                }
            }
        }
    }
}
