package com.mg.nmlonline.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AllianceGraph — fusion des camps et cibles de la ronde")
class AllianceGraphTest {

    private static final Long A = 1L;
    private static final Long B = 2L;
    private static final Long C = 3L;
    private static final Long D = 4L;

    @Test
    @DisplayName("Pont : A et C séparés, B s'abstient (aucune fusion avec un joueur à deux alliés présents)")
    void bridgeStaysAlone() {
        Map<Long, Set<Long>> allies = Map.of(A, Set.of(B), B, Set.of(A, C), C, Set.of(B));

        assertEquals(List.of(List.of(A), List.of(B), List.of(C)),
                AllianceGraph.camps(List.of(A, B, C), allies));

        assertEquals(List.of(List.of(A), List.of(B), List.of(C), List.of(D)),
                AllianceGraph.camps(List.of(A, B, C, D), allies),
                "B allié aux deux : personne ne fusionne quand D est présent");
    }

    @Test
    @DisplayName("Paires disjointes : A-B et C-D")
    void disjointPairs() {
        Map<Long, Set<Long>> allies = Map.of(A, Set.of(B), B, Set.of(A), C, Set.of(D), D, Set.of(C));
        assertEquals(List.of(List.of(A, B), List.of(C, D)),
                AllianceGraph.camps(List.of(A, B, C, D), allies));
    }

    @Test
    @DisplayName("Sans alliance : chaque joueur est un camp, la ronde suit l'ordre d'arrivée")
    void noAllianceKeepsOrder() {
        List<List<Long>> camps = AllianceGraph.camps(List.of(C, A, B), Map.of());
        assertEquals(List.of(List.of(C), List.of(A), List.of(B)), camps);
        assertArrayEquals(new int[]{1, 2, 0}, AllianceGraph.campTargets(camps, Map.of()));
    }

    @Test
    @DisplayName("Ronde A-B, B-C + D : A→C, B→D, C→D, D→A ; B ne frappe pas ses alliés")
    void ringSkipsAllies() {
        Map<Long, Set<Long>> allies = Map.of(A, Set.of(B), B, Set.of(A, C), C, Set.of(B));
        List<List<Long>> camps = AllianceGraph.camps(List.of(A, B, C, D), allies);
        assertArrayEquals(new int[]{2, 3, 3, 0}, AllianceGraph.campTargets(camps, allies));
    }

    @Test
    @DisplayName("Pont sans autre ennemi : B ne frappe personne (-1)")
    void bridgeWithoutEnemyPasses() {
        Map<Long, Set<Long>> allies = Map.of(A, Set.of(B), B, Set.of(A, C), C, Set.of(B));
        List<List<Long>> camps = AllianceGraph.camps(List.of(A, B, C), allies);
        assertArrayEquals(new int[]{2, -1, 0}, AllianceGraph.campTargets(camps, allies));
    }
}
