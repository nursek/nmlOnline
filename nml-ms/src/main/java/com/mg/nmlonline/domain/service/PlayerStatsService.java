package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerStatsService {

    public void updateCombatStats(Player player, Board board) {
        if (player == null || board == null) {
            return;
        }

        List<Sector> playerSectors = board.getSectorsByOwner(player.getId());

        for (Sector sector : playerSectors) {
            sector.recalculateMilitaryPower();
        }

        double totalAtk = playerSectors.stream()
                .flatMap(sector -> sector.getCombatEntities().stream())
                .mapToDouble(CombatEntity::getAttack)
                .sum();

        double totalPdf = playerSectors.stream()
                .flatMap(sector -> sector.getCombatEntities().stream())
                .mapToDouble(CombatEntity::getPdf)
                .sum();

        double totalPdc = playerSectors.stream()
                .flatMap(sector -> sector.getCombatEntities().stream())
                .mapToDouble(CombatEntity::getPdc)
                .sum();

        double totalDef = playerSectors.stream()
                .flatMap(sector -> sector.getCombatEntities().stream())
                .mapToDouble(CombatEntity::getDefense)
                .sum();

        double totalArmor = playerSectors.stream()
                .flatMap(sector -> sector.getCombatEntities().stream())
                .mapToDouble(CombatEntity::getArmor)
                .sum();

        player.getStats().setTotalAtk(totalAtk);
        player.getStats().setTotalPdf(totalPdf);
        player.getStats().setTotalPdc(totalPdc);
        player.getStats().setTotalDef(totalDef);
        player.getStats().setTotalArmor(totalArmor);
    }

    public void updateTotalStats(Player player, Board board) {
        if (player == null || board == null) {
            return;
        }

        List<Sector> playerSectors = board.getSectorsByOwner(player.getId());

        double totalOffensive = playerSectors.stream()
                .mapToDouble(sector -> sector.getStats().getTotalOffensive())
                .sum();

        double totalDefensive = playerSectors.stream()
                .mapToDouble(sector -> sector.getStats().getTotalDefensive())
                .sum();

        player.getStats().setTotalOffensivePower(totalOffensive);
        player.getStats().setTotalDefensivePower(totalDefensive);
    }

    public void updateGlobalStats(Player player, Board board) {
        if (player == null || board == null) {
            return;
        }

        updateTotalStats(player, board);
        double globalPower = (player.getStats().getTotalOffensivePower()
                            + player.getStats().getTotalDefensivePower()) / 2;
        player.getStats().setGlobalPower(globalPower);
    }

    public void calculateTotalIncome(Player player, Board board) {
        if (player == null || board == null) {
            return;
        }

        List<Sector> playerSectors = board.getSectorsByOwner(player.getId());

        double totalIncome = playerSectors.stream()
                .mapToDouble(Sector::getIncome)
                .sum();

        player.getStats().setTotalIncome(totalIncome);
    }

    public void recalculateStats(Player player, Board board) {
        if (player == null || board == null) {
            return;
        }

        updateCombatStats(player, board);
        updateGlobalStats(player, board);
        calculateTotalIncome(player, board);
        player.setTotalEquipmentValue();
        player.calculateTotalEconomyPower();
    }

    public List<Sector> getSectorsWithCombatEntities(Player player, Board board) {
        if (player == null || board == null) {
            return List.of();
        }

        List<Sector> playerSectors = board.getSectorsByOwner(player.getId());
        return playerSectors.stream()
                .filter(sector -> !sector.getCombatEntities().isEmpty())
                .toList();
    }
}

