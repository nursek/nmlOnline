package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.CombatScenarioDto;
import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.sniper.SniperOrder;
import com.mg.nmlonline.domain.model.sniper.SniperOrderStatus;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.PendingCaptureRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.SniperOrderRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** Dev-only, idempotent : purge les PENDING du tour et n'ajoute que les unités manquantes. */
@Service
public class TurnResolutionScenarioSeeder {

    private static final int SECTOR_ATTACKER_FROM = 41;
    private static final int SECTOR_INTERMEDIATE = 13;
    private static final int SECTOR_DEFENDER = 32;
    private static final int TARGET_DEFENDER_COUNT = 2;

    private static final int STANDOFF_ATTACKER_FROM = 43;

    private static final String MAIN_CHARACTER_SCENARIO = "MAIN_CHARACTER_ELIMINATION";
    private static final int MAIN_CHARACTER_HQ_SECTOR = 32;

    private record CombatScenarioDefinition(String code, String label, String description,
                                            List<String> observations, int arenaSector, int stagingSector,
                                            String attackerName, String defenderName, String thirdPartyName,
                                            boolean eliminatesPlayer) {
    }

    private static final String ARENA_PLAYER_PREFIX = "arene-";

    private static final Set<String> STANDALONE_SCENARIOS = Set.of("SNIPER_RANGED");

    /** Arènes neutres dédiées, vidées au seed ; un départ partagé n'est purgé que des unités du scénario. */
    private static final List<CombatScenarioDefinition> COMBAT_SCENARIOS = List.of(
            new CombatScenarioDefinition("MIXED_5050", "Infanterie vs véhicules (50/50)",
                    "3 BRUTEs de lurio attaquent 2 LARBINs et un VTT léger sans pilote de cegorach.",
                    List.of("Tirage 50/50 par attaque entre véhicules et unités",
                            "Le VTT sans pilote est une cible passive qui ne tire pas",
                            "Le surplus d'une frappe enchaîne sur la cible suivante"),
                    21, 36, "lurio", "cegorach", null, false),
            new CombatScenarioDefinition("DESTRUCTOR_VS_TANK", "Pilote destructeur vs Tank",
                    "2 MALFRATs PILOTE_DESTRUCTEUR au Gauss Cannon attaquent un Tank piloté et 2 LARBINs.",
                    List.of("Ciblage prioritaire du Tank malgré les LARBINs exposés",
                            "Gauss Cannon : bonus anti-véhicule ×2 puis résistance du Tank −50 %",
                            "Le Tank endommagé répare 50 défense en fin de tour s'il survit"),
                    42, 37, "lurio", "cegorach", null, false),
            new CombatScenarioDefinition("CREW_PROTECTED", "Équipage protégé",
                    "3 LARBINs au Gauss Flayer attaquent un VTT blindé piloté avec un passager armé.",
                    List.of("Les tirs de l'équipage sont tracés « (à bord de VTT blindé) »",
                            "Pilote et passager restent intouchables tant que le véhicule vit",
                            "Les attaquants ne touchent que le véhicule en PdF"),
                    35, 22, "lurio", "cegorach", null, false),
            new CombatScenarioDefinition("CREW_DISEMBARK", "Véhicule détruit, équipage débarqué",
                    "2 MALFRATs PILOTE_DESTRUCTEUR au Gauss Cannon détruisent un VTT léger piloté par un BRUTE.",
                    List.of("Le véhicule explose en PdF, l'équipage débarque et devient ciblable",
                            "Le surplus de la même frappe entame le pilote fraîchement débarqué"),
                    24, 19, "lurio", "cegorach", null, false),
            new CombatScenarioDefinition("AERIAL_HIT", "Aérien, jets de toucher",
                    "3 LARBINs au Tesla Carbine attaquent un hélicoptère piloté.",
                    List.of("L'aérien n'est ciblable qu'en PdF",
                            "Chaque LARBIN n'a que 10 % de toucher (le jet est affiché)",
                            "L'hélicoptère riposte à 75 % avec 250 PdF"),
                    18, 14, "lurio", "cegorach", null, false),
            new CombatScenarioDefinition("AERIAL_ANTI_AIR", "Aérien, arme anti-aérienne",
                    "3 BRUTEs PILOTE_DESTRUCTEUR au Heavy Gauss Cannon attaquent un hélicoptère piloté.",
                    List.of("Heavy Gauss Cannon : 75 % de toucher contre l'aérien",
                            "Dégâts doublés contre l'aérien, armure +200 % sur le tireur"),
                    28, 17, "lurio", "cegorach", null, false),
            new CombatScenarioDefinition("BLDG_CAPTURE", "Élimination — QG capturé",
                    "3 VOYOUs MASTODONTE au Bouclier balistique capturent la Banque, le Cache et le QG du joueur d'arène.",
                    List.of("La Banque et le Cache ripostent en phase Bâtiments secondaires, le QG en phase QG",
                            "QG capturé ⇒ son propriétaire est éliminé",
                            "Un QG seulement détruit, non capturé, reste reconstructible",
                            "Banque et Cache pillés, puis tous les bâtiments supprimés à la finalisation",
                            "Le secteur bascule au vainqueur"),
                    5, 4, "lurio", "arene-batiments", null, true),
            new CombatScenarioDefinition("BLDG_DESTROY", "Bâtiments détruits",
                    "4 BRUTEs MASTODONTE lourdement blindés rasent la Banque, le Cache puis le QG.",
                    List.of("Ordre d'exposition : Banque → Cache → QG, le surplus enchaîne",
                            "Banque et Cache détruites ne sont pas capturées",
                            "Le QG est capturé même détruit : son propriétaire est éliminé"),
                    23, 29, "lurio", "arene-ruines", null, true),
            new CombatScenarioDefinition("CHAR_KILL", "Personnage secondaire éliminé",
                    "2 MALFRATs tuent le LARBIN d'arène puis son personnage secondaire, ciblé en dernier.",
                    List.of("Le LARBIN encaisse avant le personnage (exposition : unités → personnages)",
                            "Le personnage secondaire n'est pas le personnage principal : pas d'élimination du joueur",
                            "Personnage perdu : +1 Exp à chaque unité survivante (lignes Expérience du bilan)"),
                    9, 7, "lurio", "arene-personnages", null, false),
            new CombatScenarioDefinition("MAIN_CHARACTER_ELIMINATION",
                    "Élimination — Cegorach tué par lurio",
                    "4 BRUTEs de lurio tuent le personnage principal de cegorach dans l'arène 43, loin de son QG.",
                    List.of("Cegorach est déplacé dans l'arène 43 : son QG et ses biens en secteur 32 restent intacts",
                            "Le personnage est lié à Player.character : sa mort élimine cegorach",
                            "Esquive neutralisée dans l'arène pour garantir la mort",
                            "Après finalisation, ses unités et bâtiments ont disparu et ses actions sont refusées",
                            "Re-seed : cegorach redevient actif avec un nouveau personnage principal"),
                    43, 38, "lurio", "cegorach", null, true),
            new CombatScenarioDefinition("CHAR_REGEN", "Personnage blessé, régénération",
                    "1 MALFRAT entame le personnage sans le tuer : il riposte en phase Personnages.",
                    List.of("Le personnage ne frappe qu'en phase Personnages, avec son Attaque seule",
                            "Aucun camp anéanti : pas de vainqueur, le secteur reste neutre",
                            "Après finalisation du tour, sa défense remonte de 50 (plafonnée à sa base)"),
                    12, 31, "lurio", "arene-colosse", null, false),
            new CombatScenarioDefinition("SNIPER_RANGED", "Sniper — tir isolé et contre-tir",
                    "Un sniper d'imotekh vise le sniper de cegorach dans l'arène 15, sans combat au sol.",
                    List.of("Jet de HIT unique : la cible sniper peut riposter si elle survit",
                            "Rapport « Tir à distance » : aucun vainqueur, aucune capture",
                            "La cible MALFRAT est visée par la salve ; le BRUTE tireur n'est crédité que s'il est visé par le contre-tir",
                            "Exp : cible survivante + tireur survivant au contre-tir ; blessure si défense entamée",
                            "Finalisation directe, sans hop"),
                    15, 4, "imotekh", "cegorach", null, false),
            new CombatScenarioDefinition("SNIPER_STANDOFF", "Sniper — impasse à 3 camps",
                    "Imotekh et Cegorach s'affrontent en 16 : Imotekh a 2 fantassins, Cegorach 2 snipers. Trazyn tire depuis 14 et force une impasse à 3 camps.",
                    List.of("Imotekh a 2 fantassins (aucun sniper) ; Cegorach a 2 snipers en 16 ; Trazyn a 2 snipers en 14",
                            "A → B → C → A : Imotekh attaque les snipers de Cegorach ; Cegorach tire à distance sur Trazyn (16→14) ; Trazyn tire sur Imotekh (14→16)",
                            "Les attaques au sol d'Imotekh ne peuvent pas toucher Trazyn ; Cegorach le peut avec ses snipers",
                            "Sans snipers, Cegorach ne pourrait pas cibler Trazyn : son PDF serait reporté sur Imotekh en phase PDC",
                            "C ne gagne pas et ne capture pas ; ses pertes sont retirées de son secteur"),
                    16, 14, "imotekh", "cegorach", "trazyn", false));

    private final BoardRepository boardRepository;
    private final PlayerRepository playerRepository;
    private final MovementOrderRepository movementOrderRepository;
    private final EquipmentRepository equipmentRepository;
    private final PendingCaptureRepository pendingCaptureRepository;
    private final SniperOrderRepository sniperOrderRepository;
    private final MovementService movementService;
    private final TurnService turnService;
    private final TurnLock turnLock;
    private final EntityManager entityManager;

    public TurnResolutionScenarioSeeder(BoardRepository boardRepository,
                                        PlayerRepository playerRepository,
                                        MovementOrderRepository movementOrderRepository,
                                        EquipmentRepository equipmentRepository,
                                        PendingCaptureRepository pendingCaptureRepository,
                                        SniperOrderRepository sniperOrderRepository,
                                        MovementService movementService,
                                        TurnService turnService,
                                        TurnLock turnLock,
                                        EntityManager entityManager) {
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
        this.movementOrderRepository = movementOrderRepository;
        this.equipmentRepository = equipmentRepository;
        this.pendingCaptureRepository = pendingCaptureRepository;
        this.sniperOrderRepository = sniperOrderRepository;
        this.movementService = movementService;
        this.turnService = turnService;
        this.turnLock = turnLock;
        this.entityManager = entityManager;
    }

    public boolean isAvailable() {
        return true;
    }

    @Transactional
    public ScenarioSummaryDto seedScenario() {
        return withTurnLock(this::seedScenarioUnlocked);
    }

    private ScenarioSummaryDto seedScenarioUnlocked() {
        Board board = requireBoard();
        int turn = turnService.getCurrentTurn();

        Player lurio = resolvePlayerByName("lurio", "lurio introuvable — vérifiez le seed démo");
        Player cegorach = resolvePlayerByName("cegorach", "cegorach introuvable — vérifiez le seed démo");

        Sector sDefender = requireSector(board, SECTOR_DEFENDER, "défenseur");
        int defendersAdded = ensureDefenders(sDefender, cegorach);

        // Attaquant LEGER (≥2 hops) de lurio : cherché en 41 puis en 13 (après finalize précédent),
        // à défaut ajouté en 41.
        Sector sAttackerFrom = requireSector(board, SECTOR_ATTACKER_FROM, "attaquant");
        Unit attackerUnit = pickAttacker(sAttackerFrom, lurio.getId());
        boolean addedAttacker = false;
        int fromSector = sAttackerFrom.getNumber();
        if (attackerUnit == null) {
            Sector sIntermediate = board.getSector(SECTOR_INTERMEDIATE);
            if (sIntermediate != null) {
                attackerUnit = pickAttacker(sIntermediate, lurio.getId());
                if (attackerUnit != null) {
                    fromSector = sIntermediate.getNumber();
                }
            }
        }
        if (attackerUnit == null) {
            attackerUnit = new Unit(2.0, UnitClass.LEGER);
            attackerUnit.setPlayerId(lurio.getId());
            sAttackerFrom.addUnit(attackerUnit);
            addedAttacker = true;
            entityManager.flush();
        }

        Long attackerUnitId = attackerUnit.getId();
        List<Integer> route = fromSector == SECTOR_ATTACKER_FROM
                ? List.of(SECTOR_ATTACKER_FROM, SECTOR_INTERMEDIATE, SECTOR_DEFENDER)
                : List.of(SECTOR_INTERMEDIATE, SECTOR_DEFENDER);

        deletePendingOrders(turn, List.of(lurio.getId(), cegorach.getId()));

        MovementOrder order = movementService.placeFootOrder(
                lurio.getId(), turn, List.of(attackerUnitId), route, board);

        ScenarioSummaryDto dto = new ScenarioSummaryDto();
        dto.setTurn(turn);
        dto.setAttacker(actor(lurio));
        dto.setDefender(actor(cegorach));
        dto.setAttackerUnit(unit(attackerUnit, fromSector));
        dto.setDefendersAdded(defendersAdded);
        dto.setRoute(route);
        dto.setOrderId(order.getId());
        String hops = route.size() == 3 ? "2 hops" : "1 hop";
        dto.setMessage(addedAttacker
                ? "Scénario prêt — unité attaquante ajoutée en " + fromSector + ". Démarrez la session puis " + hops + "."
                : "Scénario prêt — démarrez la session pas-à-pas, puis avancez de " + hops
                        + " et résolvez le conflit sur le secteur " + SECTOR_DEFENDER + ".");
        return dto;
    }

    /** Impasse : cegorach défend 32 ; imotekh (43) puis lurio (41) y arrivent au même hop, dans cet ordre d'envoi. */
    @Transactional
    public ScenarioSummaryDto seedStandoffScenario() {
        return withTurnLock(this::seedStandoffScenarioUnlocked);
    }

    private ScenarioSummaryDto seedStandoffScenarioUnlocked() {
        Board board = requireBoard();
        int turn = turnService.getCurrentTurn();

        Player lurio = resolvePlayerByName("lurio", "lurio introuvable — vérifiez le seed démo");
        Player imotekh = resolvePlayerByName("imotekh", "imotekh introuvable — vérifiez le seed démo");
        Player cegorach = resolvePlayerByName("cegorach", "cegorach introuvable — vérifiez le seed démo");

        Sector sDefender = requireSector(board, SECTOR_DEFENDER, "défenseur");
        int defendersAdded = ensureDefenders(sDefender, cegorach);

        Sector sLurioFrom = requireSector(board, SECTOR_ATTACKER_FROM, "attaquant");
        Sector sImotekhFrom = requireSector(board, STANDOFF_ATTACKER_FROM, "attaquant");

        Unit lurioUnit = ensureAttackerUnit(sLurioFrom, lurio);
        Unit imotekhUnit = ensureAttackerUnit(sImotekhFrom, imotekh);
        entityManager.flush();

        deletePendingOrders(turn, List.of(lurio.getId(), imotekh.getId(), cegorach.getId()));

        MovementOrder imotekhOrder = movementService.placeFootOrder(
                imotekh.getId(), turn, List.of(imotekhUnit.getId()),
                List.of(STANDOFF_ATTACKER_FROM, SECTOR_DEFENDER), board);
        MovementOrder lurioOrder = movementService.placeFootOrder(
                lurio.getId(), turn, List.of(lurioUnit.getId()),
                List.of(SECTOR_ATTACKER_FROM, SECTOR_DEFENDER), board);

        ScenarioSummaryDto dto = new ScenarioSummaryDto();
        dto.setTurn(turn);
        dto.setStandoff(true);
        dto.setDefender(actor(cegorach));
        dto.setDefendersAdded(defendersAdded);
        dto.setOrders(List.of(
                order(imotekhOrder, imotekh, imotekhUnit, STANDOFF_ATTACKER_FROM),
                order(lurioOrder, lurio, lurioUnit, SECTOR_ATTACKER_FROM)));
        dto.setMessage("Impasse prête — imotekh puis lurio arrivent en " + SECTOR_DEFENDER
                + " avec cegorach. Démarrez la session puis un seul hop : un unique conflit à résoudre.");
        return dto;
    }

    public List<CombatScenarioDto> listCombatScenarios() {
        return COMBAT_SCENARIOS.stream().map(definition -> {
            CombatScenarioDto dto = new CombatScenarioDto();
            dto.setCode(definition.code());
            dto.setLabel(definition.label());
            dto.setDescription(definition.description());
            dto.setObservations(definition.observations());
            dto.setArenaSector(definition.arenaSector());
            dto.setStagingSector(definition.stagingSector());
            dto.setEliminatesPlayer(definition.eliminatesPlayer());
            dto.setStandalone(STANDALONE_SCENARIOS.contains(definition.code()));
            return dto;
        }).toList();
    }

    /** Purge l'arène dédiée puis la peuple : chaque scénario est rejouable sans polluer les autres. */
    @Transactional
    public ScenarioSummaryDto seedCombatScenario(String code) {
        return withTurnLock(() -> seedCombatScenarioUnlocked(code));
    }

    private ScenarioSummaryDto seedCombatScenarioUnlocked(String code) {
        Board board = requireBoard();
        int turn = turnService.getCurrentTurn();

        CombatScenarioDefinition definition = COMBAT_SCENARIOS.stream()
                .filter(scenario -> scenario.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Scénario inconnu : " + code));

        Player attacker = resolvePlayerByName(definition.attackerName(),
                definition.attackerName() + " introuvable — vérifiez le seed démo");
        Player defender = resolveDefender(definition);
        Player thirdParty = definition.thirdPartyName() == null ? null
                : resolvePlayerByName(definition.thirdPartyName(),
                        definition.thirdPartyName() + " introuvable — vérifiez le seed démo");

        Sector arena = requireSector(board, definition.arenaSector(), "arène");
        Sector staging = requireSector(board, definition.stagingSector(), "départ attaquant");
        GameCharacter mainCharacter = MAIN_CHARACTER_SCENARIO.equals(definition.code())
                ? detachMainCharacter(defender) : null;
        resetArenaDefender(defender, arena);
        Set<Long> scenarioPlayers = new HashSet<>();
        scenarioPlayers.add(attacker.getId());
        if (thirdParty != null) {
            scenarioPlayers.add(thirdParty.getId());
        }
        resetScenarioArena(board, arena, staging, scenarioPlayers);
        deletePendingSniperOrdersForArena(turn, arena.getNumber());
        if (MAIN_CHARACTER_SCENARIO.equals(definition.code())) {
            attachMainCharacter(defender, arena, mainCharacter);
            restoreMainCharacterHeadquarters(board, defender);
        }

        List<Unit> attackers = new ArrayList<>();
        switch (definition.code()) {
            case "MIXED_5050" -> seedMixedFiftyFifty(arena, staging, attacker, defender, attackers);
            case "DESTRUCTOR_VS_TANK" -> seedDestructorVsTank(arena, staging, attacker, defender, attackers);
            case "CREW_PROTECTED" -> seedCrewProtected(arena, staging, attacker, defender, attackers);
            case "CREW_DISEMBARK" -> seedCrewDisembark(arena, staging, attacker, defender, attackers);
            case "AERIAL_HIT" -> seedAerialHit(arena, staging, attacker, defender, attackers);
            case "AERIAL_ANTI_AIR" -> seedAerialAntiAir(arena, staging, attacker, defender, attackers);
            case "BLDG_CAPTURE" -> seedBuildingCapture(arena, staging, attacker, defender, attackers);
            case "BLDG_DESTROY" -> seedBuildingDestroy(arena, staging, attacker, defender, attackers);
            case "CHAR_KILL" -> seedCharacterKill(arena, staging, attacker, defender, attackers);
            case "MAIN_CHARACTER_ELIMINATION" ->
                    seedMainCharacterElimination(staging, attacker, attackers);
            case "CHAR_REGEN" -> seedCharacterRegen(arena, staging, attacker, defender, attackers);
            case "SNIPER_RANGED" -> seedSniperRanged(arena, staging, attacker, defender, attackers, turn);
            case "SNIPER_STANDOFF" -> seedSniperStandoff(arena, staging, attacker, defender, thirdParty,
                    attackers, turn);
            default -> throw new IllegalArgumentException("Scénario inconnu : " + definition.code());
        }
        entityManager.flush();
        // purgeSector et place* court-circuitent addUnit : sans ce recalcul les stats du secteur restent celles d'avant le seed.
        arena.recalculateMilitaryPower();
        staging.recalculateMilitaryPower();

        deletePendingOrdersForArena(turn, arena.getNumber());

        ScenarioSummaryDto dto = new ScenarioSummaryDto();
        dto.setTurn(turn);
        dto.setScenarioCode(definition.code());
        dto.setObservations(definition.observations());
        dto.setAttacker(actor(attacker));
        dto.setDefender(actor(defender));
        dto.setAttackerUnit(unit(attackers.getFirst(), staging.getNumber()));
        if (STANDALONE_SCENARIOS.contains(definition.code())) {
            dto.setRoute(List.of());
            dto.setMessage(definition.label() + " — " + definition.description()
                    + " Démarrez la session puis finalisez directement (aucun hop) : le tir isolé,"
                    + " le contre-tir et l'expérience sont résolus à la finalisation.");
        } else {
            MovementOrder order = movementService.placeFootOrder(
                    attacker.getId(), turn, attackers.stream().map(Unit::getId).toList(),
                    List.of(staging.getNumber(), arena.getNumber()), board);
            dto.setRoute(new ArrayList<>(order.getRoute()));
            dto.setOrderId(order.getId());
            dto.setMessage(definition.label() + " — " + definition.description()
                    + " Démarrez la session puis 1 hop : un conflit à résoudre en secteur "
                    + definition.arenaSector() + ".");
        }
        return dto;
    }

    private void seedMixedFiftyFifty(Sector arena, Sector staging, Player attacker, Player defender,
                                     List<Unit> attackers) {
        placeVehicle(arena, defender, VehicleType.VTT_LEGER, null, null);
        for (int i = 0; i < 2; i++) {
            arena.addUnit(combatUnit(defender, 0.0, UnitClass.TIREUR));
        }
        for (int i = 0; i < 3; i++) {
            placeAttacker(staging, attackers, combatUnit(attacker, 8.0, UnitClass.TIREUR));
        }
    }

    private void seedDestructorVsTank(Sector arena, Sector staging, Player attacker, Player defender,
                                      List<Unit> attackers) {
        placeVehicle(arena, defender, VehicleType.TANK, combatUnit(defender, 0.0, UnitClass.PILOTE_DESTRUCTEUR), null);
        for (int i = 0; i < 2; i++) {
            arena.addUnit(combatUnit(defender, 0.0, UnitClass.TIREUR));
        }
        for (int i = 0; i < 2; i++) {
            placeAttacker(staging, attackers, combatUnit(attacker, 5.0, UnitClass.PILOTE_DESTRUCTEUR,
                    "Gauss Cannon", "Gilet Kevlar", "Protège-dents"));
        }
    }

    private void seedCrewProtected(Sector arena, Sector staging, Player attacker, Player defender,
                                   List<Unit> attackers) {
        placeVehicle(arena, defender, VehicleType.VTT_BLINDE,
                combatUnit(defender, 0.0, UnitClass.PILOTE_DESTRUCTEUR, "Gauss Cannon"),
                combatUnit(defender, 0.0, UnitClass.LEGER, "Tesla Carbine"));
        for (int i = 0; i < 3; i++) {
            placeAttacker(staging, attackers, combatUnit(attacker, 0.0, UnitClass.TIREUR, "Gauss Flayer"));
        }
    }

    private void seedCrewDisembark(Sector arena, Sector staging, Player attacker, Player defender,
                                   List<Unit> attackers) {
        placeVehicle(arena, defender, VehicleType.VTT_LEGER,
                combatUnit(defender, 8.0, UnitClass.PILOTE_DESTRUCTEUR), null);
        for (int i = 0; i < 2; i++) {
            placeAttacker(staging, attackers, combatUnit(attacker, 5.0, UnitClass.PILOTE_DESTRUCTEUR,
                    "Gauss Cannon", "Gilet Kevlar"));
        }
    }

    private void seedAerialHit(Sector arena, Sector staging, Player attacker, Player defender,
                               List<Unit> attackers) {
        placeVehicle(arena, defender, VehicleType.HELICOPTERE, combatUnit(defender, 0.0, UnitClass.PILOTE_DESTRUCTEUR), null);
        for (int i = 0; i < 3; i++) {
            placeAttacker(staging, attackers, combatUnit(attacker, 0.0, UnitClass.LEGER, "Tesla Carbine"));
        }
    }

    private void seedAerialAntiAir(Sector arena, Sector staging, Player attacker, Player defender,
                                   List<Unit> attackers) {
        placeVehicle(arena, defender, VehicleType.HELICOPTERE, combatUnit(defender, 0.0, UnitClass.PILOTE_DESTRUCTEUR), null);
        for (int i = 0; i < 3; i++) {
            placeAttacker(staging, attackers, combatUnit(attacker, 8.0, UnitClass.PILOTE_DESTRUCTEUR, "Heavy Gauss Cannon"));
        }
    }

    private void seedBuildingCapture(Sector arena, Sector staging, Player attacker, Player defender,
                                     List<Unit> attackers) {
        placeBank(arena, defender);
        placeWeaponCache(arena, defender, "Gauss Flayer");
        placeHeadquarters(arena, defender);
        for (int i = 0; i < 3; i++) {
            placeAttacker(staging, attackers, combatUnit(attacker, 2.0, UnitClass.MASTODONTE,
                    "Bouclier balistique", "Dispersion Shield"));
        }
    }

    private void seedBuildingDestroy(Sector arena, Sector staging, Player attacker, Player defender,
                                     List<Unit> attackers) {
        placeBank(arena, defender);
        placeWeaponCache(arena, defender, "Gauss Flayer");
        placeHeadquarters(arena, defender);
        for (int i = 0; i < 4; i++) {
            placeAttacker(staging, attackers, combatUnit(attacker, 8.0, UnitClass.MASTODONTE,
                    "Bouclier balistique", "Dispersion Shield"));
        }
    }

    private void seedCharacterKill(Sector arena, Sector staging, Player attacker, Player defender,
                                   List<Unit> attackers) {
        arena.addUnit(combatUnit(defender, 0.0, UnitClass.TIREUR, "Gauss Flayer"));
        placeCharacter(arena, defender, "Champion d'arène", 30, 20, 60);
        for (int i = 0; i < 2; i++) {
            placeAttacker(staging, attackers, combatUnit(attacker, 5.0, UnitClass.TIREUR));
        }
    }

    private void seedMainCharacterElimination(Sector staging, Player attacker, List<Unit> attackers) {
        for (int i = 0; i < 4; i++) {
            placeAttacker(staging, attackers, combatUnit(attacker, 8.0, UnitClass.TIREUR));
        }
    }

    /** Retire le personnage de son secteur avant la purge de l'arène : il est réattaché après. */
    private GameCharacter detachMainCharacter(Player defender) {
        GameCharacter character = defender.getCharacter();
        if (character != null && character.getSector() != null) {
            character.getSector().getCharacters().remove(character);
        }
        return character;
    }

    /** Arène de test : l'esquive est neutralisée pour rendre la mort du personnage déterministe. */
    private void attachMainCharacter(Player defender, Sector arena, GameCharacter preserved) {
        GameCharacter character = preserved;
        if (character == null) {
            character = new GameCharacter("Cegorach", 100, 50, 50, 200, 50, 25);
            character.setPlayerId(defender.getId());
            entityManager.persist(character);
            defender.setCharacter(character);
        }
        character.setSector(arena);
        character.setEvasion(0);
        arena.getCharacters().add(character);
        clearElimination(defender);
    }

    /** Une première exécution supprime tous les bâtiments de la victime : le seed recrée son QG. */
    private void restoreMainCharacterHeadquarters(Board board, Player defender) {
        boolean present = defender.getBuildings().stream().anyMatch(Headquarters.class::isInstance);
        if (!present) {
            placeHeadquarters(requireSector(board, MAIN_CHARACTER_HQ_SECTOR, "secteur du QG"), defender);
        }
    }

    private void seedCharacterRegen(Sector arena, Sector staging, Player attacker, Player defender,
                                    List<Unit> attackers) {
        arena.addUnit(combatUnit(defender, 0.0, UnitClass.TIREUR));
        placeCharacter(arena, defender, "Colosse d'arène", 30, 0, 100);
        placeAttacker(staging, attackers, combatUnit(attacker, 5.0, UnitClass.TIREUR));
    }

    private void seedSniperRanged(Sector arena, Sector staging, Player attacker, Player defender,
                                  List<Unit> attackers, int turn) {
        Unit target = combatUnit(defender, 5.0, UnitClass.SNIPER,
                "Synaptic Disintegrator", "Gilet de camouflage optique");
        arena.addUnit(target);
        Unit shooter = combatUnit(attacker, 8.0, UnitClass.SNIPER,
                "Synaptic Disintegrator", "Gilet de camouflage optique");
        staging.addUnit(shooter);
        attackers.add(shooter);
        entityManager.flush();
        sniperOrderRepository.save(SniperOrder.create(attacker.getId(), turn, shooter.getId(),
                staging.getNumber(), arena.getNumber()));
    }

    private void seedSniperStandoff(Sector arena, Sector staging, Player attacker, Player defender,
                                    Player third, List<Unit> attackers, int turn) {
        for (int i = 0; i < 2; i++) {
            placeAttacker(staging, attackers, combatUnit(attacker, 0.0, UnitClass.TIREUR, "Gauss Flayer"));
        }
        for (int i = 0; i < 2; i++) {
            arena.addUnit(combatUnit(defender, 8.0, UnitClass.SNIPER,
                    "Synaptic Disintegrator", "Gilet de camouflage optique"));
        }
        List<Unit> thirdSnipers = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Unit thirdSniper = combatUnit(third, 8.0, UnitClass.SNIPER,
                    "Synaptic Disintegrator", "Gilet de camouflage optique");
            staging.addUnit(thirdSniper);
            thirdSnipers.add(thirdSniper);
        }
        entityManager.flush();
        for (Unit thirdSniper : thirdSnipers) {
            sniperOrderRepository.save(SniperOrder.create(third.getId(), turn, thirdSniper.getId(),
                    staging.getNumber(), arena.getNumber()));
        }
    }

    private void resetScenarioArena(Board board, Sector arena, Sector staging, Set<Long> scenarioPlayers) {
        purgeSector(arena);
        purgeStaging(staging, scenarioPlayers);
        arena.setOwnerAndColor(null, "#ffffff");
        pendingCaptureRepository.findByResolvedFalseOrderByIdAsc().stream()
                .filter(pending -> Objects.equals(pending.getBoardId(), board.getId())
                        && pending.getSectorNumber() == arena.getNumber())
                .forEach(pendingCaptureRepository::delete);
        entityManager.flush();
    }

    /** Les joueurs d'arène sont jetables : re-seed nettoie l'élimination et le personnage principal de l'arène. */
    private void resetArenaDefender(Player defender, Sector arena) {
        if (!defender.getName().startsWith(ARENA_PLAYER_PREFIX)) {
            return;
        }
        GameCharacter character = defender.getCharacter();
        if (character != null && character.getSector() != null
                && character.getSector().getNumber() == arena.getNumber()) {
            // Sortir de la collection du secteur évite un double em.remove dans purgeSector.
            character.getSector().getCharacters().remove(character);
            defender.setCharacter(null);
        }
        clearElimination(defender);
        entityManager.flush();
    }

    private void clearElimination(Player defender) {
        defender.setEliminated(false);
        defender.setEliminationCause(null);
        defender.setEliminatedTurn(null);
        defender.setEliminatedByPlayerId(null);
    }

    private void purgeSector(Sector sector) {
        for (Vehicle vehicle : new ArrayList<>(sector.getVehicles())) {
            vehicle.disembarkAll();
            entityManager.remove(vehicle);
        }
        sector.getVehicles().clear();
        for (Building building : new ArrayList<>(sector.getBuildings())) {
            entityManager.remove(building);
        }
        sector.getBuildings().clear();
        for (GameCharacter character : new ArrayList<>(sector.getCharacters())) {
            entityManager.remove(character);
        }
        sector.getCharacters().clear();
        for (Unit unit : new ArrayList<>(sector.getUnits())) {
            entityManager.remove(unit);
        }
        sector.getUnits().clear();
    }

    /** Un départ peut être partagé : ne retirer que les unités du scénario, jamais celles d'un autre. */
    private void purgeStaging(Sector staging, Set<Long> playerIds) {
        for (Unit unit : new ArrayList<>(staging.getUnits())) {
            if (playerIds.contains(unit.getPlayerId())) {
                entityManager.remove(unit);
                staging.getUnits().remove(unit);
            }
        }
    }

    /** Défenseur de fixture (cegorach) ou propriétaire jetable d'arène, créé au premier seed. */
    private Player resolveDefender(CombatScenarioDefinition definition) {
        String name = definition.defenderName();
        if (!name.startsWith(ARENA_PLAYER_PREFIX)) {
            return resolvePlayerByName(name, name + " introuvable — vérifiez le seed démo");
        }
        return playerRepository.findByName(name).orElseGet(() -> {
            Player player = new Player(name);
            player.incrementMoney(10000);
            return playerRepository.save(player);
        });
    }

    private void placeHeadquarters(Sector arena, Player owner) {
        placeBuilding(arena, new Headquarters(owner.getId()));
    }

    private void placeBank(Sector arena, Player owner) {
        placeBuilding(arena, new Bank(owner.getId()));
    }

    private void placeWeaponCache(Sector arena, Player owner, String equipmentName) {
        WeaponCache cache = new WeaponCache(owner.getId());
        placeBuilding(arena, cache);
        equipmentRepository.findByName(equipmentName).ifPresent(equipment -> {
            // player_id est NOT NULL sur equipment_stacks, même pour un stack stocké en cache.
            EquipmentStack stack = new EquipmentStack(equipment);
            stack.setPlayer(owner);
            cache.getStoredEquipments().add(stack);
        });
    }

    private void placeBuilding(Sector arena, Building building) {
        building.setSector(arena);
        entityManager.persist(building);
        arena.getBuildings().add(building);
    }

    private void placeCharacter(Sector arena, Player owner, String name, double attack, double pdf, double defense) {
        GameCharacter character = new GameCharacter(name, attack, pdf, 0, defense, 0, 0);
        character.setPlayerId(owner.getId());
        character.setSector(arena);
        entityManager.persist(character);
        arena.getCharacters().add(character);
    }

    private void deletePendingOrdersForArena(int turn, int arenaNumber) {
        List<MovementOrder> pending = movementOrderRepository.findPendingByTurn(turn).stream()
                .filter(order -> !order.getRoute().isEmpty() && order.getRoute().getLast() == arenaNumber)
                .toList();
        if (!pending.isEmpty()) {
            movementOrderRepository.deleteAll(pending);
        }
    }

    private void deletePendingSniperOrdersForArena(int turn, int arenaNumber) {
        List<SniperOrder> pending = sniperOrderRepository.findByTurnAndStatus(turn, SniperOrderStatus.PENDING)
                .stream()
                .filter(order -> order.getTargetSectorNumber() == arenaNumber)
                .toList();
        if (!pending.isEmpty()) {
            sniperOrderRepository.deleteAll(pending);
        }
    }

    private Unit combatUnit(Player owner, double experience, UnitClass unitClass, String... equipmentNames) {
        Unit unit = new Unit(experience, unitClass);
        unit.setPlayerId(owner.getId());
        for (String equipmentName : equipmentNames) {
            equipmentRepository.findByName(equipmentName).ifPresent(unit::addEquipment);
        }
        return unit;
    }

    private void placeAttacker(Sector staging, List<Unit> attackers, Unit unit) {
        staging.addUnit(unit);
        attackers.add(unit);
    }

    private Vehicle placeVehicle(Sector arena, Player owner, VehicleType type, Unit pilot, Unit passenger) {
        Vehicle vehicle = new Vehicle(type, owner.getId());
        vehicle.setSector(arena);
        entityManager.persist(vehicle);
        arena.getVehicles().add(vehicle);
        if (pilot != null) {
            arena.addUnit(pilot);
            vehicle.assignPilot(pilot);
        }
        if (passenger != null) {
            arena.addUnit(passenger);
            vehicle.embark(passenger);
        }
        return vehicle;
    }

    private ScenarioSummaryDto.ActorDto actor(Player player) {
        ScenarioSummaryDto.ActorDto actor = new ScenarioSummaryDto.ActorDto();
        actor.setId(player.getId());
        actor.setName(player.getName());
        return actor;
    }

    private ScenarioSummaryDto.UnitDto unit(Unit unit, int fromSector) {
        ScenarioSummaryDto.UnitDto dto = new ScenarioSummaryDto.UnitDto();
        dto.setId(unit.getId());
        dto.setUnitClass(unit.getClasses().stream().map(UnitClass::name).findFirst().orElse("?"));
        dto.setFromSector(fromSector);
        return dto;
    }

    private ScenarioSummaryDto.OrderDto order(MovementOrder order, Player player, Unit unit, int fromSector) {
        ScenarioSummaryDto.OrderDto dto = new ScenarioSummaryDto.OrderDto();
        dto.setPlayerId(player.getId());
        dto.setPlayerName(player.getName());
        dto.setUnitId(unit.getId());
        dto.setUnitClass(unit.getClasses().stream().map(UnitClass::name).findFirst().orElse("?"));
        dto.setFromSector(fromSector);
        dto.setRoute(new ArrayList<>(order.getRoute()));
        dto.setOrderId(order.getId());
        return dto;
    }

    private Board requireBoard() {
        return boardRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun plateau trouvé — importez d'abord un board"));
    }

    private ScenarioSummaryDto withTurnLock(Supplier<ScenarioSummaryDto> seed) {
        if (!turnLock.tryAcquire()) {
            throw new IllegalStateException(
                    "Une session pas-à-pas est active — finalisez ou abandonnez-la avant de re-seeder");
        }
        try {
            return seed.get();
        } finally {
            turnLock.release();
        }
    }

    private Sector requireSector(Board board, int number, String role) {
        Sector sector = board.getSector(number);
        if (sector == null) {
            throw new IllegalStateException("Secteur " + role + " " + number + " introuvable sur le plateau");
        }
        return sector;
    }

    private int ensureDefenders(Sector sector, Player defender) {
        int defendersAdded = 0;
        long existing = sector.getUnits().stream()
                .filter(u -> defender.getId().equals(u.getPlayerId()))
                .count();
        for (int i = 0; i < TARGET_DEFENDER_COUNT - existing; i++) {
            Unit unit = new Unit(8.0, UnitClass.TIREUR);
            unit.setPlayerId(defender.getId());
            sector.addUnit(unit);
            defendersAdded++;
        }
        entityManager.flush();
        return defendersAdded;
    }

    private Unit ensureAttackerUnit(Sector sector, Player player) {
        Unit existing = sector.getUnits().stream()
                .filter(u -> player.getId().equals(u.getPlayerId()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        Unit unit = new Unit(8.0, UnitClass.TIREUR);
        unit.setPlayerId(player.getId());
        sector.addUnit(unit);
        return unit;
    }

    private void deletePendingOrders(int turn, List<Long> playerIds) {
        List<MovementOrder> pending = movementOrderRepository.findPendingByTurn(turn).stream()
                .filter(o -> playerIds.contains(o.getPlayerId()))
                .toList();
        if (!pending.isEmpty()) {
            movementOrderRepository.deleteAll(pending);
            entityManager.flush();
        }
    }

    private Unit pickAttacker(Sector sector, Long playerId) {
        return sector.getUnits().stream()
                .filter(u -> playerId.equals(u.getPlayerId()))
                .filter(u -> u.getMaxMovementHops() >= 2)
                .findFirst()
                .orElse(null);
    }

    private Player resolvePlayerByName(String name, String errorMessage) {
        return playerRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException(errorMessage));
    }
}
