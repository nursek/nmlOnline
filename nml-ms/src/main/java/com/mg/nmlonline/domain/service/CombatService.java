package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.battle.Battle;
import jakarta.persistence.EntityManager;
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

@Service
public class CombatService {

    private static final Logger logger = LoggerFactory.getLogger(CombatService.class);

    @Autowired
    private PlayerStatsService playerStatsService;

    @Autowired
    private EntityManager em;

    public Optional<Sector> findSectorWithArmy(Player player, Board board) {
        if (player == null || board == null) {
            return Optional.empty();
        }

        List<Sector> sectorsWithArmy = playerStatsService.getSectorsWithCombatEntities(player, board);
        return sectorsWithArmy.stream().findFirst();
    }

    @Deprecated
    public BattleResult simulateBattle(Player attacker, Player defender, Board board) {
        if (attacker == null || defender == null || board == null) {
            return new BattleResult(false, "Paramètres invalides");
        }

        logger.info("⚔️  DÉBUT DE LA BATAILLE\n");
        logger.info("  Attaquant: {}", attacker.getName());
        logger.info("  Défenseur: {}\n", defender.getName());

        Optional<Sector> defenderSectorOpt = findSectorWithArmy(defender, board);
        if (defenderSectorOpt.isEmpty()) {
            String message = "❌ Le défenseur n'a pas d'armée disponible pour le combat.";
            logger.info(message);
            return new BattleResult(false, message);
        }
        Sector defenderSector = defenderSectorOpt.get();

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

        playerStatsService.updateCombatStats(defender, board);
        playerStatsService.updateCombatStats(attacker, board);

        List<Unit> defenderUnits = defenderSector.getUnits();
        List<Unit> attackerUnits = attackerSector.getUnits();

        Battle battle = new Battle();
        battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

        logger.info("\n{}", "=".repeat(60));
        logger.info("⚔️  FIN DE LA BATAILLE");

        return new BattleResult(true, "Bataille terminée", battle.getWinner());
    }

    /** Combat sur un secteur donné (unités des deux camps co-localisées). Pertes supprimées via em.remove (Sector.army sans orphanRemoval — voir docs/jpa-pitfalls.md §1 + V6__sector_army_fk_cascade.sql). */
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

        // Copies filtrées par joueur (références partagées avec sector.getArmy()) : Battle les mute en place (pertes retirées, stats modifiées) — visible sur les entités du secteur.
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

        // Réconciliation : les pertes (retirées des listes de travail par Battle) sortent de sector.getArmy() pour em.remove. Survivants = mêmes références, stats déjà mutées.
        Set<Long> survivorIds = new HashSet<>();
        attackerUnits.forEach(u -> survivorIds.add(u.getId()));
        defenderUnits.forEach(u -> survivorIds.add(u.getId()));

        List<Unit> casualties = new ArrayList<>();
        for (Unit unit : new ArrayList<>(sector.getUnits())) {
            if (beforeIds.contains(unit.getId()) && !survivorIds.contains(unit.getId())) {
                // Sector.army sans orphanRemoval (docs/jpa-pitfalls.md §1, V6) : em.remove cascade vers Unit.unitEquipments (cascade=ALL) → DELETE propre. Retrait mémoire pour cohérence de sector.getUnits().
                em.remove(unit);
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

        public record BattleResult(boolean success, String message, Player winner) {
            public BattleResult(boolean success, String message) {
                this(success, message, null);
            }

    }

        public record SectorBattleResult(boolean success, String message, List<Unit> attackerCasualties,
                                         List<Unit> defenderCasualties, List<Unit> attackerInjured,
                                         List<Unit> defenderInjured, Player winner) {

    }
}

