package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.battle.BattleLogEntry;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitType;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedPostgresTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("CombatService — expérience de combat")
class CombatExperienceTest {

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

    private record World(Long attackerId, Long defenderId, int sectorNumber,
                         Long attackerLarbinId, Long attackerBruteId) {
    }

    private World seed(boolean withEnemyCharacter) {
        return new TransactionTemplate(txManager).execute(status -> {
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            Sector sector = board.getAllSectors().stream()
                    .filter(s -> s.isNeutral() && s.getArmySize() == 0
                            && s.getBuildings().isEmpty() && s.getCharacters().isEmpty())
                    .findFirst().orElseThrow();

            Player attacker = new Player("AttaquantExp");
            playerRepository.save(attacker);
            Player defender = new Player("DefenseurExp");
            playerRepository.save(defender);
            em.flush();

            Unit brute = new Unit(8.0, UnitClass.TIREUR);
            brute.setPlayerId(attacker.getId());
            sector.addUnit(brute);

            Equipment shield = new Equipment("BouclierExp", 100, 0, 0, 100, 0,
                    Set.of(UnitClass.TIREUR), EquipmentCategory.DEFENSIVE);
            em.persist(shield);
            Unit larbin = new Unit(1.0, UnitClass.TIREUR);
            larbin.setPlayerId(attacker.getId());
            larbin.addEquipment(shield);
            sector.addUnit(larbin);

            Unit defenderUnit = new Unit(0.0, UnitClass.TIREUR);
            defenderUnit.setPlayerId(defender.getId());
            sector.addUnit(defenderUnit);

            if (withEnemyCharacter) {
                GameCharacter character = new GameCharacter("HerosExp", 30, 0, 0, 30, 0, 0);
                character.setPlayerId(defender.getId());
                character.setSector(sector);
                defender.setCharacter(character);
            }

            em.flush();
            return new World(attacker.getId(), defender.getId(), sector.getNumber(),
                    larbin.getId(), brute.getId());
        });
    }

    private CombatService.SectorBattleResult runBattle(World w) {
        return new TransactionTemplate(txManager).execute(status -> {
            Player attacker = playerRepository.findById(w.attackerId()).orElseThrow();
            Player defender = playerRepository.findById(w.defenderId()).orElseThrow();
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            return combatService.simulateSectorBattle(List.of(attacker), List.of(defender), board, w.sectorNumber());
        });
    }

    private CombatService.ExperienceGain gainOf(CombatService.SectorBattleResult result, Long unitId) {
        return result.experienceGains().stream()
                .filter(gain -> gain.unitId().equals(unitId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aucun gain d'expérience pour l'unité " + unitId));
    }

    @Test
    @DisplayName("Participation : le tireur gagne 1 Exp, la cible seulement entamée 0,5 Exp")
    void survivorGainsExperienceByParticipation() {
        World w = seed(false);

        CombatService.SectorBattleResult r = runBattle(w);

        assertTrue(r.success());
        assertEquals(w.attackerId(), r.winner().getId(), "Le défenseur anéanti ⇒ victoire attaquant");
        assertEquals(2, r.experienceGains().size(), "Les deux unités attaquantes survivent");

        CombatService.ExperienceGain larbinGain = gainOf(r, w.attackerLarbinId());
        assertEquals(0.5, larbinGain.gained(), "Dégâts encaissés mais aucun tir : 0,5 Exp");
        assertEquals(1.0, larbinGain.experienceBefore());
        assertEquals(1.5, larbinGain.experienceAfter());
        assertEquals(UnitType.LARBIN, larbinGain.typeBefore());
        assertEquals(UnitType.LARBIN, larbinGain.typeAfter(), "1,5 Exp : pas encore d'évolution");

        CombatService.ExperienceGain bruteGain = gainOf(r, w.attackerBruteId());
        assertEquals(1.0, bruteGain.gained(), "Tir + cible détruite, plafonné à 1 Exp");
        assertEquals(9.0, bruteGain.experienceAfter());

        assertTrue(r.battleLog().stream().anyMatch(e -> "Résultat".equals(e.phase())
                        && BattleLogEntry.GAIN.equals(e.outcome()) && e.message().contains("10 dégâts encaissés")),
                "La section Résultat trace la participation de l'unité");
        assertTrue(r.battleLog().stream().anyMatch(e -> "Résultat".equals(e.phase())
                        && BattleLogEntry.LOSS.equals(e.outcome()) && e.message().contains("Pertes DefenseurExp")),
                "La section Résultat liste les pertes du camp vaincu");
        assertTrue(r.battleLog().stream().anyMatch(e -> "Résultat".equals(e.phase())
                        && e.message().contains("Survivants AttaquantExp")),
                "La section Résultat liste les survivants avec leurs stats");
    }

    @Test
    @DisplayName("Personnage ennemi éliminé : +1 Exp supplémentaire par survivant")
    void enemyCharacterKilledGrantsBonusExperience() {
        World w = seed(true);

        CombatService.SectorBattleResult r = runBattle(w);

        assertTrue(r.success());
        assertTrue(r.defenderCharacterLost());
        assertEquals(2, r.experienceGains().size());
        assertEquals(List.of(2.0, 1.5),
                r.experienceGains().stream().map(CombatService.ExperienceGain::gained).toList(),
                "Premier survivant : participation plafonnée (+1) + personnage (+1) ; second : dégâts encaissés (+0,5) + personnage (+1)");
        assertEquals(2.5, gainOf(r, w.attackerLarbinId()).experienceAfter());
        assertTrue(r.casualtyDetails().stream()
                        .anyMatch(casualty -> "CHARACTER".equals(casualty.category())
                                && "HerosExp".equals(casualty.label())),
                "Le personnage est listé dans les pertes");
    }
}
