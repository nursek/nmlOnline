package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.AdminTemplatePlayerRequestDto;
import com.mg.nmlonline.api.dto.PlayerDto;
import com.mg.nmlonline.domain.model.audit.AdminAuditAction;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.BuildingType;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.player.PlayerRace;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.infrastructure.repository.BuildingRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import com.mg.nmlonline.mapper.PlayerMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional
public class GameSetupService {

    static final double STARTING_MONEY = 20_000;
    static final int STARTING_UNITS = 100;

    private final BoardService boardService;
    private final PlayerService playerService;
    private final UnitRepository unitRepository;
    private final BuildingRepository buildingRepository;
    private final BuildingService buildingService;
    private final GameCharacterService gameCharacterService;
    private final PlayerStatsService playerStatsService;
    private final AdminAuditService adminAuditService;
    private final TurnService turnService;
    private final PlayerMapper playerMapper;

    public GameSetupService(BoardService boardService, PlayerService playerService,
                            UnitRepository unitRepository, BuildingRepository buildingRepository,
                            BuildingService buildingService, GameCharacterService gameCharacterService,
                            PlayerStatsService playerStatsService, AdminAuditService adminAuditService,
                            TurnService turnService, PlayerMapper playerMapper) {
        this.boardService = boardService;
        this.playerService = playerService;
        this.unitRepository = unitRepository;
        this.buildingRepository = buildingRepository;
        this.buildingService = buildingService;
        this.gameCharacterService = gameCharacterService;
        this.playerStatsService = playerStatsService;
        this.adminAuditService = adminAuditService;
        this.turnService = turnService;
        this.playerMapper = playerMapper;
    }

    public PlayerDto createTemplatePlayer(Long boardId, AdminTemplatePlayerRequestDto request) {
        Board board = boardOf(boardId);
        requirePreparation(board);
        String name = request.name().trim();
        if (playerService.findByName(boardId, name) != null) {
            throw new IllegalArgumentException("Un joueur avec ce nom existe déjà");
        }
        PlayerRace race = parseRace(request.race());

        Player player = new Player(name);
        player.setBoardId(boardId);
        player.setRace(race);
        player.getStats().setMoney(STARTING_MONEY);
        player.initializeStartingMoney();
        player = playerService.save(player);

        assignStartingSectors(board, player, request.sectorNumbers());
        createStartingUnits(player);
        createStartingBuildings(player);
        createStartingCharacter(player, request.characterName());

        playerStatsService.recalculateStats(player, board);
        boardService.save(board);
        player = playerService.save(player);
        adminAuditService.record(boardId, player.getId(), AdminAuditAction.CREATE_TEMPLATE_PLAYER,
                "Joueur prêt : " + name);
        return playerMapper.toDtoWithSectors(player, board);
    }

    public void assignStartingSectors(Long boardId, Long playerId, List<Integer> sectorNumbers) {
        Player player = playerService.findByIdForUpdate(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Joueur introuvable : " + playerId);
        }
        Board board = boardOf(boardId);
        requirePreparation(board);
        if (!boardId.equals(player.getBoardId())) {
            throw new IllegalArgumentException("Ce joueur n'appartient pas à la partie");
        }
        assignStartingSectors(board, player, sectorNumbers);
        playerStatsService.recalculateStats(player, board);
        boardService.save(board);
        adminAuditService.record(boardId, playerId, AdminAuditAction.ASSIGN_STARTING_SECTORS,
                "Secteurs de départ : " + sectorNumbers);
    }

    public void startGame(Long boardId) {
        List<Player> players = lockPlayersForStart(boardId);
        Board board = boardOf(boardId);
        requirePreparation(board);
        assertPlayerSetUnchanged(boardId, players);
        for (Player player : players) {
            if (player.getUserId() == null) {
                throw new IllegalStateException(
                        "Le joueur '" + player.getName() + "' n'a pas de compte relié");
            }
            if (board.getSectorsByOwner(player.getId()).isEmpty()) {
                throw new IllegalStateException(
                        "Le joueur '" + player.getName() + "' n'a pas de secteur de départ");
            }
            placeRemainingStartingElements(board, player);
            playerStatsService.recalculateStats(player, board);
            classifyRemainingStartingUnits(player);
        }
        board.start(Instant.now());
        boardService.save(board);
        turnService.publishTurn(boardId, 1);
        adminAuditService.record(boardId, null, AdminAuditAction.START_GAME,
                "Démarrage de la partie");
    }

    /** Verrouille les joueurs par id croissant AVANT le plateau : évite l'inversion avec les placements. */
    private List<Player> lockPlayersForStart(Long boardId) {
        List<Long> ids = playerService.findAll(boardId, Pageable.unpaged()).getContent().stream()
                .map(Player::getId)
                .sorted()
                .toList();
        if (ids.isEmpty()) {
            throw new IllegalStateException("Aucun joueur dans la partie");
        }
        List<Player> locked = new ArrayList<>(ids.size());
        for (Long id : ids) {
            Player player = playerService.findByIdForUpdate(id);
            if (player == null || !boardId.equals(player.getBoardId())) {
                throw new IllegalStateException(
                        "La liste des joueurs a changé : relancez le démarrage");
            }
            locked.add(player);
        }
        return locked;
    }

    private void assertPlayerSetUnchanged(Long boardId, List<Player> locked) {
        List<Long> currentIds = playerService.findAll(boardId, Pageable.unpaged()).getContent().stream()
                .map(Player::getId)
                .toList();
        List<Long> lockedIds = locked.stream().map(Player::getId).toList();
        if (currentIds.size() != lockedIds.size() || !currentIds.containsAll(lockedIds)) {
            throw new IllegalStateException(
                    "La liste des joueurs a changé : relancez le démarrage");
        }
    }

    private void assignStartingSectors(Board board, Player player, List<Integer> sectorNumbers) {
        if (sectorNumbers == null) {
            sectorNumbers = List.of();
        }
        if (sectorNumbers.size() > 3) {
            throw new IllegalArgumentException("Un joueur ne peut pas avoir plus de 3 secteurs de départ");
        }
        List<Integer> selected = new ArrayList<>(new LinkedHashSet<>(sectorNumbers));
        for (Integer number : selected) {
            Sector sector = board.getSector(Objects.requireNonNullElse(number, -1));
            if (sector == null) {
                throw new IllegalArgumentException("Secteur introuvable : " + number);
            }
            if (sector.getOwnerId() != null && !sector.getOwnerId().equals(player.getId())) {
                throw new IllegalArgumentException("Le secteur " + number + " est déjà occupé");
            }
        }
        for (Sector owned : board.getSectorsByOwner(player.getId())) {
            if (!selected.contains(owned.getNumber()) && hasEntities(owned, player)) {
                throw new IllegalStateException(
                        "Le joueur a déjà des entités sur le secteur " + owned.getNumber());
            }
        }
        for (Sector owned : board.getSectorsByOwner(player.getId())) {
            owned.setOwnerAndColor(null, null);
        }
        for (Integer number : selected) {
            board.assignOwner(number, player.getId(), "#ffffff");
        }
    }

    private boolean hasEntities(Sector sector, Player player) {
        if (!sector.getArmy().isEmpty() || !sector.getBuildings().isEmpty()
                || !sector.getVehicles().isEmpty()) {
            return true;
        }
        return player.getCharacter() != null && player.getCharacter().getSector() != null
                && player.getCharacter().getSector().getNumber() == sector.getNumber();
    }

    private void createStartingUnits(Player player) {
        List<Unit> units = new ArrayList<>(STARTING_UNITS);
        for (int i = 0; i < STARTING_UNITS; i++) {
            Unit unit = new Unit();
            unit.setExperience(0);
            unit.setType(UnitType.LARBIN);
            unit.setPlayerId(player.getId());
            unit.recalculateBaseStats();
            units.add(unit);
        }
        unitRepository.saveAll(units);
    }

    private void createStartingBuildings(Player player) {
        for (BuildingType type : List.of(BuildingType.HEADQUARTERS,
                BuildingType.WEAPON_CACHE, BuildingType.BANK)) {
            Building building = buildingService.createBuilding(player, type);
            buildingRepository.save(building);
        }
    }

    private void createStartingCharacter(Player player, String requestedName) {
        String name = requestedName == null || requestedName.isBlank()
                ? player.getName() : requestedName.trim();
        gameCharacterService.createCharacter(player.getId(), name, 100, 0, 0, 100, 0, 0);
    }

    /** Filet de sécurité : un élément encore non placé au démarrage va sur le premier secteur du joueur. */
    private void placeRemainingStartingElements(Board board, Player player) {
        Sector fallback = board.getSectorsByOwner(player.getId()).getFirst();
        for (Building building : player.getBuildings()) {
            if (building.getSector() == null) {
                building.setSector(fallback);
                fallback.getBuildings().add(building);
                buildingRepository.save(building);
            }
        }
        gameCharacterService.getCharacter(player.getId()).ifPresent(character -> {
            if (character.getSector() == null) {
                gameCharacterService.placeInSector(player.getId(), fallback);
            }
        });
        fallback.recalculateMilitaryPower();
    }

    private void classifyRemainingStartingUnits(Player player) {
        List<Unit> unclassified = unitRepository.findByPlayerIdAndSectorIsNull(player.getId()).stream()
                .filter(unit -> unit.getClasses().isEmpty())
                .toList();
        unclassified.forEach(unit -> {
            unit.setClasses(List.of(UnitClass.LEGER));
            unit.recalculateBaseStats();
        });
        unitRepository.saveAll(unclassified);
    }

    private PlayerRace parseRace(String race) {
        try {
            return PlayerRace.valueOf(race.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Race invalide : " + race);
        }
    }

    private Board boardOf(Long boardId) {
        return boardService.getBoardByIdForUpdate(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Plateau introuvable : " + boardId));
    }

    private void requirePreparation(Board board) {
        if (!board.isPreparation()) {
            throw new IllegalStateException("La partie a déjà démarré");
        }
    }
}
