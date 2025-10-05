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
        List<Unit> casualties = new ArrayList<>();
        System.out.println("availableAttackerPoints : " + availableAttackerPoints);
        while (availableAttackerPoints > 0 && !defender.isEmpty()) {
            Unit targetUnit = defender.getLast();
            double evasion = targetUnit.getFinalEvasion();
            double armor = targetUnit.getFinalArmor();
            double defense = targetUnit.getFinalDefense();

            // Gestion de l'évasion
            if (evasion > 0 && (rand() % 100) + 1 <= evasion) {
                System.out.println("Unit " + targetUnit.getName() + " evades this attack!");
                availableAttackerPoints -= (defense + armor);
                continue;
            }

            // Calcul des dégâts
             if ((armor + defense) <= availableAttackerPoints) {
                System.out.println("Unit " + targetUnit.getId() + " is destroyed in " + phaseName + " phase!");
                availableAttackerPoints -= (defense + armor);
                defender.remove(targetUnit);
                casualties.add(targetUnit);
            } else if (availableAttackerPoints <= armor) {
                targetUnit.setFinalArmor(armor - availableAttackerPoints);
                availableAttackerPoints = 0;
            } else {
                targetUnit.setFinalArmor(armor - availableAttackerPoints);
                double remaining = availableAttackerPoints - armor;
                targetUnit.setFinalDefense(defense - remaining);
                availableAttackerPoints = 0;
            }
        }

        // TODO: utiliser la liste casualties si besoin
        for (Unit unit : defender) {
            System.out.println("Remaining Unit after " + phaseName + " phase: " + unit);
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

        System.out.println("\n--- Combat between Attacker: " + attacker.getName() + " and Defender: " + defender.getName() + " ---");

        double attackerRemainingPoints = classicPhaseConfiguration(defender.getAllUnits(), attackerTotalPdf, "PDF");
        double defenderRemainingPoints = classicPhaseConfiguration(attacker.getAllUnits(), defenderTotalPdf, "HUH");

        System.out.println("After PDF phase, Attacker remaining points: " + attackerRemainingPoints + ", Defender remaining points: " + defenderRemainingPoints);
    }
}
