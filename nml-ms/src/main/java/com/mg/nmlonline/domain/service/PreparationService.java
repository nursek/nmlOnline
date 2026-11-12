package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.repository.BuildingRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PreparationService {

    private final BoardService boardService;
    private final PlayerRepository playerRepository;
    private final GameCharacterService gameCharacterService;
    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;
    private final PlayerStatsService playerStatsService;

    public PreparationService(BoardService boardService, PlayerRepository playerRepository,
                              GameCharacterService gameCharacterService,
                              BuildingRepository buildingRepository, UnitRepository unitRepository,
                              PlayerStatsService playerStatsService) {
        this.boardService = boardService;
        this.playerRepository = playerRepository;
        this.gameCharacterService = gameCharacterService;
        this.buildingRepository = buildingRepository;
        this.unitRepository = unitRepository;
        this.playerStatsService = playerStatsService;
    }

    public void placeCharacter(Long userId, Long boardId, int sectorNumber) {
        Player player = lockPlayer(userId, boardId);
        Board board = requirePreparation(boardId);
        GameCharacter character = gameCharacterService.getCharacter(player.getId())
                .orElseThrow(() -> new IllegalStateException("Le joueur n'a pas de personnage"));
        if (character.getSector() != null) {
            throw new IllegalStateException("Le personnage est déjà placé");
        }
        Sector sector = ownedSector(board, player, sectorNumber);
        gameCharacterService.placeInSector(player.getId(), sector);
        sector.recalculateMilitaryPower();
        playerStatsService.recalculateStats(player, board);
        boardService.save(board);
    }

    public void placeBuilding(Long userId, Long boardId, Long buildingId, int sectorNumber) {
        Player player = lockPlayer(userId, boardId);
        Board board = requirePreparation(boardId);
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Bâtiment introuvable : " + buildingId));
        if (!player.getId().equals(building.getPlayerId())) {
            throw new SecurityException("Ce bâtiment n'appartient pas au joueur");
        }
        if (building.getSector() != null) {
            throw new IllegalStateException("Le bâtiment est déjà placé");
        }
        Sector sector = ownedSector(board, player, sectorNumber);
        building.setSector(sector);
        sector.getBuildings().add(building);
        buildingRepository.save(building);
        sector.recalculateMilitaryPower();
        playerStatsService.recalculateStats(player, board);
        boardService.save(board);
    }

    public void distributeUnits(Long userId, Long boardId, int sectorNumber,
                                int quantity, List<String> classNames) {
        if (quantity < 1) {
            throw new IllegalArgumentException("La quantité doit être au moins 1");
        }
        Player player = lockPlayer(userId, boardId);
        Board board = requirePreparation(boardId);
        Sector sector = ownedSector(board, player, sectorNumber);
        List<UnitClass> classes = parseClasses(classNames);
        List<Unit> reserve = unitRepository.findByPlayerIdAndSectorIsNull(player.getId()).stream()
                .filter(unit -> unit.getClasses().isEmpty())
                .limit(quantity)
                .toList();
        if (reserve.size() < quantity) {
            throw new IllegalArgumentException(
                    "Il ne reste que " + reserve.size() + " unité(s) à répartir");
        }
        for (Unit unit : reserve) {
            if (classes.size() > 1 && !unit.canHaveTwoClasses()) {
                throw new IllegalArgumentException(
                        "Une unité " + unit.getType() + " ne peut avoir qu'une classe");
            }
            unit.setClasses(classes);
            unit.recalculateBaseStats();
            sector.addUnit(unit);
            unitRepository.save(unit);
        }
        playerStatsService.recalculateStats(player, board);
        boardService.save(board);
    }

    private List<UnitClass> parseClasses(List<String> classNames) {
        if (classNames == null || classNames.isEmpty()) {
            throw new IllegalArgumentException("Choisissez au moins une classe");
        }
        return classNames.stream()
                .map(name -> {
                    try {
                        return UnitClass.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Classe invalide : " + name);
                    }
                })
                .distinct()
                .toList();
    }

    private Sector ownedSector(Board board, Player player, int sectorNumber) {
        Sector sector = board.getSector(sectorNumber);
        if (sector == null) {
            throw new IllegalArgumentException("Secteur introuvable : " + sectorNumber);
        }
        if (!sector.isOwnedBy(player.getId())) {
            throw new SecurityException("Vous ne possédez pas ce secteur");
        }
        return sector;
    }

    private Player lockPlayer(Long userId, Long boardId) {
        return playerRepository.findByUserIdAndBoardIdForUpdate(userId, boardId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable"));
    }

    private Board requirePreparation(Long boardId) {
        Board board = boardService.getBoardByIdForUpdate(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Plateau introuvable : " + boardId));
        if (!board.isPreparation()) {
            throw new IllegalStateException("La partie a déjà démarré");
        }
        return board;
    }
}
