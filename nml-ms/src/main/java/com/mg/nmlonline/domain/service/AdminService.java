package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BoardDto;
import com.mg.nmlonline.api.dto.PlayerDto;
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
import com.mg.nmlonline.mapper.BoardMapper;
import com.mg.nmlonline.mapper.PlayerMapper;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

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
    private final PlayerMapper playerMapper;
    private final BoardMapper boardMapper;

    public AdminService(PlayerImportService playerImportService,
                        PlayerService playerService,
                        BoardImportService boardImportService,
                        BoardService boardService,
                        UserService userService,
                        UserRepository userRepository,
                        ResourceRepository resourceRepository,
                        EntityManager entityManager,
                        PlayerMapper playerMapper,
                        BoardMapper boardMapper) {
        this.playerImportService = playerImportService;
        this.playerService = playerService;
        this.boardImportService = boardImportService;
        this.boardService = boardService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.entityManager = entityManager;
        this.playerMapper = playerMapper;
        this.boardMapper = boardMapper;
    }

    /** Import JSON : supprime un joueur homonyme au préalable ; si password est fourni, crée/met à jour le compte User associé. */
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

    public Player importPlayer(String jsonContent) throws IOException {
        return importPlayer(jsonContent, null);
    }

    /** Import + mapping dans une seule transaction : le DTO parcourt equipments/resources/buildings (LAZY). */
    @Transactional
    public PlayerDto importPlayerDto(String jsonContent, String password) throws IOException {
        Player player = importPlayer(jsonContent, password);
        Board board = boardService.getAllBoards().stream().findFirst().orElse(null);
        return playerMapper.toDtoWithSectors(player, board);
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

    /** Parcourt les relations LAZY (character, buildings, sectors) : transaction readOnly requise (open-in-view désactivé). */
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

    /** Supprime joueur + secteurs, et le compte User homonyme (non admin) pour éviter un compte orphelin. */
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

    /** Import board.json. Les URLs fournies override celles du JSON ; upsert par nom via saveBoard. */
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

    /** Idem : boardMapper parcourt sectorsList et ses sous-collections (LAZY). */
    @Transactional
    public BoardDto importBoardDto(String jsonContent, String mapImageUrl, String svgOverlayUrl) throws IOException {
        Board board = importBoard(jsonContent, mapImageUrl, svgOverlayUrl);
        return boardMapper.toDto(board);
    }
}
