package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.AdminBoardAdminDto;
import com.mg.nmlonline.api.dto.AdminUserDto;
import com.mg.nmlonline.api.dto.BoardDto;
import com.mg.nmlonline.api.dto.PlayerDto;
import com.mg.nmlonline.domain.model.audit.AdminAuditAction;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.board.BoardAdmin;
import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitEquipment;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.infrastructure.repository.BoardAdminRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import com.mg.nmlonline.mapper.BoardMapper;
import com.mg.nmlonline.mapper.PlayerMapper;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private final PlayerImportService playerImportService;
    private final PlayerService playerService;
    private final BoardImportService boardImportService;
    private final BoardService boardService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final EntityManager entityManager;
    private final PlayerMapper playerMapper;
    private final BoardMapper boardMapper;
    private final AdminAuditService adminAuditService;
    private final BoardAdminRepository boardAdminRepository;

    public AdminService(PlayerImportService playerImportService,
                        PlayerService playerService,
                        BoardImportService boardImportService,
                        BoardService boardService,
                        UserService userService,
                        UserRepository userRepository,
                        VehicleRepository vehicleRepository,
                        EntityManager entityManager,
                        PlayerMapper playerMapper,
                        BoardMapper boardMapper,
                        AdminAuditService adminAuditService,
                        BoardAdminRepository boardAdminRepository) {
        this.playerImportService = playerImportService;
        this.playerService = playerService;
        this.boardImportService = boardImportService;
        this.boardService = boardService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.entityManager = entityManager;
        this.playerMapper = playerMapper;
        this.boardMapper = boardMapper;
        this.adminAuditService = adminAuditService;
        this.boardAdminRepository = boardAdminRepository;
    }

    /** Import JSON : supprime un joueur homonyme au préalable ; si password est fourni, crée le compte User absent (un compte existant n'est jamais modifié). */
    @Transactional
    public Player importPlayer(String jsonContent, String password, Long boardId) throws IOException {
        PlayerImportService.PlayerDTO dto = playerImportService.parse(jsonContent);
        Player player = playerImportService.importPlayer(dto, boardId);
        if (player == null) {
            throw new IllegalArgumentException("Impossible de parser le JSON du joueur");
        }

        Player existing = playerService.findByName(boardId, player.getName());
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

        Board board = boardService.getBoardById(boardId).orElse(null);
        if (board != null) {
            playerImportService.importVehicles(dto, player, board);
            playerImportService.importSectors(dto, player, board);
            playerImportService.importCharacter(dto, player, board);
            playerImportService.importBuildings(dto, player, board);
            boardService.save(board);
            player = playerService.save(player);
        }

        playerImportService.clearEquipmentCache();

        logger.info("Import joueur '{}' (id {})", player.getName(), player.getId());
        adminAuditService.record(boardId, player.getId(), AdminAuditAction.IMPORT_PLAYER,
                "Import : " + player.getName());
        return player;
    }

    public Player importPlayer(String jsonContent, Long boardId) throws IOException {
        return importPlayer(jsonContent, null, boardId);
    }

    /** Import + mapping dans une seule transaction : le DTO parcourt equipments/resources/buildings (LAZY). */
    @Transactional
    public PlayerDto importPlayerDto(String jsonContent, String password, Long boardId) throws IOException {
        Player player = importPlayer(jsonContent, password, boardId);
        Board board = boardService.getBoardById(boardId).orElse(null);
        return playerMapper.toDtoWithSectors(player, board);
    }

    @Transactional
    public void grantBoardAdmin(Long boardId, Long userId) {
        requireBoardAndUser(boardId, userId);
        if (!boardAdminRepository.existsByBoardIdAndUserId(boardId, userId)) {
            boardAdminRepository.save(new BoardAdmin(boardId, userId));
        }
    }

    @Transactional
    public void revokeBoardAdmin(Long boardId, Long userId) {
        boardAdminRepository.findByUserId(userId).stream()
                .filter(admin -> admin.getBoardId().equals(boardId))
                .forEach(boardAdminRepository::delete);
    }

    @Transactional
    public void linkAccount(Long playerId, String username) {
        Player player = playerService.findByIdForUpdate(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Joueur introuvable : " + playerId);
        }
        Board board = boardService.getBoardByIdForUpdate(player.getBoardId())
                .orElseThrow(() -> new IllegalStateException("Plateau introuvable"));
        if (!board.isPreparation()) {
            throw new IllegalStateException("La partie a déjà démarré : impossible de relier un compte");
        }
        if (player.getUserId() != null) {
            throw new IllegalStateException("Ce joueur a déjà un compte relié");
        }
        User user = userRepository.findByUsername(username.trim());
        if (user == null) {
            throw new IllegalArgumentException("Compte introuvable : " + username.trim());
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new SecurityException("Un compte administrateur ne peut pas être relié à un joueur");
        }
        Player existing = playerService.findByUserId(user.getId(), player.getBoardId());
        if (existing != null) {
            throw new IllegalStateException("Ce compte est déjà relié à un joueur de cette partie");
        }
        player.setUserId(user.getId());
        playerService.save(player);
        adminAuditService.record(player.getBoardId(), playerId, AdminAuditAction.LINK_ACCOUNT,
                "Compte relié : " + user.getUsername());
    }

    @Transactional
    public void unlinkAccount(Long playerId) {
        Player player = playerService.findByIdForUpdate(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Joueur introuvable : " + playerId);
        }
        Board board = boardService.getBoardByIdForUpdate(player.getBoardId())
                .orElseThrow(() -> new IllegalStateException("Plateau introuvable"));
        if (!board.isPreparation()) {
            throw new IllegalStateException("La partie a déjà démarré : impossible de délier un compte");
        }
        if (player.getUserId() == null) {
            return;
        }
        String username = userRepository.findById(player.getUserId())
                .map(User::getUsername)
                .orElse("inconnu");
        player.setUserId(null);
        playerService.save(player);
        adminAuditService.record(player.getBoardId(), playerId, AdminAuditAction.UNLINK_ACCOUNT,
                "Compte délié : " + username);
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> searchUsers(String query, Pageable pageable) {
        String q = query == null ? "" : query.trim();
        var page = q.isEmpty()
                ? userRepository.findAll(pageable)
                : userRepository.findByUsernameContainingIgnoreCase(q, pageable);
        return page.map(user -> new AdminUserDto(
                user.getId(), user.getUsername(), user.getRole()));
    }

    @Transactional(readOnly = true)
    public List<AdminBoardAdminDto> listBoardAdmins(Long boardId) {
        List<BoardAdmin> admins = boardAdminRepository.findByBoardId(boardId);
        List<Long> ids = admins.stream().map(BoardAdmin::getUserId).toList();
        Map<Long, String> usernames = new HashMap<>();
        userRepository.findAllById(ids).forEach(user -> usernames.put(user.getId(), user.getUsername()));
        return admins.stream()
                .map(admin -> new AdminBoardAdminDto(
                        admin.getUserId(), usernames.get(admin.getUserId())))
                .toList();
    }

    private void requireBoardAndUser(Long boardId, Long userId) {
        if (!boardService.getBoardById(boardId).isPresent()) {
            throw new IllegalArgumentException("Plateau introuvable : " + boardId);
        }
        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("Utilisateur introuvable : " + userId);
        }
    }

    private User ensureCredential(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            if ("ADMIN".equals(user.getRole())) {
                throw new SecurityException(
                        "Le compte '" + username + "' est réservé et ne peut pas être lié à un joueur");
            }
            // Un compte existant n'est jamais réécrit : sinon l'import permet de réinitialiser n'importe quel mot de passe.
            logger.warn("Compte existant '{}' lié au joueur sans modification du mot de passe", username);
            return user;
        }
        user = new User();
        user.setUsername(username);
        user.setRole("USER");
        user.setPassword(userService.encodePassword(password));
        return userRepository.save(user);
    }

    /** Parcourt les relations LAZY (character, buildings, sectors) : transaction readOnly requise (open-in-view désactivé). */
    @Transactional(readOnly = true)
    public Map<String, Object> exportPlayer(Long playerId) {
        Player player = playerService.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable avec l'ID " + playerId));

        Board board = boardService.getBoardById(player.getBoardId()).orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", player.getName());
        result.put("race", player.getRace() != null ? player.getRace().name() : null);
        result.put("money", player.getStats().getMoney());

        List<Map<String, Object>> resources = new ArrayList<>();
        // Le filtre bank_id exclut ces lignes de player.resources : les aplatir garde l'export relisible sans perte.
        List<PlayerResource> ownedResources = new ArrayList<>(player.getResources());
        player.getBuildings().stream()
                .filter(Bank.class::isInstance)
                .map(Bank.class::cast)
                .forEach(bank -> ownedResources.addAll(bank.getStoredResources()));
        for (PlayerResource pr : ownedResources) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("name", pr.getResourceName());
            res.put("quantity", pr.getQuantity());
            resources.add(res);
        }
        result.put("resources", resources);

        List<Map<String, Object>> equipments = new ArrayList<>();
        for (var stack : player.getAllEquipmentStacks()) {
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
                            uMap.put("injuredAtTurn", unit.getInjuredAtTurn());
                            uMap.put("healingDelayed", unit.isHealingDelayed());
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

        List<Map<String, Object>> vehiclesList = new ArrayList<>();
        for (Vehicle vehicle : vehicleRepository.findByPlayerId(player.getId())) {
            Map<String, Object> vMap = new LinkedHashMap<>();
            vMap.put("type", vehicle.getVehicleType() != null ? vehicle.getVehicleType().name() : null);
            vMap.put("sectorNumber", vehicle.getSector() != null ? vehicle.getSector().getNumber() : null);
            vehiclesList.add(vMap);
        }
        result.put("vehicles", vehiclesList);

        return result;
    }

    /** Supprime joueur + secteurs, et le compte User homonyme (non admin) pour éviter un compte orphelin. */
    @Transactional
    public void deletePlayer(Long playerId) {
        Player player = playerService.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable avec l'ID " + playerId));
        String name = player.getName();
        if (!playerService.delete(playerId)) {
            throw new IllegalArgumentException("Joueur introuvable avec l'ID " + playerId);
        }
        logger.info("Suppression joueur '{}' (id {})", name, playerId);
        adminAuditService.record(player != null ? player.getBoardId() : null, playerId, AdminAuditAction.DELETE_PLAYER,
                "Suppression : " + name);
    }

    /** Supprime une partie : ses participations d'abord (leurs cascades JPA), la carte ensuite. */
    @Transactional
    public void deleteBoard(Long boardId) {
        boardService.getBoardById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Plateau introuvable : " + boardId));
        playerService.findAll(boardId, Pageable.unpaged()).stream()
                .map(Player::getId)
                .toList()
                .forEach(playerService::delete);
        boardService.deleteBoard(boardId);
    }

    /** Import board.json. Les URLs fournies override celles du JSON ; un nom explicite déjà pris est refusé. */
    @Transactional
    public Board importBoard(String jsonContent, String mapImageUrl, String svgOverlayUrl,
                             String requestedName) throws IOException {
        Board board = boardImportService.importBoardFromJson(jsonContent);
        if (mapImageUrl != null && !mapImageUrl.isBlank()) {
            board.setMapImageUrl(mapImageUrl);
        }
        if (svgOverlayUrl != null && !svgOverlayUrl.isBlank()) {
            board.setSvgOverlayUrl(svgOverlayUrl);
        }
        String boardName = requestedName != null && !requestedName.isBlank()
                ? requestedName.trim()
                : board.getName() != null ? board.getName() : "Carte Principale";
        if (requestedName != null && !requestedName.isBlank()
                && boardService.getBoardByName(boardName).isPresent()) {
            throw new IllegalStateException("Une partie porte déjà ce nom");
        }
        board.setName(boardName);
        Board saved = boardService.saveBoard(board, boardName);
        logger.info("Import board '{}' (id {})", saved.getName(), saved.getId());
        adminAuditService.record(saved.getId(), null, AdminAuditAction.IMPORT_BOARD,
                "Board : " + saved.getName());
        return saved;
    }

    /** Idem : boardMapper parcourt sectorsList et ses sous-collections (LAZY). */
    @Transactional
    public BoardDto importBoardDto(String jsonContent, String mapImageUrl, String svgOverlayUrl,
                                   String requestedName) throws IOException {
        Board board = importBoard(jsonContent, mapImageUrl, svgOverlayUrl, requestedName);
        return boardMapper.toDto(board);
    }
}
