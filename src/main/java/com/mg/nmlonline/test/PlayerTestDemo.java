package com.mg.nmlonline.test;

import com.mg.nmlonline.model.equipement.EquipmentType;
import com.mg.nmlonline.model.player.Player;
import com.mg.nmlonline.model.unit.Unit;
import com.mg.nmlonline.model.unit.UnitClass;
import com.mg.nmlonline.model.equipement.EquipmentFactory;
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
        // Test 2: Import de tous les joueurs depuis JSON
        testImportPlayersFromJson();
        testFileArmy();

    }

    private static void testImportPlayersFromJson() {
        System.out.println("🔹 TEST: Import de tous les joueurs depuis JSON");
        System.out.println("==============================================");

        PlayerService playerService = new PlayerService();
        File playersDir = new File("src/main/resources/players/");
        File[] jsonFiles = playersDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("Aucun fichier JSON trouvé dans /players.");
            return;
        }

        // Liste pour stocker tous les joueurs importés
        List<Player> players = new ArrayList<>();

        for (File jsonFile : jsonFiles) {
            try {
                Player player = playerService.importPlayerFromJson(jsonFile.getPath());
                players.add(player);
            } catch (Exception e) {
                log.error("Erreur lors de l'import de " + jsonFile.getName(), e);
            }
        }

        // Affichage de chaque armée
        for (Player player : players) {
            player.displayArmy();
            player.displayEquipments();
            System.out.println("===============================================");
        }

    }

    private static void testFileArmy(){
        System.out.println("🔹 TEST: FILE ARMY (reproduction exemple)");
        System.out.println("===============================================");

        Player player = new Player("Ratcatcher");
        try {
            player.fromFile("src/main/resources/ratcatcher.txt");
            player.displayArmy();
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du fichier d'armée : " + e.getMessage());
        }
    }

}