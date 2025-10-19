package com.mg.nmlonline.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.api.dto.PlayerDto;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.entity.PlayerEntity;
import com.mg.nmlonline.domain.model.player.PlayerStats;
import com.mg.nmlonline.domain.model.sector.Sector;
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
                        new TypeReference<List<EquipmentStack>>() {
                        });
                p.setEquipments(eqs);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Erreur de conversion des équipements du joueur", ex);
        }

        try {
            if (e.getSectors() != null && e.getSectors().length > 0) {
                List<Sector> sectors = objectMapper.readValue(e.getSectors(),
                        new TypeReference<List<Sector>>() {
                        });
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
        dto.id = null; // l'id vient de l'entité si nécessaire
        dto.name = p.getName();

        // PlayerStats -> PlayerStatsDto
        PlayerStats stats = p.getPlayerStats();
        if (stats == null) {
            dto.stats = null;
        } else {
            PlayerDto.PlayerStatsDto sd = new PlayerDto.PlayerStatsDto();
            sd.money = stats.getMoney();
            sd.totalIncome = stats.getTotalIncome();
            sd.totalVehiclesValue = stats.getTotalVehiclesValue();
            sd.totalEquipmentValue = stats.getTotalEquipmentValue();
            sd.totalOffensivePower = stats.getTotalOffensivePower();
            sd.totalDefensivePower = stats.getTotalDefensivePower();
            sd.globalPower = stats.getGlobalPower();
            sd.totalEconomyPower = stats.getTotalEconomyPower();
            sd.totalAtk = stats.getTotalAtk();
            sd.totalPdf = stats.getTotalPdf();
            sd.totalPdc = stats.getTotalPdc();
            sd.totalDef = stats.getTotalDef();
            sd.totalArmor = stats.getTotalArmor();
            dto.stats = sd;
        }

        dto.equipments = Optional.ofNullable(p.getEquipments()).orElse(List.of()).stream().map(stack -> {
            PlayerDto.EquipmentDto ed = new PlayerDto.EquipmentDto();
            ed.name = (stack.getEquipment() == null) ? null : stack.getEquipment().getName();
            ed.quantity = stack.getQuantity();
            return ed;
        }).collect(Collectors.toList());

        dto.sectors = Optional.ofNullable(p.getSectors()).orElse(List.of()).stream().map(sec -> {
            PlayerDto.SectorDto sd = new PlayerDto.SectorDto();
            sd.number = sec.getNumber();
            sd.name = sec.getName();
            sd.income = sec.getIncome();
            sd.army = Optional.ofNullable(sec.getArmy()).orElse(List.of()).stream().map(u -> {
                PlayerDto.UnitDto ud = new PlayerDto.UnitDto();
                ud.id = u.getId();
                ud.type = (u.getType() == null) ? null : String.valueOf(u.getType());
                ud.experience = u.getExperience();
                ud.equipments = Optional.ofNullable(u.getEquipments()).orElse(List.of()).stream()
                        .map(Equipment::getName).collect(Collectors.toList());
                return ud;
            }).collect(Collectors.toList());
            return sd;
        }).collect(Collectors.toList());

        return dto;
    }

    public Player toDomain(PlayerDto dto) {
        if (dto == null) return null;

        Player p = new Player();
        p.setName(dto.name);

        // player stats : convertir PlayerStatsDto -> PlayerStats
        PlayerStats stats = new PlayerStats();
        if (dto.stats != null) {
            stats.setMoney(dto.stats.money == null ? 0d : dto.stats.money);
            stats.setTotalIncome(dto.stats.totalIncome == null ? 0d : dto.stats.totalIncome);
            stats.setTotalVehiclesValue(dto.stats.totalVehiclesValue == null ? 0d : dto.stats.totalVehiclesValue);
            stats.setTotalEquipmentValue(dto.stats.totalEquipmentValue == null ? 0d : dto.stats.totalEquipmentValue);
            stats.setTotalOffensivePower(dto.stats.totalOffensivePower == null ? 0d : dto.stats.totalOffensivePower);
            stats.setTotalDefensivePower(dto.stats.totalDefensivePower == null ? 0d : dto.stats.totalDefensivePower);
            stats.setGlobalPower(dto.stats.globalPower == null ? 0d : dto.stats.globalPower);
            stats.setTotalEconomyPower(dto.stats.totalEconomyPower == null ? 0d : dto.stats.totalEconomyPower);
            stats.setTotalAtk(dto.stats.totalAtk == null ? 0d : dto.stats.totalAtk);
            stats.setTotalPdf(dto.stats.totalPdf == null ? 0d : dto.stats.totalPdf);
            stats.setTotalPdc(dto.stats.totalPdc == null ? 0d : dto.stats.totalPdc);
            stats.setTotalDef(dto.stats.totalDef == null ? 0d : dto.stats.totalDef);
            stats.setTotalArmor(dto.stats.totalArmor == null ? 0d : dto.stats.totalArmor);
        } else {
            // déjà initialisé avec valeurs par défaut dans PlayerStats
        }
        p.setStats(stats);

        // equipments : EquipmentStack nécessite un Equipment en paramètre (final) -> on passe null et initialise les quantités
        List<EquipmentStack> eqs = Optional.ofNullable(dto.equipments).orElse(List.of()).stream().map(ed -> {
            EquipmentStack stack = new EquipmentStack(null);
            int qty = ed == null || ed.quantity == null ? 0 : ed.quantity;
            stack.setQuantity(qty);
            stack.setAvailable(qty);
            return stack;
        }).collect(Collectors.toList());
        p.setEquipments(eqs);

        // sectors : Sector n'a pas de constructeur vide -> utiliser le constructeur avec le numéro
        List<Sector> sectors = Optional.ofNullable(dto.sectors).orElse(List.of()).stream().map(sd -> {
            int number = sd == null || sd.number == null ? 0 : sd.number;
            Sector sec = new Sector(number);
            if (sd != null && sd.name != null) {
                sec.setName(sd.name);
            }
            if (sd != null && sd.income != null) {
                sec.setIncome(sd.income);
            }
            sec.setArmy(List.of());
            return sec;
        }).collect(Collectors.toList());
        p.setSectors(sectors);

        return p;
    }
}
