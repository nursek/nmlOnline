package com.mg.nmlonline.test;

import com.mg.nmlonline.model.player.Player;
import com.mg.nmlonline.model.sector.Sector;
import com.mg.nmlonline.model.unit.Unit;
import com.mg.nmlonline.model.unit.UnitClass;
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
        testImportPlayersFromJson();
        //testFileArmy();
        //testPlayerMethods();
    }

    private static void testImportPlayersFromJson() {
        log.info("🔹 TEST: Import de tous les joueurs depuis JSON");
        log.info("==============================================");

        PlayerService playerService = new PlayerService();
        File playersDir = new File("src/main/resources/players/");
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

    private static void testFileArmy(){
        log.info("🔹 TEST: FILE ARMY (reproduction exemple)");
        log.info("===============================================");
        PlayerService playerService = new PlayerService();
        try {
            Player player = playerService.fromFile("src/main/resources/players/ratcatcher.txt");
            player.displayArmy();
        } catch (Exception e) {
            log.info("Erreur lors de la lecture du fichier d'armée : " + e.getMessage());
        }
    }

    private static void testPlayerMethods() {
        System.out.println("🔹 TEST: Méthodes de la classe Player");
        System.out.println("================================================");
        Player player = new Player("TestPlayer");
        Sector sector = new Sector(1);
        player.addSector(sector);

        Unit unit = new Unit(1, "TestUnit", UnitClass.TIREUR);
        if(player.addUnitToSector(unit, 1)) {
            System.out.println("Unité ajoutée avec succès.");
        } else {
            System.out.println("Échec de l'ajout de l'unité.");
        }

        player.displayArmy();
        player.getStats().setMoney(500);
        player.getStats().setTotalEquipmentValue(10000);
        player.displayStats();
    }
}