package com.mg.nmlonline.domain.model.board;

import com.mg.nmlonline.domain.model.sector.Sector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe Board.
 * Couvre la gestion des secteurs, propriétaires, voisins et conflits.
 */
@DisplayName("Board Unit Tests")
class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board();
    }

    @Nested
    @DisplayName("Gestion des secteurs")
    class SectorManagementTests {

        @Test
        @DisplayName("Ajout d'un secteur au board")
        void shouldAddSectorToBoard() {
            Sector sector = new Sector(1, "Quartier Nord");

            board.addSector(sector);

            assertTrue(board.hasSector(1));
            assertEquals(1, board.getSectorCount());
            assertEquals("Quartier Nord", board.getSector(1).getName());
        }

        @Test
        @DisplayName("Ajout de plusieurs secteurs")
        void shouldAddMultipleSectors() {
            for (int i = 1; i <= 16; i++) {
                board.addSector(new Sector(i, "Secteur " + i));
            }

            assertEquals(16, board.getSectorCount());
            for (int i = 1; i <= 16; i++) {
                assertTrue(board.hasSector(i));
            }
        }

        @Test
        @DisplayName("Récupération d'un secteur inexistant retourne null")
        void shouldReturnNullForNonExistentSector() {
            assertNull(board.getSector(999));
            assertFalse(board.hasSector(999));
        }

        @Test
        @DisplayName("Lève une exception pour secteur null")
        void shouldThrowExceptionForNullSector() {
            assertThrows(IllegalArgumentException.class, () -> board.addSector(null));
        }

        @Test
        @DisplayName("Lève une exception pour secteur avec numéro invalide")
        void shouldThrowExceptionForInvalidSectorNumber() {
            Sector invalidSector = new Sector(0);
            assertThrows(IllegalArgumentException.class, () -> board.addSector(invalidSector));
        }

        @Test
        @DisplayName("Lève une exception pour secteur déjà existant")
        void shouldThrowExceptionForDuplicateSector() {
            board.addSector(new Sector(1, "Premier"));
            assertThrows(IllegalStateException.class, () -> board.addSector(new Sector(1, "Doublon")));
        }

        @Test
        @DisplayName("Suppression d'un secteur")
        void shouldRemoveSector() {
            board.addSector(new Sector(1, "À supprimer"));

            board.removeSector(1);

            assertFalse(board.hasSector(1));
            assertEquals(0, board.getSectorCount());
        }

        @Test
        @DisplayName("Récupération de tous les secteurs")
        void shouldGetAllSectors() {
            board.addSector(new Sector(1, "S1"));
            board.addSector(new Sector(2, "S2"));
            board.addSector(new Sector(3, "S3"));

            Collection<Sector> allSectors = board.getAllSectors();

            assertEquals(3, allSectors.size());
        }
    }

    @Nested
    @DisplayName("Gestion des propriétaires")
    class OwnerManagementTests {

        @Test
        @DisplayName("Assignation d'un propriétaire à un secteur")
        void shouldAssignOwnerToSector() {
            board.addSector(new Sector(1, "Quartier Nord"));

            board.assignOwner(1, 1L, "#FF0000");

            Sector sector = board.getSector(1);
            assertTrue(sector.isOwnedBy(1L));
            assertEquals("#FF0000", sector.getColor());
        }

        @Test
        @DisplayName("Récupération des secteurs par propriétaire")
        void shouldGetSectorsByOwner() {
            for (int i = 1; i <= 6; i++) {
                board.addSector(new Sector(i, "Secteur " + i));
            }

            // Joueur 1 possède secteurs 1, 2
            board.assignOwner(1, 1L, "#FF0000");
            board.assignOwner(2, 1L, "#FF0000");
            // Joueur 2 possède secteurs 3, 4
            board.assignOwner(3, 2L, "#0000FF");
            board.assignOwner(4, 2L, "#0000FF");
            // Secteurs 5, 6 sont neutres

            List<Sector> player1Sectors = board.getSectorsByOwner(1L);
            List<Sector> player2Sectors = board.getSectorsByOwner(2L);

            assertEquals(2, player1Sectors.size());
            assertEquals(2, player2Sectors.size());
        }

        @Test
        @DisplayName("Récupération des secteurs neutres")
        void shouldGetNeutralSectors() {
            for (int i = 1; i <= 4; i++) {
                board.addSector(new Sector(i, "Secteur " + i));
            }
            board.assignOwner(1, 1L, "#FF0000");
            board.assignOwner(2, 2L, "#0000FF");

            List<Sector> neutralSectors = board.getNeutralSectors();

            assertEquals(2, neutralSectors.size());
            assertTrue(neutralSectors.stream().allMatch(Sector::isNeutral));
        }

        @Test
        @DisplayName("Lève une exception pour assignation à un secteur inexistant")
        void shouldThrowExceptionWhenAssigningToNonExistentSector() {
            assertThrows(IllegalArgumentException.class, () -> board.assignOwner(999, 1L, "#FF0000"));
        }
    }

    @Nested
    @DisplayName("Gestion des voisins et conflits")
    class NeighborAndConflictTests {

        @BeforeEach
        void setUpNeighbors() {
            // Créer une grille 4x4
            for (int i = 1; i <= 16; i++) {
                board.addSector(new Sector(i, "Secteur " + i));
            }
            setupGridNeighbors(4);
        }

        private void setupGridNeighbors(int gridSize) {
            for (int i = 1; i <= gridSize * gridSize; i++) {
                Sector sector = board.getSector(i);
                int row = (i - 1) / gridSize;
                int col = (i - 1) % gridSize;

                if (col < gridSize - 1) sector.addNeighbor(i + 1);
                if (row < gridSize - 1) sector.addNeighbor(i + gridSize);
                if (col > 0) sector.addNeighbor(i - 1);
                if (row > 0) sector.addNeighbor(i - gridSize);
            }
        }

        @Test
        @DisplayName("Vérification que deux secteurs sont voisins")
        void shouldDetectNeighbors() {
            assertTrue(board.areNeighbors(1, 2));
            assertTrue(board.areNeighbors(1, 5));
            assertFalse(board.areNeighbors(1, 16));
        }

        @Test
        @DisplayName("Détection d'un conflit entre secteurs ennemis voisins")
        void shouldDetectConflictBetweenEnemyNeighbors() {
            board.assignOwner(1, 1L, "#FF0000");
            board.assignOwner(2, 2L, "#0000FF");

            assertTrue(board.hasConflict(1, 2));
        }

        @Test
        @DisplayName("Pas de conflit entre secteurs alliés voisins")
        void shouldNotDetectConflictBetweenAlliedNeighbors() {
            board.assignOwner(1, 1L, "#FF0000");
            board.assignOwner(2, 1L, "#FF0000");

            assertFalse(board.hasConflict(1, 2));
        }

        @Test
        @DisplayName("Pas de conflit si secteur neutre")
        void shouldNotDetectConflictWithNeutralSector() {
            board.assignOwner(1, 1L, "#FF0000");
            // Secteur 2 reste neutre

            assertFalse(board.hasConflict(1, 2));
        }

        @Test
        @DisplayName("Pas de conflit entre secteurs non voisins")
        void shouldNotDetectConflictBetweenNonNeighbors() {
            board.assignOwner(1, 1L, "#FF0000");
            board.assignOwner(16, 2L, "#0000FF");

            assertFalse(board.hasConflict(1, 16));
        }
    }

    @Nested
    @DisplayName("Gestion des ressources")
    class ResourceTests {

        @Test
        @DisplayName("Assignation d'une ressource à un secteur")
        void shouldAssignResourceToSector() {
            board.addSector(new Sector(1, "Mine d'or"));

            board.getSector(1).setResourceName("Or");

            String resourceName = board.getSector(1).getResourceName();
            assertNotNull(resourceName);
            assertEquals("Or", resourceName);
        }

        @Test
        @DisplayName("Secteur sans ressource retourne chaîne vide")
        void shouldReturnEmptyStringForSectorWithoutResource() {
            board.addSector(new Sector(1, "Quartier vide"));

            String resourceName = board.getSector(1).getResourceName();
            assertTrue(resourceName == null || resourceName.isEmpty());
        }
    }
}

