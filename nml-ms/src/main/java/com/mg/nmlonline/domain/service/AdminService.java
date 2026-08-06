package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitEquipment;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.infrastructure.repository.ResourceRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

/**
 * Service d'administration pour la gestion avancée des joueurs.
 * Permet l'import/export JSON et la suppression complète.
 */
@Service
public class AdminService {

    private final PlayerImportService playerImportService;
    private final PlayerService playerService;
    private final BoardImportService boardImportService;
    private final BoardService boardService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final EntityManager entityManager;

    public AdminService(PlayerImportService playerImportService,
                        PlayerService playerService,
                        BoardImportService boardImportService,
                        BoardService boardService,
                        UserService userService,
                        UserRepository userRepository,
                        ResourceRepository resourceRepository,
                        EntityManager entityManager) {
        this.playerImportService = playerImportService;
        this.playerService = playerService;
        this.boardImportService = boardImportService;
        this.boardService = boardService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.entityManager = entityManager;
    }

    /**
     * Importe un joueur depuis un contenu JSON.
     * Si un joueur avec le même nom existe, il est supprimé au préalable.
     * Si {@code password} est non nul, crée/met à jour le compte User portant
     * le même nom pour permettre la connexion.
     */
    @Transactional
    public Player importPlayer(String jsonContent, String password) throws IOException {
        PlayerImportService.PlayerDTO dto = playerImportService.parse(jsonContent);
        Player player = playerImportService.importPlayer(dto);
        if (player == null) {
            throw new IllegalArgumentException("Impossible de parser le JSON du joueur");
        }

        Player existing = playerService.findByName(player.getName());
        if (existing != null) {
            playerService.delete(existing.getId());
            entityManager.flush();
        }

        player = playerService.save(player);

        if (password != null && !password.isBlank()) {
            User user = ensureCredential(player.getName(), password);
            player.setUserId(user.getId());
        }

        playerImportService.importEquipments(dto, player);
        player = playerService.save(player);

        playerImportService.importResources(dto, player);
        player = playerService.save(player);

        Board board = boardService.getAllBoards().stream().findFirst().orElse(null);
        if (board != null) {
            playerImportService.importSectors(dto, player, board);
            playerImportService.importCharacter(dto, player, board);
            playerImportService.importBuildings(dto, player, board);
            boardService.save(board);
            player = playerService.save(player);
        }

        playerImportService.clearEquipmentCache();

        return player;
    }

    /** Pour compatibilité : import sans création de compte. */
    public Player importPlayer(String jsonContent) throws IOException {
        return importPlayer(jsonContent, null);
    }

    private User ensureCredential(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            user = new User();
            user.setUsername(username);
            user.setRole("USER");
        }
        user.setPassword(userService.encodePassword(password));
        return userRepository.save(user);
    }

    /**
     * Exporte un joueur au format JSON compatible avec l'import.
     * Lecture seule : on parcourt les relations LAZY (character, buildings, sectors)
     * donc une transaction ouverte est nécessaire (open-in-view est désactivé).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportPlayer(Long playerId) {
        Player player = playerService.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable avec l'ID " + playerId));

        Board board = boardService.getAllBoards().stream().findFirst().orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", player.getName());
        result.put("money", player.getStats().getMoney());

        List<Map<String, Object>> resources = new ArrayList<>();
        for (PlayerResource pr : player.getResources()) {
            Map<String, Object> res = new LinkedHashMap<>();
            var dbResource = resourceRepository.findByName(pr.getResourceName());
            res.put("resourceId", dbResource.map(com.mg.nmlonline.domain.model.resource.Resource::getId).orElse(null));
            res.put("quantity", pr.getQuantity());
            resources.add(res);
        }
        result.put("resources", resources);

        List<Map<String, Object>> equipments = new ArrayList<>();
        for (var stack : player.getEquipments()) {
            Map<String, Object> eq = new LinkedHashMap<>();
            eq.put("name", stack.getEquipment().getName());
            eq.put("quantity", stack.getQuantity());
            equipments.add(eq);
        }
        result.put("equipments", equipments);

        List<Map<String, Object>> sectors = new ArrayList<>();
        if (board != null) {
            for (Sector sector : board.getAllSectors()) {
                if (player.getId().equals(sector.getOwnerId())) {
                    Map<String, Object> sMap = new LinkedHashMap<>();
                    sMap.put("sectorNumber", sector.getNumber());

                    List<Map<String, Object>> army = new ArrayList<>();
                    for (Unit unit : sector.getArmy()) {
                        Map<String, Object> uMap = new LinkedHashMap<>();
                        uMap.put("id", unit.getId());
                        uMap.put("type", unit.getType().name());
                        uMap.put("classes", unit.getClasses().stream()
                                .map(Enum::name).toList());
                        uMap.put("experience", unit.getExperience());

                        // Récupérer les noms d'équipements (persistés ou transients)
                        List<String> eqNames = new ArrayList<>();
                        List<UnitEquipment> unitEquipments = unit.getUnitEquipments();
                        if (unitEquipments != null && !unitEquipments.isEmpty()) {
                            for (UnitEquipment ue : unitEquipments) {
                                eqNames.add(ue.getEquipment().getName());
                            }
                        } else if (unit.getEquipments() != null) {
                            for (Equipment eq : unit.getEquipments()) {
                                eqNames.add(eq.getName());
                            }
                        }
                        uMap.put("equipments", eqNames);

                        if (unit.isInjured()) {
                            uMap.put("isInjured", true);
                        }
                        army.add(uMap);
                    }
                    sMap.put("army", army);
                    sectors.add(sMap);
                }
            }
        }
        result.put("sectors", sectors);

        GameCharacter character = player.getCharacter();
        if (character != null) {
            Map<String, Object> cMap = new LinkedHashMap<>();
            cMap.put("name", character.getName());
            cMap.put("sectorNumber", character.getSector() != null ? character.getSector().getNumber() : 0);
            cMap.put("baseAttack", character.getBaseAttack());
            cMap.put("baseDefense", character.getBaseDefense());
            cMap.put("basePdf", character.getBasePdf());
            cMap.put("basePdc", character.getBasePdc());
            cMap.put("baseArmor", character.getBaseArmor());
            cMap.put("baseEvasion", character.getBaseEvasion());
            result.put("character", cMap);
        } else {
            result.put("character", null);
        }

        List<Map<String, Object>> buildingsList = new ArrayList<>();
        for (Building building : player.getBuildings()) {
            Map<String, Object> bMap = new LinkedHashMap<>();
            bMap.put("type", building.getBuildingType() != null ? building.getBuildingType().name() : null);
            bMap.put("sectorNumber", building.getSector() != null ? building.getSector().getNumber() : 0);
            buildingsList.add(bMap);
        }
        result.put("buildings", buildingsList);

        return result;
    }

    /**
     * Supprime un joueur, réinitialise ses secteurs, et supprime le compte User
     * portant le même nom (s'il existe et n'est pas admin) pour éviter un
     * compte orphelin permettant une connexion sur un joueur absent.
     */
    @Transactional
    public void deletePlayer(Long playerId) {
        Player player = playerService.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable avec l'ID " + playerId));
        String name = player.getName();
        if (!playerService.delete(playerId)) {
            throw new IllegalArgumentException("Joueur introuvable avec l'ID " + playerId);
        }
        User user = userRepository.findByUsername(name);
        if (user != null && !"ADMIN".equals(user.getRole())) {
            userRepository.delete(user);
        }
    }

    /**
     * Importe un Board depuis un contenu JSON au format board.json (liste plate de secteurs).
     * Si {@code mapImageUrl} / {@code svgOverlayUrl} sont fournis, ils override les URLs du JSON
     * (typiquement les URLs renvoyées par {@code POST /api/admin/boards/assets}).
     * L'upsert se fait par nom via {@link BoardService#saveBoard(Board, String)}.
     */
    @Transactional
    public Board importBoard(String jsonContent, String mapImageUrl, String svgOverlayUrl) throws IOException {
        Board board = boardImportService.importBoardFromJson(jsonContent);
        if (mapImageUrl != null && !mapImageUrl.isBlank()) {
            board.setMapImageUrl(mapImageUrl);
        }
        if (svgOverlayUrl != null && !svgOverlayUrl.isBlank()) {
            board.setSvgOverlayUrl(svgOverlayUrl);
        }
        String boardName = board.getName() != null ? board.getName() : "Carte Principale";
        return boardService.saveBoard(board, boardName);
    }
}
