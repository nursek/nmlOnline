package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.press.EliminationEvent;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.EliminationEventRepository;
import com.mg.nmlonline.infrastructure.repository.GazetteIssueRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class EndGameScenarioSeeder {

    private static final String SURVIVOR = "nursek";

    private final BoardRepository boardRepository;
    private final PlayerRepository playerRepository;
    private final EliminationEventRepository eliminationEventRepository;
    private final GazetteIssueRepository gazetteIssueRepository;
    private final EliminationHistoryService eliminationHistoryService;
    private final PlayerEliminationService playerEliminationService;
    private final GameEndService gameEndService;
    private final GazetteDraftFactory gazetteDraftFactory;
    private final GazetteService gazetteService;
    private final TurnService turnService;
    private final TurnLock turnLock;

    public EndGameScenarioSeeder(BoardRepository boardRepository, PlayerRepository playerRepository,
                                 EliminationEventRepository eliminationEventRepository,
                                 GazetteIssueRepository gazetteIssueRepository,
                                 EliminationHistoryService eliminationHistoryService,
                                 PlayerEliminationService playerEliminationService,
                                 GameEndService gameEndService,
                                 GazetteDraftFactory gazetteDraftFactory,
                                 GazetteService gazetteService, TurnService turnService,
                                 TurnLock turnLock) {
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
        this.eliminationEventRepository = eliminationEventRepository;
        this.gazetteIssueRepository = gazetteIssueRepository;
        this.eliminationHistoryService = eliminationHistoryService;
        this.playerEliminationService = playerEliminationService;
        this.gameEndService = gameEndService;
        this.gazetteDraftFactory = gazetteDraftFactory;
        this.gazetteService = gazetteService;
        this.turnService = turnService;
        this.turnLock = turnLock;
    }

    @Transactional
    public ScenarioSummaryDto seedEndGame(Long boardId) {
        if (!turnLock.tryAcquire(boardId)) {
            throw new IllegalStateException("Une résolution est déjà en cours sur ce plateau");
        }
        try {
            return seedEndGameUnlocked(boardId);
        } finally {
            turnLock.release(boardId);
        }
    }

    private ScenarioSummaryDto seedEndGameUnlocked(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalStateException("Plateau " + boardId + " introuvable"));
        GameEndService.GameOutcome state = gameEndService.getState(boardId);
        if (state.finished()) {
            return summary(board, "Partie déjà terminée — " + state.winnerName()
                    + " est champion. Redémarre le backend (H2) pour rejouer.", null);
        }

        List<Player> players = playerRepository.findByBoardId(boardId);
        Player survivor = players.stream()
                .filter(player -> SURVIVOR.equals(player.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        SURVIVOR + " introuvable — vérifiez le seed démo"));
        if (survivor.isEliminated()) {
            throw new IllegalStateException(SURVIVOR + " est déjà éliminé — réinitialisez la démo");
        }

        int turn = board.getCurrentTurn();
        List<Player> victims = players.stream()
                .filter(player -> !Objects.equals(player.getId(), survivor.getId()))
                .sorted(Comparator.comparing(Player::getId))
                .toList();
        eliminationEventRepository.deleteByBoardId(boardId);
        gazetteIssueRepository.deleteByBoardId(boardId);

        List<EliminationHistoryService.EliminationSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < victims.size(); i++) {
            Player victim = victims.get(i);
            EliminationCause cause = i % 2 == 0
                    ? EliminationCause.MAIN_CHARACTER_KILLED
                    : EliminationCause.HEADQUARTERS_CAPTURED;
            snapshots.add(new EliminationHistoryService.EliminationSnapshot(
                    victim.getId(), victim.getName(), victim.getRace(),
                    victim.getCharacter() == null ? null : victim.getCharacter().getName(),
                    cause, survivor.getId(), survivor.getName(), List.of(), null));
            if (!victim.isEliminated()) {
                victim.setEliminated(true);
                victim.setEliminationCause(cause);
                victim.setEliminatedTurn(turn);
                victim.setEliminatedByPlayerId(survivor.getId());
                playerEliminationService.apply(board, victim, survivor.getId(), turn);
            }
        }

        List<EliminationEvent> events = eliminationHistoryService.save(boardId, turn, snapshots);
        GameEndService.GameOutcome outcome = gameEndService.finishIfOver(board, players, events);
        if (!outcome.finished()) {
            throw new IllegalStateException("La fin de partie n'a pas pu être déclenchée");
        }
        boardRepository.save(board);
        turnService.publishTurn(boardId, board.getCurrentTurn());

        GazetteDraftFactory.GazetteDraft draft = gazetteDraftFactory.build(board, turn, events,
                outcome, List.of(), players, List.of());
        gazetteService.createDraft(board, turn, draft);
        return summary(board, "Partie terminée — " + survivor.getName()
                        + " est le dernier survivant. Brouillon de la dernière édition créé pour le tour "
                        + turn + ".",
                List.of("Ouvrez /podium", "Rédigez et publiez /admin/journal/" + turn,
                        "Les autres joueurs sont verrouillés"));
    }

    private ScenarioSummaryDto summary(Board board, String message, List<String> observations) {
        ScenarioSummaryDto dto = new ScenarioSummaryDto();
        dto.setTurn(board.getCurrentTurn());
        dto.setMessage(message);
        dto.setObservations(observations);
        dto.setScenarioCode("END_GAME");
        return dto;
    }
}
