package com.mg.nmlonline.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.api.dto.*;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.SectorStats;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.entity.PlayerEntity;
import com.mg.nmlonline.domain.model.player.PlayerStats;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.UnitType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
        dto.setId(null);
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

        // List<EquipmentStack> -> List<EquipmentStackDto>
        try {
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
                    esd.setEquipment(ed);
                }
                esd.setQuantity(stack.getQuantity());
                esd.setAvailable(stack.getAvailable());
                return esd;
            }).collect(Collectors.toList()));
        } catch (Exception ex) {
            throw new RuntimeException("Erreur de conversion des équipements du joueur", ex);
        }

        // List<Sector> -> List<SectorDto>
        try {
            dto.setSectors(Optional.ofNullable(p.getSectors()).orElse(List.of()).stream().map(sec -> {
                SectorDto sd = new SectorDto();
                sd.setNumber(sec.getNumber());
                sd.setName(sec.getName());
                sd.setIncome(sec.getIncome());

                // List<Unit> -> List<UnitDto>
                sd.setArmy(Optional.ofNullable(sec.getArmy()).orElse(List.of()).stream().map(u -> {
                    UnitDto ud = new UnitDto();
                    ud.setId(u.getId());
                    ud.setName(u.getName());
                    ud.setNumber(u.getNumber());
                    ud.setExperience(u.getExperience());

                    // UnitType -> UnitTypeDto
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

                    // List<UnitClass> -> List<UnitClassDto>
                    ud.setClasses(Optional.ofNullable(u.getClasses()).orElse(List.of()).stream().map(cls -> {
                        UnitClassDto ucd = new UnitClassDto();
                        ucd.setName(cls.name());
                        ucd.setCode(cls.getCode());
                        return ucd;
                    }).collect(Collectors.toList()));

                    ud.setIsInjured(u.isInjured());

                    // List<Equipment> -> List<EquipmentDto>
                    ud.setEquipments(Optional.ofNullable(u.getEquipments()).orElse(List.of()).stream().map(eq -> {
                        EquipmentDto eqd = new EquipmentDto();
                        eqd.setName(eq.getName());
                        eqd.setCost(eq.getCost());
                        eqd.setPdfBonus(eq.getPdfBonus());
                        eqd.setPdcBonus(eq.getPdcBonus());
                        eqd.setArmBonus(eq.getArmBonus());
                        eqd.setEvasionBonus(eq.getEvasionBonus());
                        return eqd;
                    }).collect(Collectors.toList()));

                    // Stats
                    ud.setAttack(u.getAttack());
                    ud.setDefense(u.getDefense());
                    ud.setPdf(u.getPdf());
                    ud.setPdc(u.getPdc());
                    ud.setArmor(u.getArmor());
                    ud.setEvasion(u.getEvasion());

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
        } catch (Exception ex) {
            throw new RuntimeException("Erreur de conversion des secteurs du joueur", ex);
        }

        return dto;
    }

    public Player toDomain(PlayerDto dto) {
        if (dto == null) return null;

        Player p = new Player();
        p.setName(dto.getName());

        // PlayerStatsDto -> PlayerStats
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

        // List<EquipmentStackDto> -> List<EquipmentStack>
        try {
            List<EquipmentStack> eqs = Optional.ofNullable(dto.getEquipments()).orElse(List.of()).stream().map(ed -> {
                EquipmentStack stack = new EquipmentStack(null);
                stack.setQuantity(0);
                stack.setAvailable(0);
                return stack;
            }).collect(Collectors.toList());
            p.setEquipments(eqs);
        } catch (Exception ex) {
            throw new RuntimeException("Erreur de conversion des équipements du joueur (DTO -> Domain)", ex);
        }

        // List<SectorDto> -> List<Sector>
        try {
            List<Sector> sectors = Optional.ofNullable(dto.getSectors()).orElse(List.of()).stream().map(sd -> {
                int number = sd.getNumber() == null ? 0 : sd.getNumber();
                Sector sec = new Sector(number);
                sec.setName(sd.getName());
                sec.setIncome(sd.getIncome());

                // List<UnitDto> -> List<Unit>
                List<Unit> army = Optional.ofNullable(sd.getArmy()).orElse(List.of()).stream().map(ud -> {
                    Unit u = new Unit();
                    u.setId(ud.getId());
                    u.setName(ud.getName());
                    u.setNumber(ud.getNumber());
                    u.setExperience(ud.getExperience());

                    // UnitTypeDto -> UnitType (à adapter selon ton enum ou factory)
                    if (ud.getType() != null && ud.getType().getName() != null) {
                        u.setType(UnitType.valueOf(ud.getType().getName()));
                    }

                    // List<UnitClassDto> -> List<UnitClass>
                    if (ud.getClasses() != null) {
                        List<UnitClass> classes = ud.getClasses().stream()
                                .filter(c -> c.getCode() != null)
                                .map(c -> {
                                    for (UnitClass uc : UnitClass.values()) {
                                        if (uc.getCode().equals(c.getCode())) return uc;
                                    }
                                    return null;
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        u.setClasses(classes);
                    }

                    // List<EquipmentDto> -> List<Equipment>
                    if (ud.getEquipments() != null) {
                        List<Equipment> eqs = ud.getEquipments().stream()
                                .map(eqd -> {
                                    Set<UnitClass> comp = Optional.ofNullable(eqd.getCompatibleClass())
                                            .orElse(Set.of())
                                            .stream()
                                            .map(code -> {
                                                for (UnitClass uc : UnitClass.values()) {
                                                    if (uc.getCode().equals(code.getCode())) return uc;
                                                }
                                                return null;
                                            })
                                            .filter(obj -> false)
                                            .collect(Collectors.toSet());
                                    return new Equipment(eqd.getName(),
                                            (int) Math.round(eqd.getCost()),
                                            eqd.getPdfBonus(),
                                            eqd.getPdcBonus(),
                                            eqd.getArmBonus(),
                                            eqd.getEvasionBonus(),
                                            comp,
                                            null);
                                })
                                .collect(Collectors.toList());
                        u.setEquipments(eqs);
                    }

                    u.setInjured(Boolean.TRUE.equals(ud.getIsInjured()));

                    // Stats
                    u.setAttack(ud.getAttack());
                    u.setDefense(ud.getDefense());
                    u.setPdf(ud.getPdf());
                    u.setPdc(ud.getPdc());
                    u.setArmor(ud.getArmor());
                    u.setEvasion(ud.getEvasion());

                    return u;
                }).collect(Collectors.toList());
                sec.setArmy(army);

                // SectorStatsDto -> SectorStats (à adapter si besoin)
                if (sd.getStats() != null) {
                    SectorStats sectorStats = new SectorStats();
                    sectorStats.setTotalAtk(sd.getStats().getTotalAtk());
                    sectorStats.setTotalPdf(sd.getStats().getTotalPdf());
                    sectorStats.setTotalPdc(sd.getStats().getTotalPdc());
                    sectorStats.setTotalDef(sd.getStats().getTotalDef());
                    sectorStats.setTotalArmor(sd.getStats().getTotalArmor());
                    sectorStats.setTotalOffensive(sd.getStats().getTotalOffensive());
                    sectorStats.setTotalDefensive(sd.getStats().getTotalDefensive());
                    sectorStats.setGlobalStats(sd.getStats().getGlobalStats());
                    sec.setStats(sectorStats);
                }

                return sec;
            }).collect(Collectors.toList());
            p.setSectors(sectors);
        } catch (Exception ex) {
            throw new RuntimeException("Erreur de conversion des secteurs du joueur (DTO -> Domain)", ex);
        }


        return p;
    }
}
