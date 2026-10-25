package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BuyEquipmentItemDto;
import com.mg.nmlonline.api.dto.BuyUnitRequestDto;
import com.mg.nmlonline.api.dto.PendingConflictDto;
import com.mg.nmlonline.api.dto.PendingEliminationDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.api.dto.SellResourceBatchItemDto;
import com.mg.nmlonline.api.dto.TurnFinalizeResultDto;
import com.mg.nmlonline.api.dto.TurnResolutionStateDto;
import com.mg.nmlonline.domain.model.action.PlayerActionType;
import com.mg.nmlonline.domain.model.alliance.ProposalKind;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerActionRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Dev-only : joue la démo via les services métier, donc le journal d'actions des joueurs reste réel. */
@Service
public class DemoTurnsSeeder {

    private static final int FIRST_DEMO_TURN = 1;
    private static final int SALE_BATCH_MAX = 9;
    private static final int MIN_SALE_QUANTITY = 5;

    /** Secteurs dont la ressource est encaissée à la récolte ; les autres versent leur revenu automatiquement. */
    private static final Set<String> HARVEST_RESOURCES =
            Set.of("Noctilithe", "Auramite", "Conteneur blindé", "Adamentium");

    /** Équipements visés, du plus fort au plus faible ; le premier compatible avec l'unité est équipé. */
    private static final List<String> GEAR = List.of(
            "Bouclier balistique",
            "Quantum Shielding",
            "Dispersion Shield",
            "Phase Shifter",
            "Gauss Destructor",
            "Gauss Reaper",
            "Hyperphase Glaive");

    /** Ordre des joueurs = ordre de soumission : le premier ordre d'un joueur le désigne camp attaquant. */
    private static final List<String> PLAYERS =
            List.of("nursek", "lurio", "trazyn", "imotekh", "mortarion", "angron", "cegorach");

    private record Move(String player, int from, int to, int units, boolean character) {
    }

    private record Placement(String player, int sector, int count) {
    }

    private record SalePlan(Long userId, Long resourceId, int quantity) {
    }

    private record HarvestPlan(Long userId, List<Integer> sectors) {
    }

    private record EquipOrder(Long unitId, String equipmentName) {
    }

    private record GearPlan(Long playerId, Long userId, List<BuyEquipmentItemDto> buys, List<EquipOrder> equips) {
    }

    private record MovePlan(Long userId, List<Long> entityIds) {
    }

    private static final List<Move> TURN_1_MOVES = List.of(
            new Move("nursek", 2, 15, 1, false),
            new Move("nursek", 2, 17, 1, false),
            new Move("nursek", 2, 19, 1, false),
            new Move("lurio", 13, 43, 3, false),
            new Move("lurio", 13, 33, 1, false),
            new Move("lurio", 41, 13, 2, false),
            new Move("trazyn", 30, 43, 3, false),
            new Move("trazyn", 20, 12, 1, false),
            new Move("trazyn", 20, 31, 1, false),
            new Move("trazyn", 40, 35, 1, false),
            new Move("imotekh", 26, 34, 1, false),
            new Move("imotekh", 25, 21, 1, false),
            new Move("imotekh", 39, 42, 2, false),
            new Move("mortarion", 1, 16, 0, true),
            new Move("mortarion", 1, 3, 1, false),
            new Move("angron", 6, 29, 0, true));

    private static final List<Placement> TURN_2_PLACEMENTS = List.of(
            new Placement("nursek", 15, 2),
            new Placement("nursek", 17, 1),
            new Placement("nursek", 19, 1),
            new Placement("lurio", 43, 1),
            new Placement("imotekh", 21, 1),
            new Placement("imotekh", 39, 2),
            new Placement("trazyn", 40, 2));

    private static final List<Move> TURN_2_MOVES = List.of(
            new Move("nursek", 15, 4, 1, false),
            new Move("nursek", 15, 5, 1, false),
            new Move("nursek", 17, 28, 1, false),
            new Move("nursek", 19, 24, 1, false),
            new Move("lurio", 43, 38, 1, false),
            new Move("trazyn", 40, 42, 2, false),
            new Move("trazyn", 12, 42, 1, false),
            new Move("trazyn", 35, 22, 1, false),
            new Move("imotekh", 39, 42, 3, false),
            new Move("imotekh", 21, 36, 1, false),
            new Move("mortarion", 3, 7, 1, false),
            new Move("mortarion", 16, 14, 0, true),
            new Move("angron", 29, 23, 0, true));

    private final BoardRepository boardRepository;
    private final PlayerRepository playerRepository;
    private final PlayerActionRepository playerActionRepository;
    private final EquipmentRepository equipmentRepository;
    private final PlayerService playerService;
    private final ResourceService resourceService;
    private final PlayerActionService playerActionService;
    private final UnitService unitService;
    private final AllianceService allianceService;
    private final RankingService rankingService;
    private final TurnService turnService;
    private final TurnLock turnLock;
    private final TurnResolutionOrchestrator orchestrator;
    private final TransactionTemplate txTemplate;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DemoTurnsSeeder(BoardRepository boardRepository,
                           PlayerRepository playerRepository,
                           PlayerActionRepository playerActionRepository,
                           EquipmentRepository equipmentRepository,
                           PlayerService playerService,
                           ResourceService resourceService,
                           PlayerActionService playerActionService,
                           UnitService unitService,
                           AllianceService allianceService,
                           RankingService rankingService,
                           TurnService turnService,
                           TurnLock turnLock,
                           TurnResolutionOrchestrator orchestrator,
                           PlatformTransactionManager txManager) {
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
        this.playerActionRepository = playerActionRepository;
        this.equipmentRepository = equipmentRepository;
        this.playerService = playerService;
        this.resourceService = resourceService;
        this.playerActionService = playerActionService;
        this.unitService = unitService;
        this.allianceService = allianceService;
        this.rankingService = rankingService;
        this.turnService = turnService;
        this.turnLock = turnLock;
        this.orchestrator = orchestrator;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    public ScenarioSummaryDto playDemoTurns() {
        int turn = turnService.getCurrentTurn();
        // Le journal d'actions est la marque d'une partie déjà jouée : la navigation dev vers le tour 1 le laisse intact.
        if (turn != FIRST_DEMO_TURN || playerActionRepository.count() > 0) {
            throw new IllegalStateException("La démo se joue une fois, depuis une partie vierge (tour courant : "
                    + turn + ", actions existantes : " + playerActionRepository.count()
                    + "). En dev, redémarrez le backend : la base repart vierge avec l'import démo.");
        }
        if (turnLock.isLocked()) {
            throw new IllegalStateException(
                    "Une résolution est en cours — finalisez-la ou abandonnez-la avant de jouer la démo.");
        }
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("La démo est déjà en cours d'exécution.");
        }

        try {
            List<String> log = new ArrayList<>();
            List<String> issues = new ArrayList<>();
            playOrdersTurn1(issues);
            TurnFinalizeResultDto firstTurn = resolveTurn(log);
            playOrdersTurn2(issues);
            TurnFinalizeResultDto secondTurn = resolveTurn(log);

            ScenarioSummaryDto dto = new ScenarioSummaryDto();
            dto.setTurn(secondTurn.getNewTurn());
            dto.setObservations(issues.isEmpty() ? null : issues);
            dto.setMessage("Tours 1 et 2 joués — " + captures(firstTurn) + " capture(s) au tour 1, "
                    + captures(secondTurn) + " au tour 2, tour courant : " + secondTurn.getNewTurn() + "."
                    + (log.isEmpty() ? "" : " " + String.join(" ", log)));
            return dto;
        } finally {
            running.set(false);
        }
    }

    private void playOrdersTurn1(List<String> issues) {
        PLAYERS.forEach(name -> executeSale(planSale(name)));
        PLAYERS.forEach(name -> equipGear(name, issues));
        formAlliance("mortarion", "angron");
        rankingService.updateRankingComment(requirePlayer("nursek").getId(), "Arsenal complet");
        rankingService.updateRankingComment(requirePlayer("imotekh").getId(), "Vétéran du Grand Coke");
        rankingService.updateRankingComment(requirePlayer("trazyn").getId(), "Roi des ressources");
        Set<Long> usedEntities = new HashSet<>();
        TURN_1_MOVES.forEach(move -> executeMove(move, issues, usedEntities));
    }

    private void playOrdersTurn2(List<String> issues) {
        PLAYERS.forEach(name -> {
            executeHarvest(name, issues);
            executeSale(planSale(name));
        });
        buyAndPlaceLarbins(issues);
        Set<Long> usedEntities = new HashSet<>();
        TURN_2_MOVES.forEach(move -> executeMove(move, issues, usedEntities));
    }

    private TurnFinalizeResultDto resolveTurn(List<String> log) {
        try {
            TurnResolutionStateDto state = orchestrator.startSession();
            while (!state.isCanFinalize()) {
                if (!state.getPendingEliminations().isEmpty()) {
                    for (PendingEliminationDto pending : state.getPendingEliminations()) {
                        Long killerId = pending.getParticipants().isEmpty() ? null
                                : pending.getParticipants().getFirst().getPlayerId();
                        state = orchestrator.resolveElimination(pending.getVictimPlayerId(), killerId);
                    }
                    continue;
                }
                if (state.getPendingConflicts().isEmpty()) {
                    state = orchestrator.advanceHop();
                    continue;
                }
                PendingConflictDto conflict = state.getPendingConflicts().getFirst();
                ResolvedBattleDto battle = orchestrator.resolveBattle(conflict.getConflictId());
                log.add("Secteur " + battle.getSectorNumber() + " : "
                        + (battle.getWinnerName() != null ? battle.getWinnerName() + " l'emporte" : "aucun vainqueur")
                        + (battle.isStandoff() ? " (impasse)" : "") + ".");
                state = orchestrator.getState();
            }
            return orchestrator.finalizeTurn();
        } catch (RuntimeException e) {
            // Sans abort, une session à moitié résolue garderait le verrou de tour jusqu'au prochain démarrage.
            orchestrator.abort();
            throw e;
        }
    }

    private static int captures(TurnFinalizeResultDto turn) {
        return turn.getCapturedSectors() == null ? 0 : turn.getCapturedSectors().size();
    }

    private SalePlan planSale(String playerName) {
        return txTemplate.execute(status -> {
            Player player = requirePlayer(playerName);
            return player.getResources().stream()
                    .filter(resource -> resource.getId() != null && resource.getQuantity() >= MIN_SALE_QUANTITY)
                    .max(Comparator.comparingDouble(resource -> resourceService.getBaseValue(resource.getResourceName())
                            * Math.min(SALE_BATCH_MAX, resource.getQuantity())))
                    .map(resource -> new SalePlan(player.getUserId(), resource.getId(),
                            Math.min(SALE_BATCH_MAX, resource.getQuantity())))
                    .orElse(null);
        });
    }

    private void executeSale(SalePlan plan) {
        if (plan == null) {
            return;
        }
        SellResourceBatchItemDto item = new SellResourceBatchItemDto();
        item.setPlayerResourceId(plan.resourceId());
        item.setQuantity(plan.quantity());
        resourceService.sellResourcesBatch(plan.userId(), List.of(item));
    }

    private void executeHarvest(String playerName, List<String> issues) {
        HarvestPlan plan = txTemplate.execute(status -> {
            Player player = requirePlayer(playerName);
            List<Integer> sectors = requireBoard().getSectorsByOwner(player.getId()).stream()
                    .filter(sector -> sector.getResourceName() != null
                            && HARVEST_RESOURCES.contains(sector.getResourceName()))
                    .map(Sector::getNumber)
                    .sorted()
                    .toList();
            return sectors.isEmpty() ? null : new HarvestPlan(player.getUserId(), sectors);
        });
        if (plan == null) {
            return;
        }
        try {
            playerActionService.harvest(plan.userId(), PlayerActionType.HARVEST_RESOURCE, plan.sectors());
        } catch (RuntimeException e) {
            issues.add("Récolte impossible pour " + playerName + " (" + e.getMessage() + ")");
        }
    }

    /** Équipe les unités au-delà du LARBIN avec l'inventaire du joueur, en achetant le manquant si les fonds le permettent. */
    private void equipGear(String playerName, List<String> issues) {
        GearPlan plan = txTemplate.execute(status -> {
            Player player = requirePlayer(playerName);
            List<EquipOrder> orders = new ArrayList<>();
            Map<String, Integer> needed = new LinkedHashMap<>();
            for (Sector sector : requireBoard().getSectorsByOwner(player.getId())) {
                for (Unit unit : sector.getUnits()) {
                    if (unit.getType() == UnitType.LARBIN || unit.getType() == UnitType.PERSONNAGE) {
                        continue;
                    }
                    for (EquipmentCategory category : EquipmentCategory.values()) {
                        if (freeSlots(unit, category) == 0) {
                            continue;
                        }
                        pickGear(unit, category).ifPresent(equipment -> {
                            orders.add(new EquipOrder(unit.getId(), equipment.getName()));
                            needed.merge(equipment.getName(), 1, Integer::sum);
                        });
                    }
                }
            }
            // Le besoin se compte par unité : un exemplaire « disponible » ne couvre qu'une seule unité.
            List<BuyEquipmentItemDto> buys = needed.entrySet().stream()
                    .map(entry -> Map.entry(entry.getKey(),
                            entry.getValue() - availableCount(player, entry.getKey())))
                    .filter(entry -> entry.getValue() > 0)
                    .map(entry -> {
                        BuyEquipmentItemDto item = new BuyEquipmentItemDto();
                        item.setName(entry.getKey());
                        item.setQuantity(entry.getValue());
                        return item;
                    })
                    .toList();
            return new GearPlan(player.getId(), player.getUserId(), buys, orders);
        });
        if (plan == null || plan.equips().isEmpty()) {
            return;
        }
        try {
            if (!plan.buys().isEmpty()) {
                playerService.buyEquipments(plan.playerId(), plan.buys());
            }
        } catch (RuntimeException e) {
            // Fonds insuffisants : on équipe ce qui est déjà en inventaire.
            issues.add("Équipement non acheté pour " + playerName + " (fonds insuffisants)");
        }
        int failed = 0;
        for (EquipOrder order : plan.equips()) {
            try {
                unitService.assignEquipment(order.unitId(), plan.userId(), order.equipmentName());
            } catch (RuntimeException ignored) {
                // Emplacement pris ou exemplaire manquant : on passe à l'ordre suivant.
                failed++;
            }
        }
        if (failed > 0) {
            issues.add(failed + " équipement(s) non posé(s) pour " + playerName);
        }
    }

    private static int availableCount(Player player, String equipmentName) {
        return player.getEquipments().stream()
                .filter(stack -> stack.getEquipment() != null && equipmentName.equals(stack.getEquipment().getName()))
                .mapToInt(EquipmentStack::getAvailable)
                .sum();
    }

    private Optional<Equipment> pickGear(Unit unit, EquipmentCategory category) {
        for (String name : GEAR) {
            Equipment candidate = equipmentRepository.findByName(name).orElse(null);
            if (candidate != null
                    && candidate.getCategory() == category
                    && unit.canEquip(candidate)
                    && unit.getEquipmentsByCategory(category).stream()
                            .noneMatch(equipped -> equipped.getName().equals(name))) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static int freeSlots(Unit unit, EquipmentCategory category) {
        int max = switch (category) {
            case FIREARM -> unit.getType().getMaxFirearms();
            case MELEE -> unit.getType().getMaxMeleeWeapons();
            case DEFENSIVE -> unit.getType().getMaxDefensiveEquipment();
        };
        return max - (int) unit.countEquipmentsByCategory(category);
    }

    /** Le secteur 43 peut changer de camp : les actions qui en dépendent sont sautées, pas fatales. */
    private void buyAndPlaceLarbins(List<String> issues) {
        Long boardId = txTemplate.execute(status -> requireBoard().getId());
        Map<String, Integer> perPlayer = new HashMap<>();
        TURN_2_PLACEMENTS.forEach(placement -> perPlayer.merge(placement.player(), placement.count(), Integer::sum));

        Map<String, Iterator<Long>> freshUnits = new HashMap<>();
        perPlayer.forEach((playerName, count) -> {
            Player player = requirePlayer(playerName);
            BuyUnitRequestDto item = new BuyUnitRequestDto();
            item.setUnitType(UnitType.LARBIN.name());
            item.setUnitClass(UnitClass.LEGER.name());
            item.setQuantity(count);
            try {
                freshUnits.put(playerName, unitService.buyUnits(player.getUserId(), List.of(item)).stream()
                        .map(Unit::getId)
                        .toList()
                        .iterator());
            } catch (RuntimeException e) {
                issues.add(count + " recrue(s) non achetée(s) pour " + playerName + " (" + e.getMessage() + ")");
                freshUnits.put(playerName, List.<Long>of().iterator());
            }
        });

        int skipped = 0;
        for (Placement placement : TURN_2_PLACEMENTS) {
            Player player = requirePlayer(placement.player());
            Iterator<Long> cursor = freshUnits.get(placement.player());
            for (int i = 0; i < placement.count(); i++) {
                if (!cursor.hasNext()) {
                    // Achat déjà signalé : ne pas compter un placement qui n'a jamais eu de recrue.
                    continue;
                }
                Long unitId = cursor.next();
                try {
                    unitService.placeUnit(player.getUserId(), unitId, boardId, placement.sector());
                } catch (RuntimeException e) {
                    skipped++;
                }
            }
        }
        if (skipped > 0) {
            issues.add(skipped + " recrue(s) non placée(s) — secteur perdu, elles restent en réserve");
        }
    }

    private void executeMove(Move move, List<String> issues, Set<Long> usedEntities) {
        MovePlan plan;
        try {
            plan = txTemplate.execute(status -> {
                Player player = requirePlayer(move.player());
                Sector from = requireBoard().getSector(move.from());
                if (from == null) {
                    throw new IllegalStateException("Secteur " + move.from() + " introuvable sur le plateau");
                }
                List<Long> entityIds = new ArrayList<>(selectUnits(from, player.getId(), move.units(), usedEntities));
                if (move.character()) {
                    entityIds.add(requireCharacterId(from, player.getId()));
                }
                return new MovePlan(player.getUserId(), entityIds);
            });
        } catch (RuntimeException e) {
            // Capture manquée au tour précédent : l'ordre devient impossible, on continue la démo.
            issues.add("Ordre " + move.player() + " " + move.from() + "→" + move.to() + " sauté");
            return;
        }
        try {
            unitService.placeFootOrder(plan.userId(), plan.entityIds(), List.of(move.from(), move.to()));
            usedEntities.addAll(plan.entityIds());
        } catch (RuntimeException e) {
            issues.add("Ordre " + move.player() + " " + move.from() + "→" + move.to() + " sauté");
        }
    }

    /** Unités les plus faibles d'abord : les LARBIN prennent les secteurs, les BRUTEs restent en défense. */
    private static List<Long> selectUnits(Sector sector, Long playerId, int count, Set<Long> usedEntities) {
        List<Unit> units = sector.getUnits().stream()
                .filter(unit -> playerId.equals(unit.getPlayerId()))
                .filter(unit -> !usedEntities.contains(unit.getId()))
                .sorted(Comparator.comparingInt((Unit unit) -> unit.getType().getLevel())
                        .thenComparing(Unit::getNumber))
                .toList();
        if (units.size() < count) {
            throw new IllegalStateException("secteur " + sector.getNumber() + " : " + units.size()
                    + " unité(s) disponible(s), " + count + " attendue(s)");
        }
        return units.stream().limit(count).map(Unit::getId).toList();
    }

    private static Long requireCharacterId(Sector sector, Long playerId) {
        return sector.getCharacters().stream()
                .filter(character -> playerId.equals(character.getPlayerId()))
                .map(GameCharacter::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Personnage introuvable au secteur " + sector.getNumber() + " pour ce joueur"));
    }

    private void formAlliance(String proposerName, String partnerName) {
        Player proposer = requirePlayer(proposerName);
        Player partner = requirePlayer(partnerName);
        Long proposalId = allianceService
                .propose(proposer.getUserId(), ProposalKind.ALLIANCE, partner.getId())
                .getId();
        allianceService.accept(partner.getUserId(), proposalId);
    }

    private Player requirePlayer(String name) {
        return playerRepository.findByName(name).orElseThrow(() -> new IllegalStateException(
                name + " introuvable — vérifiez l'import des données de démo"));
    }

    private Board requireBoard() {
        return boardRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun plateau trouvé — importez d'abord un board"));
    }
}
