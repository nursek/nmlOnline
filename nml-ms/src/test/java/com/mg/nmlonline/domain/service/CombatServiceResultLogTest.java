package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.battle.BattleLogEntry;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EmbeddedPostgresTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("CombatService — bilan par joueur")
class CombatServiceResultLogTest {

    @Autowired
    private CombatService combatService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager txManager;

    private record World(Long allyOneId, Long allyTwoId, Long targetId, int sectorNumber) {
    }

    @Test
    @DisplayName("Le bilan sépare les survivants et les pertes de chaque joueur allié")
    void resultLogSeparatesAlliedPlayers() {
        World world = seed();
        CombatService.SectorBattleResult result = new TransactionTemplate(txManager).execute(status -> {
            Player allyOne = playerRepository.findById(world.allyOneId()).orElseThrow();
            Player allyTwo = playerRepository.findById(world.allyTwoId()).orElseThrow();
            Player target = playerRepository.findById(world.targetId()).orElseThrow();
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            return combatService.simulateSectorBattle(List.of(allyOne, allyTwo), List.of(target),
                    board, world.sectorNumber());
        });

        assertNotNull(result);
        assertTrue(result.success());
        List<String> messages = result.battleLog().stream().map(BattleLogEntry::message).toList();
        assertTrue(messages.contains("Attaquant — AllieUn (1 entité(s)) :"));
        assertTrue(messages.contains("Attaquant — AllieDeux (1 entité(s)) :"));
        assertTrue(messages.stream().anyMatch(message -> message.startsWith("Survivants AllieUn")),
                "Les survivants du premier allié sont sous son nom");
        assertTrue(messages.stream().anyMatch(message -> message.startsWith("Survivants AllieDeux")),
                "Les survivants du second allié sont sous son nom");
        assertTrue(messages.stream().anyMatch(message -> message.startsWith("Pertes Cible")),
                "Les pertes de la cible restent séparées");
        assertFalse(messages.stream().anyMatch(message -> message.contains("Unique id")));

        assertEquals(3, result.forces().size());
        assertEquals("ATTAQUANT", result.forces().getFirst().getSide());
        assertEquals(1, result.forces().getFirst().getInfantry().size());
        assertEquals(1, result.forces().get(1).getInfantry().size());
        assertEquals("DEFENSEUR", result.forces().get(2).getSide());
        assertEquals("WINNER", result.highlights().getFirst().getKind());
        assertFalse(result.highlights().isEmpty());
    }

    private World seed() {
        return new TransactionTemplate(txManager).execute(status -> {
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            Sector sector = board.getAllSectors().stream()
                    .filter(candidate -> candidate.isNeutral() && candidate.getArmySize() == 0
                            && candidate.getBuildings().isEmpty() && candidate.getCharacters().isEmpty())
                    .findFirst().orElseThrow();

            Player allyOne = new Player("AllieUn");
            allyOne.setBoardId(board.getId());
            playerRepository.save(allyOne);
            Player allyTwo = new Player("AllieDeux");
            allyTwo.setBoardId(board.getId());
            playerRepository.save(allyTwo);
            Player target = new Player("Cible");
            target.setBoardId(board.getId());
            playerRepository.save(target);
            em.flush();

            sector.addUnit(unit(allyOne.getId()));
            sector.addUnit(unit(allyTwo.getId()));
            Unit targetUnit = new Unit(0.0, UnitClass.TIREUR);
            targetUnit.setPlayerId(target.getId());
            sector.addUnit(targetUnit);

            em.flush();
            return new World(allyOne.getId(), allyTwo.getId(), target.getId(), sector.getNumber());
        });
    }

    private static Unit unit(Long playerId) {
        Unit unit = new Unit(8.0, UnitClass.TIREUR);
        unit.setPlayerId(playerId);
        return unit;
    }
}
