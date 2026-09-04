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

@Slf4j
@Component
public class PlayerStartupImporter implements ApplicationRunner {

    private final PlayerImportService playerImportService;
    private final PlayerService playerService;
    private final BoardService boardService;
    private final BoardImportService boardImportService;
    private final UserRepository userRepository;

    @Value("${app.import-demo-data:true}")
    private boolean importDemoData;

    @Value("classpath:boards/board.json")
    private Resource boardResource;

    @Value("classpath:players/lurio.json")
    private Resource player1;

    @Value("classpath:players/nursek.json")
    private Resource player2;

    @Value("classpath:players/mortarion.json")
    private Resource player3;

    @Value("classpath:players/angron.json")
    private Resource player4;

    @Value("classpath:players/cegorach.json")
    private Resource player5;

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

        // prod (app.import-demo-data=false) : rien n'est importé ; l'admin crée le board et les joueurs via l'API.
        if (!importDemoData) {
            log.info("Import des données de démo désactivé (app.import-demo-data=false). " +
                     "L'admin doit importer le board et les joueurs via l'API.");
            return;
        }

        Board board = importBoard();
        if (board == null) {
            log.error("Impossible d'importer le Board ! Arrêt de l'import.");
            return;
        }
        log.info("Board importé en mémoire avec {} secteurs neutres", board.getAllSectors().size());

        // Persister le board avant les joueurs : sinon les @ManyToOne sector (FK composite
        // board_id+sector_number, sans cascade) pointent vers des secteurs transients → Hibernate écrit la FK à NULL.
        board = boardService.saveBoard(board, "Carte Principale");
        log.info("Board neutre persisté avec {} secteurs (board_id={})", board.getAllSectors().size(), board.getId());

        importIfPresent(player1, board);
        importIfPresent(player2, board);
        importIfPresent(player3, board);
        importIfPresent(player4, board);
        importIfPresent(player5, board);

        log.info("Sauvegarde finale du Board (merge) avec {} secteurs...", board.getAllSectors().size());
        boardService.save(board);
        long ownedCount = board.getAllSectors().stream().filter(s -> !s.isNeutral()).count();
        log.info("✅ Board sauvegardé : {} secteurs neutres + {} secteurs de players",
                 board.getAllSectors().size() - ownedCount, ownedCount);

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

                PlayerImportService.PlayerDTO dto = playerImportService.parse(
                        new String(is.readAllBytes(), StandardCharsets.UTF_8));

                Player player = playerImportService.importPlayer(dto);

                if (player != null) {
                    // Réimporter = remplacer, pas fusionner : une fusion duplique l'armée, et les
                    // doublons n'ont plus d'équipement (stock du joueur déjà consommé).
                    Player existingPlayer = playerService.findByName(player.getName());
                    if (existingPlayer != null) {
                        log.info("Joueur {} déjà existant, remplacement...", player.getName());
                        playerService.delete(existingPlayer.getId());
                    }

                    User user = userRepository.findByUsername(player.getName());
                    if (user != null) {
                        player.setUserId(user.getId());
                        log.info("userId {} lié au joueur {}", user.getId(), player.getName());
                    } else {
                        log.warn("Aucun compte CREDENTIALS trouvé pour le joueur '{}'", player.getName());
                    }
                    player = playerService.save(player);
                    log.info("Joueur {} créé avec l'ID {}", player.getName(), player.getId());

                    // Équipements/ressources/character ajoutés après persistance du Player (besoin de son ID).
                    playerImportService.importEquipments(dto, player);
                    log.info("Équipements importés pour {}", player.getName());

                    playerImportService.importResources(dto, player);
                    log.info("Ressources importées pour {}", player.getName());

                    var character = playerImportService.importCharacter(dto, player, board);
                    if (character != null) {
                        log.info("Personnage '{}' importé pour {} (ATK={}, DEF={}, PDF={})",
                                character.getName(), player.getName(),
                                character.getBaseAttack(), character.getBaseDefense(), character.getBasePdf());
                    }

                    player = playerService.save(player);

                    playerImportService.importSectors(dto, player, board);
                    log.info("Secteurs et unités importés pour {} (en mémoire)", player.getName());

                    var buildings = playerImportService.importBuildings(dto, player, board);
                    if (!buildings.isEmpty()) {
                        log.info("{} bâtiment(s) importé(s) pour {}", buildings.size(), player.getName());
                    }

                    log.info("✓ Stats {} : ATK={}, DEF={}, ARMOR={}, Income={}, EconomyPower={}",
                             player.getName(),
                             player.getStats().getTotalAtk(),
                             player.getStats().getTotalDef(),
                             player.getStats().getTotalArmor(),
                             player.getStats().getTotalIncome(),
                             player.getStats().getTotalEconomyPower());

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
