package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.GamePodiumDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.player.PlayerRace;
import com.mg.nmlonline.domain.model.press.EliminationEvent;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.EliminationEventRepository;
import com.mg.nmlonline.infrastructure.repository.GazetteIssueRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GamePodiumService")
class GamePodiumServiceTest {

    private final BoardRepository boardRepository = mock(BoardRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final EliminationEventRepository eliminationEventRepository =
            mock(EliminationEventRepository.class);
    private final GazetteIssueRepository gazetteIssueRepository = mock(GazetteIssueRepository.class);
    private final GamePodiumService service = new GamePodiumService(boardRepository,
            playerRepository, eliminationEventRepository, gazetteIssueRepository);

    @Test
    void placesSurvivorThenDeathsInReverseOrderWithStats() {
        Board board = finishedBoard(1L);
        Player nursek = player(1L, "nursek", false);
        Player lurio = player(2L, "lurio", true);
        Player trazyn = player(3L, "trazyn", true);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(playerRepository.findByBoardId(1L)).thenReturn(List.of(nursek, lurio, trazyn));
        when(eliminationEventRepository.findByBoardIdOrderByDeathIndexAsc(1L)).thenReturn(List.of(
                event(3L, "trazyn", 0, EliminationCause.HEADQUARTERS_CAPTURED, 2L, "lurio",
                        List.of(1L)),
                event(2L, "lurio", 1, EliminationCause.MAIN_CHARACTER_KILLED, 1L, "nursek",
                        List.of())));

        GamePodiumDto dto = service.getPodium(1L);

        assertTrue(dto.isFinished());
        assertEquals(3, dto.getTotalPlayers());
        assertEquals(1, dto.getActivePlayers());
        assertFalse(dto.isPosthumous());
        assertFalse(dto.isChampionAdjusted());
        assertEquals(1L, dto.getChampion().getPlayerId());
        assertEquals(List.of(1L, 2L, 3L), dto.getPlaces().stream()
                .map(GamePodiumDto.PlaceDto::getPlayerId).toList());
        GamePodiumDto.PlaceDto trazynPlace = dto.getPlaces().get(2);
        assertEquals(3, trazynPlace.getPlace());
        assertFalse(trazynPlace.isDefinitive());
        assertEquals(0, trazynPlace.getAssists());
        assertEquals(1, dto.getPlaces().get(0).getAssists());
        assertEquals(List.of("trazyn"), dto.getPlaces().get(0).getAssistedVictims());
        assertEquals(1, dto.getPlaces().get(1).getKills());
        assertTrue(dto.getPlaces().get(1).isDefinitive());
        assertNull(dto.getPlaces().get(0).getEliminatedTurn());
    }

    @Test
    void adjustedPosthumousChampionMovesToFirstPlace() {
        Board board = finishedBoard(3L);
        board.setWinnerAdjusted(true);
        Player lurio = player(2L, "lurio", true);
        Player trazyn = player(3L, "trazyn", true);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(playerRepository.findByBoardId(1L)).thenReturn(List.of(lurio, trazyn));
        when(eliminationEventRepository.findByBoardIdOrderByDeathIndexAsc(1L)).thenReturn(List.of(
                event(3L, "trazyn", 0, EliminationCause.HEADQUARTERS_CAPTURED, null, null, List.of()),
                event(2L, "lurio", 1, EliminationCause.MAIN_CHARACTER_KILLED, null, null, List.of())));

        GamePodiumDto dto = service.getPodium(1L);

        assertTrue(dto.isPosthumous());
        assertTrue(dto.isChampionAdjusted());
        assertEquals(3L, dto.getChampion().getPlayerId());
        assertEquals(List.of(3L, 2L), dto.getPlaces().stream()
                .map(GamePodiumDto.PlaceDto::getPlayerId).toList());
        assertEquals(2, dto.getPlaces().get(1).getPlace());
    }

    @Test
    void runningGameHasNoPlaces() {
        Board board = new Board();
        board.setId(1L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(playerRepository.findByBoardId(1L)).thenReturn(List.of(player(1L, "nursek", false),
                player(2L, "lurio", true)));

        GamePodiumDto dto = service.getPodium(1L);

        assertFalse(dto.isFinished());
        assertEquals(2, dto.getTotalPlayers());
        assertEquals(1, dto.getActivePlayers());
        assertNull(dto.getChampion());
        assertTrue(dto.getPlaces().isEmpty());
    }

    private static Board finishedBoard(Long winnerId) {
        Board board = new Board();
        board.setId(1L);
        board.setCurrentTurn(5);
        board.setFinishedAt(Instant.now());
        board.setWinnerPlayerId(winnerId);
        return board;
    }

    private static Player player(Long id, String name, boolean eliminated) {
        Player player = new Player(name);
        player.setId(id);
        player.setRace(PlayerRace.ORKS);
        if (eliminated) {
            player.eliminate(EliminationCause.MAIN_CHARACTER_KILLED, 5, 1L);
        }
        return player;
    }

    private static EliminationEvent event(Long victimId, String victimName, int index,
                                          EliminationCause cause, Long killerId, String killerName,
                                          List<Long> assists) {
        EliminationEvent event = new EliminationEvent();
        event.setVictimPlayerId(victimId);
        event.setVictimName(victimName);
        event.setVictimRace(PlayerRace.ORKS);
        event.setVictimCharacterName(victimName + "-char");
        event.setDeathIndex(index);
        event.setEliminatedTurn(5);
        event.setCause(cause);
        event.setKillerPlayerId(killerId);
        event.setKillerName(killerName);
        event.setAssistPlayerIds(assists.isEmpty() ? null
                : String.join(",", assists.stream().map(String::valueOf).toList()));
        return event;
    }
}
