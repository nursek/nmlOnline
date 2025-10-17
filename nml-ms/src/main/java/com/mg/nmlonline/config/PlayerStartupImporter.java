package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.service.PlayerImportService;
import com.mg.nmlonline.domain.service.PlayerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class PlayerStartupImporter implements ApplicationRunner {

    private final PlayerImportService playerImportService;
    private final PlayerService playerService;

    @Value("classpath:players/player1.json")
    private Resource player1;

    @Value("classpath:players/player2.json")
    private Resource player2;

    public PlayerStartupImporter(PlayerImportService playerImportService,
                                 PlayerService playerService) {
        this.playerImportService = playerImportService;
        this.playerService = playerService;
    }

    @Override
    public void run(ApplicationArguments args) {
        importIfPresent(player1);
        importIfPresent(player2);
    }

    private void importIfPresent(Resource resource) {
        try {
            if (resource == null || !resource.exists()) return;
            try (InputStream is = resource.getInputStream()) {
                Path tmp = Files.createTempFile("player-import-", ".json");
                Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                Player player = playerImportService.importPlayerFromJson(tmp.toString());
                if (player != null && playerService.findByName(player.getName()) == null) {
                    playerService.create(player);
                    System.out.println("Imported player " + player.getName());
                }
                Files.deleteIfExists(tmp);
            }
        } catch (Exception e) {
            System.err.println("Échec import " + resource.getFilename() + " : " + e.getMessage());
        }
    }
}
