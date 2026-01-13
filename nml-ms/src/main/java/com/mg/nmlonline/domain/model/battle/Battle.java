package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.Unit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Battle {
    private int sectorId; // ID of the sector where the battle takes place

    private List<Player> playersInvolved = new ArrayList<>(); // Principal List of players involved in the battle
    // Players are then moved to according List
    private List<Player> defenders = new ArrayList<>();
    private List<Player> attackers = new ArrayList<>();

    // A battle can have a winner, but not mandatory. A winner claims or keeps the sector.
    private Player winner;

    private static final Random RANDOM = new Random();

    private int rand() {
        return RANDOM.nextInt(100);
    }

    public PhaseResult classicPhaseConfiguration(List<Unit> defender, double availableAttackerPoints, String damageType) {
        List<Unit> casualties = new ArrayList<>();
        System.out.println("    Points d'attaque disponibles : " + availableAttackerPoints);

        while (availableAttackerPoints > 0 && !defender.isEmpty()) {
            Unit targetUnit = defender.getLast();
            double evasion = targetUnit.getEvasion();
            double armor = targetUnit.getArmor();
            double defense = targetUnit.getDefense();
            double resistance = targetUnit.getDamageReduction(damageType);

            // Gestion de l'évasion
            if (evasion > 0 && (rand() % 100) + 1 <= evasion) {
                System.out.println("      > " + targetUnit.getType().name() + " esquive l'attaque !");
                availableAttackerPoints -= (defense + armor);
                continue;
            }

            // Calcul des dégâts avec résistance
            double effectivePoints = availableAttackerPoints * (1 - resistance);
            if (availableAttackerPoints != effectivePoints) {
                System.out.printf("      > Résistance de %.0f%% appliquée. Dégâts effectifs : %.2f%n", resistance * 100, effectivePoints);
            }

            if ((armor + defense) <= effectivePoints) {
                System.out.println("      > " + targetUnit.getType().name() + " (ID: " + targetUnit.getId() + ") est détruit pendant la phase " + damageType + " !");
                availableAttackerPoints -= (defense + armor) / (1 - resistance);
                defender.remove(targetUnit);
                casualties.add(targetUnit);
            } else if (effectivePoints <= armor) {
                targetUnit.setArmor(armor - effectivePoints);
                System.out.printf("      > %s perd %.2f d'armure (reste: %.2f)%n", targetUnit.getType().name(), effectivePoints, targetUnit.getArmor());
                availableAttackerPoints = 0;
            } else {
                targetUnit.setArmor(0);
                double remainingPoints = effectivePoints - armor;
                targetUnit.setDefense(defense - remainingPoints);
                System.out.printf("      > %s perd toute son armure et %.2f de défense (reste: %.2f)%n", targetUnit.getType().name(), remainingPoints, targetUnit.getDefense());
                availableAttackerPoints = 0;
            }
        }

        if (!defender.isEmpty()) {
            System.out.println("    Unités restantes après la phase " + damageType + " :");
            for (Unit unit : defender) {
                System.out.println("      - " + unit);
            }
        }
        if (!casualties.isEmpty()) {
            System.out.println("    Pertes pendant la phase " + damageType + " :");
            for (Unit unit : casualties) {
                System.out.println("      - " + unit);
            }
        }
        return new PhaseResult(casualties, defender, availableAttackerPoints);
    }

    double checkPointsTypeInUnits(List<Unit> units, String pointsType) {
        return switch (pointsType) {
            case "PDF" -> units.stream().mapToDouble(Unit::getPdf).sum();
            case "PDC" -> units.stream().mapToDouble(Unit::getPdc).sum();
            case "ATK" -> units.stream().mapToDouble(Unit::getAttack).sum();
            default -> 0;
        };
    }


    private Unit handleInjuredUnit(Unit unit) {
        unit.setInjured(true);
        unit.recalculateBaseStats();
        return unit;
    }

    private List<Unit> replaceWithInjured(List<Unit> survivors, List<Unit> casualties) {
        Set<Integer> casualtiesIds = casualties.stream().map(Unit::getId).collect(Collectors.toSet());
        List<Unit> result = new ArrayList<>();
        for (Unit unit : survivors) {
            if (casualtiesIds.contains(unit.getId()) ||
                    (unit.getClasses().stream().noneMatch(c -> c.getCode().equals("BLESSE")) && unit.getDefense() < unit.getBaseDefense())) {
                result.add(handleInjuredUnit(unit));
            } else {
                result.add(unit);
            }
        }
        return result;
    }

    /**
     * Nouvelle méthode pour résoudre un combat à partir d'un BattleSetup
     */
    public void resolveBattle(BattleSetup battleSetup) {
        if (battleSetup == null || !battleSetup.hasBattle()) {
            System.out.println("Aucune bataille à résoudre");
            return;
        }

        this.sectorId = battleSetup.getSectorId();
        this.playersInvolved = new ArrayList<>(battleSetup.getPlayers());

        System.out.println("\n====================================================");
        System.out.println("BATAILLE AU SECTEUR " + sectorId);
        System.out.println("Type de bataille : " + battleSetup.getBattleType());
        System.out.println("Joueurs impliqués : " + playersInvolved.size());
        System.out.println("====================================================");

        // Si combat à 2 joueurs, utiliser la méthode classique
        if (playersInvolved.size() == 2) {
            Player player1 = playersInvolved.get(0);
            Player player2 = playersInvolved.get(1);

            List<Unit> units1 = battleSetup.getUnitsForPlayer(player1);
            List<Unit> units2 = battleSetup.getUnitsForPlayer(player2);

            // Déterminer attaquant et défenseur
            Player attacker;
            Player defender;
            List<Unit> attackerUnits;
            List<Unit> defenderUnits;

            if (battleSetup.getOriginalOwnerId() != null &&
                player1.getId().equals(battleSetup.getOriginalOwnerId())) {
                defender = player1;
                attacker = player2;
                defenderUnits = units1;
                attackerUnits = units2;
            } else if (battleSetup.getOriginalOwnerId() != null &&
                       player2.getId().equals(battleSetup.getOriginalOwnerId())) {
                defender = player2;
                attacker = player1;
                defenderUnits = units2;
                attackerUnits = units1;
            } else {
                // Secteur neutre, le premier est attaquant par défaut
                attacker = player1;
                defender = player2;
                attackerUnits = units1;
                defenderUnits = units2;
            }

            classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);
        } else {
            // TODO: Implémenter le combat multi-joueurs
            System.out.println("Combat multi-joueurs non encore implémenté");
        }
    }


    public void classicCombatConfiguration(Player attacker, Player defender, List<Unit> attackerUnits, List<Unit> defenderUnits) {
        if (attackerUnits == null) attackerUnits = new ArrayList<>();
        if (defenderUnits == null) defenderUnits = new ArrayList<>();

        printUnitsIndented(defenderUnits, "Défenseurs début");
        printUnitsIndented(attackerUnits, "Attaquants début");

        System.out.println("\n=== Début du combat entre " + attacker.getName() + " et " + defender.getName() + " ===");

        // Phase PDF
        CombatUnits combatState = new CombatUnits(attackerUnits, defenderUnits);
        combatState = executeLethalPhase(combatState, "PDF");
        if (combatState == null) return;

        // Phase PDC
        combatState = executeLethalPhase(combatState, "PDC");
        if (combatState == null) return;

        // Phase ATK (non-létale)
        executeNonLethalPhase(combatState);

        printUnitsIndented(combatState.defenderUnits, "Défenseurs restants");
        printUnitsIndented(combatState.attackerUnits, "Attaquants restants");

        if (combatState.defenderUnits.isEmpty() || combatState.attackerUnits.isEmpty()) {
            System.out.println("\n=== Combat terminé après la phase ATK ! ===");
        } else {
            System.out.println("\n=== Combat terminé, il reste des unités dans les deux camps. ===");
        }
    }

    /**
     * Exécute une phase létale de combat (PDF ou PDC) avec gestion automatique des rounds multiples
     * Les deux camps attaquent simultanément sur l'état initial des unités
     * @return null si le combat est terminé, sinon les unités mises à jour
     */
    private CombatUnits executeLethalPhase(CombatUnits combatState, String phaseType) {
        int round = 1;
        CombatUnits currentState = combatState;

        while (checkPointsTypeInUnits(currentState.attackerUnits, phaseType) > 0
               || checkPointsTypeInUnits(currentState.defenderUnits, phaseType) > 0) {

            String phaseName = phaseType + (round > 1 ? " - Round " + round : "");
            printPhaseHeader(phaseName);

            // Créer des copies pour que les deux camps attaquent l'état initial
            List<Unit> defendersCopy = new ArrayList<>(currentState.defenderUnits);
            List<Unit> attackersCopy = new ArrayList<>(currentState.attackerUnits);

            // Calculer les résultats des deux attaques simultanément
            PhaseResult attackerPhaseResult = classicPhaseConfiguration(
                defendersCopy,
                getAvailablePoints(currentState.attackerUnits, phaseType),
                phaseType
            );
            PhaseResult defenderPhaseResult = classicPhaseConfiguration(
                attackersCopy,
                getAvailablePoints(currentState.defenderUnits, phaseType),
                phaseType
            );

            // Appliquer les résultats après les deux attaques
            currentState = new CombatUnits(
                defenderPhaseResult.survivors(),
                attackerPhaseResult.survivors()
            );

            reassignPointsForNextPhase(currentState.attackerUnits, attackerPhaseResult.remainingPoints(), phaseType);
            reassignPointsForNextPhase(currentState.defenderUnits, defenderPhaseResult.remainingPoints(), phaseType);

            printUnitsIndented(currentState.defenderUnits, "Défenseurs restants");
            printUnitsIndented(currentState.attackerUnits, "Attaquants restants");

            if (currentState.defenderUnits.isEmpty() || currentState.attackerUnits.isEmpty()) {
                System.out.println("\n=== Combat terminé après la phase " + phaseName + " ! ===");
                return null;
            }

            round++;
        }

        return currentState;
    }

    /**
     * Exécute la phase non-létale de combat (ATK)
     * Les unités qui devraient mourir deviennent blessées
     */
    private void executeNonLethalPhase(CombatUnits combatState) {
        printPhaseHeader("ATK");

        // Créer des copies pour que les deux camps attaquent l'état initial
        List<Unit> defendersCopy = new ArrayList<>(combatState.defenderUnits);
        List<Unit> attackersCopy = new ArrayList<>(combatState.attackerUnits);

        PhaseResult attackerPhaseResult = classicPhaseConfiguration(
            defendersCopy,
            getAvailablePoints(combatState.attackerUnits, "ATK"),
            "ATK"
        );
        PhaseResult defenderPhaseResult = classicPhaseConfiguration(
            attackersCopy,
            getAvailablePoints(combatState.defenderUnits, "ATK"),
            "ATK"
        );

        // Fin du combat, on remplace les unités censées mourir par des blessées
        combatState.defenderUnits = replaceWithInjured(
            attackerPhaseResult.survivors(),
            attackerPhaseResult.casualties()
        );
        combatState.attackerUnits = replaceWithInjured(
            defenderPhaseResult.survivors(),
            defenderPhaseResult.casualties()
        );

        reassignPointsForNextPhase(combatState.attackerUnits, attackerPhaseResult.remainingPoints(), "ATK");
        reassignPointsForNextPhase(combatState.defenderUnits, defenderPhaseResult.remainingPoints(), "ATK");
    }

    /**
     * Classe interne pour encapsuler les listes d'unités en combat
     */
    private static class CombatUnits {
        List<Unit> attackerUnits;
        List<Unit> defenderUnits;

        CombatUnits(List<Unit> attackerUnits, List<Unit> defenderUnits) {
            this.attackerUnits = attackerUnits;
            this.defenderUnits = defenderUnits;
        }
    }

    private void printPhaseHeader(String phase) {
        System.out.println("\n  === Phase " + phase + " ===");
    }

    private void printUnitsIndented(List<Unit> units, String label) {
        System.out.println("    " + label + " :");
        for (Unit unit : units) {
            System.out.println("      - " + unit);
        }
    }

    private void reassignPointsForNextPhase(List<Unit> units, double points, String pointsType) {
        if (units == null || units.isEmpty()) return;

        double totalMax = units.stream().mapToDouble(u -> getUnitPoints(u, pointsType)).sum();

        if (points <= 0) {
            units.forEach(u -> setUnitPoints(u, pointsType, 0));
        } else if (points >= totalMax) {
            units.forEach(u -> setUnitPoints(u, pointsType, getUnitPoints(u, pointsType)));
        } else {
            for (Unit unit : units) {
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

    private double getUnitPoints(Unit unit, String pointsType) {
        return switch (pointsType) {
            case "PDF" -> unit.getPdf();
            case "PDC" -> unit.getPdc();
            case "ATK" -> unit.getAttack();
            default -> throw new IllegalArgumentException("Type de points inconnu : " + pointsType);
        };
    }

    private void setUnitPoints(Unit unit, String pointsType, double value) {
        switch (pointsType) {
            case "PDF" -> unit.setPdf(value);
            case "PDC" -> unit.setPdc(value);
            case "ATK" -> unit.setAttack(value);
            default -> throw new IllegalArgumentException("Type de points inconnu : " + pointsType);
        }
    }

    private double getAvailablePoints(List<Unit> units, String pointsType) {
        return units.stream().mapToDouble(u -> getUnitPoints(u, pointsType)).sum();
    }
}
