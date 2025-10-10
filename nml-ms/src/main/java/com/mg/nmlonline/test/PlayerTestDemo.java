package com.mg.nmlonline.test;

import com.mg.nmlonline.entity.battle.Battle;
import com.mg.nmlonline.entity.player.Player;
import com.mg.nmlonline.entity.sector.Sector;
import com.mg.nmlonline.service.PlayerService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Test de la classe Player avec tri des unités
 */
@Slf4j
public class PlayerTestDemo {

    public static void main(String[] args) {
        System.out.println("=== DÉMO CLASSE PLAYER ===\n");
        testCombatSimulation();
    }

    private static void testCombatSimulation() {
        log.info("🔹 TEST: Simulation de combat entre deux joueurs");
        log.info("==============================================");

        PlayerService playerService = new PlayerService();
        try {
            String player1Path = Objects.requireNonNull(PlayerTestDemo.class.getClassLoader().getResource("players/player1.json")).getFile();
            String player2Path = Objects.requireNonNull(PlayerTestDemo.class.getClassLoader().getResource("players/player2.json")).getFile();

            Player defender = playerService.importPlayerFromJson(player1Path);
            Player attacker = playerService.importPlayerFromJson(player2Path);

            Battle battleHandler = new Battle();
            battleHandler.classicCombatConfiguration(attacker, defender);
        } catch (IOException e) {
            throw new RuntimeException("Skill issue lors de l'import des joueurs pour le test de combat", e);
        }

    // Ajouter ici la logique de simulation de combat entre defender et attacker}
    }
}