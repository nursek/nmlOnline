package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.BoardImportService;
import com.mg.nmlonline.domain.service.BoardService;
import com.mg.nmlonline.domain.service.PlayerImportService;
import com.mg.nmlonline.domain.service.PlayerService;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
    private final UserRepository userRepository;

    @Value("${app.import-demo-players:true}")
    private boolean importDemoPlayers;

    @Value("classpath:boards/board.json")
    private Resource boardResource;

    @Value("classpath:players/lurio.json")
    private Resource player1;

    @Value("classpath:players/nursek.json")
    private Resource player2;

    public PlayerStartupImporter(PlayerImportService playerImportService,
                                 PlayerService playerService,
                                 BoardService boardService,
                                 BoardImportService boardImportService,
                                 UserRepository userRepository) {
        this.playerImportService = playerImportService;
        this.playerService = playerService;
        this.boardService = boardService;
        this.boardImportService = boardImportService;
        this.userRepository = userRepository;
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

        // Importer les joueurs qui ajouteront leurs secteurs au Board en mémoire.
        // Désactivé en prod : seul le board neutre est importé, l'admin crée les joueurs via l'API.
        if (importDemoPlayers) {
            importIfPresent(player1, board);
            importIfPresent(player2, board);
        } else {
            log.info("Import des joueurs de démo désactivé (app.import-demo-players=false).");
        }

        // Sauvegarder le Board UNE SEULE FOIS avec TOUS les secteurs (neutres + players)
        log.info("Sauvegarde du Board complet avec {} secteurs...", board.getAllSectors().size());
        boardService.saveBoard(board, "Carte Principale");
        int neutralCount = importDemoPlayers ? board.getAllSectors().size() - 3 : board.getAllSectors().size();
        int playerCount = importDemoPlayers ? 3 : 0;
        log.info("✅ Board sauvegardé : {} secteurs neutres + {} secteurs de players",
                 neutralCount, playerCount);

        log.info("=== Import des données terminé ===");
    }

    private Board importBoard() {
        try {
            if (boardResource == null || !boardResource.exists()) {
                log.warn("Fichier board.json non trouvé");
                return null;
            }

            try (InputStream is = boardResource.getInputStream()) {
                log.info("Import du Board depuis : {}", boardResource.getFilename());

                // Importer le Board (en mémoire uniquement, pas encore sauvegardé)
                Board board = boardImportService.importBoardFromJson(new String(is.readAllBytes(), StandardCharsets.UTF_8));
                log.info("Board importé en mémoire avec {} secteurs", board.getAllSectors().size());

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
                log.info("Import du joueur depuis : {}", resource.getFilename());

                // Parser le JSON une seule fois, puis passer le DTO aux imports
                PlayerImportService.PlayerDTO dto = playerImportService.parse(
                        new String(is.readAllBytes(), StandardCharsets.UTF_8));

                // 1. Importer le joueur (stats uniquement, SANS équipements)
                Player player = playerImportService.importPlayer(dto);

                if (player != null) {
                    // 2. Vérifier si le joueur existe déjà ou le créer
                    Player existingPlayer = playerService.findByName(player.getName());
                    if (existingPlayer != null) {
                        log.info("Joueur {} déjà existant, mise à jour...", player.getName());
                        player.setId(existingPlayer.getId());
                        // Conserver le userId ou le résoudre si absent
                        if (existingPlayer.getUserId() != null) {
                            player.setUserId(existingPlayer.getUserId());
                        } else {
                            User user = userRepository.findByUsername(player.getName());
                            if (user != null) {
                                player.setUserId(user.getId());
                                log.info("userId {} lié au joueur {}", user.getId(), player.getName());
                            } else {
                                log.warn("Aucun compte CREDENTIALS trouvé pour le joueur '{}'", player.getName());
                            }
                        }
                        player = playerService.save(player);
                    } else {
                        // Lier le userId avant la création
                        User user = userRepository.findByUsername(player.getName());
                        if (user != null) {
                            player.setUserId(user.getId());
                            log.info("userId {} lié au joueur {}", user.getId(), player.getName());
                        } else {
                            log.warn("Aucun compte CREDENTIALS trouvé pour le joueur '{}'", player.getName());
                        }
                        // Créer le joueur en base pour obtenir un ID (SANS équipements)
                        player = playerService.save(player);
                        log.info("Joueur {} créé avec l'ID {}", player.getName(), player.getId());
                    }

                    // 3. Maintenant que le Player est persisté, ajouter les équipements
                    playerImportService.importEquipments(dto, player);
                    log.info("Équipements importés pour {}", player.getName());

                    // 3b. Importer les ressources
                    playerImportService.importResources(dto, player);
                    log.info("Ressources importées pour {}", player.getName());

                    // 3c. Importer le personnage principal (GameCharacter) si défini
                    var character = playerImportService.importCharacter(dto, player, board);
                    if (character != null) {
                        log.info("Personnage '{}' importé pour {} (ATK={}, DEF={}, PDF={})",
                                character.getName(), player.getName(),
                                character.getBaseAttack(), character.getBaseDefense(), character.getBasePdf());
                    }

                    // Sauvegarder le joueur avec ses équipements et ressources AVANT d'importer les secteurs
                    player = playerService.save(player);

                    // 4. Importer les secteurs et unités dans le Board (en mémoire)
                    playerImportService.importSectors(dto, player, board);
                    log.info("Secteurs et unités importés pour {} (en mémoire)", player.getName());

                    // 4b. Importer les bâtiments dans le Board
                    var buildings = playerImportService.importBuildings(dto, player, board);
                    if (!buildings.isEmpty()) {
                        log.info("{} bâtiment(s) importé(s) pour {}", buildings.size(), player.getName());
                    }

                    // Afficher les stats calculées
                    log.info("✓ Stats {} : ATK={}, DEF={}, ARMOR={}, Income={}, EconomyPower={}",
                             player.getName(),
                             player.getStats().getTotalAtk(),
                             player.getStats().getTotalDef(),
                             player.getStats().getTotalArmor(),
                             player.getStats().getTotalIncome(),
                             player.getStats().getTotalEconomyPower());

                    // 5. Sauvegarder le joueur avec les stats à jour (pas les équipements qui sont déjà sauvés)
                    log.info("✓ Joueur {} sauvegardé en base", player.getName());
                    log.info("Joueur {} prêt avec {} secteurs", player.getName(), board.getSectorsByOwner(player.getId()).size());
                    playerService.save(player);

                }
            }
        } catch (Exception e) {
            log.error("Échec import {} : {}", resource.getFilename(), e.getMessage(), e);
        }
    }
}
