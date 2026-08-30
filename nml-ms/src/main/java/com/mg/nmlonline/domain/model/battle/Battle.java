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

    private int sectorId; // ID du secteur où se déroule le combat

    private List<Player> defenders = new ArrayList<>();
    private List<Player> attackers = new ArrayList<>();

    // Un combat peut avoir un vainqueur (qui prend ou garde le secteur) — optionnel.
    private Player winner;

    private Random random;

    public Battle() {
        this.random = new Random();
    }

    private int rand() {
        return random.nextInt(100) + 1;
    }

    public PhaseResult classicPhaseConfiguration(List<CombatEntity> defender, double availableAttackerPoints, String damageType) {
        List<CombatEntity> casualties = new ArrayList<>();
        logger.info("    Points d'attaque disponibles : {}", availableAttackerPoints);

        while (availableAttackerPoints > 0 && !defender.isEmpty()) {
            CombatEntity targetUnit = defender.getLast();
            double evasion = targetUnit.getEvasion();
            double armor = targetUnit.getArmor();
            double defense = targetUnit.getDefense();
            double resistance = targetUnit.getDamageReduction(damageType);

            if (evasion > 0 && rand() <= evasion) {
                logger.info("      > {} esquive l'attaque !", targetUnit.getDisplayName());
                availableAttackerPoints -= (defense + armor);
                continue;
            }

            double effectivePoints = availableAttackerPoints * (1 - resistance);
            if (availableAttackerPoints != effectivePoints) {
                logger.info("      > Résistance de {}% appliquée. Dégâts effectifs : {}", String.format("%.0f", resistance * 100), String.format("%.2f", effectivePoints));
            }

            if ((armor + defense) <= effectivePoints) {
                logger.info("      > {} (ID: {}) est détruit pendant la phase {} !", targetUnit.getDisplayName(), targetUnit.getId(), damageType);
                availableAttackerPoints -= (defense + armor) / (1 - resistance);
                defender.remove(targetUnit);
                casualties.add(targetUnit);
            } else if (effectivePoints <= armor) {
                targetUnit.setArmor(armor - effectivePoints);
                logger.info("      > {} perd {} d'armure (reste: {})", targetUnit.getDisplayName(), String.format("%.2f", effectivePoints), String.format("%.2f", targetUnit.getArmor()));
                availableAttackerPoints = 0;
            } else {
                targetUnit.setArmor(0);
                double remainingPoints = effectivePoints - armor;
                targetUnit.setDefense(defense - remainingPoints);
                logger.info("      > {} perd toute son armure et {} de défense (reste: {})", targetUnit.getDisplayName(), String.format("%.2f", remainingPoints), String.format("%.2f", targetUnit.getDefense()));
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

    /**
     * Séquence : PDF (+r2) → bâtiments secondaires (riposte, hors pool partagé) → PDC (+r2)
     * → ATK (pool unités seules) → QG → personnages (dernier intervenant, attack seul).
     * DO NOT REMOVE CODE COMMENT IN THIS METHOD.
     */
    public void classicCombatConfiguration(Player attacker, Player defender, List<CombatEntity> attackerUnits, List<CombatEntity> defenderUnits) {
        if (attackerUnits == null) attackerUnits = new ArrayList<>();
        if (defenderUnits == null) defenderUnits = new ArrayList<>();

        printUnitsIndented(defenderUnits, "Défenseurs en présence");
        printUnitsIndented(attackerUnits, "Attaquants en présence");

        logger.info("\n=== Début du combat entre {} et {} ===", attacker.getName(), defender.getName());

        // =================
        // === PHASE PDF ===
        // =================
        printPhaseHeader("PDF");
        double attackerTotalPdf = getAvailablePoints(attackerUnits, "PDF");
        double defenderTotalPdf = getAvailablePoints(defenderUnits, "PDF");

        PhaseResult attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalPdf, "PDF");
        PhaseResult defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalPdf, "PDF");

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

        // Vérifier si cette méthode peut-être fusionnée avec la phase suivante, de prise de batiments.
        if (checkPointsTypeInUnits(attackerUnits, "PDF") > 0 || checkPointsTypeInUnits(defenderUnits, "PDF") > 0) {
            printPhaseHeader("PDF - Round 2");
            attackerTotalPdf = getAvailablePoints(attackerUnits, "PDF");
            defenderTotalPdf = getAvailablePoints(defenderUnits, "PDF");

            attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalPdf, "PDF");
            defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalPdf, "PDF");

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

        // ===================================
        // === PHASE BATIMENTS SECONDAIRES ===
        // ===================================
        // Riposte des bâtiments secondaires (Cache/Banque, hors QG) : leur attack n'entre jamais dans les pools partagés.
        printPhaseHeader("Bâtiments secondaires");
        double attackerSecondariesAtk = sumAttack(attackerUnits, Battle::isSecondaryBuilding);
        double defenderSecondariesAtk = sumAttack(defenderUnits, Battle::isSecondaryBuilding);

        attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerSecondariesAtk, "ATK");
        defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderSecondariesAtk, "ATK");

        defenderUnits = attackerPhaseResult.survivors();
        attackerUnits = defenderPhaseResult.survivors();

        printUnitsIndented(defenderUnits, "Défenseurs restants");
        printUnitsIndented(attackerUnits, "Attaquants restants");

        if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
            logger.info("\n=== Combat terminé après la phase des bâtiments secondaires ! ===");
            endBattle(attacker, defender, attackerUnits, defenderUnits);
            return;
        }

        // =================
        // === PHASE PDC ===
        // =================
        printPhaseHeader("PDC");
        double attackerTotalPdc = getAvailablePoints(attackerUnits, "PDC");
        double defenderTotalPdc = getAvailablePoints(defenderUnits, "PDC");

        attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalPdc, "PDC");
        defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalPdc, "PDC");

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

            attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalPdc, "PDC");
            defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalPdc, "PDC");

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

        // =================
        // === PHASE ATK ===
        // =================
        // Pool unités seules : l'attaque des bâtiments/QG/personnage est réservée à leurs phases dédiées.
        printPhaseHeader("ATK");
        double attackerTotalAtk = sumAttack(attackerUnits, Battle::isInfantry);
        double defenderTotalAtk = sumAttack(defenderUnits, Battle::isInfantry);

        attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerTotalAtk, "ATK");
        defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderTotalAtk, "ATK");

        defenderUnits = attackerPhaseResult.survivors();
        attackerUnits = defenderPhaseResult.survivors();

        // Reassign unités seules : ne pas zéro l'attaque des QG/personnages avant leurs phases.
        reassignPointsForNextPhase(infantryOnly(attackerUnits), attackerPhaseResult.remainingPoints(), "ATK");
        reassignPointsForNextPhase(infantryOnly(defenderUnits), defenderPhaseResult.remainingPoints(), "ATK");

        printUnitsIndented(defenderUnits, "Défenseurs restants");
        printUnitsIndented(attackerUnits, "Attaquants restants");

        if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
            logger.info("\n=== Combat terminé après la phase ATK ! ===");
            endBattle(attacker, defender, attackerUnits, defenderUnits);
            return;
        }

        // ================
        // === PHASE QG ===
        // ================
        // Riposte du QG après la phase ATK.
        printPhaseHeader("Quartier Général");
        double attackerHqAtk = sumAttack(attackerUnits, Battle::isHeadquarters);
        double defenderHqAtk = sumAttack(defenderUnits, Battle::isHeadquarters);

        attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerHqAtk, "ATK");
        defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderHqAtk, "ATK");

        defenderUnits = attackerPhaseResult.survivors();
        attackerUnits = defenderPhaseResult.survivors();

        printUnitsIndented(defenderUnits, "Défenseurs restants");
        printUnitsIndented(attackerUnits, "Attaquants restants");

        if (defenderUnits.isEmpty() || attackerUnits.isEmpty()) {
            logger.info("\n=== Combat terminé après la phase QG ! ===");
            endBattle(attacker, defender, attackerUnits, defenderUnits);
            return;
        }

        // =========================
        // === PHASE PERSONNAGES ===
        // =========================
        // Dernier intervenant : le personnage (attack seul — ses pdf/pdc ont servi dans les phases partagées).
        printPhaseHeader("Personnages");
        double attackerCharacterAtk = sumAttack(attackerUnits, Battle::isCharacter);
        double defenderCharacterAtk = sumAttack(defenderUnits, Battle::isCharacter);

        attackerPhaseResult = classicPhaseConfiguration(defenderUnits, attackerCharacterAtk, "ATK");
        defenderPhaseResult = classicPhaseConfiguration(attackerUnits, defenderCharacterAtk, "ATK");

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

    /**
     * Conversion blessure (infanterie seule) + vainqueur, à chaque sortie de combat.
     */
    private void endBattle(Player attacker, Player defender, List<CombatEntity> attackerUnits, List<CombatEntity> defenderUnits) {
        injureDamagedInfantry(attackerUnits);
        injureDamagedInfantry(defenderUnits);
        finishBattle(attacker, defender, attackerUnits, defenderUnits);
    }

    /**
     * Vainqueur : l'attaquant l'emporte si le défenseur n'a plus de combattant (unités/personnages,
     * les bâtiments ne comptent pas) et qu'il lui en reste au moins un. Attaquant anéanti ⇒ défenseur
     * (le secteur tient, même si les défenseurs sont morts aussi). Deux camps survivants ⇒ aucun vainqueur.
     */
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
        }
    }

    private boolean hasNoSurvivingFighters(List<CombatEntity> entities) {
        return entities.stream().noneMatch(e -> e.getEntityCategory() != EntityCategory.BUILDING);
    }

    /**
     * Seules les unités d'infanterie terminent blessées (defense < baseDefense) ; personnages/bâtiments jamais.
     */
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
