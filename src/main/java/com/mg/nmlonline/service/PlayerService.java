package com.mg.nmlonline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.model.player.Player;
import com.mg.nmlonline.model.unit.Unit;
import com.mg.nmlonline.model.unit.UnitClass;
import com.mg.nmlonline.model.equipement.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

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
        for (String equipments : dto.equipments) {
            player.addEquipment(EquipmentFactory.createFromName(equipments));
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

    // DTO internes pour la désérialisation
    private static class PlayerDTO {
        public String name;
        public List<UnitDTO> army;
        public List<String> equipments;
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
}