package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.service.BoardImportService;
import com.mg.nmlonline.domain.service.BoardService;
import com.mg.nmlonline.domain.service.PlayerImportService;
import com.mg.nmlonline.domain.service.PlayerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Service d'import automatique des joueurs au démarrage de l'application.
 * Charge les fichiers JSON des joueurs et les importe dans le Board.
 */
@Slf4j
@Component
public class PlayerStartupImporter implements ApplicationRunner {

    private final PlayerImportService playerImportService;
    private final PlayerService playerService;
    private final BoardService boardService;
    private final BoardImportService boardImportService;

    @Value("classpath:boards/board.json")
    private Resource boardResource;

    @Value("classpath:players/lurio.json")
    private Resource player1;

    @Value("classpath:players/nursek.json")
    private Resource player2;

    public PlayerStartupImporter(PlayerImportService playerImportService,
                                 PlayerService playerService,
                                 BoardService boardService,
                                 BoardImportService boardImportService) {
        this.playerImportService = playerImportService;
        this.playerService = playerService;
        this.boardService = boardService;
        this.boardImportService = boardImportService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Démarrage de l'import des données ===");

        // Importer le Board depuis board.json (secteurs neutres en mémoire)
        Board board = importBoard();
        if (board == null) {
            log.error("Impossible d'importer le Board ! Arrêt de l'import.");
            return;
        }
        log.info("Board importé en mémoire avec {} secteurs neutres", board.getAllSectors().size());

        // Importer les joueurs qui ajouteront leurs secteurs au Board en mémoire
        importIfPresent(player1, board);
        importIfPresent(player2, board);

        // Sauvegarder le Board UNE SEULE FOIS avec TOUS les secteurs (neutres + players)
        log.info("Sauvegarde du Board complet avec {} secteurs...", board.getAllSectors().size());
        boardService.saveBoard(board, "Carte Principale");
        log.info("✅ Board sauvegardé : {} secteurs neutres + {} secteurs de players",
                 board.getAllSectors().size() - 3, 3);

        log.info("=== Import des données terminé ===");
    }

    private Board importBoard() {
        try {
            if (boardResource == null || !boardResource.exists()) {
                log.warn("Fichier board.json non trouvé");
                return null;
            }

            try (InputStream is = boardResource.getInputStream()) {
                Path tmp = Files.createTempFile("board-import-", ".json");
                Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                String filePath = tmp.toString();

                log.info("Import du Board depuis : {}", boardResource.getFilename());

                // Importer le Board (en mémoire uniquement, pas encore sauvegardé)
                Board board = boardImportService.importBoardFromJson(filePath);
                log.info("Board importé en mémoire avec {} secteurs", board.getAllSectors().size());

                Files.deleteIfExists(tmp);
                return board;
            }
        } catch (Exception e) {
            log.error("Échec import board.json : {}", e.getMessage(), e);
            return null;
        }
    }

    public void importIfPresent(Resource resource, Board board) {
        try {
            if (resource == null || !resource.exists()) {
                log.warn("Ressource non trouvée : {}", resource);
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                // Créer un fichier temporaire pour l'import
                Path tmp = Files.createTempFile("player-import-", ".json");
                Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                String filePath = tmp.toString();

                log.info("Import du joueur depuis : {}", resource.getFilename());

                // 1. Importer le joueur (stats uniquement, SANS équipements)
                Player player = playerImportService.importPlayerFromJson(filePath);

                if (player != null) {
                    // 2. Vérifier si le joueur existe déjà ou le créer
                    Player existingPlayer = playerService.findByName(player.getName());
                    if (existingPlayer != null) {
                        log.info("Joueur {} déjà existant, mise à jour...", player.getName());
                        player.setId(existingPlayer.getId());
                        player = playerService.save(player);
                    } else {
                        // Créer le joueur en base pour obtenir un ID (SANS équipements)
                        player = playerService.create(player);
                        log.info("Joueur {} créé avec l'ID {}", player.getName(), player.getId());
                    }

                    // 3. Maintenant que le Player est persisté, ajouter les équipements
                    playerImportService.importEquipmentsToPlayer(filePath, player);
                    log.info("Équipements importés pour {}", player.getName());

                    // 3b. Importer les ressources
                    playerImportService.importResourcesToPlayer(filePath, player);
                    log.info("Ressources importées pour {}", player.getName());

                    // Sauvegarder le joueur avec ses équipements et ressources AVANT d'importer les secteurs
                    player = playerService.save(player);

                    // 4. Importer les secteurs et unités dans le Board (en mémoire)
                    playerImportService.importSectorsToBoard(filePath, player, board);
                    log.info("Secteurs et unités importés pour {} (en mémoire)", player.getName());

                    // Afficher les stats calculées
                    log.info("✓ Stats {} : ATK={}, DEF={}, ARMOR={}, Income={}, EconomyPower={}",
                             player.getName(),
                             player.getStats().getTotalAtk(),
                             player.getStats().getTotalDef(),
                             player.getStats().getTotalArmor(),
                             player.getStats().getTotalIncome(),
                             player.getStats().getTotalEconomyPower());

                    // 5. Sauvegarder le joueur avec les stats à jour (pas les équipements qui sont déjà sauvés)
                    player = playerService.save(player);
                    log.info("✓ Joueur {} sauvegardé en base", player.getName());

                    log.info("Joueur {} prêt avec {} secteurs", player.getName(), player.getOwnedSectorCount());
                }

                // Nettoyer le fichier temporaire
                Files.deleteIfExists(tmp);
            }
        } catch (Exception e) {
            log.error("Échec import {} : {}", resource.getFilename(), e.getMessage(), e);
        }
    }
}
