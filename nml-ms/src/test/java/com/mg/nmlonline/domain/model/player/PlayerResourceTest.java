package com.mg.nmlonline.domain.model.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la gestion des ressources du joueur
 */
@DisplayName("Player Resource Management Tests")
class PlayerResourceTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Testeur");
    }

    @Test
    @DisplayName("Ajout d'une ressource Or")
    void shouldAddGoldResource() {
        // Given
        String resourceName = "Or";
        int quantity = 1800;

        // When
        player.addResource(resourceName, quantity);

        // Then
        assertEquals(1, player.getResources().size());
        assertEquals(1800, player.getResourceQuantity("Or"));
    }

    @Test
    @DisplayName("Ajout d'une ressource Ivoire")
    void shouldAddIvoryResource() {
        // Given
        String resourceName = "Ivoire";
        int quantity = 1300;

        // When
        player.addResource(resourceName, quantity);

        // Then
        assertEquals(1, player.getResources().size());
        assertEquals(1300, player.getResourceQuantity("Ivoire"));
    }

    @Test
    @DisplayName("Ajout de plusieurs ressources différentes")
    void shouldAddMultipleResources() {
        // When
        player.addResource("Or", 1800);
        player.addResource("Ivoire", 1300);

        // Then
        assertEquals(2, player.getResources().size());
        assertEquals(1800, player.getResourceQuantity("Or"));
        assertEquals(1300, player.getResourceQuantity("Ivoire"));
    }

    @Test
    @DisplayName("Incrémentation d'une ressource existante")
    void shouldIncrementExistingResource() {
        // Given
        player.addResource("Or", 1800);

        // When
        player.addResource("Or", 200);

        // Then
        assertEquals(1, player.getResources().size());
        assertEquals(2000, player.getResourceQuantity("Or"));
    }

    @Test
    @DisplayName("Retrait d'une ressource")
    void shouldRemoveResource() {
        // Given
        player.addResource("Or", 1800);

        // When
        boolean removed = player.removeResource("Or", 500);

        // Then
        assertTrue(removed);
        assertEquals(1300, player.getResourceQuantity("Or"));
    }

    @Test
    @DisplayName("Retrait complet d'une ressource supprime le stack")
    void shouldRemoveStackWhenQuantityReachesZero() {
        // Given
        player.addResource("Or", 1800);

        // When
        player.removeResource("Or", 1800);

        // Then
        assertEquals(0, player.getResources().size());
        assertEquals(0, player.getResourceQuantity("Or"));
    }

    @Test
    @DisplayName("Vérification de possession d'une ressource")
    void shouldCheckResourceAvailability() {
        // Given
        player.addResource("Or", 1800);

        // Then
        assertTrue(player.hasResource("Or", 1800));
        assertTrue(player.hasResource("Or", 1000));
        assertFalse(player.hasResource("Or", 2000));
        assertFalse(player.hasResource("Ivoire", 1));
    }

    @Test
    @DisplayName("Tentative de retrait d'une quantité supérieure échoue")
    void shouldFailToRemoveMoreThanAvailable() {
        // Given
        player.addResource("Or", 1800);

        // When
        boolean removed = player.removeResource("Or", 2000);

        // Then
        assertFalse(removed);
        assertEquals(1800, player.getResourceQuantity("Or"));
    }
}
