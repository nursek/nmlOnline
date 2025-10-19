package com.mg.nmlonline.demo;

import com.mg.nmlonline.domain.model.battle.Battle;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.service.PlayerImportService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;

@Slf4j
public class PlayerDemo {

    public static void main(String[] args) {
        System.out.println("=== DÉMO CLASSE PLAYER ===\n");
        testCombatSimulation();
    }

    private static void testCombatSimulation() {
        log.info("🔹 TEST: Simulation de combat entre deux joueurs");
        log.info("==============================================");

        PlayerImportService importService = new PlayerImportService();

        try {
            URL p1 = PlayerDemo.class.getClassLoader().getResource("players/player3.json");
            URL p2 = PlayerDemo.class.getClassLoader().getResource("players/player3.json");
            if (p1 == null || p2 == null) {
                throw new NullPointerException("Fichier player1.json ou player2.json introuvable dans /players");
            }

            Player defender = importService.importPlayerFromJson(p1.getFile());
            Player attacker = importService.importPlayerFromJson(p2.getFile());

            Battle battleHandler = new Battle();
            battleHandler.classicCombatConfiguration(attacker, defender);

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'import des joueurs pour le test de combat", e);
        } catch (NullPointerException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
