package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.PlayerRace;
import com.mg.nmlonline.domain.model.press.EliminationEvent;
import com.mg.nmlonline.infrastructure.repository.EliminationEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("EliminationHistoryService")
class EliminationHistoryServiceTest {

    private final EliminationEventRepository repository = mock(EliminationEventRepository.class);
    private final EliminationHistoryService service = new EliminationHistoryService(repository);

    @Test
    void snapshotsBecomeIndexedEventsBeforeCleanup() {
        when(repository.countByBoardId(1L)).thenReturn(4L);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        EliminationHistoryService.EliminationSnapshot first =
                new EliminationHistoryService.EliminationSnapshot(7L, "lurio", PlayerRace.ORKS,
                        "Ratcatcher", EliminationCause.MAIN_CHARACTER_KILLED, 8L, "nursek",
                        List.of(9L, 10L), 43);
        EliminationHistoryService.EliminationSnapshot second =
                new EliminationHistoryService.EliminationSnapshot(8L, "nursek", PlayerRace.NECRONS,
                        null, EliminationCause.HEADQUARTERS_CAPTURED, null, null, List.of(), null);

        List<EliminationEvent> events = service.save(1L, 5, List.of(first, second));

        assertEquals(2, events.size());
        EliminationEvent event = events.getFirst();
        assertEquals(1L, event.getBoardId());
        assertEquals(4, event.getDeathIndex());
        assertEquals(5, event.getEliminatedTurn());
        assertEquals(7L, event.getVictimPlayerId());
        assertEquals("lurio", event.getVictimName());
        assertEquals(PlayerRace.ORKS, event.getVictimRace());
        assertEquals("Ratcatcher", event.getVictimCharacterName());
        assertEquals(EliminationCause.MAIN_CHARACTER_KILLED, event.getCause());
        assertEquals(8L, event.getKillerPlayerId());
        assertEquals("nursek", event.getKillerName());
        assertEquals("9,10", event.getAssistPlayerIds());
        assertEquals(43, event.getBattleSector());
        assertEquals(5, events.get(1).getDeathIndex());
        assertNull(events.get(1).getAssistPlayerIds());
    }
}
