package com.mg.nmlonline.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.api.dto.*;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.entity.PlayerEntity;
import com.mg.nmlonline.domain.model.player.PlayerStats;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.UnitType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PlayerMapper {

    private final ObjectMapper objectMapper;

    public PlayerMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Player toDomain(PlayerEntity e) {
        Player p = new Player();
        if (e == null) return p;
        p.setName(e.getUsername());

        try {
            if (e.getStats() != null && e.getStats().length > 0) {
                PlayerStats stats = objectMapper.readValue(e.getStats(), PlayerStats.class);
                p.setStats(stats);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Erreur de conversion des stats du joueur", ex);
        }

        try {
            if (e.getEquipments() != null && e.getEquipments().length > 0) {
                List<EquipmentStack> eqs = objectMapper.readValue(e.getEquipments(),
                        new TypeReference<List<EquipmentStack>>() {});
                p.setEquipments(eqs);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Erreur de conversion des équipements du joueur", ex);
        }

        try {
            if (e.getSectors() != null && e.getSectors().length > 0) {
                List<Sector> sectors = objectMapper.readValue(e.getSectors(),
                        new TypeReference<List<Sector>>() {});
                p.setSectors(sectors);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Erreur de conversion des secteurs du joueur", ex);
        }

        return p;
    }

    public PlayerEntity toEntity(Player domain) {
        PlayerEntity e = new PlayerEntity();
        e.setUsername(domain.getName());
        try {
            if (domain.getPlayerStats() != null) {
                e.setStats(objectMapper.writeValueAsBytes(domain.getPlayerStats()));
            }
            if (domain.getEquipments() != null) {
                e.setEquipments(objectMapper.writeValueAsBytes(domain.getEquipments()));
            }
            if (domain.getSectors() != null) {
                e.setSectors(objectMapper.writeValueAsBytes(domain.getSectors()));
            }
        } catch (IOException ex) {
            throw new RuntimeException("Erreur de conversion vers l'entité joueur", ex);
        }
        return e;
    }

    public PlayerDto toDto(Player p) {
        PlayerDto dto = new PlayerDto();
        if (p == null) return dto;
        dto.setId(null); // l'id vient de l'entité si nécessaire
        dto.setName(p.getName());

        // PlayerStats -> PlayerStatsDto
        PlayerStats stats = p.getPlayerStats();
        if (stats == null) {
            dto.setStats(null);
        } else {
            PlayerStatsDto sd = new PlayerStatsDto();
            sd.setMoney(stats.getMoney());
            sd.setTotalIncome(stats.getTotalIncome());
            sd.setTotalVehiclesValue(stats.getTotalVehiclesValue());
            sd.setTotalEquipmentValue(stats.getTotalEquipmentValue());
            sd.setTotalOffensivePower(stats.getTotalOffensivePower());
            sd.setTotalDefensivePower(stats.getTotalDefensivePower());
            sd.setGlobalPower(stats.getGlobalPower());
            sd.setTotalEconomyPower(stats.getTotalEconomyPower());
            sd.setTotalAtk(stats.getTotalAtk());
            sd.setTotalPdf(stats.getTotalPdf());
            sd.setTotalPdc(stats.getTotalPdc());
            sd.setTotalDef(stats.getTotalDef());
            sd.setTotalArmor(stats.getTotalArmor());
            dto.setStats(sd);
        }

        // Equipments
        dto.setEquipments(Optional.ofNullable(p.getEquipments()).orElse(List.of()).stream().map(stack -> {
            EquipmentStackDto esd = new EquipmentStackDto();
            Equipment equipment = stack.getEquipment();
            if (equipment != null) {
                EquipmentDto ed = new EquipmentDto();
                ed.setName(equipment.getName());
                ed.setCost(equipment.getCost());
                ed.setPdfBonus(equipment.getPdfBonus());
                ed.setPdcBonus(equipment.getPdcBonus());
                ed.setArmBonus(equipment.getArmBonus());
                ed.setEvasionBonus(equipment.getEvasionBonus());
                // compatibleClass et category à compléter si besoin
                esd.setEquipment(ed);
            }
            esd.setQuantity(stack.getQuantity());
            esd.setAvailable(stack.getAvailable());
            return esd;
        }).collect(Collectors.toList()));


        // Sectors
        dto.setSectors(Optional.ofNullable(p.getSectors()).orElse(List.of()).stream().map(sec -> {
            SectorDto sd = new SectorDto();
            sd.setNumber(sec.getNumber());
            sd.setName(sec.getName());
            sd.setIncome(sec.getIncome());

            // Army
            sd.setArmy(Optional.ofNullable(sec.getArmy()).orElse(List.of()).stream().map(u -> {
                UnitDto ud = new UnitDto();
                ud.setId(u.getId());
                ud.setName(u.getName());
                ud.setNumber(u.getNumber());
                ud.setExperience(u.getExperience());

                // UnitType
                UnitType type = u.getType();
                if (type != null) {
                    UnitTypeDto utd = new UnitTypeDto();
                    utd.setName(type.name());
                    utd.setLevel(type.getLevel());
                    utd.setMinExp(type.getMinExp());
                    utd.setMaxExp(type.getMaxExp());
                    utd.setBaseAttack(type.getBaseAttack());
                    utd.setBaseDefense(type.getBaseDefense());
                    utd.setMaxFirearms(type.getMaxFirearms());
                    utd.setMaxMeleeWeapons(type.getMaxMeleeWeapons());
                    utd.setMaxDefensiveEquipment(type.getMaxDefensiveEquipment());
                    ud.setType(utd);
                }

                // UnitClasses
                ud.setClasses(Optional.ofNullable(u.getClasses()).orElse(List.of()).stream().map(cls -> {
                    UnitClassDto ucd = new UnitClassDto();
                    ucd.setName(cls.name());
                    ucd.setCode(cls.getCode());
                    // autres champs à compléter si besoin
                    return ucd;
                }).collect(Collectors.toList()));

                ud.setIsInjured(u.isInjured());

                // Equipments
                ud.setEquipments(Optional.ofNullable(u.getEquipments()).orElse(List.of()).stream().map(eq -> {
                    EquipmentDto eqd = new EquipmentDto();
                    eqd.setName(eq.getName());
                    eqd.setCost(eq.getCost());
                    eqd.setPdfBonus(eq.getPdfBonus());
                    eqd.setPdcBonus(eq.getPdcBonus());
                    eqd.setArmBonus(eq.getArmBonus());
                    eqd.setEvasionBonus(eq.getEvasionBonus());
                    // compatibleClass et category à compléter si besoin
                    return eqd;
                }).collect(Collectors.toList()));

                // Stats
                ud.setAttack(u.getAttack());
                ud.setDefense(u.getDefense());
                ud.setPdf(u.getPdf());
                ud.setPdc(u.getPdc());
                ud.setArmor(u.getArmor());
                ud.setEvasion(u.getEvasion());
                ud.setTotalAttack(u.getTotalAttack());
                ud.setTotalDefense(u.getTotalDefense());

                return ud;
            }).collect(Collectors.toList()));

            // SectorStats
            if (sec.getStats() != null) {
                SectorStatsDto ssd = new SectorStatsDto();
                ssd.setTotalAtk(sec.getStats().getTotalAtk());
                ssd.setTotalPdf(sec.getStats().getTotalPdf());
                ssd.setTotalPdc(sec.getStats().getTotalPdc());
                ssd.setTotalDef(sec.getStats().getTotalDef());
                ssd.setTotalArmor(sec.getStats().getTotalArmor());
                ssd.setTotalOffensive(sec.getStats().getTotalOffensive());
                ssd.setTotalDefensive(sec.getStats().getTotalDefensive());
                ssd.setGlobalStats(sec.getStats().getGlobalStats());
                sd.setStats(ssd);
            }

            return sd;
        }).collect(Collectors.toList()));

        return dto;
    }

    public Player toDomain(PlayerDto dto) {
        if (dto == null) return null;

        Player p = new Player();
        p.setName(dto.getName());

        // player stats : convertir PlayerStatsDto -> PlayerStats
        PlayerStats stats = new PlayerStats();
        PlayerStatsDto psd = dto.getStats();
        if (psd != null) {
            stats.setMoney(psd.getMoney() == null ? 0d : psd.getMoney());
            stats.setTotalIncome(psd.getTotalIncome() == null ? 0d : psd.getTotalIncome());
            stats.setTotalVehiclesValue(psd.getTotalVehiclesValue() == null ? 0d : psd.getTotalVehiclesValue());
            stats.setTotalEquipmentValue(psd.getTotalEquipmentValue() == null ? 0d : psd.getTotalEquipmentValue());
            stats.setTotalOffensivePower(psd.getTotalOffensivePower() == null ? 0d : psd.getTotalOffensivePower());
            stats.setTotalDefensivePower(psd.getTotalDefensivePower() == null ? 0d : psd.getTotalDefensivePower());
            stats.setGlobalPower(psd.getGlobalPower() == null ? 0d : psd.getGlobalPower());
            stats.setTotalEconomyPower(psd.getTotalEconomyPower() == null ? 0d : psd.getTotalEconomyPower());
            stats.setTotalAtk(psd.getTotalAtk() == null ? 0d : psd.getTotalAtk());
            stats.setTotalPdf(psd.getTotalPdf() == null ? 0d : psd.getTotalPdf());
            stats.setTotalPdc(psd.getTotalPdc() == null ? 0d : psd.getTotalPdc());
            stats.setTotalDef(psd.getTotalDef() == null ? 0d : psd.getTotalDef());
            stats.setTotalArmor(psd.getTotalArmor() == null ? 0d : psd.getTotalArmor());
        }
        p.setStats(stats);

        // Equipments : EquipmentStack nécessite un Equipment en paramètre (final) -> on passe null et initialise les quantités
        List<EquipmentStack> eqs = Optional.ofNullable(dto.getEquipments()).orElse(List.of()).stream().map(ed -> {
            EquipmentStack stack = new EquipmentStack(null);
            // quantité non présente dans EquipmentDto, à adapter si besoin
            stack.setQuantity(0);
            stack.setAvailable(0);
            return stack;
        }).collect(Collectors.toList());
        p.setEquipments(eqs);

        // Sectors
        List<Sector> sectors = Optional.ofNullable(dto.getSectors()).orElse(List.of()).stream().map(sd -> {
            int number = sd.getNumber() == null ? 0 : sd.getNumber();
            Sector sec = new Sector(number);
            sec.setName(sd.getName());
            sec.setIncome(sd.getIncome());
            // Army et stats à compléter si besoin
            sec.setArmy(List.of());
            return sec;
        }).collect(Collectors.toList());
        p.setSectors(sectors);

        return p;
    }
}
