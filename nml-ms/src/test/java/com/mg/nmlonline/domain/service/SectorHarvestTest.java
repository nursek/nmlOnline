package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.PlayerActionDto;
import com.mg.nmlonline.config.TestDataInitializer;
import com.mg.nmlonline.domain.exception.HarvestClosedException;
import com.mg.nmlonline.domain.exception.PlayerActionUndoException;
import com.mg.nmlonline.domain.model.action.PlayerActionType;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedPostgresTest
@Transactional
@DisplayName("Récolte des revenus par secteur — argent ou ressource")
class SectorHarvestTest {

    @Autowired
    private PlayerActionService playerActionService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private BoardService boardService;

    @Autowired
    private TurnService turnService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private HarvestAutoCollector harvestAutoCollector;

    @Autowired
    private MovementAdminService movementAdminService;

    @Autowired
    private TurnLock turnLock;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void pinSharedBoardState() {
        // Board partagé par toute la suite : une classe commitante (TurnServiceTest…) a pu laisser turn/revenueClaimedTurn.
        Board board = boardService.getAllBoards().stream().findFirst().orElseThrow();
        board.setCurrentTurn(1);
        board.setRevenueClaimedTurn(null);
        entityManager.flush();
        turnService.invalidateTurnCache();
    }

    @AfterEach
    void clearTurnCache() {
        // Le tour modifié par un test est rollbacké, mais cachedTurn (singleton) ne l'est pas.
        turnService.invalidateTurnCache();
    }

    @Test
    @DisplayName("Tour 2 : un secteur donne son revenu, un autre 1 ressource, et un secteur n'est récolté qu'une fois")
    void harvestCreditsEachSectorOnce() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        double moneyBefore = player.getStats().getMoney();

        assertThrows(IllegalStateException.class, () -> playerActionService.harvest(player.getUserId(),
                PlayerActionType.HARVEST_MONEY, List.of(1)), "Aucune récolte au tour 1");

        Board board = turnTwoBoard();
        Sector moneySector = ownedSector(board, player, 1, 100.0, "Or");
        Sector resourceSector = ownedSector(board, player, 2, 200.0, "Ivoire");

        assertThrows(SecurityException.class, () -> playerActionService.harvest(player.getUserId(),
                PlayerActionType.HARVEST_MONEY, List.of(3)), "Secteur non possédé");

        playerActionService.harvest(player.getUserId(),
                PlayerActionType.HARVEST_MONEY, List.of(moneySector.getNumber()));
        List<PlayerActionDto> actions = playerActionService.harvest(player.getUserId(),
                PlayerActionType.HARVEST_RESOURCE, List.of(resourceSector.getNumber()));

        assertEquals(2, actions.size());
        assertEquals(moneyBefore + 100.0, player.getStats().getMoney(), 0.001);
        assertEquals(1, player.getResourceQuantity("Ivoire"));
        assertEquals(0, player.getResourceQuantity("Or"));

        assertThrows(IllegalStateException.class, () -> playerActionService.harvest(player.getUserId(),
                PlayerActionType.HARVEST_RESOURCE, List.of(moneySector.getNumber())));
    }

    @Test
    @DisplayName("Annulation : possible tant que la ressource est en stock, refusée après transfert")
    void undoSellRefusedOnceResourceTransferred() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        Board board = turnTwoBoard();
        Sector sector = ownedSector(board, player, 4, 100.0, "Or");

        playerActionService.harvest(player.getUserId(), PlayerActionType.HARVEST_RESOURCE, List.of(4));
        assertEquals(1, player.getResourceQuantity("Or"));

        Long actionId = playerActionService.getCurrentTurnActions(player.getUserId()).getFirst().getId();
        playerActionService.undoFrom(player.getUserId(), actionId);
        assertEquals(0, player.getResourceQuantity("Or"));

        playerActionService.harvest(player.getUserId(), PlayerActionType.HARVEST_RESOURCE, List.of(4));
        Player receiver = playerOfTestUser(TestDataInitializer.USER_2);
        assertTrue(resourceService.transferResource(player, receiver, "Or", 1));

        Long secondActionId = playerActionService.getCurrentTurnActions(player.getUserId()).getFirst().getId();
        assertThrows(PlayerActionUndoException.class,
                () -> playerActionService.undoFrom(player.getUserId(), secondActionId));
    }

    @Test
    @DisplayName("Auto-crédit : seuls les secteurs non récoltés versent leur revenu, à partir du tour 2")
    void autoCollectSkipsHarvestedSectors() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        double moneyBefore = player.getStats().getMoney();
        Board board = turnTwoBoard();
        ownedSector(board, player, 5, 100.0, "Or");
        Sector resourceSector = ownedSector(board, player, 6, 200.0, "Ivoire");

        playerActionService.harvest(player.getUserId(), PlayerActionType.HARVEST_RESOURCE,
                List.of(resourceSector.getNumber()));

        harvestAutoCollector.collectRemainingMoney(board, 1);
        assertEquals(moneyBefore, player.getStats().getMoney(), 0.001, "Tour 1 : aucun revenu versé");

        harvestAutoCollector.collectRemainingMoney(board, 2);
        assertEquals(moneyBefore + 100.0, player.getStats().getMoney(), 0.001,
                "Seul le secteur non récolté verse son revenu");
    }

    @Test
    @DisplayName("Secteur capturé pendant la résolution : le revenu va au propriétaire du début, pas au vainqueur")
    void autoCollectPaysOwnerAtResolutionStart() {
        Player owner = playerOfTestUser(TestDataInitializer.USER_1);
        Player winner = playerOfTestUser(TestDataInitializer.USER_2);
        Board board = turnTwoBoard();
        Sector sector = ownedSector(board, owner, 7, 100.0, "Or");
        Map<Integer, Long> startOwners = Map.of(sector.getNumber(), owner.getId());
        double ownerBefore = owner.getStats().getMoney();
        double winnerBefore = winner.getStats().getMoney();

        sector.setOwnerId(winner.getId());
        entityManager.flush();

        harvestAutoCollector.collectRemainingMoney(board, 2, startOwners);

        assertEquals(ownerBefore + 100.0, owner.getStats().getMoney(), 0.001);
        assertEquals(winnerBefore, winner.getStats().getMoney(), 0.001, "Le vainqueur ne touche pas le revenu");
    }

    @Test
    @DisplayName("Raccourci admin : le claim précède les mouvements et advanceTurn ne le rejoue pas")
    void adminResolveMovementsClaimsRevenueOnce() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        Board board = turnTwoBoard();
        ownedSector(board, player, 9, 100.0, "Or");
        double before = player.getStats().getMoney();

        movementAdminService.resolveMovements(2);
        assertEquals(before + 100.0, player.getStats().getMoney(), 0.001);
        assertEquals(2, board.getRevenueClaimedTurn().intValue(), "Le claim du tour est persisté sur le plateau");

        turnService.advanceTurnAndReport();
        assertEquals(before + 100.0, player.getStats().getMoney(), 0.001,
                "Le claim du raccourci ne doit pas être versé une seconde fois");
    }

    @Test
    @DisplayName("Annulation d'une récolte refusée quand le revenu du tour est déjà versé")
    void undoHarvestRefusedOnceRevenueClaimed() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        Board board = turnTwoBoard();
        ownedSector(board, player, 11, 100.0, "Or");

        playerActionService.harvest(player.getUserId(), PlayerActionType.HARVEST_MONEY, List.of(11));
        double afterHarvest = player.getStats().getMoney();
        Long actionId = playerActionService.getCurrentTurnActions(player.getUserId()).getFirst().getId();

        movementAdminService.resolveMovements(2);

        assertThrows(HarvestClosedException.class,
                () -> playerActionService.undoFrom(player.getUserId(), actionId));
        assertEquals(afterHarvest, player.getStats().getMoney(), 0.001, "L'argent récolté reste crédité");
    }

    @Test
    @DisplayName("Annulation d'une récolte d'argent refusée si le revenu a été dépensé")
    void undoHarvestMoneyRefusedOnceSpent() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        Board board = turnTwoBoard();
        ownedSector(board, player, 10, 100.0, "Or");

        playerActionService.harvest(player.getUserId(), PlayerActionType.HARVEST_MONEY, List.of(10));
        Long actionId = playerActionService.getCurrentTurnActions(player.getUserId()).getFirst().getId();

        player.getStats().setMoney(0);

        assertThrows(PlayerActionUndoException.class,
                () -> playerActionService.undoFrom(player.getUserId(), actionId));
    }

    @Test
    @DisplayName("Récolte manuelle fermée pendant une résolution de tour")
    void harvestClosedWhileResolving() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        Board board = turnTwoBoard();
        ownedSector(board, player, 8, 100.0, "Or");

        turnLock.tryAcquire();
        try {
            assertThrows(HarvestClosedException.class, () -> playerActionService.harvest(player.getUserId(),
                    PlayerActionType.HARVEST_MONEY, List.of(8)));
        } finally {
            turnLock.release();
        }
    }

    private Board turnTwoBoard() {
        Board board = boardService.getAllBoards().stream().findFirst().orElseThrow();
        board.setCurrentTurn(2);
        turnService.publishTurn(2);
        return board;
    }

    private Sector ownedSector(Board board, Player player, int number, double income, String resource) {
        Sector sector = board.getSector(number);
        sector.setOwnerId(player.getId());
        sector.setIncome(income);
        sector.setResourceName(resource);
        entityManager.flush();
        return sector;
    }

    private Player playerOfTestUser(String username) {
        User user = userRepository.findByUsername(username);
        assertNotNull(user, "L'utilisateur de test " + username + " doit exister");
        Player player = playerService.findByUserId(user.getId());
        assertNotNull(player, "Le joueur lié à " + username + " doit exister");
        return player;
    }
}
