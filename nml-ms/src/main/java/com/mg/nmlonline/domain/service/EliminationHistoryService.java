package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.PlayerRace;
import com.mg.nmlonline.domain.model.press.EliminationEvent;
import com.mg.nmlonline.infrastructure.repository.EliminationEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class EliminationHistoryService {

    private final EliminationEventRepository repository;

    public EliminationHistoryService(EliminationEventRepository repository) {
        this.repository = repository;
    }

    public List<EliminationEvent> save(Long boardId, int turn, List<EliminationSnapshot> snapshots) {
        int nextIndex = Math.toIntExact(repository.countByBoardId(boardId));
        List<EliminationEvent> events = new ArrayList<>(snapshots.size());
        for (EliminationSnapshot snapshot : snapshots) {
            EliminationEvent event = new EliminationEvent();
            event.setBoardId(boardId);
            event.setDeathIndex(nextIndex++);
            event.setVictimPlayerId(snapshot.victimId());
            event.setVictimName(snapshot.victimName());
            event.setVictimRace(snapshot.race());
            event.setVictimCharacterName(snapshot.characterName());
            event.setEliminatedTurn(turn);
            event.setCause(snapshot.cause());
            event.setKillerPlayerId(snapshot.killerId());
            event.setKillerName(snapshot.killerName());
            event.setAssistPlayerIds(join(snapshot.assistIds()));
            event.setBattleSector(snapshot.battleSector());
            events.add(event);
        }
        return repository.saveAll(events);
    }

    private static String join(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }

    public record EliminationSnapshot(Long victimId, String victimName, PlayerRace race,
                                      String characterName, EliminationCause cause, Long killerId,
                                      String killerName, List<Long> assistIds, Integer battleSector) {
    }
}
