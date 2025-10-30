package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // Calculer les stats totales de combat
        double totalAtk = playerSectors.stream()
                .flatMap(sector -> sector.getUnits().stream())
                .mapToDouble(Unit::getAttack)
                .sum();

        double totalPdf = playerSectors.stream()
                .flatMap(sector -> sector.getUnits().stream())
                .mapToDouble(Unit::getPdf)
                .sum();

        double totalPdc = playerSectors.stream()
                .flatMap(sector -> sector.getUnits().stream())
                .mapToDouble(Unit::getPdc)
                .sum();

        double totalDef = playerSectors.stream()
                .flatMap(sector -> sector.getUnits().stream())
                .mapToDouble(Unit::getDefense)
                .sum();

        double totalArmor = playerSectors.stream()
                .flatMap(sector -> sector.getUnits().stream())
                .mapToDouble(Unit::getArmor)
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
     * Rafraîchit la disponibilité des équipements dans l'inventaire du joueur.
     * Compare le nombre total d'équipements avec ceux actuellement équipés par les unités.
     *
     * @param player Le joueur dont on veut rafraîchir l'inventaire
     * @param board Le plateau de jeu contenant les secteurs
     */
    public void refreshEquipmentAvailability(Player player, Board board) {
        if (player == null || board == null) {
            return;
        }

        // Récupérer tous les secteurs du joueur
        List<Sector> playerSectors = board.getSectorsByOwner(player.getId());

        // Compter le nombre d'exemplaires portés pour chaque équipement
        Map<Equipment, Integer> equippedCount = new HashMap<>();
        for (Sector sector : playerSectors) {
            for (Unit unit : sector.getUnits()) {
                for (Equipment eq : unit.getEquipments()) {
                    equippedCount.put(eq, equippedCount.getOrDefault(eq, 0) + 1);
                }
            }
        }

        // Mettre à jour l'attribut available de chaque stack
        for (EquipmentStack stack : player.getEquipments()) {
            int total = stack.getQuantity();
            int used = equippedCount.getOrDefault(stack.getEquipment(), 0);
            int available = total - used;
            stack.setAvailable(available);
        }
    }

    /**
     * Réassigne les numéros d'unités pour un joueur.
     * Parcourt toutes les unités du joueur et leur assigne un numéro séquentiel par type.
     *
     * @param player Le joueur dont on veut réassigner les numéros d'unités
     * @param board Le plateau de jeu contenant les secteurs
     */
    public void reassignUnitNumbers(Player player, Board board) {
        if (player == null || board == null) {
            return;
        }

        List<Sector> playerSectors = board.getSectorsByOwner(player.getId());
        Map<String, Integer> typeCounters = new HashMap<>();

        for (Sector sector : playerSectors) {
            for (Unit unit : sector.getUnits()) {
                String unitType = unit.getType().name();
                int currentCount = typeCounters.getOrDefault(unitType, 0) + 1;
                typeCounters.put(unitType, currentCount);
                unit.setNumber(currentCount);
            }
        }
    }

    /**
     * Récupère toutes les unités d'un joueur sur tout le plateau.
     *
     * @param player Le joueur dont on veut récupérer les unités
     * @param board Le plateau de jeu contenant les secteurs
     * @return Liste de toutes les unités du joueur
     */
    public List<Unit> getAllPlayerUnits(Player player, Board board) {
        if (player == null || board == null) {
            return List.of();
        }

        List<Sector> playerSectors = board.getSectorsByOwner(player.getId());
        return playerSectors.stream()
                .flatMap(sector -> sector.getUnits().stream())
                .toList();
    }

    /**
     * Compte le nombre total d'unités d'un joueur.
     *
     * @param player Le joueur dont on veut compter les unités
     * @param board Le plateau de jeu contenant les secteurs
     * @return Le nombre total d'unités
     */
    public int getTotalArmySize(Player player, Board board) {
        if (player == null || board == null) {
            return 0;
        }

        List<Sector> playerSectors = board.getSectorsByOwner(player.getId());
        return playerSectors.stream()
                .mapToInt(Sector::getArmySize)
                .sum();
    }

    /**
     * Récupère les secteurs du joueur qui contiennent des unités.
     *
     * @param player Le joueur dont on veut récupérer les secteurs avec armée
     * @param board Le plateau de jeu contenant les secteurs
     * @return Liste des secteurs avec au moins une unité
     */
    public List<Sector> getSectorsWithArmy(Player player, Board board) {
        if (player == null || board == null) {
            return List.of();
        }

        List<Sector> playerSectors = board.getSectorsByOwner(player.getId());
        return playerSectors.stream()
                .filter(sector -> !sector.getUnits().isEmpty())
                .toList();
    }

    /**
     * Affiche toutes les armées des secteurs du joueur.
     *
     * @param player Le joueur dont on veut afficher l'armée
     * @param board Le plateau de jeu contenant les secteurs
     */
    public void displayArmy(Player player, Board board) {
        if (player == null || board == null) {
            return;
        }

        System.out.println("=== ARMÉES DE " + player.getName().toUpperCase() + " ===");

        List<Sector> sectorsWithArmy = getSectorsWithArmy(player, board);
        if (sectorsWithArmy.isEmpty()) {
            System.out.println("Aucune unité dans les secteurs.");
        } else {
            for (Sector sector : sectorsWithArmy) {
                sector.displayArmy();
                System.out.println();
            }
        }

        int totalUnits = getTotalArmySize(player, board);
        int totalSectors = (int) player.getOwnedSectorIds().size();
        System.out.printf("TOTAL : %d unités réparties dans %d secteurs%n", totalUnits, totalSectors);
    }

    /**
     * Affiche les statistiques complètes d'un joueur.
     *
     * @param player Le joueur dont on veut afficher les stats
     * @param board Le plateau de jeu contenant les secteurs
     */
    public void displayStats(Player player, Board board) {
        if (player == null || board == null) {
            return;
        }

        // Recalculer les stats avant affichage
        recalculateStats(player, board);

        System.out.printf("=== %s ===%n", player.getName().toUpperCase());

        System.out.println("--- Statistiques Économiques ---");
        System.out.println(formatStat(player.getStats().getMoney(), "$ "));
        System.out.println(formatStat(player.getStats().getTotalIncome(), "revenu quotidien"));
        System.out.println(formatStat(player.getStats().getTotalVehiclesValue(), "valeur des véhicules"));
        System.out.println(formatStat(player.getStats().getTotalEquipmentValue(), "valeur des équipements"));
        System.out.println(formatStat(player.getStats().getTotalEconomyPower(), "puissance économique totale"));
        System.out.println();

        System.out.println("--- Statistiques Militaires ---");
        System.out.println(formatStat(player.getStats().getTotalOffensivePower(), "puissance offensive totale"));
        System.out.println(formatStat(player.getStats().getTotalDefensivePower(), "puissance défensive totale"));
        System.out.println(formatStat(player.getStats().getGlobalPower(), "puissance globale"));
        System.out.println();
    }

    private static final String FORMAT_INT = "%,.0f";
    private static final String FORMAT_FLOAT = "%,.2f";

    /**
     * Formate une statistique pour l'affichage.
     */
    private String formatStat(double value, String label) {
        String formatted = (value % 1 == 0)
            ? String.format(FORMAT_INT, value)
            : String.format(FORMAT_FLOAT, value);
        return formatted + " " + label;
    }
}

