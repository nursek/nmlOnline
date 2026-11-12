package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.BoardImportService;
import com.mg.nmlonline.domain.service.BoardService;
import com.mg.nmlonline.domain.service.PlayerImportService;
import com.mg.nmlonline.domain.service.PlayerService;
import com.mg.nmlonline.domain.service.UserService;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@Order(2)
public class PlayerStartupImporter implements ApplicationRunner {

    private record DemoGame(String boardName, String boardPath, List<String> players) {
    }

    static final DemoGame MAIN_GAME = new DemoGame("Carte Principale", "boards/board.json", List.of(
            "players/lurio.json", "players/nursek.json", "players/mortarion.json", "players/angron.json",
            "players/cegorach.json", "players/imotekh.json", "players/trazyn.json"));

    static final DemoGame ARCS_GAME = new DemoGame("Carte 6 Joueurs (Arcs)", "boards/carte-25.json", List.of(
            "players-carte-25/lurio.json", "players-carte-25/nursek.json", "players-carte-25/mortarion.json",
            "players-carte-25/angron.json", "players-carte-25/cegorach.json", "players-carte-25/imotekh.json"));

    private final PlayerImportService playerImportService;
    private final PlayerService playerService;
    private final BoardService boardService;
    private final BoardImportService boardImportService;
    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${app.import-demo-data:true}")
    private boolean importDemoData;

    public PlayerStartupImporter(PlayerImportService playerImportService,
                                 PlayerService playerService,
                                 BoardService boardService,
                                 BoardImportService boardImportService,
                                 UserRepository userRepository,
                                 UserService userService) {
        this.playerImportService = playerImportService;
        this.playerService = playerService;
        this.boardService = boardService;
        this.boardImportService = boardImportService;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Démarrage de l'import des données ===");

        if (!importDemoData) {
            log.info("Import des données de démo désactivé (app.import-demo-data=false). " +
                     "L'admin doit importer le board et les joueurs via l'API.");
            return;
        }

        importGame(MAIN_GAME);
        importGame(ARCS_GAME);

        log.info("=== Import des données terminé ===");
    }

    private void importGame(DemoGame game) {
        Board board = importBoard(game.boardPath());
        if (board == null) {
            log.error("Impossible d'importer le plateau {} ! Partie ignorée.", game.boardPath());
            return;
        }
        log.info("Board '{}' importé en mémoire avec {} secteurs neutres",
                game.boardName(), board.getAllSectors().size());

        // Persister le board avant les joueurs : sinon les @ManyToOne sector (FK composite
        // board_id+sector_number, sans cascade) pointent vers des secteurs transients → Hibernate écrit la FK à NULL.
        board = boardService.saveBoard(board, game.boardName());
        log.info("Board neutre persisté avec {} secteurs (board_id={})", board.getAllSectors().size(), board.getId());

        for (String player : game.players()) {
            importIfPresent(new ClassPathResource(player), board);
        }

        log.info("Sauvegarde finale du Board (merge) avec {} secteurs...", board.getAllSectors().size());
        boardService.save(board);
        long ownedCount = board.getAllSectors().stream().filter(s -> !s.isNeutral()).count();
        log.info("✅ Board '{}' sauvegardé : {} secteurs neutres + {} secteurs de players",
                game.boardName(), board.getAllSectors().size() - ownedCount, ownedCount);
    }

    private Board importBoard(String path) {
        Resource resource = new ClassPathResource(path);
        try {
            if (!resource.exists()) {
                log.warn("Fichier {} introuvable", path);
                return null;
            }

            try (InputStream is = resource.getInputStream()) {
                log.info("Import du Board depuis : {}", resource.getFilename());

                Board board = boardImportService.importBoardFromJson(new String(is.readAllBytes(), StandardCharsets.UTF_8));
                log.info("Board importé en mémoire avec {} secteurs", board.getAllSectors().size());

                return board;
            }
        } catch (Exception e) {
            log.error("Échec import de {} : {}", path, e.getMessage(), e);
            return null;
        }
    }

    private void importIfPresent(Resource resource, Board board) {

        try {
            if (resource == null || !resource.exists()) {
                log.warn("Ressource non trouvée : {}", resource);
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                log.info("Import du joueur depuis : {}", resource.getFilename());

                PlayerImportService.PlayerDTO dto = playerImportService.parse(
                        new String(is.readAllBytes(), StandardCharsets.UTF_8));

                Player player = playerImportService.importPlayer(dto, board.getId());

                if (player != null) {
                    // Réimporter = remplacer, pas fusionner : une fusion duplique l'armée, et les
                    // doublons n'ont plus d'équipement (stock du joueur déjà consommé).
                    Player existingPlayer = playerService.findByName(board.getId(), player.getName());
                    if (existingPlayer != null) {
                        log.info("Joueur {} déjà existant, remplacement...", player.getName());
                        playerService.delete(existingPlayer.getId());
                    }

                    User user = userRepository.findByUsername(player.getName());
                    if (user == null) {
                        // Fixture sans compte (démo prod) : même convention que DevDataInitializer, mdp = nom.
                        user = new User();
                        user.setUsername(player.getName());
                        user.setPassword(userService.encodePassword(player.getName()));
                        user.setRole("USER");
                        user = userRepository.save(user);
                        log.info("Compte démo créé pour {}", player.getName());
                    }
                    player.setUserId(user.getId());
                    log.info("userId {} lié au joueur {}", user.getId(), player.getName());
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

                    playerImportService.importVehicles(dto, player, board);
                    log.info("Véhicules importés pour {} (en mémoire)", player.getName());

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
