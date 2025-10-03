package com.mg.nmlonline.entity.battle;

import com.mg.nmlonline.entity.player.Player;
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


    public void combatFake(Player defender, Player attacker) {
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
