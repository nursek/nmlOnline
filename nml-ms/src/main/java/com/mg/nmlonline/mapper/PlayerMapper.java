package com.mg.nmlonline.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.dto.PlayerDto;
import com.mg.nmlonline.entity.equipment.EquipmentStack;
import com.mg.nmlonline.entity.player.Player;
import com.mg.nmlonline.entity.player.PlayerEntity;
import com.mg.nmlonline.entity.player.PlayerStats;
import com.mg.nmlonline.entity.sector.Sector;
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

    public Player entityToDomain(PlayerEntity e) {
        Player p = new Player();
        if (e == null) return p;
        p.setName(e.getUsername());

        try {
            if (e.getStats() != null && e.getStats().length > 0) {
                PlayerStats stats = objectMapper.readValue(e.getStats(), PlayerStats.class);
                p.setStats(stats);
            }
        } catch (IOException ex) {
            // log if nécessaire
        }

        try {
            if (e.getEquipments() != null && e.getEquipments().length > 0) {
                List<EquipmentStack> eqs = objectMapper.readValue(e.getEquipments(),
                        new TypeReference<List<EquipmentStack>>() {});
                p.setEquipments(eqs);
            }
        } catch (IOException ex) {
            // log si besoin
        }

        try {
            if (e.getSectors() != null && e.getSectors().length > 0) {
                List<Sector> sectors = objectMapper.readValue(e.getSectors(),
                        new TypeReference<List<Sector>>() {});
                p.setSectors(sectors);
            }
        } catch (IOException ex) {
            // log si besoin
        }

        return p;
    }

    public PlayerEntity domainToEntity(Player domain) {
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
            // log si besoin
        }
        return e;
    }

    public PlayerDto domainToDto(Player p) {
        PlayerDto dto = new PlayerDto();
        if (p == null) return dto;
        dto.id = null; // l'id vient de l'entité si nécessaire
        dto.name = p.getName();
        dto.money = Optional.ofNullable(p.getPlayerStats()).map(PlayerStats::getMoney).orElse(0d);

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
                        .map(eq -> eq.getName()).collect(Collectors.toList());
                return ud;
            }).collect(Collectors.toList());
            return sd;
        }).collect(Collectors.toList());

        return dto;
    }

    public PlayerDto entityToDto(PlayerEntity e) {
        return domainToDto(entityToDomain(e));
    }
}
