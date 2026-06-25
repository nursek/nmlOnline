package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.battle.Battle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service pour gérer les combats entre joueurs.
 * Coordonne les batailles en utilisant Board comme source de données.
 */
@Service
public class CombatService {

    private static final Logger logger = LoggerFactory.getLogger(CombatService.class);

    @Autowired
    private PlayerStatsService playerStatsService;

    /**
     * Trouve un secteur avec une armée pour un joueur donné.
     *
     * @param player Le joueur
     * @param board Le plateau de jeu
     * @return Le premier secteur trouvé avec une armée
     */
    public Optional<Sector> findSectorWithArmy(Player player, Board board) {
        if (player == null || board == null) {
            return Optional.empty();
        }

        List<Sector> sectorsWithArmy = playerStatsService.getSectorsWithCombatEntities(player, board);
        return sectorsWithArmy.stream().findFirst();
    }

    /**
     * Simule une bataille entre deux joueurs.
     *
     * @param attacker Le joueur attaquant
     * @param defender Le joueur défenseur
     * @param board Le plateau de jeu
     * @return Le résultat de la bataille
     */
    public BattleResult simulateBattle(Player attacker, Player defender, Board board) {
        if (attacker == null || defender == null || board == null) {
            return new BattleResult(false, "Paramètres invalides");
        }

        logger.info("⚔️  DÉBUT DE LA BATAILLE\n");
        logger.info("  Attaquant: {}", attacker.getName());
        logger.info("  Défenseur: {}\n", defender.getName());

        // Trouver un secteur du défenseur avec une armée
        Optional<Sector> defenderSectorOpt = findSectorWithArmy(defender, board);
        if (defenderSectorOpt.isEmpty()) {
            String message = "❌ Le défenseur n'a pas d'armée disponible pour le combat.";
            logger.info(message);
            return new BattleResult(false, message);
        }
        Sector defenderSector = defenderSectorOpt.get();

        // Trouver un secteur de l'attaquant avec une armée
        Optional<Sector> attackerSectorOpt = findSectorWithArmy(attacker, board);
        if (attackerSectorOpt.isEmpty()) {
            String message = "❌ L'attaquant n'a pas d'armée disponible pour le combat.";
            logger.info(message);
            return new BattleResult(false, message);
        }
        Sector attackerSector = attackerSectorOpt.get();

        logger.info("  📍 Secteur attaqué: {} (n°{})", defenderSector.getName(), defenderSector.getNumber());
        logger.info("  📍 Secteur d'origine: {} (n°{})", attackerSector.getName(), attackerSector.getNumber());
        logger.info("\n{}\n", "=".repeat(60));

        // Mettre à jour les stats de combat avant la bataille
        playerStatsService.updateCombatStats(defender, board);
        playerStatsService.updateCombatStats(attacker, board);

        // Récupérer les unités
        List<Unit> defenderUnits = defenderSector.getUnits();
        List<Unit> attackerUnits = attackerSector.getUnits();

        // Lancer la bataille
        Battle battle = new Battle();
        battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

        logger.info("\n{}", "=".repeat(60));
        logger.info("⚔️  FIN DE LA BATAILLE");

        return new BattleResult(true, "Bataille terminée", battle.getWinner());
    }

    /**
     * Classe pour encapsuler le résultat d'une bataille.
     */
    public static class BattleResult {
        private final boolean success;
        private final String message;
        private final Player winner;

        public BattleResult(boolean success, String message) {
            this(success, message, null);
        }

        public BattleResult(boolean success, String message, Player winner) {
            this.success = success;
            this.message = message;
            this.winner = winner;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Player getWinner() {
            return winner;
        }
    }
}

