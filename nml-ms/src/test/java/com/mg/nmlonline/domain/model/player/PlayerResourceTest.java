package com.mg.nmlonline.domain.model.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        String resourceName = "Or";
        int quantity = 1800;

        player.addResource(resourceName, quantity);

        assertEquals(1, player.getResources().size());
        assertEquals(1800, player.getResourceQuantity("Or"));
    }

    @Test
    @DisplayName("Ajout d'une ressource Ivoire")
    void shouldAddIvoryResource() {
        String resourceName = "Ivoire";
        int quantity = 1300;

        player.addResource(resourceName, quantity);

        assertEquals(1, player.getResources().size());
        assertEquals(1300, player.getResourceQuantity("Ivoire"));
    }

    @Test
    @DisplayName("Ajout de plusieurs ressources différentes")
    void shouldAddMultipleResources() {
        player.addResource("Or", 1800);
        player.addResource("Ivoire", 1300);

        assertEquals(2, player.getResources().size());
        assertEquals(1800, player.getResourceQuantity("Or"));
        assertEquals(1300, player.getResourceQuantity("Ivoire"));
    }

    @Test
    @DisplayName("Incrémentation d'une ressource existante")
    void shouldIncrementExistingResource() {
        player.addResource("Or", 1800);

        player.addResource("Or", 200);

        assertEquals(1, player.getResources().size());
        assertEquals(2000, player.getResourceQuantity("Or"));
    }

    @Test
    @DisplayName("Retrait d'une ressource")
    void shouldRemoveResource() {
        player.addResource("Or", 1800);

        boolean removed = player.removeResource("Or", 500);

        assertTrue(removed);
        assertEquals(1300, player.getResourceQuantity("Or"));
    }

    @Test
    @DisplayName("Retrait complet d'une ressource supprime le stack")
    void shouldRemoveStackWhenQuantityReachesZero() {
        player.addResource("Or", 1800);

        player.removeResource("Or", 1800);

        assertEquals(0, player.getResources().size());
        assertEquals(0, player.getResourceQuantity("Or"));
    }

    @Test
    @DisplayName("Vérification de possession d'une ressource")
    void shouldCheckResourceAvailability() {
        player.addResource("Or", 1800);

        assertTrue(player.hasResource("Or", 1800));
        assertTrue(player.hasResource("Or", 1000));
        assertFalse(player.hasResource("Or", 2000));
        assertFalse(player.hasResource("Ivoire", 1));
    }

    @Test
    @DisplayName("Tentative de retrait d'une quantité supérieure échoue")
    void shouldFailToRemoveMoreThanAvailable() {
        player.addResource("Or", 1800);

        boolean removed = player.removeResource("Or", 2000);

        assertFalse(removed);
        assertEquals(1800, player.getResourceQuantity("Or"));
    }
}
