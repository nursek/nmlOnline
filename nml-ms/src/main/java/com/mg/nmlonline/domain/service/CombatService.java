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
    private AllianceGraph allianceGraph;

    @Autowired
    private EntityManager em;

    public Optional<Sector> findSectorWithArmy(Player player, Board board) {
        if (player == null || board == null) {
            return Optional.empty();
        }

        List<Sector> sectorsWithArmy = playerStatsService.getSectorsWithCombatEntities(player, board);
        return sectorsWithArmy.stream().findFirst();
    }

    /**
     * Combat sur un secteur : chaque camp (1 ou 2 alliés fusionnés) engage unités + personnages + bâtiments
     * co-localisés (véhicules exclus). Les bâtiments du camp perdant passent au joueur dominant du camp vainqueur.
     */
    public SectorBattleResult simulateSectorBattle(List<Player> attackerCamp, List<Player> defenderCamp,
                                                   Board board, int sectorNumber) {
        if (attackerCamp == null || attackerCamp.isEmpty() || defenderCamp == null || defenderCamp.isEmpty()
                || board == null) {
            return failedResult("Paramètres invalides");
        }
        Sector sector = board.getSector(sectorNumber);
        if (sector == null) {
            return failedResult("Secteur inexistant : " + sectorNumber);
        }

        attackerCamp.forEach(player -> playerStatsService.updateCombatStats(player, board));
        defenderCamp.forEach(player -> playerStatsService.updateCombatStats(player, board));

        // Ordre inverse de la mort (getLast()) : unités → Banque → Cache → QG → personnage en dernier.
        List<CombatEntity> attackerFighters = collectBattleParticipants(sector, idsOf(attackerCamp));
        List<CombatEntity> defenderFighters = collectBattleParticipants(sector, idsOf(defenderCamp));

        if (attackerFighters.isEmpty() || defenderFighters.isEmpty()) {
            return failedResult("Aucune entité combattante au secteur " + sectorNumber
                    + " (attaquant=" + attackerFighters.size() + ", défenseur=" + defenderFighters.size() + ")");
        }

        Set<Long> beforeIds = new HashSet<>();
        attackerFighters.forEach(u -> beforeIds.add(u.getId()));
        defenderFighters.forEach(u -> beforeIds.add(u.getId()));

        int turn = board.getCurrentTurn();
        Battle battle = new Battle();
        battle.setAttackerBonusPercent(campBonusPercent(turn, attackerCamp, defenderCamp));
        battle.setDefenderBonusPercent(campBonusPercent(turn, defenderCamp, attackerCamp));
        battle.classicCombatConfiguration(attackerCamp.getFirst(), defenderCamp.getFirst(),
                attackerFighters, defenderFighters);

        Set<Long> survivorIds = new HashSet<>();
        attackerFighters.forEach(u -> survivorIds.add(u.getId()));
        defenderFighters.forEach(u -> survivorIds.add(u.getId()));

        List<CombatEntity> casualties = new ArrayList<>();
        Map<Long, Player> playersById = new HashMap<>();
        attackerCamp.forEach(player -> playersById.put(player.getId(), player));
        defenderCamp.forEach(player -> playersById.put(player.getId(), player));
        Map<Long, Boolean> characterLost = new HashMap<>();
        casualties.addAll(removeCasualties(sector, beforeIds, survivorIds, playersById, characterLost));

        for (CombatEntity entity : attackerFighters) {
            regenerateAfterBattle(entity);
        }
        for (CombatEntity entity : defenderFighters) {
            regenerateAfterBattle(entity);
        }

        boolean attackerCharacterLost = attackerCamp.stream()
                .anyMatch(player -> characterLost.getOrDefault(player.getId(), false));
        boolean defenderCharacterLost = defenderCamp.stream()
                .anyMatch(player -> characterLost.getOrDefault(player.getId(), false));

        List<ExperienceGain> experienceGains = new ArrayList<>();
        awardExperience(attackerFighters, defenderCharacterLost, experienceGains);
        awardExperience(defenderFighters, attackerCharacterLost, experienceGains);
        List<CasualtyInfo> casualtyDetails = casualties.stream().map(this::toCasualtyInfo).toList();

        sector.recalculateMilitaryPower();

        int capturedBuildings = 0;
        boolean defenderHeadquartersCaptured = false;
        Player winner = battle.getWinner();
        if (winner != null && idsOf(attackerCamp).contains(winner.getId())) {
            List<CombatEntity> allSurvivors = new ArrayList<>(attackerFighters);
            allSurvivors.addAll(defenderFighters);
            Player capturer = dominantPlayer(attackerCamp, allSurvivors);
            CaptureReport capture = captureBuildings(sector, capturer, idsOf(defenderCamp), turn);
            capturedBuildings = capture.capturedBuildings();
            defenderHeadquartersCaptured = capture.headquartersCaptured();
        }

        Set<Long> attackerIds = idsOf(attackerCamp);
        Set<Long> defenderIds = idsOf(defenderCamp);
        List<CombatEntity> attackerCasualties = casualties.stream()
                .filter(u -> attackerIds.contains(u.getPlayerId()))
                .collect(Collectors.toList());
        List<CombatEntity> defenderCasualties = casualties.stream()
                .filter(u -> defenderIds.contains(u.getPlayerId()))
                .collect(Collectors.toList());
        List<CombatEntity> attackerInjured = attackerFighters.stream()
                .filter(CombatEntity::isInjured)
                .collect(Collectors.toList());
        List<CombatEntity> defenderInjured = defenderFighters.stream()
                .filter(CombatEntity::isInjured)
                .collect(Collectors.toList());

        logger.info("[Combat secteur {}] {} vs {}: {} pertes attaquant, {} pertes défenseur, {} bâtiments capturés, vainqueur: {}",
                sectorNumber, campLabel(attackerCamp), campLabel(defenderCamp),
                attackerCasualties.size(), defenderCasualties.size(), capturedBuildings,
                winner != null ? winner.getName() : "aucun");

        appendResultLog(battle,
                List.of(new ResultCamp(campLabel(attackerCamp), attackerFighters,
                                filteredDetails(casualtyDetails, attackerIds),
                                filteredGains(experienceGains, attackerIds)),
                        new ResultCamp(campLabel(defenderCamp), defenderFighters,
                                filteredDetails(casualtyDetails, defenderIds),
                                filteredGains(experienceGains, defenderIds))),
                capturedBuildings, defenderHeadquartersCaptured);

        return new SectorBattleResult(true, "Bataille terminée au secteur " + sectorNumber,
                attackerCasualties, defenderCasualties, attackerInjured, defenderInjured, winner,
                capturedBuildings, attackerCharacterLost, defenderCharacterLost, defenderHeadquartersCaptured,
                casualtyDetails, experienceGains, List.copyOf(battle.getLog()));
    }

    /** L'unique camp avec des combattants hors bâtiments l'emporte et capture les bâtiments des autres. */
    public StandoffBattleResult simulateSectorStandoff(List<List<Player>> playerCamps, Board board, int sectorNumber) {
        if (playerCamps == null || playerCamps.size() < 3 || board == null) {
            return failedStandoff("Paramètres invalides");
        }
        Sector sector = board.getSector(sectorNumber);
        if (sector == null) {
            return failedStandoff("Secteur inexistant : " + sectorNumber);
        }

        List<List<Player>> activeCamps = new ArrayList<>();
        List<List<CombatEntity>> camps = new ArrayList<>();
        for (List<Player> camp : playerCamps) {
            camp.forEach(player -> playerStatsService.updateCombatStats(player, board));
            List<CombatEntity> fighters = collectBattleParticipants(sector, idsOf(camp));
            if (!fighters.isEmpty()) {
                activeCamps.add(camp);
                camps.add(fighters);
            }
        }
        if (activeCamps.size() < 3) {
            return failedStandoff("Impasse incomplète au secteur " + sectorNumber
                    + " (" + activeCamps.size() + " camp(s) combattant(s))");
        }

        Set<Long> beforeIds = new HashSet<>();
        camps.forEach(camp -> camp.forEach(e -> beforeIds.add(e.getId())));

        int turn = board.getCurrentTurn();
        List<List<Long>> campIds = activeCamps.stream()
                .map(camp -> camp.stream().map(Player::getId).toList())
                .toList();
        int[] targets = AllianceGraph.campTargets(campIds, allianceGraph.activeAdjacency());
        double[] bonuses = new double[activeCamps.size()];
        for (int i = 0; i < activeCamps.size(); i++) {
            if (targets[i] >= 0) {
                bonuses[i] = campBonusPercent(turn, activeCamps.get(i), activeCamps.get(targets[i]));
            }
        }

        Battle battle = new Battle();
        battle.classicStandoffConfiguration(activeCamps.stream().map(List::getFirst).toList(), camps, targets, bonuses);

        Set<Long> survivorIds = new HashSet<>();
        camps.forEach(camp -> camp.forEach(e -> survivorIds.add(e.getId())));

        Map<Long, Player> playersById = activeCamps.stream()
                .flatMap(Collection::stream)
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
        for (int i = 0; i < activeCamps.size(); i++) {
            int struck = targets[i];
            boolean struckCharacterLost = struck >= 0 && activeCamps.get(struck).stream()
                    .anyMatch(player -> characterLost.getOrDefault(player.getId(), false));
            awardExperience(camps.get(i), struckCharacterLost, experienceGains);
        }
        List<CasualtyInfo> casualtyDetails = casualties.stream().map(this::toCasualtyInfo).toList();

        sector.recalculateMilitaryPower();

        Player winner = battle.getWinner();
        int capturedBuildings = 0;
        CaptureReport capture = null;
        if (winner != null) {
            List<Player> winningCamp = activeCamps.stream()
                    .filter(camp -> camp.stream().anyMatch(player -> player.getId().equals(winner.getId())))
                    .findFirst()
                    .orElse(List.of(winner));
            List<CombatEntity> allSurvivors = camps.stream().flatMap(Collection::stream).toList();
            Player capturer = dominantPlayer(winningCamp, allSurvivors);
            List<Long> losers = activeCamps.stream()
                    .flatMap(Collection::stream)
                    .map(Player::getId)
                    .filter(id -> !winningCamp.stream().anyMatch(player -> player.getId().equals(id)))
                    .toList();
            capture = captureBuildings(sector, capturer, losers, turn);
            capturedBuildings = capture.capturedBuildings();
        }

        List<StandoffBattleResult.PlayerOutcome> outcomes = new ArrayList<>();
        for (int i = 0; i < activeCamps.size(); i++) {
            List<CombatEntity> camp = camps.get(i);
            for (Player player : activeCamps.get(i)) {
                List<CombatEntity> playerFighters = camp.stream()
                        .filter(e -> player.getId().equals(e.getPlayerId()))
                        .toList();
                outcomes.add(new StandoffBattleResult.PlayerOutcome(
                        player.getId(),
                        (int) casualties.stream().filter(e -> player.getId().equals(e.getPlayerId())).count(),
                        (int) playerFighters.stream().filter(CombatEntity::isInjured).count(),
                        characterLost.getOrDefault(player.getId(), false),
                        playerFighters.stream().noneMatch(e -> e.getEntityCategory() != EntityCategory.BUILDING)));
            }
        }

        logger.info("[Impasse secteur {}] {} — vainqueur: {}", sectorNumber,
                outcomes.stream().map(o -> o.playerId() + ":" + o.casualties() + " pertes").toList(),
                winner != null ? winner.getName() : "aucun");

        List<ResultCamp> resultCamps = new ArrayList<>();
        for (int i = 0; i < activeCamps.size(); i++) {
            Set<Long> campPlayerIds = new HashSet<>(campIds.get(i));
            resultCamps.add(new ResultCamp(campLabel(activeCamps.get(i)), camps.get(i),
                    filteredDetails(casualtyDetails, campPlayerIds),
                    filteredGains(experienceGains, campPlayerIds)));
        }
        appendResultLog(battle, resultCamps, capturedBuildings,
                capture != null && capture.headquartersCaptured());

        return new StandoffBattleResult(true, "Impasse mexicaine terminée au secteur " + sectorNumber,
                winner, capturedBuildings, outcomes, casualtyDetails, experienceGains, List.copyOf(battle.getLog()));
    }

    /** Bonus de trahison : actif si un membre du camp est le traître et un membre du camp adverse sa victime. */
    private double campBonusPercent(int turn, List<Player> strikers, List<Player> targets) {
        double best = 0;
        for (Player striker : strikers) {
            for (Player target : targets) {
                best = Math.max(best, allianceGraph.betrayalBonusPercent(turn, striker.getId(), target.getId()));
            }
        }
        return best;
    }

    /** Plus de points de combat restants (attack + pdf + pdc hors bâtiments) ; égalité → premier de l'ordre du camp. */
    private Player dominantPlayer(List<Player> camp, List<CombatEntity> survivors) {
        Player best = camp.getFirst();
        double bestScore = -1;
        for (Player player : camp) {
            double score = survivors.stream()
                    .filter(e -> player.getId().equals(e.getPlayerId()))
                    .filter(e -> e.getEntityCategory() != EntityCategory.BUILDING)
                    .mapToDouble(e -> e.getAttack() + e.getPdf() + e.getPdc())
                    .sum();
            if (score > bestScore) {
                bestScore = score;
                best = player;
            }
        }
        return best;
    }

    private static String campLabel(List<Player> camp) {
        return camp.stream().map(Player::getName).collect(Collectors.joining(" + "));
    }

    private static Set<Long> idsOf(Collection<Player> camp) {
        return camp.stream().map(Player::getId).collect(Collectors.toSet());
    }

    /** Retire du secteur les entités engagées absentes des survivants, en gérant les FK et les flags par joueur. */
    private List<CombatEntity> removeCasualties(Sector sector, Set<Long> beforeIds, Set<Long> survivorIds,
                                                Map<Long, Player> playersById,
                                                Map<Long, Boolean> characterLost) {
        List<CombatEntity> casualties = new ArrayList<>();

        for (Unit unit : new ArrayList<>(sector.getUnits())) {
            if (isCasualty(unit, beforeIds, survivorIds)) {
                detachPilotFromVehicles(unit);
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

    /** Filtrage par joueurs ; véhicules exclus (listes unités/personnages/bâtiments uniquement). */
    private List<CombatEntity> collectBattleParticipants(Sector sector, Collection<Long> playerIds) {
        List<CombatEntity> fighters = new ArrayList<>();

        sector.getCharacters().stream()
                .filter(c -> playerIds.contains(c.getPlayerId()))
                .filter(c -> !c.isDestroyed())
                .forEach(fighters::add);

        List<Building> buildings = sector.getBuildings().stream()
                .filter(b -> playerIds.contains(b.getPlayerId()))
                .filter(b -> !b.isDestroyed() && !b.isCaptured())
                .toList();
        for (BuildingType type : List.of(BuildingType.HEADQUARTERS, BuildingType.WEAPON_CACHE, BuildingType.BANK)) {
            buildings.stream().filter(b -> b.getBuildingType() == type).forEach(fighters::add);
        }

        sector.getUnits().stream()
                .filter(u -> playerIds.contains(u.getPlayerId()))
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
            String name = camp.label();
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

    private static List<CasualtyInfo> filteredDetails(List<CasualtyInfo> details, Collection<Long> playerIds) {
        return details.stream().filter(detail -> playerIds.contains(detail.playerId())).toList();
    }

    private static List<ExperienceGain> filteredGains(List<ExperienceGain> gains, Collection<Long> playerIds) {
        return gains.stream().filter(gain -> playerIds.contains(gain.playerId())).toList();
    }

    private static String formatExperience(double value) {
        return Math.abs(value - Math.rint(value)) < 0.005
                ? String.valueOf((long) Math.rint(value))
                : String.format("%.1f", value);
    }

    private record ResultCamp(String label, List<CombatEntity> survivors,
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

    // pilot_id est porté par la ligne du véhicule : un pilote supprimé doit être détaché avant le DELETE.
    private void detachPilotFromVehicles(CombatEntity entity) {
        List<Vehicle> piloted = em.createQuery(
                        "select v from Vehicle v where v.pilot.id = :entityId", Vehicle.class)
                .setParameter("entityId", entity.getId())
                .getResultList();
        piloted.forEach(Vehicle::removePilot);
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
