package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.Unit;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class Battle {

    private static final Logger logger = LoggerFactory.getLogger(Battle.class);

    private int sectorId; // ID of the sector where the battle takes place

    private List<Player> defenders = new ArrayList<>();
    private List<Player> attackers = new ArrayList<>();

    // A battle can have a winner, but not mandatory. A winner claims or keeps the sector.
    private Player winner;

    private Random random;

    public Battle() {
        this.random = new Random();
    }

    /**
     * Génère un nombre aléatoire entre 1 et 100 (inclus)
     */
    private int rand() {
        return random.nextInt(100) + 1;
    }

    public PhaseResult classicPhaseConfiguration(List<Unit> defender, double availableAttackerPoints, String damageType) {
        List<Unit> casualties = new ArrayList<>();
        logger.info("    Points d'attaque disponibles : {}", availableAttackerPoints);

        while (availableAttackerPoints > 0 && !defender.isEmpty()) {
            Unit targetUnit = defender.getLast();
            double evasion = targetUnit.getEvasion();
            double armor = targetUnit.getArmor();
            double defense = targetUnit.getDefense();
            double resistance = targetUnit.getDamageReduction(damageType);

            // Gestion de l'évasion
            if (evasion > 0 && rand() <= evasion) {
                logger.info("      > {} esquive l'attaque !", targetUnit.getType().name());
                availableAttackerPoints -= (defense + armor);
                continue;
            }

            // Calcul des dégâts avec résistance
            double effectivePoints = availableAttackerPoints * (1 - resistance);
            if (availableAttackerPoints != effectivePoints) {
                logger.info("      > Résistance de {}% appliquée. Dégâts effectifs : {}", String.format("%.0f", resistance * 100), String.format("%.2f", effectivePoints));
            }

            if ((armor + defense) <= effectivePoints) {
                logger.info("      > {} (ID: {}) est détruit pendant la phase {} !", targetUnit.getType().name(), targetUnit.getId(), damageType);
                availableAttackerPoints -= (defense + armor) / (1 - resistance);
                defender.remove(targetUnit);
                casualties.add(targetUnit);
            } else if (effectivePoints <= armor) {
                targetUnit.setArmor(armor - effectivePoints);
                logger.info("      > {} perd {} d'armure (reste: {})", targetUnit.getType().name(), String.format("%.2f", effectivePoints), String.format("%.2f", targetUnit.getArmor()));
                availableAttackerPoints = 0;
            } else {
                targetUnit.setArmor(0);
                double remainingPoints = effectivePoints - armor;
                targetUnit.setDefense(defense - remainingPoints);
                logger.info("      > {} perd toute son armure et {} de défense (reste: {})", targetUnit.getType().name(), String.format("%.2f", remainingPoints), String.format("%.2f", targetUnit.getDefense()));
                availableAttackerPoints = 0;
            }
        }

        if (!defender.isEmpty()) {
            logger.info("    Unités restantes après la phase {} :", damageType);
            for (Unit unit : defender) {
                logger.info("      - {}", unit);
            }
        }
        if (!casualties.isEmpty()) {
            logger.info("    Pertes pendant la phase {} :", damageType);
            for (Unit unit : casualties) {
                logger.info("      - {}", unit);
            }
        }
        return new PhaseResult(casualties, defender, availableAttackerPoints);
    }

    double getTotalPoints(Player player, String pointsType) {
        return switch (pointsType) {
            case "PDF" -> player.getStats().getTotalPdf();
            case "PDC" -> player.getStats().getTotalPdc();
            case "ATK" -> player.getStats().getTotalAtk();
            default -> 0;
        };
    }

    double checkPointsTypeInUnits(List<Unit> units, String pointsType) {
        return switch (pointsType) {
            case "PDF" -> units.stream().mapToDouble(Unit::getPdf).sum();
            case "PDC" -> units.stream().mapToDouble(Unit::getPdc).sum();
            case "ATK" -> units.stream().mapToDouble(Unit::getAttack).sum();
            default -> 0;
        };
    }


    private Unit handleInjuredUnit(Unit unit) {
        unit.setInjured(true);
        unit.recalculateBaseStats();
        return unit;
    }

    private List<Unit> replaceWithInjured(List<Unit> survivors, List<Unit> casualties) {
        Set<Long> casualtiesIds = casualties.stream().map(Unit::getId).collect(Collectors.toSet());
        List<Unit> result = new ArrayList<>();
        for (Unit unit : survivors) {
            if (casualtiesIds.contains(unit.getId()) ||
                    (!unit.isInjured() && unit.getDefense() < unit.getBaseDefense())) {
                result.add(handleInjuredUnit(unit));
            } else {
                result.add(unit);
            }
        }
        return result;
    }

    public void classicCombatConfiguration(Player attacker, Player defender, List<Unit> attackerUnits, List<Unit> defenderUnits) {
        if (attackerUnits == null) attackerUnits = new ArrayList<>();
        if (defenderUnits == null) defenderUnits = new ArrayList<>();

        printUnitsIndented(defenderUnits, "Défenseurs début");
        printUnitsIndented(attackerUnits, "Attaquants début");

        logger.info("\n=== Début du combat entre {} et {} ===", attacker.getName(), defender.getName());

        // Phase PDF
        printPhaseHeader("PDF");
        double attackerTotalPdf = getAvailablePoints(attackerUnits, "PDF");
        double defenderTotalPdf = getAvailablePoints(defenderUnits, "PDF");

        PhaseResult attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalPdf, "PDF");
        PhaseResult defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalPdf, "PDF");

        defenderUnits = attackerPhaseResult.survivors();
        attackerUnits = defenderPhaseResult.survivors();

        reassignPointsForNextPhase(attackerUnits, attackerPhaseResult.remainingPoints(), "PDF");
        reassignPointsForNextPhase(defenderUnits, defenderPhaseResult.remainingPoints(), "PDF");

        printUnitsIndented(defenderUnits, "Défenseurs restants");
        printUnitsIndented(attackerUnits, "Attaquants restants");

        if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
            logger.info("\n=== Combat terminé après la phase PDF ! ===");
            return;
        }

        // Check if there is leftover Pdf points to make a second PDF phase It will be used when buildings are implemented
        if (checkPointsTypeInUnits(attackerUnits, "PDF") > 0 || checkPointsTypeInUnits(defenderUnits, "PDF") > 0) {
            printPhaseHeader("PDF - Round 2");
            attackerTotalPdf = getAvailablePoints(attackerUnits, "PDF");
            defenderTotalPdf = getAvailablePoints(defenderUnits, "PDF");

            attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalPdf, "PDF");
            defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalPdf, "PDF");

            defenderUnits = attackerPhaseResult.survivors();
            attackerUnits = defenderPhaseResult.survivors();

            reassignPointsForNextPhase(attackerUnits, attackerPhaseResult.remainingPoints(), "PDF");
            reassignPointsForNextPhase(defenderUnits, defenderPhaseResult.remainingPoints(), "PDF");

            printUnitsIndented(defenderUnits, "Défenseurs restants");
            printUnitsIndented(attackerUnits, "Attaquants restants");

            if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
                logger.info("\n=== Combat terminé après la phase PDF round 2 ! ===");
                return;
            }
        }

        // Phase PDC
        printPhaseHeader("PDC");
        double attackerTotalPdc = getAvailablePoints(attackerUnits, "PDC");
        double defenderTotalPdc = getAvailablePoints(defenderUnits, "PDC");

        attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalPdc, "PDC");
        defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalPdc, "PDC");

        defenderUnits = attackerPhaseResult.survivors();
        attackerUnits = defenderPhaseResult.survivors();

        reassignPointsForNextPhase(attackerUnits, attackerPhaseResult.remainingPoints(), "PDC");
        reassignPointsForNextPhase(defenderUnits, defenderPhaseResult.remainingPoints(), "PDC");

        printUnitsIndented(defenderUnits, "Défenseurs restants");
        printUnitsIndented(attackerUnits, "Attaquants restants");

        if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
            logger.info("\n=== Combat terminé après la phase PDC ! ===");
            return;
        }

        if (checkPointsTypeInUnits(attackerUnits, "PDC") > 0 || checkPointsTypeInUnits(defenderUnits, "PDC") > 0) {
            printPhaseHeader("PDC - Round 2");
            attackerTotalPdc = getTotalPoints(attacker, "PDC");
            defenderTotalPdc = getTotalPoints(defender, "PDC");

            attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalPdc, "PDC");
            defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalPdc, "PDC");

            defenderUnits = attackerPhaseResult.survivors();
            attackerUnits = defenderPhaseResult.survivors();

            reassignPointsForNextPhase(attackerUnits, attackerPhaseResult.remainingPoints(), "PDC");
            reassignPointsForNextPhase(defenderUnits, defenderPhaseResult.remainingPoints(), "PDC");

            printUnitsIndented(defenderUnits, "Défenseurs restants");
            printUnitsIndented(attackerUnits, "Attaquants restants");

            if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
                logger.info("\n=== Combat terminé après la phase PDC round 2 ! ===");
                return;
            }
        }

        // Phase ATK
        printPhaseHeader("ATK");
        double attackerTotalAtk = getAvailablePoints(attackerUnits, "ATK");
        double defenderTotalAtk = getAvailablePoints(defenderUnits, "ATK");

        // Make it non-lethal.
        attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalAtk, "ATK");
        defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalAtk, "ATK");

        defenderUnits = attackerPhaseResult.survivors();
        attackerUnits = defenderPhaseResult.survivors();

        // Fin du combat, on remplace les unités détruites par des blessées etc, on recalcule les stats.

        defenderUnits = replaceWithInjured(defenderUnits, attackerPhaseResult.casualties());
        attackerUnits = replaceWithInjured(attackerUnits, defenderPhaseResult.casualties());

        reassignPointsForNextPhase(attackerUnits, attackerPhaseResult.remainingPoints(), "ATK");
        reassignPointsForNextPhase(defenderUnits, defenderPhaseResult.remainingPoints(), "ATK");

        printUnitsIndented(defenderUnits, "Défenseurs restants");
        printUnitsIndented(attackerUnits, "Attaquants restants");

        if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
            logger.info("\n=== Combat terminé après la phase ATK ! ===");
        } else {
            logger.info("\n=== Combat terminé, il reste des unités dans les deux camps. ===");
        }
    }

    private void printPhaseHeader(String phase) {
        logger.info("\n  === Phase {} ===", phase);
    }

    private void printUnitsIndented(List<Unit> units, String label) {
        logger.info("    {} :", label);
        for (Unit unit : units) {
            logger.info("      - {}", unit);
        }
    }

    private void reassignPointsForNextPhase(List<Unit> units, double points, String pointsType) {
        if (units == null || units.isEmpty()) return;

        double totalMax = units.stream().mapToDouble(u -> getUnitPoints(u, pointsType)).sum();

        if (points <= 0) {
            units.forEach(u -> setUnitPoints(u, pointsType, 0));
        } else if (points >= totalMax) {
            units.forEach(u -> setUnitPoints(u, pointsType, getUnitPoints(u, pointsType)));
        } else {
            for (Unit unit : units) {
                double max = getUnitPoints(unit, pointsType);
                double toAssign = Math.min(points, max);
                setUnitPoints(unit, pointsType, toAssign);
                points -= toAssign;
                if (points <= 0) break;
            }
            units.stream()
                    .filter(u -> getUnitPoints(u, pointsType) == 0)
                    .forEach(u -> setUnitPoints(u, pointsType, 0));
        }
    }

    private double getUnitPoints(Unit unit, String pointsType) {
        return switch (pointsType) {
            case "PDF" -> unit.getPdf();
            case "PDC" -> unit.getPdc();
            case "ATK" -> unit.getAttack();
            default -> throw new IllegalArgumentException("Type de points inconnu : " + pointsType);
        };
    }

    private void setUnitPoints(Unit unit, String pointsType, double value) {
        switch (pointsType) {
            case "PDF" -> unit.setPdf(value);
            case "PDC" -> unit.setPdc(value);
            case "ATK" -> unit.setAttack(value);
            default -> throw new IllegalArgumentException("Type de points inconnu : " + pointsType);
        }
    }

    private double getAvailablePoints(List<Unit> units, String pointsType) {
        return units.stream().mapToDouble(u -> getUnitPoints(u, pointsType)).sum();
    }
}
