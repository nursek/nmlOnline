package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.Unit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Battle {
    private int sectorId; // ID of the sector where the battle takes place

    private List<Player> playersInvolved = new ArrayList<>(); // Principal List of players involved in the battle
    // Players are then moved to according List
    private List<Player> defenders = new ArrayList<>();
    private List<Player> attackers = new ArrayList<>();

    // A battle can have a winner, but not mandatory. A winner claims or keeps the sector.
    private Player winner;

    private static final Random RANDOM = new Random();

    private int rand() {
        return RANDOM.nextInt(100);
    }

    public PhaseResult classicPhaseConfiguration(List<Unit> defender, double availableAttackerPoints, String damageType) {
        List<Unit> casualties = new ArrayList<>();
        System.out.println("    Points d'attaque disponibles : " + availableAttackerPoints);

        while (availableAttackerPoints > 0 && !defender.isEmpty()) {
            Unit targetUnit = defender.getLast();
            double evasion = targetUnit.getEvasion();
            double armor = targetUnit.getArmor();
            double defense = targetUnit.getDefense();
            double resistance = targetUnit.getDamageReduction(damageType);

            // Gestion de l'évasion
            if (evasion > 0 && (rand() % 100) + 1 <= evasion) {
                System.out.println("      > " + targetUnit.getType().name() + " esquive l'attaque !");
                availableAttackerPoints -= (defense + armor);
                continue;
            }

            // Calcul des dégâts avec résistance
            double effectivePoints = availableAttackerPoints * (1 - resistance);
            if (availableAttackerPoints != effectivePoints) {
                System.out.printf("      > Résistance de %.0f%% appliquée. Dégâts effectifs : %.2f%n", resistance * 100, effectivePoints);
            }

            if ((armor + defense) <= effectivePoints) {
                System.out.println("      > " + targetUnit.getType().name() + " (ID: " + targetUnit.getId() + ") est détruit pendant la phase " + damageType + " !");
                availableAttackerPoints -= (defense + armor) / (1 - resistance);
                defender.remove(targetUnit);
                casualties.add(targetUnit);
            } else if (effectivePoints <= armor) {
                targetUnit.setArmor(armor - effectivePoints);
                System.out.printf("      > %s perd %.2f d'armure (reste: %.2f)%n", targetUnit.getType().name(), effectivePoints, targetUnit.getArmor());
                availableAttackerPoints = 0;
            } else {
                targetUnit.setArmor(0);
                double remainingPoints = effectivePoints - armor;
                targetUnit.setDefense(defense - remainingPoints);
                System.out.printf("      > %s perd toute son armure et %.2f de défense (reste: %.2f)%n", targetUnit.getType().name(), remainingPoints, targetUnit.getDefense());
                availableAttackerPoints = 0;
            }
        }

        if (!defender.isEmpty()) {
            System.out.println("    Unités restantes après la phase " + damageType + " :");
            for (Unit unit : defender) {
                System.out.println("      - " + unit);
            }
        }
        if (!casualties.isEmpty()) {
            System.out.println("    Pertes pendant la phase " + damageType + " :");
            for (Unit unit : casualties) {
                System.out.println("      - " + unit);
            }
        }
        return new PhaseResult(casualties, defender, availableAttackerPoints);
    }

    double getTotalPoints(Player player, String pointsType) {
        return switch (pointsType) {
            case "PDF" -> player.getPlayerStats().getTotalPdf();
            case "PDC" -> player.getPlayerStats().getTotalPdc();
            case "ATK" -> player.getPlayerStats().getTotalAtk();
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
        Set<Integer> casualtiesIds = casualties.stream().map(Unit::getId).collect(Collectors.toSet());
        List<Unit> result = new ArrayList<>();
        for (Unit unit : survivors) {
            if (casualtiesIds.contains(unit.getId()) ||
                    (unit.getClasses().stream().noneMatch(c -> c.getCode().equals("BLESSE")) && unit.getDefense() < unit.getBaseDefense())) {
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

        System.out.println("\n=== Début du combat entre " + attacker.getName() + " et " + defender.getName() + " ===");

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
            System.out.println("\n=== Combat terminé après la phase PDF ! ===");
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
                System.out.println("\n=== Combat terminé après la phase PDF round 2 ! ===");
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
            System.out.println("\n=== Combat terminé après la phase PDC ! ===");
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
                System.out.println("\n=== Combat terminé après la phase PDC round 2 ! ===");
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
            System.out.println("\n=== Combat terminé après la phase ATK ! ===");
        } else {
            System.out.println("\n=== Combat terminé, il reste des unités dans les deux camps. ===");
        }
    }

    private void printPhaseHeader(String phase) {
        System.out.println("\n  === Phase " + phase + " ===");
    }

    private void printUnitsIndented(List<Unit> units, String label) {
        System.out.println("    " + label + " :");
        for (Unit unit : units) {
            System.out.println("      - " + unit);
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
