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
        System.out.println("availableAttackerPoints : " + availableAttackerPoints);

        while (availableAttackerPoints > 0 && !defender.isEmpty()) {
            Unit targetUnit = defender.getLast();
            double evasion = targetUnit.getFinalEvasion();
            double armor = targetUnit.getFinalArmor();
            double defense = targetUnit.getFinalDefense();
            double resistance = targetUnit.getDamageReduction(damageType); // ex: 0.25 pour 25%

            // Gestion de l'évasion (pas de résistance appliquée ici)
            if (evasion > 0 && (rand() % 100) + 1 <= evasion) {
                System.out.println("Unit " + targetUnit.getName() + " evades this attack!");
                availableAttackerPoints -= (defense + armor);
                continue;
            }

            // Calcul des dégâts avec résistance
            double effectivePoints = availableAttackerPoints * (1 - resistance);
            if( availableAttackerPoints != effectivePoints) {
                System.out.println("Resistance of " + (resistance * 100) + "% applied. Effective damage points: " + effectivePoints);
            }

            if ((armor + defense) <= effectivePoints) {
                System.out.println("Unit " + targetUnit.getId() + " is destroyed in " + damageType + " phase!");
                availableAttackerPoints -= (defense + armor) / (1 - resistance);
                defender.remove(targetUnit);
                casualties.add(targetUnit);
            } else if (effectivePoints <= armor) {
                targetUnit.setFinalArmor(armor - effectivePoints);
                availableAttackerPoints = 0;
            } else {
                targetUnit.setFinalArmor(armor - effectivePoints);
                double remainingPoints = effectivePoints - armor;
                targetUnit.setFinalDefense(defense - remainingPoints);
                availableAttackerPoints = 0;
            }
        }

        for (Unit unit : defender) {
            System.out.println("Remaining Unit after " + damageType + " phase: " + unit);
        }
        return new PhaseResult(casualties, defender, availableAttackerPoints);
    }

    public boolean doesFightEnd() {
        return attackers.isEmpty() || defenders.isEmpty();
    }

    public void printUnits(List<Unit> units) {
        for (Unit unit : units) {
            System.out.println(unit);
        }
    }


    public void classicCombatConfiguration(Player attacker, Player defender) {
        // Calculate total stats for defender
        defender.updateCombatStats();
        attacker.updateCombatStats();

        List<Unit> defenderUnits = defender.getAllUnits();
        List<Unit> attackerUnits = attacker.getAllUnits();

        printUnits(defenderUnits);
        System.out.println("-----");
        printUnits(attackerUnits);

//
//        double defenderTotalAtk = defender.getPlayerStats().getTotalAtk();
        double defenderTotalPdf = defender.getPlayerStats().getTotalPdf();
//        double defenderTotalPdc = defender.getPlayerStats().getTotalPdc();
//        double defenderTotalDef = defender.getPlayerStats().getTotalDef();
//        double defenderTotalArmor = defender.getPlayerStats().getTotalArmor();
//
//        // Calculate total stats for attacker
//        double attackerTotalAtk = attacker.getPlayerStats().getTotalAtk();
        double attackerTotalPdf = attacker.getPlayerStats().getTotalPdf();
//        double attackerTotalPdc = attacker.getPlayerStats().getTotalPdc();
//        double attackerTotalDef = attacker.getPlayerStats().getTotalDef();
//        double attackerTotalArmor = attacker.getPlayerStats().getTotalArmor();

        System.out.println("\n--- Combat between Attacker: " + attacker.getName() + " and Defender: " + defender.getName() + " ---");

        PhaseResult attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalPdf, "PDF");
        PhaseResult defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalPdf, "HUH");

        defenderUnits = attackerPhaseResult.survivors();
        attackerUnits = defenderPhaseResult.survivors();

        printUnits(defenderUnits);
        System.out.println("-----");
        printUnits(attackerUnits);
    }
}
