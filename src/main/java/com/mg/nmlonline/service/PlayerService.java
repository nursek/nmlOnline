package com.mg.nmlonline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.model.player.Player;
import com.mg.nmlonline.model.unit.Unit;
import com.mg.nmlonline.model.unit.UnitClass;
import com.mg.nmlonline.model.equipement.*;
import com.mg.nmlonline.model.unit.UnitType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Player importPlayerFromJson(String filePath) throws IOException {
        // 1. Désérialisation brute dans un DTO
        PlayerDTO dto = objectMapper.readValue(new File(filePath), PlayerDTO.class);

        Player player = new Player(dto.name);

        // Après la création du Player et l'ajout des unités
        player.applyAttackBonus(dto.attackBonusPercent);
        player.applyDefenseBonus(dto.defenseBonusPercent);
        player.applyPdfBonus(dto.pdfBonusPercent);
        player.applyPdcBonus(dto.pdcBonusPercent);
        player.applyArmorBonus(dto.armorBonusPercent);
        player.applyEvasionBonus(dto.evasionBonusPercent);


        // Équipements généraux
        for (EquipmentDTO equipments : dto.equipments) {
            player.addEquipment(EquipmentFactory.createFromName(equipments.name));
        }

        for (UnitDTO unitDto : dto.army) {
            Unit unit = new Unit(unitDto.id, unitDto.type, UnitClass.valueOf(unitDto.classes.get(0)));
            unit.gainExperience(unitDto.experience);

            // Ajout de la seconde classe si présente
            if (unitDto.classes.size() > 1) {
                unit.addSecondClass(UnitClass.valueOf(unitDto.classes.get(1)));
            }

            // Équipements appliqués à l'unité
            for (String equipment : unitDto.equipments) {
                unit.equip(EquipmentFactory.createFromName(equipment));
            }
            player.addUnit(unit);
        }
        return player;
    }

    // Méthode pour créer l'armée d'un joueur via un fichier texte
    public Player fromFile(String filePath) throws IOException {
        Player player = new Player("Skiriga");
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
                player.addUnit(unit);

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
            player.addUnit(personnage);
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
                        unit.equip(EquipmentFactory.createFromName(trimmed));
                    } catch (Exception e) {
                        System.out.println("Erreur lors de la création de l'équipement : " + e.getMessage());
                    }
                }
            }
        }
    }

    // DTO internes pour la désérialisation
    private static class PlayerDTO {
        public String name;
        public List<UnitDTO> army;
        public List<EquipmentDTO> equipments;

        public double attackBonusPercent;
        public double defenseBonusPercent;
        public double pdfBonusPercent;
        public double pdcBonusPercent;
        public double armorBonusPercent;
        public double evasionBonusPercent;
    }
    private static class UnitDTO {
        public int id;
        public String type;
        public List<String> classes;
        public double experience;
        public List<String> equipments = new ArrayList<>();
    }
    private static class EquipmentDTO {
        public String name;
        public int quantity;
    }
}