package com.mg.nmlonline.test;

import com.mg.nmlonline.entity.player.Player;
import com.mg.nmlonline.entity.sector.Sector;
import com.mg.nmlonline.service.PlayerService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Test de la classe Player avec tri des unités
 */
@Slf4j
public class PlayerTestDemo {

    public static void main(String[] args) {
        System.out.println("=== DÉMO CLASSE PLAYER ===\n");
        testImportSinglePlayerFromJson();
        //testImportPlayersFromJson();
    }

    private static void testImportPlayersFromJson() {
        log.info("🔹 TEST: Import de tous les joueurs depuis JSON");
        log.info("==============================================");

        PlayerService playerService = new PlayerService();
        File playersDir = new File(PlayerTestDemo.class.getClassLoader().getResource("players").getFile());
        File[] jsonFiles = playersDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (jsonFiles == null || jsonFiles.length == 0) {
            log.info("Aucun fichier JSON trouvé dans /players.");
            return;
        }

        List<Player> players = new ArrayList<>();

        for (File jsonFile : jsonFiles) {
            try {
                Player player = playerService.importPlayerFromJson(jsonFile.getPath());
                players.add(player);
            } catch (Exception e) {
                log.error("Erreur lors de l'import de {}", jsonFile.getName(), e);
            }
        }

        for (Player player : players) {
            player.displayEquipments();
            for (Sector sector : player.getSectors()) {
                sector.displayArmy(); // Affiche l'armée du quartier
            }
            player.displayStats();
        }
    }

    private static void testImportSinglePlayerFromJson() {
        log.info("🔹 TEST: Import d’un seul joueur depuis JSON");

        PlayerService playerService = new PlayerService();
        File playersDir = new File(PlayerTestDemo.class.getClassLoader().getResource("players").getFile());
        File[] jsonFiles = playersDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (jsonFiles == null || jsonFiles.length == 0) {
            return;
        }

        File jsonFile = jsonFiles[0];
        try {
            Player player = playerService.importPlayerFromJson(jsonFile.getPath());
            player.refreshEquipmentAvailability();
            player.reassignUnitNumbers();
            player.displayEquipments();
            if (!(player.equipToUnit(2, 10, "Pistolet 9mm")))
                System.out.println("fail");

            for (Sector sector : player.getSectors()) {
                sector.displayArmy();
            }
            if(!(player.transferUnitBetweenSectors(player.getUnitById(10), 2, 1))){
                System.out.println("fail to transfer unit");
            }

            player.reassignUnitNumbers();
            for (Sector sector : player.getSectors()) {
                sector.displayArmy();
            }
            //playerService.savePlayerToJson(player, "src/main/resources/players/player1-test.json");
            // Ajoute ici d’autres tests spécifiques sur le joueur
        } catch (Exception e) {
            log.error("Erreur lors de l'import de {}", jsonFile.getName(), e);
        }
    }
}