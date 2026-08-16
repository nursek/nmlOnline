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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
     * Simule une bataille sur un secteur précis entre deux joueurs dont les
     * unités y sont présentes (typiquement : un attaquant arrive sur un secteur
     * défendu, ou deux armées se rencontrent sur un même secteur). Contrairement
     * à {@link #simulateBattle}, cible le secteur explicitement et sépare, dans
     * ce secteur, les unités de l'attaquant de celles du défenseur.
     *
     * <p>Les pertes sont retirées de l'armée du secteur (supprimées en base par
     * {@code orphanRemoval} JPA au flush) et les survivants blessés voient leurs
     * stats recalculées et persistées (les listes de travail partagent les mêmes
     * références d'entités que la collection du secteur). {@code recalculateMilitaryPower}
     * est rappelé en fin de combat.</p>
     *
     * @param attacker     joueur attaquant
     * @param defender     joueur défenseur
     * @param board        plateau de jeu
     * @param sectorNumber numéro du secteur où se déroule le combat
     * @return le compte-rendu (pertes, blessés, vainqueur)
     */
    public SectorBattleResult simulateSectorBattle(Player attacker, Player defender, Board board, int sectorNumber) {
        if (attacker == null || defender == null || board == null) {
            return new SectorBattleResult(false, "Paramètres invalides",
                    List.of(), List.of(), List.of(), List.of(), null);
        }
        Sector sector = board.getSector(sectorNumber);
        if (sector == null) {
            return new SectorBattleResult(false, "Secteur inexistant : " + sectorNumber,
                    List.of(), List.of(), List.of(), List.of(), null);
        }

        playerStatsService.updateCombatStats(attacker, board);
        playerStatsService.updateCombatStats(defender, board);

        // Copies filtrées par joueur (références partagées avec sector.getArmy()).
        // Battle mute ces listes en place (retrait des pertes) et les stats des
        // Unit survivantes — les mutations sont visibles sur les entités du secteur.
        List<Unit> attackerUnits = sector.getUnits().stream()
                .filter(u -> attacker.getId().equals(u.getPlayerId()))
                .collect(Collectors.toCollection(ArrayList::new));
        List<Unit> defenderUnits = sector.getUnits().stream()
                .filter(u -> defender.getId().equals(u.getPlayerId()))
                .collect(Collectors.toCollection(ArrayList::new));

        if (attackerUnits.isEmpty() || defenderUnits.isEmpty()) {
            return new SectorBattleResult(false,
                    "Unités manquantes au secteur " + sectorNumber
                            + " (attaquant=" + attackerUnits.size() + ", défenseur=" + defenderUnits.size() + ")",
                    List.of(), List.of(), List.of(), List.of(), null);
        }

        // Snapshot des IDs avant combat pour identifier les pertes après coup.
        Set<Long> beforeIds = new HashSet<>();
        attackerUnits.forEach(u -> beforeIds.add(u.getId()));
        defenderUnits.forEach(u -> beforeIds.add(u.getId()));

        Battle battle = new Battle();
        battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

        // Réconciliation de l'armée du secteur : les pertes (retirées des listes
        // de travail par Battle) doivent sortir de sector.getArmy() pour être
        // supprimées en base (orphanRemoval). Les survivants y restent (mêmes
        // références), leurs stats modifiées sont déjà visibles.
        Set<Long> survivorIds = new HashSet<>();
        attackerUnits.forEach(u -> survivorIds.add(u.getId()));
        defenderUnits.forEach(u -> survivorIds.add(u.getId()));

        List<Unit> casualties = new ArrayList<>();
        for (Unit unit : new ArrayList<>(sector.getUnits())) {
            if (beforeIds.contains(unit.getId()) && !survivorIds.contains(unit.getId())) {
                sector.getUnits().remove(unit);
                casualties.add(unit);
            }
        }

        List<Unit> attackerCasualties = casualties.stream()
                .filter(u -> attacker.getId().equals(u.getPlayerId()))
                .collect(Collectors.toList());
        List<Unit> defenderCasualties = casualties.stream()
                .filter(u -> defender.getId().equals(u.getPlayerId()))
                .collect(Collectors.toList());
        List<Unit> attackerInjured = attackerUnits.stream()
                .filter(Unit::isInjured)
                .collect(Collectors.toList());
        List<Unit> defenderInjured = defenderUnits.stream()
                .filter(Unit::isInjured)
                .collect(Collectors.toList());

        sector.recalculateMilitaryPower();

        logger.info("[Combat secteur {}] {} vs {}: {} pertes attaquant, {} pertes défenseur",
                sectorNumber, attacker.getName(), defender.getName(),
                attackerCasualties.size(), defenderCasualties.size());

        return new SectorBattleResult(true, "Bataille terminée au secteur " + sectorNumber,
                attackerCasualties, defenderCasualties, attackerInjured, defenderInjured, battle.getWinner());
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

    /**
     * Compte-rendu d'une bataille sur un secteur précis
     * ({@link #simulateSectorBattle}). Porte les pertes et blessés par camp
     * pour alimentation du compte-rendu admin.
     */
    public static class SectorBattleResult {
        private final boolean success;
        private final String message;
        private final List<Unit> attackerCasualties;
        private final List<Unit> defenderCasualties;
        private final List<Unit> attackerInjured;
        private final List<Unit> defenderInjured;
        private final Player winner;

        public SectorBattleResult(boolean success, String message,
                                   List<Unit> attackerCasualties, List<Unit> defenderCasualties,
                                   List<Unit> attackerInjured, List<Unit> defenderInjured,
                                   Player winner) {
            this.success = success;
            this.message = message;
            this.attackerCasualties = attackerCasualties;
            this.defenderCasualties = defenderCasualties;
            this.attackerInjured = attackerInjured;
            this.defenderInjured = defenderInjured;
            this.winner = winner;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<Unit> getAttackerCasualties() { return attackerCasualties; }
        public List<Unit> getDefenderCasualties() { return defenderCasualties; }
        public List<Unit> getAttackerInjured() { return attackerInjured; }
        public List<Unit> getDefenderInjured() { return defenderInjured; }
        public Player getWinner() { return winner; }
    }
}

