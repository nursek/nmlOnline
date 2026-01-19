package com.mg.nmlonline.domain.integration;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.service.CombatService;
import com.mg.nmlonline.domain.service.PlayerStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour le scénario complet de simulation de jeu.
 * Couvre exactement ce que couvrait PlayerDemo :
 * - Création de joueurs
 * - Création du Board avec secteurs
 * - Configuration des voisins en grille
 * - Assignation des ressources
 * - Attribution des couleurs
 * - Détection des conflits
 * - Simulation de bataille
 */
@DisplayName("Game Simulation Integration Tests")
class GameSimulationIT {

    private Player player1;
    private Player player2;
    private Player player3;
    private Board board;
    private PlayerStatsService playerStatsService;
    private CombatService combatService;

    @BeforeEach
    void setUp() {
        // Créer les services
        playerStatsService = new PlayerStatsService();
        combatService = new CombatService();

        // Injection manuelle de PlayerStatsService dans CombatService (comme dans PlayerDemo)
        try {
            Field field = CombatService.class.getDeclaredField("playerStatsService");
            field.setAccessible(true);
            field.set(combatService, playerStatsService);
        } catch (Exception e) {
            fail("Erreur d'initialisation: " + e.getMessage());
        }

        // Créer les joueurs
        player1 = new Player("Général Suprême");
        player1.setId(1L);
        player1.getStats().setMoney(20000.0);

        player2 = new Player("Baron de Guerre");
        player2.setId(2L);
        player2.getStats().setMoney(15000.0);

        player3 = new Player("Seigneur des Ombres");
        player3.setId(3L);
        player3.getStats().setMoney(18000.0);

        // Créer le board
        board = new Board();
    }

    @Nested
    @DisplayName("Étape 1: Création et configuration des joueurs")
    class PlayerCreationTests {

        @Test
        @DisplayName("Joueurs créés avec noms et argent corrects")
        void shouldCreatePlayersWithCorrectData() {
            assertEquals("Général Suprême", player1.getName());
            assertEquals(20000.0, player1.getStats().getMoney());
            assertEquals(1L, player1.getId());

            assertEquals("Baron de Guerre", player2.getName());
            assertEquals(15000.0, player2.getStats().getMoney());
            assertEquals(2L, player2.getId());

            assertEquals("Seigneur des Ombres", player3.getName());
            assertEquals(18000.0, player3.getStats().getMoney());
            assertEquals(3L, player3.getId());
        }
    }

    @Nested
    @DisplayName("Étape 2: Création du Board avec secteurs")
    class BoardCreationTests {

        @BeforeEach
        void setUpBoard() {
            setupCompleteBoard();
        }

        @Test
        @DisplayName("Board créé avec 16 secteurs")
        void shouldCreateBoardWith16Sectors() {
            assertEquals(16, board.getSectorCount());
        }

        @Test
        @DisplayName("Secteurs répartis entre les 3 joueurs")
        void shouldDistributeSectorsBetweenPlayers() {
            List<Sector> p1Sectors = board.getSectorsByOwner(1L);
            List<Sector> p2Sectors = board.getSectorsByOwner(2L);
            List<Sector> p3Sectors = board.getSectorsByOwner(3L);
            List<Sector> neutralSectors = board.getNeutralSectors();

            // Vérifier que les secteurs sont bien assignés
            assertFalse(p1Sectors.isEmpty(), "Joueur 1 devrait avoir des secteurs");
            assertFalse(p2Sectors.isEmpty(), "Joueur 2 devrait avoir des secteurs");
            assertFalse(p3Sectors.isEmpty(), "Joueur 3 devrait avoir des secteurs");

            // Le total doit faire 16
            int totalAssigned = p1Sectors.size() + p2Sectors.size() + p3Sectors.size() + neutralSectors.size();
            assertEquals(16, totalAssigned);
        }

        @Test
        @DisplayName("Couleurs correctement assignées")
        void shouldAssignCorrectColors() {
            for (Sector sector : board.getSectorsByOwner(1L)) {
                assertEquals("#FF0000", sector.getColor());
            }
            for (Sector sector : board.getSectorsByOwner(2L)) {
                assertEquals("#0000FF", sector.getColor());
            }
            for (Sector sector : board.getSectorsByOwner(3L)) {
                assertEquals("#00FF00", sector.getColor());
            }
        }
    }

    @Nested
    @DisplayName("Étape 3: Configuration des voisins en grille 4x4")
    class NeighborConfigurationTests {

        @BeforeEach
        void setUpBoardWithNeighbors() {
            setupCompleteBoard();
            setupGridNeighbors(4);
        }

        @Test
        @DisplayName("Secteurs adjacents sont voisins horizontalement")
        void shouldHaveHorizontalNeighbors() {
            assertTrue(board.areNeighbors(1, 2), "Secteur 1 et 2 devraient être voisins");
            assertTrue(board.areNeighbors(2, 3), "Secteur 2 et 3 devraient être voisins");
            assertTrue(board.areNeighbors(3, 4), "Secteur 3 et 4 devraient être voisins");
        }

        @Test
        @DisplayName("Secteurs adjacents sont voisins verticalement")
        void shouldHaveVerticalNeighbors() {
            assertTrue(board.areNeighbors(1, 5), "Secteur 1 et 5 devraient être voisins");
            assertTrue(board.areNeighbors(5, 9), "Secteur 5 et 9 devraient être voisins");
            assertTrue(board.areNeighbors(9, 13), "Secteur 9 et 13 devraient être voisins");
        }

        @Test
        @DisplayName("Secteurs non adjacents ne sont pas voisins")
        void shouldNotHaveDiagonalNeighbors() {
            assertFalse(board.areNeighbors(1, 6), "Secteur 1 et 6 ne devraient pas être voisins (diagonale)");
            assertFalse(board.areNeighbors(1, 16), "Secteur 1 et 16 ne devraient pas être voisins (éloignés)");
        }

        @Test
        @DisplayName("Coins ont le bon nombre de voisins")
        void shouldHaveCorrectNeighborCountForCorners() {
            // Coin supérieur gauche (secteur 1) : 2 voisins
            assertEquals(2, board.getSector(1).getNeighbors().size());

            // Coin supérieur droit (secteur 4) : 2 voisins
            assertEquals(2, board.getSector(4).getNeighbors().size());

            // Coin inférieur gauche (secteur 13) : 2 voisins
            assertEquals(2, board.getSector(13).getNeighbors().size());

            // Coin inférieur droit (secteur 16) : 2 voisins
            assertEquals(2, board.getSector(16).getNeighbors().size());
        }

        @Test
        @DisplayName("Secteurs centraux ont 4 voisins")
        void shouldHave4NeighborsForCenterSectors() {
            // Secteur 6 (position 1,1 dans grille 0-indexed) : 4 voisins
            assertEquals(4, board.getSector(6).getNeighbors().size());
        }
    }

    @Nested
    @DisplayName("Étape 4: Assignation des ressources")
    class ResourceAssignmentTests {

        @BeforeEach
        void setUpBoardWithResources() {
            setupCompleteBoard();
            assignResources();
        }

        @Test
        @DisplayName("Ressource or assignée au secteur 1")
        void shouldAssignGoldToSector1() {
            String resourceName = board.getSector(1).getResourceName();
            assertNotNull(resourceName);
            assertEquals("Or", resourceName);
        }

        @Test
        @DisplayName("Ressource joyaux assignée au secteur 5")
        void shouldAssignJewelsToSector5() {
            String resourceName = board.getSector(5).getResourceName();
            assertNotNull(resourceName);
            assertEquals("Joyaux", resourceName);
        }

        @Test
        @DisplayName("Ressource cigares assignée au secteur 9")
        void shouldAssignCigarsToSector9() {
            String resourceName = board.getSector(9).getResourceName();
            assertNotNull(resourceName);
            assertEquals("Cigares", resourceName);
        }

        @Test
        @DisplayName("Ressource uranium assignée au secteur 13")
        void shouldAssignUraniumToSector13() {
            String resourceName = board.getSector(13).getResourceName();
            assertNotNull(resourceName);
            assertEquals("Uranium", resourceName);
        }

        @Test
        @DisplayName("Secteurs sans ressource ont null ou vide")
        void shouldHaveNullResourceForOtherSectors() {
            String resourceName = board.getSector(2).getResourceName();
            assertTrue(resourceName == null || resourceName.isEmpty());
        }
    }

    @Nested
    @DisplayName("Étape 5: Détection des conflits")
    class ConflictDetectionTests {

        @BeforeEach
        void setUpBoardForConflicts() {
            setupCompleteBoard();
            setupGridNeighbors(4);
        }

        @Test
        @DisplayName("Détection conflit entre secteurs ennemis voisins")
        void shouldDetectConflictBetweenEnemyNeighbors() {
            // Secteurs 2 (joueur 1) et 3 (joueur 2) sont voisins
            // Ils devraient avoir un conflit si assignés à des joueurs différents
            // Dans notre setup, secteur 2 est au joueur 1 et secteur 6 au joueur 2

            // On vérifie un cas de conflit réel
            if (board.areNeighbors(2, 6)) {
                Sector s2 = board.getSector(2);
                Sector s6 = board.getSector(6);
                if (!s2.getOwnerId().equals(s6.getOwnerId())) {
                    assertTrue(board.hasConflict(2, 6));
                }
            }
        }

        @Test
        @DisplayName("Pas de conflit entre secteurs alliés")
        void shouldNotDetectConflictBetweenAllies() {
            // Deux secteurs du même joueur
            List<Sector> p1Sectors = board.getSectorsByOwner(1L);
            if (p1Sectors.size() >= 2) {
                int s1 = p1Sectors.get(0).getNumber();
                int s2 = p1Sectors.get(1).getNumber();
                assertFalse(board.hasConflict(s1, s2));
            }
        }

        @Test
        @DisplayName("Pas de conflit avec secteur neutre")
        void shouldNotDetectConflictWithNeutral() {
            List<Sector> neutralSectors = board.getNeutralSectors();
            if (!neutralSectors.isEmpty()) {
                int neutralNum = neutralSectors.get(0).getNumber();
                List<Sector> p1Sectors = board.getSectorsByOwner(1L);
                if (!p1Sectors.isEmpty()) {
                    int p1Num = p1Sectors.get(0).getNumber();
                    assertFalse(board.hasConflict(neutralNum, p1Num));
                }
            }
        }
    }

    @Nested
    @DisplayName("Étape 6: Simulation de bataille complète")
    class BattleSimulationTests {

        @BeforeEach
        void setUpBattleScenario() {
            setupCompleteBoard();
            setupGridNeighbors(4);
            addArmiesToSectors();
        }

        @Test
        @DisplayName("Bataille échoue si défenseur n'a pas d'armée")
        void shouldFailBattleIfDefenderHasNoArmy() {
            Player noArmyPlayer = new Player("Sans Armée");
            noArmyPlayer.setId(99L);

            CombatService.BattleResult result = combatService.simulateBattle(player1, noArmyPlayer, board);

            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("Bataille réussie entre joueurs avec armées")
        void shouldSucceedBattleBetweenPlayersWithArmies() {
            CombatService.BattleResult result = combatService.simulateBattle(player2, player3, board);

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Résultat de bataille a un vainqueur ou un message")
        void shouldHaveWinnerOrMessage() {
            CombatService.BattleResult result = combatService.simulateBattle(player2, player3, board);

            assertNotNull(result.getMessage());
        }
    }

    @Nested
    @DisplayName("Calcul des statistiques")
    class StatsCalculationTests {

        @BeforeEach
        void setUpStatsScenario() {
            setupCompleteBoard();
            addArmiesToSectors();
        }

        @Test
        @DisplayName("Recalcul des stats met à jour les revenus")
        void shouldRecalculateIncome() {
            playerStatsService.recalculateStats(player1, board);

            double totalIncome = player1.getStats().getTotalIncome();
            assertTrue(totalIncome > 0);
        }

        @Test
        @DisplayName("Recalcul des stats met à jour la puissance de combat")
        void shouldRecalculateCombatPower() {
            playerStatsService.recalculateStats(player1, board);

            double totalAtk = player1.getStats().getTotalAtk();
            assertTrue(totalAtk >= 0);
        }

        @Test
        @DisplayName("Comptage correct du nombre de secteurs par joueur")
        void shouldCountSectorsCorrectly() {
            int p1Count = board.getSectorsByOwner(1L).size();
            int p2Count = board.getSectorsByOwner(2L).size();
            int p3Count = board.getSectorsByOwner(3L).size();

            assertEquals(player1.getOwnedSectorCount(), p1Count);
            assertEquals(player2.getOwnedSectorCount(), p2Count);
            assertEquals(player3.getOwnedSectorCount(), p3Count);
        }

        @Test
        @DisplayName("Récupération des secteurs avec armée")
        void shouldGetSectorsWithArmy() {
            List<Sector> sectorsWithArmy = playerStatsService.getSectorsWithArmy(player1, board);

            for (Sector sector : sectorsWithArmy) {
                assertTrue(sector.getArmySize() > 0);
            }
        }
    }

    // === Méthodes utilitaires pour setup ===

    private void setupCompleteBoard() {
        // Créer 16 secteurs
        for (int i = 1; i <= 16; i++) {
            board.addSector(new Sector(i, "Secteur " + i));
        }

        // Assigner les secteurs aux joueurs (comme dans PlayerDemo)
        // Joueur 1: secteurs 1, 2, 4, 5
        for (int s : new int[]{1, 2, 4, 5}) {
            board.assignOwner(s, 1L, "#FF0000");
            player1.addOwnedSectorId((long) s);
        }

        // Joueur 2: secteurs 3, 6, 7, 8
        for (int s : new int[]{3, 6, 7, 8}) {
            board.assignOwner(s, 2L, "#0000FF");
            player2.addOwnedSectorId((long) s);
        }

        // Joueur 3: secteurs 9, 10, 11, 12
        for (int s : new int[]{9, 10, 11, 12}) {
            board.assignOwner(s, 3L, "#00FF00");
            player3.addOwnedSectorId((long) s);
        }

        // Secteurs 13, 14, 15, 16 restent neutres
    }

    private void setupGridNeighbors(int gridSize) {
        for (int i = 1; i <= gridSize * gridSize; i++) {
            Sector sector = board.getSector(i);
            if (sector == null) continue;

            int row = (i - 1) / gridSize;
            int col = (i - 1) % gridSize;

            // Voisin à droite
            if (col < gridSize - 1) {
                sector.addNeighbor(i + 1);
            }
            // Voisin en bas
            if (row < gridSize - 1) {
                sector.addNeighbor(i + gridSize);
            }
            // Voisin à gauche
            if (col > 0) {
                sector.addNeighbor(i - 1);
            }
            // Voisin en haut
            if (row > 0) {
                sector.addNeighbor(i - gridSize);
            }
        }
    }

    private void assignResources() {
        if (board.hasSector(1)) board.getSector(1).setResourceName("Or");
        if (board.hasSector(5)) board.getSector(5).setResourceName("Joyaux");
        if (board.hasSector(9)) board.getSector(9).setResourceName("Cigare");
        if (board.hasSector(13)) board.getSector(13).setResourceName("Uranium");
    }

    private void addArmiesToSectors() {
        // Ajouter des unités aux secteurs du joueur 1
        Sector s1 = board.getSector(1);
        s1.addUnit(new Unit(9.0, UnitClass.TIREUR));
        s1.addUnit(new Unit(9.0, UnitClass.MASTODONTE));

        Sector s2 = board.getSector(2);
        s2.addUnit(new Unit(8.5, UnitClass.LEGER));

        // Ajouter des unités aux secteurs du joueur 2
        Sector s3 = board.getSector(3);
        s3.addUnit(new Unit(8.0, UnitClass.TIREUR));
        s3.addUnit(new Unit(7.0, UnitClass.MASTODONTE));

        Sector s6 = board.getSector(6);
        s6.addUnit(new Unit(6.0, UnitClass.LEGER));

        // Ajouter des unités aux secteurs du joueur 3
        Sector s9 = board.getSector(9);
        s9.addUnit(new Unit(8.0, UnitClass.TIREUR));
        s9.addUnit(new Unit(8.0, UnitClass.PILOTE_DESTRUCTEUR));

        Sector s10 = board.getSector(10);
        s10.addUnit(new Unit(5.0, UnitClass.SNIPER));
    }
}

