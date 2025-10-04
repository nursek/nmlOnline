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


    public double classicPhaseConfiguration(List<Unit> defender, double availableAttackerPoints, String phaseName) {
        double lastFetchedEvasion = 0;
        double lastFetchedArmor = 0;
        double lastFetchedDef = 0;

        while (availableAttackerPoints > 0 && (!defender.isEmpty())) {
            // Select last unit in the defender list
            Unit targetUnit = defender.getLast();
            lastFetchedEvasion = targetUnit.getFinalEvasion();
            lastFetchedArmor = targetUnit.getFinalArmor();
            lastFetchedDef = targetUnit.getFinalDefense();

            // Handle unit evasion
            if (lastFetchedEvasion > 0 && (rand() % 100) + 1 <= lastFetchedEvasion) {
                // Unit evades the attack
                System.out.println("Unit " + targetUnit.getName() + " evades this attack!");
                availableAttackerPoints = availableAttackerPoints - (lastFetchedDef + lastFetchedArmor);
                continue; // Move to the next iteration
            }

            // Calculate damage, no evasion.
            if ((lastFetchedArmor + lastFetchedDef) <= availableAttackerPoints) {
                // Unit is directly killed
                System.out.println("Unit " + targetUnit.getId() + " is destroyed in " + phaseName + " phase!");
                availableAttackerPoints = availableAttackerPoints - (lastFetchedDef + lastFetchedArmor);
                defender.remove(targetUnit);
                //todo: move targetUnit to casualty list, created after.
            } else {
                // Unit armor is damaged, but not wounded
                if (availableAttackerPoints <= lastFetchedArmor) {
                    // On enlève les points d'armure
                    targetUnit.setFinalArmor(lastFetchedArmor - availableAttackerPoints);
                    availableAttackerPoints = 0;
                } else {
                    // Unit armor is destroyed and unit is wounded.
                    targetUnit.setFinalArmor(lastFetchedArmor - availableAttackerPoints);
                    availableAttackerPoints = lastFetchedArmor - availableAttackerPoints;
                    targetUnit.setFinalDefense(lastFetchedDef - availableAttackerPoints);
                }
            }
            return availableAttackerPoints;
        }
        return availableAttackerPoints;
    }


    public void classicCombatConfiguration(Player attacker, Player defender) {
        // Calculate total stats for defender
        defender.updateCombatStats();
        attacker.updateCombatStats();

        double defenderTotalAtk = defender.getPlayerStats().getTotalAtk();
        double defenderTotalPdf = defender.getPlayerStats().getTotalPdf();
        double defenderTotalPdc = defender.getPlayerStats().getTotalPdc();
        double defenderTotalDef = defender.getPlayerStats().getTotalDef();
        double defenderTotalArmor = defender.getPlayerStats().getTotalArmor();

        // Calculate total stats for attacker
        double attackerTotalAtk = attacker.getPlayerStats().getTotalAtk();
        double attackerTotalPdf = attacker.getPlayerStats().getTotalPdf();
        double attackerTotalPdc = attacker.getPlayerStats().getTotalPdc();
        double attackerTotalDef = attacker.getPlayerStats().getTotalDef();
        double attackerTotalArmor = attacker.getPlayerStats().getTotalArmor();

        // Simple combat resolution logic (to be replaced with actual mechanics)
        double defenderPower = defenderTotalAtk + defenderTotalPdf + defenderTotalPdc + defenderTotalDef + defenderTotalArmor;
        double attackerPower = attackerTotalAtk + attackerTotalPdf + attackerTotalPdc + attackerTotalDef + attackerTotalArmor;

        if (attackerPower > defenderPower) {
            this.winner = attacker;
            // Attacker wins, takes control of the sector
            System.out.println("Attacker wins the battle!");
        } else if (defenderPower > attackerPower) {
            this.winner = defender;
            // Defender wins, retains control of the sector
            System.out.println("Defender wins the battle!");
        } else {
            this.winner = null; // Draw
            System.out.println("The battle ends in a draw!");
        }

    }


}
