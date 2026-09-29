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
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Dev-only, idempotent : purge les PENDING du tour et n'ajoute que les unités manquantes. */
@Service
public class TurnResolutionScenarioSeeder {

    private static final int SECTOR_ATTACKER_FROM = 41;
    private static final int SECTOR_INTERMEDIATE = 13;
    private static final int SECTOR_DEFENDER = 32;
    private static final int TARGET_DEFENDER_COUNT = 2;

    private static final int STANDOFF_ATTACKER_FROM = 43;

    private record CombatScenarioDefinition(String code, String label, String description,
                                            List<String> observations, int arenaSector, int stagingSector,
                                            String defenderName) {
    }

    private static final String ARENA_PLAYER_PREFIX = "arene-";

    /** Arènes neutres dédiées : tout ce qui s'y trouve est purgé au seed, aucun fixture n'y place d'entité. */
    private static final List<CombatScenarioDefinition> COMBAT_SCENARIOS = List.of(
            new CombatScenarioDefinition("MIXED_5050", "Infanterie vs véhicules (50/50)",
                    "3 BRUTEs de lurio attaquent 2 LARBINs et un VTT léger sans pilote de cegorach.",
                    List.of("Tirage 50/50 par attaque entre véhicules et unités",
                            "Le VTT sans pilote est une cible passive qui ne tire pas",
                            "Le surplus d'une frappe enchaîne sur la cible suivante"),
                    21, 36, "cegorach"),
            new CombatScenarioDefinition("DESTRUCTOR_VS_TANK", "Pilote destructeur vs Tank",
                    "2 MALFRATs PILOTE_DESTRUCTEUR au Gauss Cannon attaquent un Tank piloté et 2 LARBINs.",
                    List.of("Ciblage prioritaire du Tank malgré les LARBINs exposés",
                            "Gauss Cannon : bonus anti-véhicule ×2 puis résistance du Tank −50 %",
                            "Le Tank endommagé répare 50 défense en fin de tour s'il survit"),
                    42, 37, "cegorach"),
            new CombatScenarioDefinition("CREW_PROTECTED", "Équipage protégé",
                    "3 LARBINs au Gauss Flayer attaquent un VTT blindé piloté avec un passager armé.",
                    List.of("Les tirs de l'équipage sont tracés « (à bord de VTT blindé) »",
                            "Pilote et passager restent intouchables tant que le véhicule vit",
                            "Les attaquants ne touchent que le véhicule en PdF"),
                    35, 22, "cegorach"),
            new CombatScenarioDefinition("CREW_DISEMBARK", "Véhicule détruit, équipage débarqué",
                    "2 MALFRATs PILOTE_DESTRUCTEUR au Gauss Cannon détruisent un VTT léger piloté par un BRUTE.",
                    List.of("Le véhicule explose en PdF, l'équipage débarque et devient ciblable",
                            "Le surplus de la même frappe entame le pilote fraîchement débarqué"),
                    24, 19, "cegorach"),
            new CombatScenarioDefinition("AERIAL_HIT", "Aérien, jets de toucher",
                    "3 LARBINs au Tesla Carbine attaquent un hélicoptère piloté.",
                    List.of("L'aérien n'est ciblable qu'en PdF",
                            "Chaque LARBIN n'a que 10 % de toucher (le jet est affiché)",
                            "L'hélicoptère riposte à 75 % avec 250 PdF"),
                    18, 14, "cegorach"),
            new CombatScenarioDefinition("AERIAL_ANTI_AIR", "Aérien, arme anti-aérienne",
                    "3 BRUTEs PILOTE_DESTRUCTEUR au Heavy Gauss Cannon attaquent un hélicoptère piloté.",
                    List.of("Heavy Gauss Cannon : 75 % de toucher contre l'aérien",
                            "Dégâts doublés contre l'aérien, armure +200 % sur le tireur"),
                    28, 17, "cegorach"),
            new CombatScenarioDefinition("BLDG_CAPTURE", "Capture de bâtiments",
                    "3 VOYOUs MASTODONTE au Bouclier balistique attaquent une Banque, un Cache et un QG sans unité.",
                    List.of("La Banque et le Cache ripostent en phase Bâtiments secondaires, le QG en phase QG",
                            "Un camp réduit à ses bâtiments est déclaré perdant",
                            "Les trois bâtiments, seulement endommagés, sont capturés — QG compris",
                            "Le secteur bascule à lurio à la résolution"),
                    5, 4, "arene-batiments"),
            new CombatScenarioDefinition("BLDG_DESTROY", "Bâtiments détruits",
                    "4 BRUTEs MASTODONTE lourdement blindés rasent la Banque, le Cache puis le QG.",
                    List.of("Ordre d'exposition : Banque → Cache → QG, le surplus enchaîne",
                            "Banque et Cache détruites ne sont pas capturées",
                            "Le QG est capturé même détruit (arbitrage MJ)"),
                    23, 29, "arene-ruines"),
            new CombatScenarioDefinition("CHAR_KILL", "Personnage éliminé",
                    "2 MALFRATs tuent le LARBIN d'arène puis son personnage, ciblé en dernier.",
                    List.of("Le LARBIN encaisse avant le personnage (exposition : unités → personnages)",
                            "Le personnage tire sa PdF en phase PdF puis meurt en phase ATK, avant sa phase Personnages",
                            "Personnage perdu : +1 Exp à chaque unité survivante (lignes Expérience du bilan)"),
                    9, 7, "arene-personnages"),
            new CombatScenarioDefinition("CHAR_REGEN", "Personnage blessé, régénération",
                    "1 MALFRAT entame le personnage sans le tuer : il riposte en phase Personnages.",
                    List.of("Le personnage ne frappe qu'en phase Personnages, avec son Attaque seule",
                            "Aucun camp anéanti : pas de vainqueur, le secteur reste neutre",
                            "Après finalisation du tour, sa défense remonte de 50 (plafonnée à sa base)"),
                    12, 31, "arene-colosse"));

    private final BoardRepository boardRepository;
    private final PlayerRepository playerRepository;
    private final MovementOrderRepository movementOrderRepository;
    private final EquipmentRepository equipmentRepository;
    private final PendingCaptureRepository pendingCaptureRepository;
    private final MovementService movementService;
    private final TurnService turnService;
    private final TurnLock turnLock;
    private final EntityManager entityManager;

    public TurnResolutionScenarioSeeder(BoardRepository boardRepository,
                                        PlayerRepository playerRepository,
                                        MovementOrderRepository movementOrderRepository,
                                        EquipmentRepository equipmentRepository,
                                        PendingCaptureRepository pendingCaptureRepository,
                                        MovementService movementService,
                                        TurnService turnService,
                                        TurnLock turnLock,
                                        EntityManager entityManager) {
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
        this.movementOrderRepository = movementOrderRepository;
        this.equipmentRepository = equipmentRepository;
        this.pendingCaptureRepository = pendingCaptureRepository;
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
        Board board = requireBoard();
        int turn = turnService.getCurrentTurn();
        requireNoActiveSession();

        Player lurio = resolvePlayerByName("lurio", "lurio introuvable — vérifiez le seed démo");
        Player cegorach = resolvePlayerByName("cegorach", "cegorach introuvable — vérifiez le seed démo");

        Sector sDefender = requireSector(board, SECTOR_DEFENDER, "défenseur");
        int defendersAdded = ensureDefenders(sDefender, cegorach);

        // Attaquant LEGER (≥2 hops) de lurio : cherché en 41 puis en 13 (après finalize précédent),
        // à défaut ajouté en 41.
        Sector sAttackerFrom = requireSector(board, SECTOR_ATTACKER_FROM, "attaquant");
        Unit attackerUnit = pickAttacker(sAttackerFrom, lurio.getId());
        boolean addedAttacker = false;
        if (attackerUnit == null) {
            Sector sIntermediate = board.getSector(SECTOR_INTERMEDIATE);
            if (sIntermediate != null) {
                attackerUnit = pickAttacker(sIntermediate, lurio.getId());
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
        int fromSector = sAttackerFrom.getNumber();

        deletePendingOrders(turn, List.of(lurio.getId(), cegorach.getId()));

        MovementOrder order = movementService.placeFootOrder(
                lurio.getId(), turn, List.of(attackerUnitId),
                List.of(SECTOR_ATTACKER_FROM, SECTOR_INTERMEDIATE, SECTOR_DEFENDER), board);

        ScenarioSummaryDto dto = new ScenarioSummaryDto();
        dto.setTurn(turn);
        dto.setAttacker(actor(lurio));
        dto.setDefender(actor(cegorach));
        dto.setAttackerUnit(unit(attackerUnit, fromSector));
        dto.setDefendersAdded(defendersAdded);
        dto.setRoute(List.of(SECTOR_ATTACKER_FROM, SECTOR_INTERMEDIATE, SECTOR_DEFENDER));
        dto.setOrderId(order.getId());
        dto.setMessage(addedAttacker
                ? "Scénario prêt — unité attaquante ajoutée en " + fromSector + ". Démarrez la session puis 2 hops."
                : "Scénario prêt — démarrez la session pas-à-pas, puis avancez de 2 hops et résolvez le conflit sur le secteur " + SECTOR_DEFENDER + ".");
        return dto;
    }

    /** Impasse : cegorach défend 32 ; imotekh (43) puis lurio (41) y arrivent au même hop, dans cet ordre d'envoi. */
    @Transactional
    public ScenarioSummaryDto seedStandoffScenario() {
        Board board = requireBoard();
        int turn = turnService.getCurrentTurn();
        requireNoActiveSession();

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
            return dto;
        }).toList();
    }

    /** Purge l'arène dédiée puis la peuple : chaque scénario est rejouable sans polluer les autres. */
    @Transactional
    public ScenarioSummaryDto seedCombatScenario(String code) {
        Board board = requireBoard();
        int turn = turnService.getCurrentTurn();
        requireNoActiveSession();

        CombatScenarioDefinition definition = COMBAT_SCENARIOS.stream()
                .filter(scenario -> scenario.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Scénario inconnu : " + code));

        Player lurio = resolvePlayerByName("lurio", "lurio introuvable — vérifiez le seed démo");
        Player defender = resolveDefender(definition);

        Sector arena = requireSector(board, definition.arenaSector(), "arène");
        Sector staging = requireSector(board, definition.stagingSector(), "départ attaquant");
        resetScenarioArena(board, arena, staging);

        List<Unit> attackers = new ArrayList<>();
        switch (definition.code()) {
            case "MIXED_5050" -> seedMixedFiftyFifty(arena, staging, lurio, defender, attackers);
            case "DESTRUCTOR_VS_TANK" -> seedDestructorVsTank(arena, staging, lurio, defender, attackers);
            case "CREW_PROTECTED" -> seedCrewProtected(arena, staging, lurio, defender, attackers);
            case "CREW_DISEMBARK" -> seedCrewDisembark(arena, staging, lurio, defender, attackers);
            case "AERIAL_HIT" -> seedAerialHit(arena, staging, lurio, defender, attackers);
            case "AERIAL_ANTI_AIR" -> seedAerialAntiAir(arena, staging, lurio, defender, attackers);
            case "BLDG_CAPTURE" -> seedBuildingCapture(arena, staging, lurio, defender, attackers);
            case "BLDG_DESTROY" -> seedBuildingDestroy(arena, staging, lurio, defender, attackers);
            case "CHAR_KILL" -> seedCharacterKill(arena, staging, lurio, defender, attackers);
            case "CHAR_REGEN" -> seedCharacterRegen(arena, staging, lurio, defender, attackers);
            default -> throw new IllegalArgumentException("Scénario inconnu : " + definition.code());
        }
        entityManager.flush();
        // purgeSector et place* court-circuitent addUnit : sans ce recalcul les stats du secteur restent celles d'avant le seed.
        arena.recalculateMilitaryPower();
        staging.recalculateMilitaryPower();

        deletePendingOrdersForArena(turn, arena.getNumber());
        MovementOrder order = movementService.placeFootOrder(
                lurio.getId(), turn, attackers.stream().map(Unit::getId).toList(),
                List.of(staging.getNumber(), arena.getNumber()), board);

        ScenarioSummaryDto dto = new ScenarioSummaryDto();
        dto.setTurn(turn);
        dto.setScenarioCode(definition.code());
        dto.setObservations(definition.observations());
        dto.setAttacker(actor(lurio));
        dto.setDefender(actor(defender));
        dto.setAttackerUnit(unit(attackers.getFirst(), staging.getNumber()));
        dto.setRoute(new ArrayList<>(order.getRoute()));
        dto.setOrderId(order.getId());
        dto.setMessage(definition.label() + " — " + definition.description()
                + " Démarrez la session puis 1 hop : un conflit à résoudre en secteur "
                + definition.arenaSector() + ".");
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

    private void seedCharacterRegen(Sector arena, Sector staging, Player attacker, Player defender,
                                    List<Unit> attackers) {
        arena.addUnit(combatUnit(defender, 0.0, UnitClass.TIREUR));
        placeCharacter(arena, defender, "Colosse d'arène", 30, 0, 100);
        placeAttacker(staging, attackers, combatUnit(attacker, 5.0, UnitClass.TIREUR));
    }

    private void resetScenarioArena(Board board, Sector arena, Sector staging) {
        purgeSector(arena);
        purgeSector(staging);
        arena.setOwnerAndColor(null, "#ffffff");
        pendingCaptureRepository.findByResolvedFalseOrderByIdAsc().stream()
                .filter(pending -> Objects.equals(pending.getBoardId(), board.getId())
                        && pending.getSectorNumber() == arena.getNumber())
                .forEach(pendingCaptureRepository::delete);
        entityManager.flush();
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

    private void requireNoActiveSession() {
        if (turnLock.isLocked()) {
            throw new IllegalStateException(
                    "Une session pas-à-pas est active — finalisez ou abandonnez-la avant de re-seeder");
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
