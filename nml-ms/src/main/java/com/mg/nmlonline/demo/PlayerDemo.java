package com.mg.nmlonline.demo;

import com.mg.nmlonline.domain.model.battle.Battle;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.service.PlayerImportService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class PlayerDemo {

    public static void main(String[] args) {
        System.out.println("=== DÉMO CLASSE PLAYER ===\n");
        //testImportPlayersFromJson();
        //testImportSinglePlayerFromJson();
        testCombatSimulation();
    }

    private static void testImportPlayersFromJson() {
        log.info("🔹 TEST: Import de tous les joueurs depuis JSON");
        log.info("==============================================");

        PlayerImportService importService = new PlayerImportService();

        URL playersResource = PlayerDemo.class.getClassLoader().getResource("players");
        if (playersResource == null) {
            log.info("Aucun dossier /players trouvé dans les resources.");
            return;
        }

        File playersDir = new File(playersResource.getFile());
        File[] jsonFiles = playersDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

        if (jsonFiles == null || jsonFiles.length == 0) {
            log.info("Aucun fichier JSON trouvé dans /players.");
            return;
        }

        List<Player> players = new ArrayList<>();

        for (File jsonFile : jsonFiles) {
            try {
                Player player = importService.importPlayerFromJson(jsonFile.getAbsolutePath());
                if (player != null) {
                    players.add(player);
                    log.info("Import réussi: {}", player.getName());
                } else {
                    log.warn("Import retourné null pour {}", jsonFile.getName());
                }
            } catch (IOException e) {
                log.error("Erreur lors de l'import de {}", jsonFile.getName(), e);
            }
        }

        for (Player player : players) {
            player.displayEquipments();
            for (Sector sector : player.getSectors()) {
                sector.displayArmy();
            }
            player.displayStats();
        }
    }

    private static void testImportSinglePlayerFromJson() {
        log.info("🔹 TEST: Import d’un seul joueur depuis JSON");
        log.info("===========================================");

        PlayerImportService importService = new PlayerImportService();

        URL playersResource = PlayerDemo.class.getClassLoader().getResource("players");
        if (playersResource == null) {
            log.info("Aucun dossier /players trouvé dans les resources.");
            return;
        }

        File playersDir = new File(playersResource.getFile());
        File[] jsonFiles = playersDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

        if (jsonFiles == null || jsonFiles.length == 0) {
            log.info("Aucun fichier JSON trouvé dans /players.");
            return;
        }

        File jsonFile = jsonFiles[0];
        try {
            Player player = importService.importPlayerFromJson(jsonFile.getAbsolutePath());
            if (player != null) {
                log.info("Player importé: {}", player.getName());
                player.displayEquipments();
                for (Sector sector : player.getSectors()) {
                    sector.displayArmy();
                }
                player.displayStats();
            } else {
                log.warn("Import retourné null pour {}", jsonFile.getName());
            }
        } catch (IOException e) {
            log.error("Erreur lors de l'import de {}", jsonFile.getName(), e);
        }
    }

    private static void testCombatSimulation() {
        log.info("🔹 TEST: Simulation de combat entre deux joueurs");
        log.info("==============================================");

        PlayerImportService importService = new PlayerImportService();

        try {
            URL p1 = PlayerDemo.class.getClassLoader().getResource("players/player1.json");
            URL p2 = PlayerDemo.class.getClassLoader().getResource("players/player2.json");
            if (p1 == null || p2 == null) {
                throw new NullPointerException("Fichier player1.json ou player2.json introuvable dans /players");
            }

            Player defender = importService.importPlayerFromJson(p1.getFile());
            Player attacker = importService.importPlayerFromJson(p2.getFile());

            attacker.displayArmy();
            defender.displayArmy();

            Battle battleHandler = new Battle();
            battleHandler.classicCombatConfiguration(attacker, defender);

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'import des joueurs pour le test de combat", e);
        } catch (NullPointerException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
