package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service pour calculer et mettre à jour les statistiques d'un joueur.
 * Ce service remplace les méthodes de calcul qui étaient auparavant dans Player
 * et qui nécessitaient un accès direct aux secteurs.
 *
 * Avec la nouvelle architecture, Board est la source unique de vérité pour les secteurs,
 * donc ce service prend Board en paramètre pour accéder aux secteurs du joueur.
 */
@Service
public class PlayerStatsService {

    /**
     * Met à jour les statistiques de combat d'un joueur.
     * Recalcule toutes les stats de combat basées sur les unités du joueur.
     *
     * @param player Le joueur dont on veut mettre à jour les stats
     * @param board Le plateau de jeu contenant les secteurs
     */
    public void updateCombatStats(Player player, Board board) {
        if (player == null || board == null) {
            return;
        }

        // Récupérer les secteurs du joueur depuis le board
        List<Sector> playerSectors = board.getSectorsByOwner(player.getId());

        // Mettre à jour les stats de chaque secteur
        for (Sector sector : playerSectors) {
            sector.recalculateMilitaryPower();
        }

        // Calculer les stats totales de combat (unités + bâtiments + personnages)
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

        // Mettre à jour les stats du joueur
        player.getStats().setTotalAtk(totalAtk);
        player.getStats().setTotalPdf(totalPdf);
        player.getStats().setTotalPdc(totalPdc);
        player.getStats().setTotalDef(totalDef);
        player.getStats().setTotalArmor(totalArmor);
    }

    /**
     * Met à jour les statistiques offensives et défensives totales.
     *
     * @param player Le joueur dont on veut mettre à jour les stats
     * @param board Le plateau de jeu contenant les secteurs
     */
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

    /**
     * Met à jour la puissance globale du joueur.
     *
     * @param player Le joueur dont on veut mettre à jour les stats
     * @param board Le plateau de jeu contenant les secteurs
     */
    public void updateGlobalStats(Player player, Board board) {
        if (player == null || board == null) {
            return;
        }

        updateTotalStats(player, board);
        double globalPower = (player.getStats().getTotalOffensivePower()
                            + player.getStats().getTotalDefensivePower()) / 2;
        player.getStats().setGlobalPower(globalPower);
    }

    /**
     * Calcule les revenus totaux du joueur.
     *
     * @param player Le joueur dont on veut calculer les revenus
     * @param board Le plateau de jeu contenant les secteurs
     */
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

    /**
     * Recalcule toutes les statistiques du joueur.
     * Cette méthode est un point d'entrée pratique qui met à jour toutes les stats.
     *
     * @param player Le joueur dont on veut recalculer les stats
     * @param board Le plateau de jeu contenant les secteurs
     */
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

    /**
     * Récupère les secteurs du joueur qui contiennent des unités.
     *
     * @param player Le joueur dont on veut récupérer les secteurs avec armée
     * @param board Le plateau de jeu contenant les secteurs
     * @return Liste des secteurs avec au moins une unité
     */
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

