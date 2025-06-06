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

        for (UnitDTO unitDto : dto.army) {
            Unit unit = new Unit(unitDto.id, unitDto.type, UnitClass.valueOf(unitDto.classes.get(0)));
            unit.gainExperience(unitDto.experience);

            // Ajout de la seconde classe si présente
            if (unitDto.classes.size() > 1) {
                unit.addSecondClass(UnitClass.valueOf(unitDto.classes.get(1)));
            }

            // Équipements à feu
            for (String firearmName : unitDto.firearms) {
                EquipmentType type = EquipmentType.fromDisplayName(firearmName);
                unit.equipFirearm((FirearmEquipment) EquipmentFactory.createEquipmentByType(type));
            }
            // Équipements défensifs
            for (String defName : unitDto.defensive) {
                EquipmentType type = EquipmentType.fromDisplayName(defName);
                unit.equipDefensive((DefensiveEquipment) EquipmentFactory.createEquipmentByType(type));
            }
            player.addUnit(unit);
        }
        return player;
    }

    // DTO internes pour la désérialisation
    private static class PlayerDTO {
        public String name;
        public List<UnitDTO> army;
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
        public List<String> firearms = new ArrayList<>();
        public List<String> defensive = new ArrayList<>();
    }
}