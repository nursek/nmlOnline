package com.mg.nmlonline.entity.battle;

import com.mg.nmlonline.entity.player.Player;
import com.mg.nmlonline.entity.unit.Unit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

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
                System.out.println("      > " + targetUnit.getName() + " esquive l'attaque !");
                availableAttackerPoints -= (defense + armor);
                continue;
            }

            // Calcul des dégâts avec résistance
            double effectivePoints = availableAttackerPoints * (1 - resistance);
            if (availableAttackerPoints != effectivePoints) {
                System.out.printf("      > Résistance de %.0f%% appliquée. Dégâts effectifs : %.2f\n", resistance * 100, effectivePoints);
            }

            if ((armor + defense) <= effectivePoints) {
                System.out.println("      > " + targetUnit.getName() + " (ID: " + targetUnit.getId() + ") est détruit pendant la phase " + damageType + " !");
                availableAttackerPoints -= (defense + armor) / (1 - resistance);
                defender.remove(targetUnit);
                casualties.add(targetUnit);
            } else if (effectivePoints <= armor) {
                targetUnit.setArmor(armor - effectivePoints);
                System.out.printf("      > %s perd %.2f d'armure (reste: %.2f)\n", targetUnit.getName(), effectivePoints, targetUnit.getArmor());
                availableAttackerPoints = 0;
            } else {
                targetUnit.setArmor(0);
                double remainingPoints = effectivePoints - armor;
                targetUnit.setDefense(defense - remainingPoints);
                System.out.printf("      > %s perd toute son armure et %.2f de défense (reste: %.2f)\n", targetUnit.getName(), remainingPoints, targetUnit.getDefense());
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


    public void classicCombatConfiguration(Player attacker, Player defender) {
        defender.updateCombatStats();
        attacker.updateCombatStats();

        List<Unit> defenderUnits = defender.getAllUnits();
        List<Unit> attackerUnits = attacker.getAllUnits();

        System.out.println("\n=== Début du combat entre " + attacker.getName() + " et " + defender.getName() + " ===");

        // Phase PDF
        printPhaseHeader("PDF");
        double attackerTotalPdf = attacker.getPlayerStats().getTotalPdf();
        double defenderTotalPdf = defender.getPlayerStats().getTotalPdf();

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

        // Phase PDC
        printPhaseHeader("PDC");
        double attackerTotalPdc = attacker.getPlayerStats().getTotalPdc();
        double defenderTotalPdc = defender.getPlayerStats().getTotalPdc();

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

        // Phase ATK
        printPhaseHeader("ATK");
        double attackerTotalAtk = attacker.getPlayerStats().getTotalAtk();
        double defenderTotalAtk = defender.getPlayerStats().getTotalAtk();

        attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalAtk, "ATK");
        defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalAtk, "ATK");

        defenderUnits = attackerPhaseResult.survivors();
        attackerUnits = defenderPhaseResult.survivors();

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
        // Basically, if you there are remaining points after a phase, they're bound over to be used in the next phase.
        // If points are 0, set allUnits points type at 0 : ex Pdf = 0 then all units are pdf = 0.
        // If points > 0, then verify it doesn't exceed the total points of all units, if it does, set all units points to their max.
        // If points < total points of all units, go through all units and assign points until you reach the available points, from the top to the bottom of the list.
        if (units == null || units.isEmpty()) return;

        // Calcul du total des points max pour ce type
        double totalMax = 0;
        for (Unit unit : units) {
            switch (pointsType) {
                case "PDF" -> totalMax += unit.getPdf();
                case "PDC" -> totalMax += unit.getPdc();
                case "ATK" -> totalMax += unit.getAttack();
            }
        }

        if (points <= 0) {
            // Tous les points à 0
            for (Unit unit : units) {
                switch (pointsType) {
                    case "PDF" -> unit.setPdf(0);
                    case "PDC" -> unit.setPdc(0);
                    case "ATK" -> unit.setAttack(0);
                }
            }
        } else if (points >= totalMax) {
            // Tous les points à leur max
            for (Unit unit : units) {
                switch (pointsType) {
                    case "PDF" -> unit.setPdf(unit.getPdf());
                    case "PDC" -> unit.setPdc(unit.getPdc());
                    case "ATK" -> unit.setAttack(unit.getAttack());
                }
            }
        } else {
            // Répartition des points du haut vers le bas
            for (Unit unit : units) {
                double max = switch (pointsType) {
                    case "PDF" -> unit.getPdf();
                    case "PDC" -> unit.getPdc();
                    case "ATK" -> unit.getAttack();
                    default -> 0;
                };
                double toAssign = Math.min(points, max);
                switch (pointsType) {
                    case "PDF" -> unit.setPdf(toAssign);
                    case "PDC" -> unit.setPdc(toAssign);
                    case "ATK" -> unit.setAttack(toAssign);
                }
                points -= toAssign;
                if (points <= 0) break;
            }
            // Les unités restantes reçoivent 0
            boolean assignZero = points <= 0;
            if (assignZero) {
                for (Unit unit : units) {
                    double current = switch (pointsType) {
                        case "PDF" -> unit.getPdf();
                        case "PDC" -> unit.getPdc();
                        case "ATK" -> unit.getAttack();
                        default -> 0;
                    };
                    if (current == 0) {
                        switch (pointsType) {
                            case "PDF" -> unit.setPdf(0);
                            case "PDC" -> unit.setPdc(0);
                            case "ATK" -> unit.setAttack(0);
                        }
                    }
                }
            }
        }
    }
}
