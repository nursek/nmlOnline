package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.BuildingType;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.EntityCategory;
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

    public Battle() {
        this.random = new Random();
    }

    private int rand() {
        return random.nextInt(100) + 1;
    }

    public PhaseResult classicPhaseConfiguration(List<CombatEntity> defender, double availableAttackerPoints, String damageType) {
        return classicPhaseConfiguration(defender, availableAttackerPoints, damageType, null, null);
    }

    private PhaseResult classicPhaseConfiguration(List<CombatEntity> defender, double availableAttackerPoints,
                                                  String damageType, String actor, String targetOwner) {
        List<CombatEntity> casualties = new ArrayList<>();
        logger.info("    Points d'attaque disponibles : {}", availableAttackerPoints);
        if (actor != null && availableAttackerPoints > 0) {
            recordEvent(BattleLogEntry.INFO, actor + " engage " + formatPoints(availableAttackerPoints)
                    + " points (" + damageType + ") contre " + targetOwner);
        }

        while (availableAttackerPoints > 0 && !defender.isEmpty()) {
            CombatEntity targetUnit = defender.getLast();
            double evasion = targetUnit.getEvasion();
            double armor = targetUnit.getArmor();
            double defense = targetUnit.getDefense();
            double resistance = targetUnit.getDamageReduction(damageType);
            String target = targetLabel(targetOwner, targetUnit);

            if (evasion > 0 && rand() <= evasion) {
                String message = target + " esquive l'attaque" + (actor != null ? " de " + actor : "");
                logger.info("      > {}", message);
                recordEvent(BattleLogEntry.DODGE, message);
                availableAttackerPoints -= (defense + armor);
                continue;
            }

            double effectivePoints = availableAttackerPoints * (1 - resistance);
            if (availableAttackerPoints != effectivePoints) {
                logger.info("      > Résistance de {}% appliquée. Dégâts effectifs : {}", String.format("%.0f", resistance * 100), String.format("%.2f", effectivePoints));
            }

            if ((armor + defense) <= effectivePoints) {
                String message = (actor != null ? actor + " détruit " : "Détruit ") + target + " (" + damageType + ")";
                logger.info("      > {}", message);
                recordEvent(BattleLogEntry.DESTROYED, message);
                availableAttackerPoints -= (defense + armor) / (1 - resistance);
                defender.remove(targetUnit);
                casualties.add(targetUnit);
            } else if (effectivePoints <= armor) {
                targetUnit.setArmor(armor - effectivePoints);
                String message = attackMessage(actor, target, "armure " + formatPoints(armor)
                        + " → " + formatPoints(targetUnit.getArmor()));
                logger.info("      > {}", message);
                recordEvent(BattleLogEntry.DAMAGE, message);
                availableAttackerPoints = 0;
            } else {
                targetUnit.setArmor(0);
                double remainingPoints = effectivePoints - armor;
                targetUnit.setDefense(defense - remainingPoints);
                String damage = (armor > 0 ? "armure " + formatPoints(armor) + " → 0, " : "")
                        + "défense " + formatPoints(defense) + " → " + formatPoints(targetUnit.getDefense());
                String message = attackMessage(actor, target, damage);
                logger.info("      > {}", message);
                recordEvent(BattleLogEntry.DAMAGE, message);
                availableAttackerPoints = 0;
            }
        }

        if (!defender.isEmpty()) {
            logger.info("    Unités restantes après la phase {} :", damageType);
            for (CombatEntity unit : defender) {
                logUnit(unit);
            }
        }
        if (!casualties.isEmpty()) {
            logger.info("    Pertes pendant la phase {} :", damageType);
            for (CombatEntity unit : casualties) {
                logUnit(unit);
            }
        }
        return new PhaseResult(casualties, defender, availableAttackerPoints);
    }

    private static String targetLabel(String owner, CombatEntity entity) {
        return owner != null ? owner + " · " + entity.getDisplayName() : entity.getDisplayName();
    }

    private static String attackMessage(String actor, String target, String damage) {
        return (actor != null ? actor + " attaque " : "Attaque sur ") + target + " : " + damage;
    }

    private static String formatPoints(double value) {
        return Math.abs(value - Math.rint(value)) < 0.05 ? String.valueOf((long) Math.rint(value)) : String.format("%.1f", value);
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

    double checkPointsTypeInUnits(List<CombatEntity> units, String pointsType) {
        return switch (pointsType) {
            case "PDF" -> units.stream().mapToDouble(CombatEntity::getPdf).sum();
            case "PDC" -> units.stream().mapToDouble(CombatEntity::getPdc).sum();
            case "ATK" -> units.stream().mapToDouble(CombatEntity::getAttack).sum();
            default -> 0;
        };
    }

    private void handleInjuredUnit(CombatEntity unit) {
        unit.setInjured(true);
        unit.recalculateBaseStats();
    }

    /** Séquence : PDF (+r2) → secondaires → PDC (+r2) → ATK (unités seules) → QG → personnages. */
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

        printPhaseHeader("PDF");
        double attackerTotalPdf = getAvailablePoints(attackerUnits, "PDF");
        double defenderTotalPdf = getAvailablePoints(defenderUnits, "PDF");

        PhaseResult attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalPdf, "PDF", attacker.getName(), defender.getName());
        PhaseResult defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalPdf, "PDF", defender.getName(), attacker.getName());

        defenderUnits = attackerPhaseResult.survivors();
        attackerUnits = defenderPhaseResult.survivors();

        reassignPointsForNextPhase(attackerUnits, attackerPhaseResult.remainingPoints(), "PDF");
        reassignPointsForNextPhase(defenderUnits, defenderPhaseResult.remainingPoints(), "PDF");

        printUnitsIndented(defenderUnits, "Défenseurs restants");
        printUnitsIndented(attackerUnits, "Attaquants restants");

        if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
            logger.info("\n=== Combat terminé après la phase PDF ! ===");
            endBattle(attacker, defender, attackerUnits, defenderUnits);
            return;
        }

        if (checkPointsTypeInUnits(attackerUnits, "PDF") > 0 || checkPointsTypeInUnits(defenderUnits, "PDF") > 0) {
            printPhaseHeader("PDF - Round 2");
            attackerTotalPdf = getAvailablePoints(attackerUnits, "PDF");
            defenderTotalPdf = getAvailablePoints(defenderUnits, "PDF");

            attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalPdf, "PDF", attacker.getName(), defender.getName());
            defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalPdf, "PDF", defender.getName(), attacker.getName());

            defenderUnits = attackerPhaseResult.survivors();
            attackerUnits = defenderPhaseResult.survivors();

            reassignPointsForNextPhase(attackerUnits, attackerPhaseResult.remainingPoints(), "PDF");
            reassignPointsForNextPhase(defenderUnits, defenderPhaseResult.remainingPoints(), "PDF");

            printUnitsIndented(defenderUnits, "Défenseurs restants");
            printUnitsIndented(attackerUnits, "Attaquants restants");

            if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
                logger.info("\n=== Combat terminé après la phase PDF round 2 ! ===");
                endBattle(attacker, defender, attackerUnits, defenderUnits);
                return;
            }
        }

        // Riposte des secondaires : leur attack n'entre jamais dans les pools partagés.
        printPhaseHeader("Bâtiments secondaires");
        double attackerSecondariesAtk = sumAttack(attackerUnits, Battle::isSecondaryBuilding);
        double defenderSecondariesAtk = sumAttack(defenderUnits, Battle::isSecondaryBuilding);

        attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerSecondariesAtk, "ATK", attacker.getName(), defender.getName());
        defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderSecondariesAtk, "ATK", defender.getName(), attacker.getName());

        defenderUnits = attackerPhaseResult.survivors();
        attackerUnits = defenderPhaseResult.survivors();

        printUnitsIndented(defenderUnits, "Défenseurs restants");
        printUnitsIndented(attackerUnits, "Attaquants restants");

        if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
            logger.info("\n=== Combat terminé après la phase des bâtiments secondaires ! ===");
            endBattle(attacker, defender, attackerUnits, defenderUnits);
            return;
        }

        printPhaseHeader("PDC");
        double attackerTotalPdc = getAvailablePoints(attackerUnits, "PDC");
        double defenderTotalPdc = getAvailablePoints(defenderUnits, "PDC");

        attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalPdc, "PDC", attacker.getName(), defender.getName());
        defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalPdc, "PDC", defender.getName(), attacker.getName());

        defenderUnits = attackerPhaseResult.survivors();
        attackerUnits = defenderPhaseResult.survivors();

        reassignPointsForNextPhase(attackerUnits, attackerPhaseResult.remainingPoints(), "PDC");
        reassignPointsForNextPhase(defenderUnits, defenderPhaseResult.remainingPoints(), "PDC");

        printUnitsIndented(defenderUnits, "Défenseurs restants");
        printUnitsIndented(attackerUnits, "Attaquants restants");

        if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
            logger.info("\n=== Combat terminé après la phase PDC ! ===");
            endBattle(attacker, defender, attackerUnits, defenderUnits);
            return;
        }

        if (checkPointsTypeInUnits(attackerUnits, "PDC") > 0 || checkPointsTypeInUnits(defenderUnits, "PDC") > 0) {
            printPhaseHeader("PDC - Round 2");
            attackerTotalPdc = getAvailablePoints(attackerUnits, "PDC");
            defenderTotalPdc = getAvailablePoints(defenderUnits, "PDC");

            attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalPdc, "PDC", attacker.getName(), defender.getName());
            defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalPdc, "PDC", defender.getName(), attacker.getName());

            defenderUnits = attackerPhaseResult.survivors();
            attackerUnits = defenderPhaseResult.survivors();

            reassignPointsForNextPhase(attackerUnits, attackerPhaseResult.remainingPoints(), "PDC");
            reassignPointsForNextPhase(defenderUnits, defenderPhaseResult.remainingPoints(), "PDC");

            printUnitsIndented(defenderUnits, "Défenseurs restants");
            printUnitsIndented(attackerUnits, "Attaquants restants");

            if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
                logger.info("\n=== Combat terminé après la phase PDC round 2 ! ===");
                endBattle(attacker, defender, attackerUnits, defenderUnits);
                return;
            }
        }

        // Pool unités seules : l'attack des bâtiments/QG/personnage est réservée à leur phase.
        printPhaseHeader("ATK");
        double attackerTotalAtk = sumAttack(attackerUnits, Battle::isInfantry);
        double defenderTotalAtk = sumAttack(defenderUnits, Battle::isInfantry);

        attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalAtk, "ATK", attacker.getName(), defender.getName());
        defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalAtk, "ATK", defender.getName(), attacker.getName());

        defenderUnits = attackerPhaseResult.survivors();
        attackerUnits = defenderPhaseResult.survivors();

        // Idem au reassign : ne pas zéro l'attack du QG et du personnage avant leur phase.
        reassignPointsForNextPhase(infantryOnly(attackerUnits), attackerPhaseResult.remainingPoints(), "ATK");
        reassignPointsForNextPhase(infantryOnly(defenderUnits), defenderPhaseResult.remainingPoints(), "ATK");

        printUnitsIndented(defenderUnits, "Défenseurs restants");
        printUnitsIndented(attackerUnits, "Attaquants restants");

        if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
            logger.info("\n=== Combat terminé après la phase ATK ! ===");
            endBattle(attacker, defender, attackerUnits, defenderUnits);
            return;
        }

        printPhaseHeader("Quartier Général");
        double attackerHqAtk = sumAttack(attackerUnits, Battle::isHeadquarters);
        double defenderHqAtk = sumAttack(defenderUnits, Battle::isHeadquarters);

        attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerHqAtk, "ATK", attacker.getName(), defender.getName());
        defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderHqAtk, "ATK", defender.getName(), attacker.getName());

        defenderUnits = attackerPhaseResult.survivors();
        attackerUnits = defenderPhaseResult.survivors();

        printUnitsIndented(defenderUnits, "Défenseurs restants");
        printUnitsIndented(attackerUnits, "Attaquants restants");

        if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
            logger.info("\n=== Combat terminé après la phase QG ! ===");
            endBattle(attacker, defender, attackerUnits, defenderUnits);
            return;
        }

        // attack seul : ses pdf/pdc ont déjà servi dans les phases partagées.
        printPhaseHeader("Personnages");
        double attackerCharacterAtk = sumAttack(attackerUnits, Battle::isCharacter);
        double defenderCharacterAtk = sumAttack(defenderUnits, Battle::isCharacter);

        attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerCharacterAtk, "ATK", attacker.getName(), defender.getName());
        defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderCharacterAtk, "ATK", defender.getName(), attacker.getName());

        defenderUnits = attackerPhaseResult.survivors();
        attackerUnits = defenderPhaseResult.survivors();

        printUnitsIndented(defenderUnits, "Défenseurs restants");
        printUnitsIndented(attackerUnits, "Attaquants restants");

        if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
            logger.info("\n=== Combat terminé après la phase des personnages ! ===");
        } else {
            logger.info("\n=== Combat terminé, il reste des unités dans les deux camps. ===");
        }
        endBattle(attacker, defender, attackerUnits, defenderUnits);
    }

    private void endBattle(Player attacker, Player defender, List<CombatEntity> attackerUnits, List<CombatEntity> defenderUnits) {
        injureDamagedInfantry(attackerUnits);
        injureDamagedInfantry(defenderUnits);
        finishBattle(attacker, defender, attackerUnits, defenderUnits);
    }

    /**
     * Impasse mexicaine : le camp i frappe le camp (i+1) % n. À chaque phase, tous les points
     * sortants sont calculés avant application — un camp détruit dans la phase frappe quand même.
     */
    public void classicStandoffConfiguration(List<Player> players, List<List<CombatEntity>> camps) {
        int n = camps.size();
        if (n < 3 || players.size() != n) {
            throw new IllegalArgumentException("Une impasse mexicaine nécessite au moins 3 camps.");
        }
        logger.info("\n=== Impasse mexicaine à {} camps ===", n);
        this.currentPhase = "État initial";
        for (int i = 0; i < n; i++) {
            recordInitialState(players.get(i), camps.get(i));
        }
        this.currentPhase = "Combat";
        recordEvent(BattleLogEntry.INFO, "Impasse mexicaine à " + n + " camps : "
                + players.stream().map(Player::getName).collect(Collectors.joining(" → "))
                + " → " + players.getFirst().getName());

        this.currentPhase = "Impasse — PDF";
        standoffPhase(players, camps, "PDF", e -> true, e -> true);
        if (anyPointsInCamps(camps, "PDF")) {
            this.currentPhase = "Impasse — PDF round 2";
            standoffPhase(players, camps, "PDF", e -> true, e -> true);
        }

        this.currentPhase = "Impasse — Bâtiments secondaires";
        standoffPhase(players, camps, "ATK", null, Battle::isSecondaryBuilding);

        this.currentPhase = "Impasse — PDC";
        standoffPhase(players, camps, "PDC", e -> true, e -> true);
        if (anyPointsInCamps(camps, "PDC")) {
            this.currentPhase = "Impasse — PDC round 2";
            standoffPhase(players, camps, "PDC", e -> true, e -> true);
        }

        this.currentPhase = "Impasse — ATK";
        standoffPhase(players, camps, "ATK", Battle::isInfantry, Battle::isInfantry);
        this.currentPhase = "Impasse — QG";
        standoffPhase(players, camps, "ATK", null, Battle::isHeadquarters);
        this.currentPhase = "Impasse — Personnages";
        standoffPhase(players, camps, "ATK", null, Battle::isCharacter);

        finishStandoff(players, camps);
    }

    private void standoffPhase(List<Player> players, List<List<CombatEntity>> camps, String pointsType,
                               Predicate<CombatEntity> reassignFilter,
                               Predicate<CombatEntity> strikeFilter) {
        int n = camps.size();
        double[] points = new double[n];
        for (int i = 0; i < n; i++) {
            points[i] = camps.get(i).stream()
                    .filter(strikeFilter)
                    .mapToDouble(e -> getUnitPoints(e, pointsType))
                    .sum();
        }

        PhaseResult[] results = new PhaseResult[n];
        for (int i = 0; i < n; i++) {
            Player striker = players.get(i);
            Player target = players.get((i + 1) % n);
            recordEvent(BattleLogEntry.INFO, striker.getName() + " frappe " + target.getName() + " (" + pointsType + ")");
            results[i] = classicPhaseConfiguration(camps.get((i + 1) % n), points[i], pointsType,
                    striker.getName(), target.getName());
        }

        if (reassignFilter != null) {
            for (int i = 0; i < n; i++) {
                reassignPointsForNextPhase(
                        camps.get(i).stream().filter(reassignFilter).collect(Collectors.toList()),
                        results[i].remainingPoints(), pointsType);
            }
        }
    }

    private boolean anyPointsInCamps(List<List<CombatEntity>> camps, String pointsType) {
        return camps.stream().anyMatch(camp -> checkPointsTypeInUnits(camp, pointsType) > 0);
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

    private static List<CombatEntity> infantryOnly(List<CombatEntity> entities) {
        return entities.stream().filter(Battle::isInfantry).collect(Collectors.toList());
    }

    private double sumAttack(List<CombatEntity> entities, Predicate<CombatEntity> filter) {
        return entities.stream().filter(filter).mapToDouble(CombatEntity::getAttack).sum();
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

    private void reassignPointsForNextPhase(List<CombatEntity> units, double points, String pointsType) {
        if (units == null || units.isEmpty()) return;

        double totalMax = units.stream().mapToDouble(u -> getUnitPoints(u, pointsType)).sum();

        if (points <= 0) {
            units.forEach(u -> setUnitPoints(u, pointsType, 0));
        } else if (points >= totalMax) {
            units.forEach(u -> setUnitPoints(u, pointsType, getUnitPoints(u, pointsType)));
        } else {
            for (CombatEntity unit : units) {
                double max = getUnitPoints(unit, pointsType);
                double toAssign = Math.min(points, max);
                setUnitPoints(unit, pointsType, toAssign);
                points -= toAssign;
                if (points <= 0) break;
            }
            units.stream()
                    .filter(u -> getUnitPoints(u, pointsType) == 0)
                    .forEach(u -> setUnitPoints(u, pointsType, 0));
        }
    }

    private double getUnitPoints(CombatEntity unit, String pointsType) {
        return switch (pointsType) {
            case "PDF" -> unit.getPdf();
            case "PDC" -> unit.getPdc();
            case "ATK" -> unit.getAttack();
            default -> throw new IllegalArgumentException("Type de points inconnu : " + pointsType);
        };
    }

    private void setUnitPoints(CombatEntity unit, String pointsType, double value) {
        switch (pointsType) {
            case "PDF" -> unit.setPdf(value);
            case "PDC" -> unit.setPdc(value);
            case "ATK" -> unit.setAttack(value);
            default -> throw new IllegalArgumentException("Type de points inconnu : " + pointsType);
        }
    }

    private double getAvailablePoints(List<CombatEntity> units, String pointsType) {
        return units.stream().mapToDouble(u -> getUnitPoints(u, pointsType)).sum();
    }
}
