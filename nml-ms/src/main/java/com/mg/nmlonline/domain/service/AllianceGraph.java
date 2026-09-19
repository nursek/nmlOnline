package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.alliance.Alliance;
import com.mg.nmlonline.infrastructure.repository.AllianceRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lecture partagée du graphe d'alliances par le mouvement et le combat.
 * {@code camps} et {@code campTargets} sont statiques et purs (testables sans Spring).
 */
@Component
public class AllianceGraph {

    private final AllianceRepository allianceRepository;

    public AllianceGraph(AllianceRepository allianceRepository) {
        this.allianceRepository = allianceRepository;
    }

    /** Adjacence joueur → alliés actifs. Chargée à chaque appel : volume faible, pas de cache à invalider. */
    public Map<Long, Set<Long>> activeAdjacency() {
        Map<Long, Set<Long>> adjacency = new HashMap<>();
        for (Alliance alliance : allianceRepository.findAllActive()) {
            adjacency.computeIfAbsent(alliance.getPlayerOneId(), key -> new HashSet<>())
                    .add(alliance.getPlayerTwoId());
            adjacency.computeIfAbsent(alliance.getPlayerTwoId(), key -> new HashSet<>())
                    .add(alliance.getPlayerOneId());
        }
        return adjacency;
    }

    public Set<Long> alliedPlayerIds(Long playerId) {
        return activeAdjacency().getOrDefault(playerId, Set.of());
    }

    public boolean areAllied(Long firstPlayerId, Long secondPlayerId) {
        return alliedPlayerIds(firstPlayerId).contains(secondPlayerId);
    }

    /** Bonus de trahison du briseur contre la victime, actif uniquement le tour de la trahison ; sans plafond. */
    public double betrayalBonusPercent(int turn, Long breakerPlayerId, Long victimPlayerId) {
        return allianceRepository.findBetrayalsAtTurn(turn).stream()
                .filter(alliance -> breakerPlayerId.equals(alliance.getEndedByPlayerId()))
                .filter(alliance -> alliance.involves(breakerPlayerId) && alliance.involves(victimPlayerId))
                .mapToDouble(alliance -> 15 + 10.0 * alliance.durationTurns())
                .findFirst()
                .orElse(0);
    }

    public List<Alliance> endedAlliancesAtTurn(int turn) {
        return allianceRepository.findEndedAtTurn(turn);
    }

    /**
     * Camps parmi les joueurs ordonnés : deux alliés fusionnent seulement si chacun n'a que l'autre
     * comme allié présent (sinon le joueur-pont reste seul et s'abstient).
     */
    public static List<List<Long>> camps(List<Long> orderedPlayers, Map<Long, Set<Long>> alliesOf) {
        Set<Long> assigned = new HashSet<>();
        List<List<Long>> camps = new ArrayList<>();
        for (Long playerId : orderedPlayers) {
            if (assigned.contains(playerId)) {
                continue;
            }
            Set<Long> presentAllies = new HashSet<>(alliesOf.getOrDefault(playerId, Set.of()));
            presentAllies.retainAll(orderedPlayers);
            Long partner = null;
            if (presentAllies.size() == 1) {
                Long candidate = presentAllies.iterator().next();
                Set<Long> candidateAllies = new HashSet<>(alliesOf.getOrDefault(candidate, Set.of()));
                candidateAllies.retainAll(orderedPlayers);
                if (!assigned.contains(candidate) && candidateAllies.size() == 1
                        && candidateAllies.contains(playerId)) {
                    partner = candidate;
                }
            }
            List<Long> camp = partner != null ? List.of(playerId, partner) : List.of(playerId);
            camps.add(camp);
            assigned.addAll(camp);
        }
        return camps;
    }

    /** Cible de chaque camp : le prochain camp non allié dans l'ordre circulaire ; -1 = personne (passe son tour). */
    public static int[] campTargets(List<List<Long>> camps, Map<Long, Set<Long>> alliesOf) {
        int[] targets = new int[camps.size()];
        for (int i = 0; i < camps.size(); i++) {
            targets[i] = -1;
            for (int offset = 1; offset < camps.size(); offset++) {
                int candidate = (i + offset) % camps.size();
                if (!campsAllied(camps.get(i), camps.get(candidate), alliesOf)) {
                    targets[i] = candidate;
                    break;
                }
            }
        }
        return targets;
    }

    private static boolean campsAllied(List<Long> first, List<Long> second, Map<Long, Set<Long>> alliesOf) {
        for (Long a : first) {
            for (Long b : second) {
                if (alliesOf.getOrDefault(a, Set.of()).contains(b)) {
                    return true;
                }
            }
        }
        return false;
    }
}
