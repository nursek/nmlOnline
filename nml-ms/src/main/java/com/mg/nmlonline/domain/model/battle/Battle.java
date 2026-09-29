package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.BuildingType;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.EntityCategory;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class Battle {

    private static final Logger logger = LoggerFactory.getLogger(Battle.class);

    private int sectorId;

    private List<Player> defenders = new ArrayList<>();
    private List<Player> attackers = new ArrayList<>();

    // Un combat peut avoir un vainqueur (qui prend ou garde le secteur) — optionnel.
    private Player winner;

    private Random random;

    private final List<BattleLogEntry> log = new ArrayList<>();

    private String currentPhase = "Combat";

    /** Bonus de trahison (en %) du camp, actif uniquement le tour de la trahison. */
    private double attackerBonusPercent;
    private double defenderBonusPercent;

    private final Map<CombatEntity, Tracker> trackers = new IdentityHashMap<>();
    private final Set<CombatEntity> embarked = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<CombatEntity, Vehicle> carriedBy = new IdentityHashMap<>();

    public Battle() {
        this.random = new Random();
    }

    public record Participation(boolean fired, boolean damaged, boolean dodged, boolean destroyedTarget,
                                double damageDealt, double damageTaken) {
        public static final Participation NONE = new Participation(false, false, false, false, 0, 0);
    }

    private static final class Tracker {
        private boolean fired;
        private boolean damaged;
        private boolean dodged;
        private boolean destroyedTarget;
        private double damageDealt;
        private double damageTaken;

        private Participation snapshot() {
            return new Participation(fired, damaged, dodged, destroyedTarget, damageDealt, damageTaken);
        }
    }

    public Map<CombatEntity, Participation> getParticipation() {
        Map<CombatEntity, Participation> result = new IdentityHashMap<>();
        trackers.forEach((entity, tracker) -> result.put(entity, tracker.snapshot()));
        return result;
    }

    private int rand() {
        return random.nextInt(100) + 1;
    }

    private double attackerMultiplier() {
        return 1 + attackerBonusPercent / 100.0;
    }

    private double defenderMultiplier() {
        return 1 + defenderBonusPercent / 100.0;
    }

    private Tracker tracker(CombatEntity entity) {
        return trackers.computeIfAbsent(entity, k -> new Tracker());
    }

    private void recordEvent(String outcome, String message) {
        log.add(new BattleLogEntry(currentPhase, outcome, message));
    }

    public void appendLog(String phase, String outcome, String message) {
        log.add(new BattleLogEntry(phase, outcome, message));
    }

    private void recordInitialState(Player player, List<CombatEntity> units) {
        if (units.isEmpty()) {
            return;
        }
        recordEvent(BattleLogEntry.INFO, player.getName() + " engage " + units.size() + " entité(s) :");
        for (CombatEntity entity : units) {
            recordEvent(BattleLogEntry.INFO, "  " + entity);
        }
    }

    private static void logUnit(CombatEntity unit) {
        logger.info("      - {}", unit);
    }

    private void handleInjuredUnit(CombatEntity unit) {
        unit.setInjured(true);
        unit.recalculateBaseStats();
    }

    /** Séquence : PDF → secondaires → PDC → ATK (unités seules) → QG → personnages. */
    public void classicCombatConfiguration(Player attacker, Player defender, List<CombatEntity> attackerUnits, List<CombatEntity> defenderUnits) {
        if (attackerUnits == null) attackerUnits = new ArrayList<>();
        if (defenderUnits == null) defenderUnits = new ArrayList<>();

        printUnitsIndented(defenderUnits, "Défenseurs en présence");
        printUnitsIndented(attackerUnits, "Attaquants en présence");

        logger.info("\n=== Début du combat entre {} et {} ===", attacker.getName(), defender.getName());

        this.currentPhase = "État initial";
        recordInitialState(attacker, attackerUnits);
        recordInitialState(defender, defenderUnits);
        this.currentPhase = "Combat";
        recordEvent(BattleLogEntry.INFO, "Début du combat : " + attacker.getName() + " attaque " + defender.getName());

        collectEmbarked(attackerUnits);
        collectEmbarked(defenderUnits);

        duelPhase("PDF", "PDF", attackerUnits, defenderUnits, e -> true, attacker, defender);
        if (abortIfWiped(attacker, defender, attackerUnits, defenderUnits)) return;

        duelPhase("Bâtiments secondaires", "ATK", attackerUnits, defenderUnits, Battle::isSecondaryBuilding, attacker, defender);
        if (abortIfWiped(attacker, defender, attackerUnits, defenderUnits)) return;

        duelPhase("PDC", "PDC", attackerUnits, defenderUnits, e -> true, attacker, defender);
        if (abortIfWiped(attacker, defender, attackerUnits, defenderUnits)) return;

        duelPhase("ATK", "ATK", attackerUnits, defenderUnits, Battle::isInfantry, attacker, defender);
        if (abortIfWiped(attacker, defender, attackerUnits, defenderUnits)) return;

        duelPhase("Quartier Général", "ATK", attackerUnits, defenderUnits, Battle::isHeadquarters, attacker, defender);
        if (abortIfWiped(attacker, defender, attackerUnits, defenderUnits)) return;

        duelPhase("Personnages", "ATK", attackerUnits, defenderUnits, Battle::isCharacter, attacker, defender);

        printUnitsIndented(defenderUnits, "Défenseurs restants");
        printUnitsIndented(attackerUnits, "Attaquants restants");
        if (attackerUnits.isEmpty() || defenderUnits.isEmpty()) {
            logger.info("\n=== Combat terminé ===");
        } else {
            logger.info("\n=== Combat terminé, il reste des unités dans les deux camps. ===");
        }
        endBattle(attacker, defender, attackerUnits, defenderUnits);
    }

    private boolean abortIfWiped(Player attacker, Player defender,
                                 List<CombatEntity> attackerUnits, List<CombatEntity> defenderUnits) {
        if (!attackerUnits.isEmpty() && !defenderUnits.isEmpty()) {
            return false;
        }
        printUnitsIndented(defenderUnits, "Défenseurs restants");
        printUnitsIndented(attackerUnits, "Attaquants restants");
        logger.info("\n=== Combat terminé ===");
        endBattle(attacker, defender, attackerUnits, defenderUnits);
        return true;
    }

    private void duelPhase(String phase, String damageType, List<CombatEntity> attackerUnits,
                           List<CombatEntity> defenderUnits, Predicate<CombatEntity> extraFilter,
                           Player attacker, Player defender) {
        List<CombatEntity> attackerStrikers = collectStrikers(attackerUnits, damageType, extraFilter);
        List<CombatEntity> defenderStrikers = collectStrikers(defenderUnits, damageType, extraFilter);
        printPhaseHeader(phase);
        resolveStrikes(attackerStrikers, defenderUnits, damageType, attackerMultiplier(),
                attacker.getName(), defender.getName());
        resolveStrikes(defenderStrikers, attackerUnits, damageType, defenderMultiplier(),
                defender.getName(), attacker.getName());
    }

    private void endBattle(Player attacker, Player defender, List<CombatEntity> attackerUnits, List<CombatEntity> defenderUnits) {
        injureDamagedInfantry(attackerUnits);
        injureDamagedInfantry(defenderUnits);
        finishBattle(attacker, defender, attackerUnits, defenderUnits);
    }

    /** Compat sans alliance : chaque camp frappe le suivant du cercle, sans bonus. */
    public void classicStandoffConfiguration(List<Player> players, List<List<CombatEntity>> camps) {
        int[] targets = new int[camps.size()];
        for (int i = 0; i < camps.size(); i++) {
            targets[i] = camps.size() > 1 ? (i + 1) % camps.size() : -1;
        }
        classicStandoffConfiguration(players, camps, targets, new double[camps.size()]);
    }

    /** Le camp i frappe son camp cible (prochain non-allié de la ronde) ; -1 = passe. */
    public void classicStandoffConfiguration(List<Player> players, List<List<CombatEntity>> camps,
                                             int[] targets, double[] bonusPercents) {
        int n = camps.size();
        if (n < 3 || players.size() != n || targets.length != n || bonusPercents.length != n) {
            throw new IllegalArgumentException("Une impasse mexicaine nécessite au moins 3 camps.");
        }
        logger.info("\n=== Impasse mexicaine à {} camps ===", n);
        this.currentPhase = "État initial";
        for (int i = 0; i < n; i++) {
            recordInitialState(players.get(i), camps.get(i));
            collectEmbarked(camps.get(i));
        }
        this.currentPhase = "Combat";
        recordEvent(BattleLogEntry.INFO, "Impasse mexicaine à " + n + " camps : "
                + players.stream().map(Player::getName).collect(Collectors.joining(" → "))
                + " → " + players.getFirst().getName());

        standoffPhase("Impasse — PDF", "PDF", players, camps, targets, bonusPercents, e -> true);
        standoffPhase("Impasse — Bâtiments secondaires", "ATK", players, camps, targets, bonusPercents, Battle::isSecondaryBuilding);
        standoffPhase("Impasse — PDC", "PDC", players, camps, targets, bonusPercents, e -> true);
        standoffPhase("Impasse — ATK", "ATK", players, camps, targets, bonusPercents, Battle::isInfantry);
        standoffPhase("Impasse — QG", "ATK", players, camps, targets, bonusPercents, Battle::isHeadquarters);
        standoffPhase("Impasse — Personnages", "ATK", players, camps, targets, bonusPercents, Battle::isCharacter);

        finishStandoff(players, camps);
    }

    private void standoffPhase(String phase, String damageType, List<Player> players, List<List<CombatEntity>> camps,
                               int[] targets, double[] bonusPercents, Predicate<CombatEntity> extraFilter) {
        // Snapshot de tous les camps avant résolution : un camp éliminé frappe quand même pendant la phase.
        List<List<CombatEntity>> strikers = new ArrayList<>();
        for (List<CombatEntity> camp : camps) {
            strikers.add(collectStrikers(camp, damageType, extraFilter));
        }
        printPhaseHeader(phase);
        for (int i = 0; i < camps.size(); i++) {
            int targetIndex = targets[i];
            if (targetIndex < 0) {
                recordEvent(BattleLogEntry.INFO, players.get(i).getName() + " ne frappe personne (" + damageType + ")");
                continue;
            }
            resolveStrikes(strikers.get(i), camps.get(targetIndex), damageType,
                    1 + bonusPercents[i] / 100.0, players.get(i).getName(), players.get(targetIndex).getName());
        }
    }

    private void resolveStrikes(List<CombatEntity> strikers, List<CombatEntity> enemies, String damageType,
                                double multiplier, String strikerOwner, String targetOwner) {
        for (CombatEntity striker : strikers) {
            if (statValue(striker, damageType) <= 0) {
                continue;
            }
            performStrike(striker, enemies, damageType, multiplier, strikerOwner, targetOwner);
        }
    }

    private void performStrike(CombatEntity striker, List<CombatEntity> enemies, String damageType,
                               double multiplier, String strikerOwner, String targetOwner) {
        double remaining = statValue(striker, damageType) * multiplier;
        String actor = actorLabel(striker, strikerOwner);
        boolean preferVehicle = striker instanceof Unit unit
                && unit.getClassesSet().contains(UnitClass.PILOTE_DESTRUCTEUR);
        Boolean vehicleCategory = chooseCategory(enemies, damageType, preferVehicle);
        List<String> segments = new ArrayList<>();
        boolean firstSegment = true;

        while (remaining > 0 && !enemies.isEmpty()) {
            CombatEntity target = pickTarget(enemies, damageType, vehicleCategory);
            if (target == null) {
                if (firstSegment) {
                    recordEvent(BattleLogEntry.INFO, actor + " n'a aucune cible (" + damageType + ")");
                }
                break;
            }
            firstSegment = false;

            double evasion = target.getEvasion();
            if (evasion > 0) {
                int roll = rand();
                if (roll <= evasion) {
                    tracker(striker).fired = true;
                    tracker(target).dodged = true;
                    segments.add(actor + " tire sur " + targetLabel(targetOwner, target) + " : esquive (jet "
                            + roll + " ≤ " + formatPoints(evasion) + " %), attaque perdue ("
                            + formatPoints(remaining) + " " + damageType + ")");
                    break;
                }
            }

            if (target instanceof Vehicle vehicle && vehicle.isAerial()) {
                int roll = rand();
                int hitChance = (int) Math.round(striker.getAerialHitChance() * 100);
                if (roll > hitChance) {
                    tracker(striker).fired = true;
                    segments.add(actor + " tire sur " + targetLabel(targetOwner, target)
                            + " : loupe la cible aérienne (jet " + roll + " > " + hitChance
                            + " %), attaque perdue (" + formatPoints(remaining) + " " + damageType + ")");
                    break;
                }
            }

            double bonus = striker instanceof Unit unit
                    ? unit.getVehicleDamageBonus(target instanceof Vehicle vehicle ? vehicle : null, damageType)
                    : 0;
            double gross = remaining * (1 + bonus / 100);
            double resistance = target.getDamageReduction(damageType);
            double effective = gross * (1 - resistance);
            double armor = target.getArmor();
            double defense = target.getDefense();
            String bonusText = bonus > 0
                    ? ", bonus anti-véhicule +" + formatPoints(bonus) + " % (" + formatPoints(remaining)
                            + " → " + formatPoints(gross) + ")"
                    : "";
            String resistanceText = resistance > 0
                    ? ", résistance " + formatPoints(resistance * 100) + " % (" + formatPoints(gross)
                            + " → " + formatPoints(effective) + ")"
                    : "";

            tracker(striker).fired = true;
            if (armor + defense <= effective) {
                double cost = (armor + defense) / (1 - resistance);
                // Reste en points de base : le bonus anti-véhicule ne doit pas être réappliqué au surplus enchaîné.
                remaining -= cost / (1 + bonus / 100);
                tracker(striker).destroyedTarget = true;
                tracker(striker).damageDealt += armor + defense;
                tracker(target).damaged = true;
                tracker(target).damageTaken += armor + defense;
                destroyTarget(enemies, target, segments, actor, targetOwner, damageType, bonusText, resistanceText);
            } else if (effective <= armor) {
                target.setArmor(armor - effective);
                tracker(striker).damageDealt += effective;
                tracker(target).damaged = true;
                tracker(target).damageTaken += effective;
                remaining = 0;
                segments.add(actor + " touche " + targetLabel(targetOwner, target) + " : armure "
                        + formatPoints(armor) + " → " + formatPoints(target.getArmor()) + bonusText + resistanceText);
            } else {
                target.setArmor(0);
                target.setDefense(Math.max(0, defense - (effective - armor)));
                tracker(striker).damageDealt += effective;
                tracker(target).damaged = true;
                tracker(target).damageTaken += effective;
                remaining = 0;
                segments.add(actor + " touche " + targetLabel(targetOwner, target) + " : armure "
                        + formatPoints(armor) + " → 0, défense " + formatPoints(defense) + " → "
                        + formatPoints(target.getDefense()) + bonusText + resistanceText);
            }
        }

        if (!segments.isEmpty()) {
            String message = segments.getFirst() + (segments.size() > 1 ? " ; surplus enchaîné → " + String.join(" ; ", segments.subList(1, segments.size())) : "");
            logger.info("      > {}", message);
            recordEvent(firstSegmentOutcome(segments.getFirst()), message);
        }
    }

    private static String firstSegmentOutcome(String firstSegment) {
        if (firstSegment.contains("détruit")) return BattleLogEntry.DESTROYED;
        if (firstSegment.contains("esquive") || firstSegment.contains("loupe")) return BattleLogEntry.DODGE;
        return BattleLogEntry.DAMAGE;
    }

    private void destroyTarget(List<CombatEntity> enemies, CombatEntity target, List<String> segments,
                               String actor, String targetOwner, String damageType, String bonusText,
                               String resistanceText) {
        enemies.remove(target);
        String segment = actor + " détruit " + targetLabel(targetOwner, target) + " (" + damageType + ")"
                + bonusText + resistanceText;
        if (target instanceof Vehicle vehicle) {
            List<CombatEntity> occupants = vehicle.disembarkAll();
            if (!occupants.isEmpty()) {
                embarked.removeAll(occupants);
                carriedBy.keySet().removeAll(occupants);
                segment += ", équipage débarqué : "
                        + occupants.stream().map(CombatEntity::getDisplayName).collect(Collectors.joining(", "));
            }
        }
        segments.add(segment);
    }

    private Boolean chooseCategory(List<CombatEntity> enemies, String damageType, boolean preferVehicle) {
        boolean hasVehicles = !vehicleTargets(enemies, damageType).isEmpty();
        boolean hasOthers = !otherTargets(enemies).isEmpty();
        if (preferVehicle && hasVehicles) return Boolean.TRUE;
        if (!hasVehicles) return Boolean.FALSE;
        if (!hasOthers) return Boolean.TRUE;
        return rand() <= 50;
    }

    private CombatEntity pickTarget(List<CombatEntity> enemies, String damageType, Boolean vehicleCategory) {
        List<CombatEntity> vehicles = vehicleTargets(enemies, damageType);
        List<CombatEntity> others = otherTargets(enemies);
        if (Boolean.TRUE.equals(vehicleCategory)) {
            return vehicles.isEmpty() ? weakestOther(others) : weakestVehicle(vehicles);
        }
        return others.isEmpty() ? weakestVehicle(vehicles) : weakestOther(others);
    }

    private List<CombatEntity> vehicleTargets(List<CombatEntity> enemies, String damageType) {
        return enemies.stream()
                .filter(e -> e instanceof Vehicle)
                .filter(e -> !embarked.contains(e))
                .filter(e -> "PDF".equals(damageType) || !((Vehicle) e).isAerial())
                .toList();
    }

    private List<CombatEntity> otherTargets(List<CombatEntity> enemies) {
        return enemies.stream()
                .filter(e -> !(e instanceof Vehicle))
                .filter(e -> !embarked.contains(e))
                .toList();
    }

    private static CombatEntity weakestVehicle(List<CombatEntity> vehicles) {
        return vehicles.stream()
                .min(Comparator.comparingDouble(CombatEntity::getTotalDefense)
                        .thenComparing(CombatEntity::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingInt(System::identityHashCode))
                .orElse(null);
    }

    private static CombatEntity weakestOther(List<CombatEntity> others) {
        return others.stream().min(EXPOSURE_ORDER).orElse(null);
    }

    private static final Comparator<CombatEntity> EXPOSURE_ORDER = Comparator
            .comparingInt(Battle::exposureRank)
            .thenComparingDouble(Battle::exposureExperience)
            .thenComparingDouble(CombatEntity::getTotalDefense)
            .thenComparingDouble(CombatEntity::getTotalAttack)
            .thenComparing(CombatEntity::getId, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparingInt(System::identityHashCode);

    private static int exposureRank(CombatEntity entity) {
        if (entity instanceof Unit) return 0;
        if (entity instanceof Bank) return 1;
        if (entity instanceof WeaponCache) return 2;
        if (entity instanceof GameCharacter) return 4;
        if (entity instanceof Headquarters) return 3;
        return 3;
    }

    private static double exposureExperience(CombatEntity entity) {
        return entity instanceof Unit unit ? unit.getExperience() : 0;
    }

    private String actorLabel(CombatEntity striker, String owner) {
        Vehicle carrier = carriedBy.get(striker);
        String label = owner + " · " + striker.getDisplayName();
        return carrier != null && embarked.contains(striker)
                ? label + " (à bord de " + carrier.getDisplayName() + ")"
                : label;
    }

    private static String targetLabel(String owner, CombatEntity entity) {
        return owner + " · " + entity.getDisplayName();
    }

    private static String formatPoints(double value) {
        return Math.abs(value - Math.rint(value)) < 0.05 ? String.valueOf((long) Math.rint(value)) : String.format("%.1f", value);
    }

    private void collectEmbarked(List<CombatEntity> camp) {
        for (CombatEntity entity : camp) {
            if (entity instanceof Vehicle vehicle) {
                for (CombatEntity occupant : vehicle.getAllOccupants()) {
                    embarked.add(occupant);
                    carriedBy.put(occupant, vehicle);
                }
            }
        }
    }

    private List<CombatEntity> collectStrikers(List<CombatEntity> camp, String damageType,
                                               Predicate<CombatEntity> extraFilter) {
        List<CombatEntity> strikers = new ArrayList<>();
        for (CombatEntity entity : camp) {
            if (!extraFilter.test(entity)) {
                continue;
            }
            if (embarked.contains(entity)) {
                if (canCrewStrike(entity, damageType)) {
                    strikers.add(entity);
                }
            } else if (canStrike(entity, damageType)) {
                strikers.add(entity);
            }
        }
        return strikers;
    }

    private boolean canStrike(CombatEntity entity, String damageType) {
        if (statValue(entity, damageType) <= 0) {
            return false;
        }
        return !(entity instanceof Vehicle vehicle) || vehicle.hasPilot();
    }

    private boolean canCrewStrike(CombatEntity crew, String damageType) {
        if (!"PDF".equals(damageType) || crew.getPdf() <= 0) {
            return false;
        }
        Vehicle carrier = carriedBy.get(crew);
        return carrier != null && carrier.hasPilot() && carrier.getPdf() > 0;
    }

    private static double statValue(CombatEntity entity, String damageType) {
        return switch (damageType) {
            case "PDF" -> entity.getPdf();
            case "PDC" -> entity.getPdc();
            case "ATK" -> entity.getAttack();
            default -> throw new IllegalArgumentException("Type de points inconnu : " + damageType);
        };
    }

    private void printPhaseHeader(String phase) {
        logger.info("\n  === Phase {} ===", phase);
        this.currentPhase = phase;
        recordEvent(BattleLogEntry.INFO, "=== Phase " + phase + " ===");
    }

    private void printUnitsIndented(List<CombatEntity> units, String label) {
        logger.info("    {} :", label);
        for (CombatEntity unit : units) {
            logUnit(unit);
        }
    }

    private void finishStandoff(List<Player> players, List<List<CombatEntity>> camps) {
        for (List<CombatEntity> camp : camps) {
            injureDamagedInfantry(camp);
        }
        List<Integer> survivingCamps = new ArrayList<>();
        for (int i = 0; i < camps.size(); i++) {
            if (!hasNoSurvivingFighters(camps.get(i))) {
                survivingCamps.add(i);
            }
        }
        this.winner = survivingCamps.size() == 1 ? players.get(survivingCamps.getFirst()) : null;
        if (this.winner != null) {
            logger.info("=== Vainqueur de l'impasse : {} ===", this.winner.getName());
            recordEvent(BattleLogEntry.WINNER, "Vainqueur de l'impasse : " + this.winner.getName());
        } else {
            recordEvent(BattleLogEntry.WINNER, "Aucun vainqueur — plusieurs camps conservent des combattants");
        }
    }

    /** Les bâtiments ne comptent pas : attaquant anéanti ⇒ le défenseur garde le secteur. */
    private void finishBattle(Player attacker, Player defender, List<CombatEntity> attackerUnits, List<CombatEntity> defenderUnits) {
        if (hasNoSurvivingFighters(attackerUnits)) {
            this.winner = defender;
        } else if (hasNoSurvivingFighters(defenderUnits)) {
            this.winner = attacker;
        } else {
            this.winner = null;
        }
        if (this.winner != null) {
            logger.info("=== Vainqueur : {} ===", this.winner.getName());
            recordEvent(BattleLogEntry.WINNER, "Vainqueur : " + this.winner.getName());
        } else {
            recordEvent(BattleLogEntry.WINNER, "Aucun vainqueur — les deux camps conservent des combattants");
        }
    }

    private boolean hasNoSurvivingFighters(List<CombatEntity> entities) {
        return entities.stream().noneMatch(e -> e.getEntityCategory() != EntityCategory.BUILDING);
    }

    private void injureDamagedInfantry(List<CombatEntity> survivors) {
        for (CombatEntity entity : survivors) {
            if (entity.getEntityCategory() == EntityCategory.INFANTRY
                    && !entity.isInjured() && entity.getDefense() < entity.getBaseDefense()) {
                handleInjuredUnit(entity);
            }
        }
    }

    private static boolean isInfantry(CombatEntity entity) {
        return entity.getEntityCategory() == EntityCategory.INFANTRY;
    }

    private static boolean isSecondaryBuilding(CombatEntity entity) {
        return entity instanceof Building building && building.getBuildingType() != BuildingType.HEADQUARTERS;
    }

    private static boolean isHeadquarters(CombatEntity entity) {
        return entity instanceof Headquarters;
    }

    private static boolean isCharacter(CombatEntity entity) {
        return entity.getEntityCategory() == EntityCategory.CHARACTER;
    }
}
