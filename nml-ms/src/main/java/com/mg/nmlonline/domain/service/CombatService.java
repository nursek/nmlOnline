package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.BuildingType;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.EntityCategory;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.domain.model.battle.Battle;
import com.mg.nmlonline.domain.model.battle.BattleLogEntry;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CombatService {

    private static final Logger logger = LoggerFactory.getLogger(CombatService.class);

    @Autowired
    private PlayerStatsService playerStatsService;

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private EntityManager em;

    public Optional<Sector> findSectorWithArmy(Player player, Board board) {
        if (player == null || board == null) {
            return Optional.empty();
        }

        List<Sector> sectorsWithArmy = playerStatsService.getSectorsWithCombatEntities(player, board);
        return sectorsWithArmy.stream().findFirst();
    }

    /** Combat sur un secteur : unités + personnages + bâtiments co-localisés (véhicules exclus). */
    public SectorBattleResult simulateSectorBattle(Player attacker, Player defender, Board board, int sectorNumber) {
        if (attacker == null || defender == null || board == null) {
            return failedResult("Paramètres invalides");
        }
        Sector sector = board.getSector(sectorNumber);
        if (sector == null) {
            return failedResult("Secteur inexistant : " + sectorNumber);
        }

        playerStatsService.updateCombatStats(attacker, board);
        playerStatsService.updateCombatStats(defender, board);

        // Ordre inverse de la mort (getLast()) : unités → Banque → Cache → QG → personnage en dernier.
        List<CombatEntity> attackerFighters = collectBattleParticipants(sector, attacker.getId());
        List<CombatEntity> defenderFighters = collectBattleParticipants(sector, defender.getId());

        if (attackerFighters.isEmpty() || defenderFighters.isEmpty()) {
            return failedResult("Aucune entité combattante au secteur " + sectorNumber
                    + " (attaquant=" + attackerFighters.size() + ", défenseur=" + defenderFighters.size() + ")");
        }

        // Snapshot des IDs avant combat pour identifier les pertes après coup.
        Set<Long> beforeIds = new HashSet<>();
        attackerFighters.forEach(u -> beforeIds.add(u.getId()));
        defenderFighters.forEach(u -> beforeIds.add(u.getId()));

        Battle battle = new Battle();
        battle.classicCombatConfiguration(attacker, defender, attackerFighters, defenderFighters);

        // Réconciliation : les pertes (retirées des listes de travail par Battle) sortent de sector.getArmy() pour em.remove. Survivants = mêmes références, stats déjà mutées.
        Set<Long> survivorIds = new HashSet<>();
        attackerFighters.forEach(u -> survivorIds.add(u.getId()));
        defenderFighters.forEach(u -> survivorIds.add(u.getId()));

        List<CombatEntity> casualties = new ArrayList<>();
        Map<Long, Player> playersById = Map.of(attacker.getId(), attacker, defender.getId(), defender);
        Map<Long, Boolean> characterLost = new HashMap<>();
        casualties.addAll(removeCasualties(sector, beforeIds, survivorIds, playersById, characterLost));
        boolean attackerCharacterLost = characterLost.getOrDefault(attacker.getId(), false);
        boolean defenderCharacterLost = characterLost.getOrDefault(defender.getId(), false);

        // Bâtiments : stats de base (annule le reassign-zéro). Personnages : offense/soak, pas la défense.
        for (CombatEntity entity : attackerFighters) {
            regenerateAfterBattle(entity);
        }
        for (CombatEntity entity : defenderFighters) {
            regenerateAfterBattle(entity);
        }

        List<ExperienceGain> experienceGains = new ArrayList<>();
        awardExperience(attackerFighters, defenderCharacterLost, experienceGains);
        awardExperience(defenderFighters, attackerCharacterLost, experienceGains);
        List<CasualtyInfo> casualtyDetails = casualties.stream().map(this::toCasualtyInfo).toList();

        sector.recalculateMilitaryPower();

        int capturedBuildings = 0;
        boolean defenderHeadquartersCaptured = false;
        if (battle.getWinner() != null && battle.getWinner().getId().equals(attacker.getId())) {
            CaptureReport capture = captureBuildings(
                    sector, attacker, List.of(defender.getId()), board.getCurrentTurn());
            capturedBuildings = capture.capturedBuildings();
            defenderHeadquartersCaptured = capture.headquartersCaptured();
        }

        List<CombatEntity> attackerCasualties = casualties.stream()
                .filter(u -> attacker.getId().equals(u.getPlayerId()))
                .collect(Collectors.toList());
        List<CombatEntity> defenderCasualties = casualties.stream()
                .filter(u -> defender.getId().equals(u.getPlayerId()))
                .collect(Collectors.toList());
        List<CombatEntity> attackerInjured = attackerFighters.stream()
                .filter(CombatEntity::isInjured)
                .collect(Collectors.toList());
        List<CombatEntity> defenderInjured = defenderFighters.stream()
                .filter(CombatEntity::isInjured)
                .collect(Collectors.toList());

        logger.info("[Combat secteur {}] {} vs {}: {} pertes attaquant, {} pertes défenseur, {} bâtiments capturés, vainqueur: {}",
                sectorNumber, attacker.getName(), defender.getName(),
                attackerCasualties.size(), defenderCasualties.size(), capturedBuildings,
                battle.getWinner() != null ? battle.getWinner().getName() : "aucun");

        appendResultLog(battle,
                List.of(new ResultCamp(attacker, attackerFighters,
                                filteredDetails(casualtyDetails, attacker.getId()),
                                filteredGains(experienceGains, attacker.getId())),
                        new ResultCamp(defender, defenderFighters,
                                filteredDetails(casualtyDetails, defender.getId()),
                                filteredGains(experienceGains, defender.getId()))),
                capturedBuildings, defenderHeadquartersCaptured);

        return new SectorBattleResult(true, "Bataille terminée au secteur " + sectorNumber,
                attackerCasualties, defenderCasualties, attackerInjured, defenderInjured, battle.getWinner(),
                capturedBuildings, attackerCharacterLost, defenderCharacterLost, defenderHeadquartersCaptured,
                casualtyDetails, experienceGains, List.copyOf(battle.getLog()));
    }

    /**
     * Impasse mexicaine : chaque camp frappe le suivant (cercle fourni par la détection).
     * L'unique camp avec des combattants hors bâtiments l'emporte et capture les bâtiments des autres.
     */
    public StandoffBattleResult simulateSectorStandoff(List<Player> participants, Board board, int sectorNumber) {
        if (participants == null || participants.size() < 3 || board == null) {
            return failedStandoff("Paramètres invalides");
        }
        Sector sector = board.getSector(sectorNumber);
        if (sector == null) {
            return failedStandoff("Secteur inexistant : " + sectorNumber);
        }

        List<Player> activePlayers = new ArrayList<>();
        List<List<CombatEntity>> camps = new ArrayList<>();
        for (Player player : participants) {
            playerStatsService.updateCombatStats(player, board);
            List<CombatEntity> fighters = collectBattleParticipants(sector, player.getId());
            if (!fighters.isEmpty()) {
                activePlayers.add(player);
                camps.add(fighters);
            }
        }
        if (activePlayers.size() < 3) {
            return failedStandoff("Impasse incomplète au secteur " + sectorNumber
                    + " (" + activePlayers.size() + " camp(s) combattant(s))");
        }

        Set<Long> beforeIds = new HashSet<>();
        camps.forEach(camp -> camp.forEach(e -> beforeIds.add(e.getId())));

        Battle battle = new Battle();
        battle.classicStandoffConfiguration(activePlayers, camps);

        Set<Long> survivorIds = new HashSet<>();
        camps.forEach(camp -> camp.forEach(e -> survivorIds.add(e.getId())));

        Map<Long, Player> playersById = activePlayers.stream()
                .collect(Collectors.toMap(Player::getId, player -> player, (a, b) -> a));
        Map<Long, Boolean> characterLost = new HashMap<>();
        List<CombatEntity> casualties =
                removeCasualties(sector, beforeIds, survivorIds, playersById, characterLost);

        for (List<CombatEntity> camp : camps) {
            for (CombatEntity entity : camp) {
                regenerateAfterBattle(entity);
            }
        }

        List<ExperienceGain> experienceGains = new ArrayList<>();
        for (int i = 0; i < activePlayers.size(); i++) {
            Long struckPlayerId = activePlayers.get((i + 1) % activePlayers.size()).getId();
            awardExperience(camps.get(i), characterLost.getOrDefault(struckPlayerId, false), experienceGains);
        }
        List<CasualtyInfo> casualtyDetails = casualties.stream().map(this::toCasualtyInfo).toList();

        sector.recalculateMilitaryPower();

        Player winner = battle.getWinner();
        int capturedBuildings = 0;
        CaptureReport capture = null;
        if (winner != null) {
            List<Long> losers = activePlayers.stream()
                    .map(Player::getId)
                    .filter(id -> !id.equals(winner.getId()))
                    .toList();
            capture = captureBuildings(sector, winner, losers, board.getCurrentTurn());
            capturedBuildings = capture.capturedBuildings();
        }

        List<StandoffBattleResult.PlayerOutcome> outcomes = new ArrayList<>();
        for (int i = 0; i < activePlayers.size(); i++) {
            Player player = activePlayers.get(i);
            List<CombatEntity> camp = camps.get(i);
            outcomes.add(new StandoffBattleResult.PlayerOutcome(
                    player.getId(),
                    (int) casualties.stream().filter(e -> player.getId().equals(e.getPlayerId())).count(),
                    (int) camp.stream().filter(CombatEntity::isInjured).count(),
                    characterLost.getOrDefault(player.getId(), false),
                    camp.stream().noneMatch(e -> e.getEntityCategory() != EntityCategory.BUILDING)));
        }

        logger.info("[Impasse secteur {}] {} — vainqueur: {}", sectorNumber,
                outcomes.stream().map(o -> o.playerId() + ":" + o.casualties() + " pertes").toList(),
                winner != null ? winner.getName() : "aucun");

        List<ResultCamp> resultCamps = new ArrayList<>();
        for (int i = 0; i < activePlayers.size(); i++) {
            Player player = activePlayers.get(i);
            resultCamps.add(new ResultCamp(player, camps.get(i),
                    filteredDetails(casualtyDetails, player.getId()),
                    filteredGains(experienceGains, player.getId())));
        }
        appendResultLog(battle, resultCamps, capturedBuildings,
                capture != null && capture.headquartersCaptured());

        return new StandoffBattleResult(true, "Impasse mexicaine terminée au secteur " + sectorNumber,
                winner, capturedBuildings, outcomes, casualtyDetails, experienceGains, List.copyOf(battle.getLog()));
    }

    /** Retire du secteur les entités engagées absentes des survivants, en gérant les FK et les flags par joueur. */
    private List<CombatEntity> removeCasualties(Sector sector, Set<Long> beforeIds, Set<Long> survivorIds,
                                                Map<Long, Player> playersById,
                                                Map<Long, Boolean> characterLost) {
        List<CombatEntity> casualties = new ArrayList<>();

        for (Unit unit : new ArrayList<>(sector.getUnits())) {
            if (isCasualty(unit, beforeIds, survivorIds)) {
                // Sector.army sans orphanRemoval (docs/jpa-pitfalls.md §1, V6) : em.remove cascade vers Unit.unitEquipments (cascade=ALL) → DELETE propre. Retrait mémoire pour cohérence de sector.getUnits().
                em.remove(unit);
                sector.getUnits().remove(unit);
                casualties.add(unit);
            }
        }

        for (GameCharacter character : new ArrayList<>(sector.getCharacters())) {
            if (isCasualty(character, beforeIds, survivorIds)) {
                detachCharacterFromVehicles(character);
                Player owner = playersById.get(character.getPlayerId());
                // players.character_id doit passer à NULL avant le DELETE, sinon la FK bloque.
                if (owner != null && owner.getCharacter() != null
                        && character.getId().equals(owner.getCharacter().getId())) {
                    owner.setCharacter(null); // @OneToOne orphanRemoval → DELETE au flush
                } else {
                    em.remove(character); // personnage non porté par Player.character
                }
                sector.getCharacters().remove(character);
                casualties.add(character);
                if (owner != null) {
                    characterLost.put(owner.getId(), true);
                }
            }
        }

        for (Building building : new ArrayList<>(sector.getBuildings())) {
            if (isCasualty(building, beforeIds, survivorIds)) {
                // Jamais DELETE (orphanRemoval Player.buildings — docs/jpa-pitfalls.md) : marqué détruit.
                if (building instanceof Headquarters headquarters) {
                    headquarters.destroy();
                } else {
                    building.setDestroyed(true);
                    building.recalculateBaseStats();
                }
                casualties.add(building);
            }
        }

        return casualties;
    }

    /** Capture les bâtiments des joueurs perdants ; le QG est capturé même détruit (arbitrage MJ). */
    private CaptureReport captureBuildings(Sector sector, Player winner, Collection<Long> loserPlayerIds, int turn) {
        int capturedBuildings = 0;
        boolean headquartersCaptured = false;
        for (Building building : sector.getBuildings()) {
            if (building.isCaptured() || !loserPlayerIds.contains(building.getPlayerId())) {
                continue;
            }
            switch (building.getBuildingType()) {
                case HEADQUARTERS -> {
                    building.onCapture(winner.getId(), turn);
                    headquartersCaptured = true;
                    capturedBuildings++;
                }
                case BANK -> {
                    if (!building.isDestroyed()) {
                        buildingService.captureBank(building.getId(), winner.getId(), turn);
                        capturedBuildings++;
                    }
                }
                case WEAPON_CACHE -> {
                    if (!building.isDestroyed()) {
                        buildingService.captureWeaponCache(building.getId(), winner.getId(), turn);
                        capturedBuildings++;
                    }
                }
            }
        }
        return new CaptureReport(capturedBuildings, headquartersCaptured);
    }

    private record CaptureReport(int capturedBuildings, boolean headquartersCaptured) {
    }

    /** Filtrage par joueur ; véhicules exclus (listes unités/personnages/bâtiments uniquement). */
    private List<CombatEntity> collectBattleParticipants(Sector sector, Long playerId) {
        List<CombatEntity> fighters = new ArrayList<>();

        sector.getCharacters().stream()
                .filter(c -> playerId.equals(c.getPlayerId()))
                .filter(c -> !c.isDestroyed())
                .forEach(fighters::add);

        List<Building> buildings = sector.getBuildings().stream()
                .filter(b -> playerId.equals(b.getPlayerId()))
                .filter(b -> !b.isDestroyed() && !b.isCaptured())
                .toList();
        for (BuildingType type : List.of(BuildingType.HEADQUARTERS, BuildingType.WEAPON_CACHE, BuildingType.BANK)) {
            buildings.stream().filter(b -> b.getBuildingType() == type).forEach(fighters::add);
        }

        sector.getUnits().stream()
                .filter(u -> playerId.equals(u.getPlayerId()))
                .filter(u -> !u.isDestroyed())
                .forEach(fighters::add);

        return fighters;
    }

    private boolean isCasualty(CombatEntity entity, Set<Long> beforeIds, Set<Long> survivorIds) {
        return beforeIds.contains(entity.getId()) && !survivorIds.contains(entity.getId());
    }

    private void regenerateAfterBattle(CombatEntity entity) {
        if (entity.getEntityCategory() == EntityCategory.BUILDING) {
            entity.recalculateBaseStats();
        } else if (entity.getEntityCategory() == EntityCategory.CHARACTER) {
            ((GameCharacter) entity).regenerateAfterBattle();
        }
    }

    /** +1 Exp pour la participation, +1 si le camp adverse a perdu son personnage. */
    private void awardExperience(List<CombatEntity> survivors, boolean enemyCharacterLost,
                                 List<ExperienceGain> gains) {
        double amount = enemyCharacterLost ? 2 : 1;
        for (CombatEntity entity : survivors) {
            if (entity instanceof Unit unit) {
                UnitType typeBefore = unit.getType();
                double expBefore = unit.getExperience();
                unit.gainExperience(amount);
                gains.add(new ExperienceGain(unit.getPlayerId(), unit.getId(), unit.getNumber(), typeBefore,
                        expBefore, amount, unit.getType(), unit.getExperience()));
            }
        }
    }

    private CasualtyInfo toCasualtyInfo(CombatEntity entity) {
        if (entity instanceof Unit unit) {
            return new CasualtyInfo(entity.getPlayerId(), unit.getDisplayName(),
                    EntityCategory.INFANTRY.name(), unit.getType(), unit.getNumber(), unit.getExperience());
        }
        return new CasualtyInfo(entity.getPlayerId(), entity.getDisplayName(),
                entity.getEntityCategory().name(), null, null, null);
    }

    private void appendResultLog(Battle battle, List<ResultCamp> camps, int capturedBuildings,
                                 boolean headquartersCaptured) {
        appendResult(battle, BattleLogEntry.INFO, "=== Bilan du combat ===");
        for (ResultCamp camp : camps) {
            String name = camp.player().getName();
            if (camp.survivors().isEmpty()) {
                appendResult(battle, BattleLogEntry.LOSS, name + " : aucun survivant");
            } else {
                appendResult(battle, BattleLogEntry.INFO,
                        "Survivants " + name + " (" + camp.survivors().size() + ") :");
                for (CombatEntity survivor : camp.survivors()) {
                    appendResult(battle, BattleLogEntry.INFO, "  " + survivor);
                }
            }
            if (!camp.casualties().isEmpty()) {
                String labels = camp.casualties().stream()
                        .map(casualty -> casualty.label() + (casualty.experience() != null
                                ? " (" + formatExperience(casualty.experience()) + " Exp)" : ""))
                        .collect(Collectors.joining(", "));
                appendResult(battle, BattleLogEntry.LOSS, "Pertes " + name + " : " + labels);
            }
            if (camp.casualties().stream()
                    .anyMatch(casualty -> EntityCategory.CHARACTER.name().equals(casualty.category()))) {
                appendResult(battle, BattleLogEntry.LOSS, "Personnage perdu : " + name);
            }
            for (ExperienceGain gain : camp.gains()) {
                appendResult(battle, BattleLogEntry.GAIN,
                        "Expérience " + name + " : " + gain.typeBefore() + " n°" + gain.unitNumber()
                                + " " + formatExperience(gain.experienceBefore()) + " Exp + "
                                + formatExperience(gain.gained()) + " → " + gain.typeAfter()
                                + " (" + formatExperience(gain.experienceAfter()) + " Exp)");
            }
        }
        if (capturedBuildings > 0) {
            appendResult(battle, BattleLogEntry.INFO, capturedBuildings + " bâtiment(s) capturé(s)"
                    + (headquartersCaptured ? " — quartier général capturé" : ""));
        }
    }

    private void appendResult(Battle battle, String outcome, String message) {
        battle.appendLog("Résultat", outcome, message);
        logger.info("[Résultat] {}", message);
    }

    private static List<CasualtyInfo> filteredDetails(List<CasualtyInfo> details, Long playerId) {
        return details.stream().filter(detail -> playerId.equals(detail.playerId())).toList();
    }

    private static List<ExperienceGain> filteredGains(List<ExperienceGain> gains, Long playerId) {
        return gains.stream().filter(gain -> playerId.equals(gain.playerId())).toList();
    }

    private static String formatExperience(double value) {
        return Math.abs(value - Math.rint(value)) < 0.005
                ? String.valueOf((long) Math.rint(value))
                : String.format("%.1f", value);
    }

    private record ResultCamp(Player player, List<CombatEntity> survivors,
                              List<CasualtyInfo> casualties, List<ExperienceGain> gains) {
    }

    /** Détacher le personnage de tout véhicule (pilote ou passager) — sinon les FK pilot_id/vehicle_id bloquent le DELETE. */
    private void detachCharacterFromVehicles(GameCharacter character) {
        List<Vehicle> carrying = em.createQuery(
                        "select distinct v from Vehicle v left join v.passengers p " +
                                "where v.pilot.id = :characterId or p.id = :characterId", Vehicle.class)
                .setParameter("characterId", character.getId())
                .getResultList();
        for (Vehicle vehicle : carrying) {
            if (vehicle.getPilot() != null && character.getId().equals(vehicle.getPilot().getId())) {
                vehicle.removePilot();
            }
            vehicle.disembark(character);
        }
    }

    private SectorBattleResult failedResult(String message) {
        return new SectorBattleResult(false, message, List.of(), List.of(), List.of(), List.of(), null,
                0, false, false, false, List.of(), List.of(), List.of());
    }

    private StandoffBattleResult failedStandoff(String message) {
        return new StandoffBattleResult(false, message, null, 0, List.of(), List.of(), List.of(), List.of());
    }

    public record SectorBattleResult(boolean success, String message, List<CombatEntity> attackerCasualties,
                                     List<CombatEntity> defenderCasualties, List<CombatEntity> attackerInjured,
                                     List<CombatEntity> defenderInjured, Player winner, int capturedBuildings,
                                     boolean attackerCharacterLost, boolean defenderCharacterLost,
                                     boolean defenderHeadquartersCaptured,
                                     List<CasualtyInfo> casualtyDetails, List<ExperienceGain> experienceGains,
                                     List<BattleLogEntry> battleLog) {
    }

    public record StandoffBattleResult(boolean success, String message, Player winner, int capturedBuildings,
                                       List<PlayerOutcome> outcomes, List<CasualtyInfo> casualtyDetails,
                                       List<ExperienceGain> experienceGains, List<BattleLogEntry> battleLog) {

        public record PlayerOutcome(Long playerId, int casualties, int injured, boolean characterLost,
                                    boolean eliminated) {
        }
    }

    public record CasualtyInfo(Long playerId, String label, String category, UnitType unitType,
                               Integer unitNumber, Double experience) {
    }

    public record ExperienceGain(Long playerId, Long unitId, int unitNumber, UnitType typeBefore,
                                 double experienceBefore, double gained, UnitType typeAfter,
                                 double experienceAfter) {
    }
}
