package com.mg.nmlonline.domain.model.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerEliminationTest {

    @Test
    void headquartersCaptureIsNotOverriddenByCharacterDeath() {
        Player player = new Player("Priorite");

        player.eliminate(EliminationCause.HEADQUARTERS_CAPTURED, 3, 7L);
        player.eliminate(EliminationCause.MAIN_CHARACTER_KILLED, 3, 9L);

        assertTrue(player.isEliminated());
        assertEquals(EliminationCause.HEADQUARTERS_CAPTURED, player.getEliminationCause());
        assertEquals(7L, player.getEliminatedByPlayerId(), "Le capturant du QG reste le tueur");
    }

    @Test
    void headquartersCaptureOverridesEarlierCharacterDeath() {
        Player player = new Player("Conquete");

        player.eliminate(EliminationCause.MAIN_CHARACTER_KILLED, 3);
        player.eliminate(EliminationCause.HEADQUARTERS_CAPTURED, 3, 9L);

        assertEquals(EliminationCause.HEADQUARTERS_CAPTURED, player.getEliminationCause());
        assertEquals(9L, player.getEliminatedByPlayerId());
    }
}
