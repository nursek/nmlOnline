package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.GamePodiumDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.player.PlayerRace;
import com.mg.nmlonline.domain.model.press.EliminationEvent;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.EliminationEventRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GamePodiumService {

    private final BoardRepository boardRepository;
    private final PlayerRepository playerRepository;
    private final EliminationEventRepository eliminationEventRepository;

    public GamePodiumService(BoardRepository boardRepository, PlayerRepository playerRepository,
                             EliminationEventRepository eliminationEventRepository) {
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
        this.eliminationEventRepository = eliminationEventRepository;
    }

    @Transactional(readOnly = true)
    public GamePodiumDto getPodium(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Plateau " + boardId + " introuvable"));
        List<Player> players = playerRepository.findByBoardId(boardId);
        if (!board.isFinished()) {
            return running(players);
        }
        List<EliminationEvent> events =
                eliminationEventRepository.findByBoardIdOrderByDeathIndexAsc(boardId);
        List<DeathEntry> deaths = deaths(players, events);
        List<Player> alive = players.stream().filter(player -> !player.isEliminated()).toList();
        if (alive.size() > 1) {
            throw new IllegalStateException("Partie terminée avec plusieurs survivants");
        }

        Stats stats = stats(events);
        int total = deaths.size() + alive.size();
        List<GamePodiumDto.PlaceDto> places = new ArrayList<>();
        if (alive.size() == 1) {
            places.add(alivePlace(alive.getFirst(), 1, stats));
            for (int i = deaths.size() - 1; i >= 0; i--) {
                places.add(deathPlace(deaths.get(i), total - i, stats));
            }
        } else {
            Long championId = board.getWinnerPlayerId();
            DeathEntry champion = deaths.stream()
                    .filter(death -> death.playerId().equals(championId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Champion posthume introuvable"));
            places.add(deathPlace(champion, 1, stats));
            int place = 2;
            for (int i = deaths.size() - 1; i >= 0; i--) {
                DeathEntry death = deaths.get(i);
                if (!death.playerId().equals(championId)) {
                    places.add(deathPlace(death, place++, stats));
                }
            }
        }
        places.sort(Comparator.comparingInt(GamePodiumDto.PlaceDto::getPlace));

        GamePodiumDto dto = new GamePodiumDto();
        dto.setFinished(true);
        dto.setFinalTurn(board.getCurrentTurn());
        dto.setFinishedAt(board.getFinishedAt() != null ? board.getFinishedAt().toString() : null);
        dto.setTotalPlayers(total);
        dto.setActivePlayers(alive.size());
        dto.setChampion(places.getFirst());
        dto.setPosthumous(!places.getFirst().isAlive());
        dto.setChampionAdjusted(board.isWinnerAdjusted() && dto.isPosthumous());
        dto.setPlaces(places);
        dto.setLastGazetteTurn(null);
        return dto;
    }

    private static GamePodiumDto running(List<Player> players) {
        GamePodiumDto dto = new GamePodiumDto();
        dto.setFinished(false);
        dto.setTotalPlayers(players.size());
        dto.setActivePlayers((int) players.stream().filter(player -> !player.isEliminated()).count());
        dto.setPlaces(List.of());
        return dto;
    }

    private List<DeathEntry> deaths(List<Player> players, List<EliminationEvent> events) {
        Map<Long, Player> playersById = new HashMap<>();
        players.forEach(player -> playersById.put(player.getId(), player));
        Set<Long> recorded = new HashSet<>();
        List<DeathEntry> deaths = new ArrayList<>();
        for (EliminationEvent event : events) {
            recorded.add(event.getVictimPlayerId());
            deaths.add(new DeathEntry(event.getVictimPlayerId(), event.getVictimName(),
                    event.getVictimRace(), event.getVictimCharacterName(), event.getEliminatedTurn(),
                    event.getCause(), event.getKillerPlayerId(), event.getKillerName(),
                    event.assistIds(), event.getDeathIndex()));
        }
        players.stream()
                .filter(player -> player.isEliminated() && !recorded.contains(player.getId()))
                .sorted(Comparator.comparing(Player::getId))
                .map(player -> new DeathEntry(player.getId(), player.getName(), player.getRace(),
                        null, player.getEliminatedTurn(), player.getEliminationCause(),
                        player.getEliminatedByPlayerId(), killerName(player.getEliminatedByPlayerId()),
                        List.of(), Integer.MAX_VALUE))
                .forEach(deaths::add);
        deaths.sort(Comparator.comparing(DeathEntry::eliminatedTurn,
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparingInt(DeathEntry::orderKey));
        return deaths;
    }

    private String killerName(Long killerId) {
        return killerId == null ? null : playerRepository.findById(killerId)
                .map(Player::getName)
                .orElse(null);
    }

    private Stats stats(List<EliminationEvent> events) {
        Stats stats = new Stats();
        for (EliminationEvent event : events) {
            if (event.getKillerPlayerId() != null) {
                stats.kills.merge(event.getKillerPlayerId(), 1, Integer::sum);
                stats.victims.computeIfAbsent(event.getKillerPlayerId(), key -> new ArrayList<>())
                        .add(event.getVictimName());
            }
            for (Long assistId : event.assistIds()) {
                stats.assists.merge(assistId, 1, Integer::sum);
                stats.assistedVictims.computeIfAbsent(assistId, key -> new ArrayList<>())
                        .add(event.getVictimName());
            }
        }
        return stats;
    }

    private GamePodiumDto.PlaceDto alivePlace(Player player, int place, Stats stats) {
        GamePodiumDto.PlaceDto dto = new GamePodiumDto.PlaceDto();
        dto.setPlace(place);
        dto.setPlayerId(player.getId());
        dto.setPlayerName(player.getName());
        dto.setRace(raceName(player.getRace()));
        dto.setCharacterName(player.getCharacter() != null ? player.getCharacter().getName() : null);
        dto.setAlive(true);
        applyStats(dto, player.getId(), stats);
        return dto;
    }

    private GamePodiumDto.PlaceDto deathPlace(DeathEntry death, int place, Stats stats) {
        GamePodiumDto.PlaceDto dto = new GamePodiumDto.PlaceDto();
        dto.setPlace(place);
        dto.setPlayerId(death.playerId());
        dto.setPlayerName(death.name());
        dto.setRace(raceName(death.race()));
        dto.setCharacterName(death.characterName());
        dto.setEliminatedTurn(death.eliminatedTurn());
        dto.setEliminationCause(death.cause() != null ? death.cause().name() : null);
        dto.setDefinitive(death.cause() == EliminationCause.MAIN_CHARACTER_KILLED);
        dto.setKillerPlayerId(death.killerId());
        dto.setKillerName(death.killerName());
        dto.setAlive(false);
        applyStats(dto, death.playerId(), stats);
        return dto;
    }

    private static void applyStats(GamePodiumDto.PlaceDto dto, Long playerId, Stats stats) {
        dto.setKills(stats.kills.getOrDefault(playerId, 0));
        dto.setAssists(stats.assists.getOrDefault(playerId, 0));
        dto.setVictims(stats.victims.getOrDefault(playerId, List.of()));
        dto.setAssistedVictims(stats.assistedVictims.getOrDefault(playerId, List.of()));
    }

    private static String raceName(PlayerRace race) {
        return race != null ? race.name() : null;
    }

    private record DeathEntry(Long playerId, String name, PlayerRace race, String characterName,
                              Integer eliminatedTurn, EliminationCause cause, Long killerId,
                              String killerName, List<Long> assists, int orderKey) {
    }

    private static final class Stats {
        final Map<Long, Integer> kills = new HashMap<>();
        final Map<Long, Integer> assists = new HashMap<>();
        final Map<Long, List<String>> victims = new HashMap<>();
        final Map<Long, List<String>> assistedVictims = new HashMap<>();
    }
}
