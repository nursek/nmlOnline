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
    @DisplayName("Ajout de plusieurs ressources différentes")
    void shouldAddMultipleResources() {
        player.addResource("Auramite", 1800);
        player.addResource("Adamentium", 1300);

        assertEquals(2, player.getResources().size());
        assertEquals(1800, player.getResourceQuantity("Auramite"));
        assertEquals(1300, player.getResourceQuantity("Adamentium"));
    }

    @Test
    @DisplayName("Incrémentation d'une ressource existante")
    void shouldIncrementExistingResource() {
        player.addResource("Auramite", 1800);

        player.addResource("Auramite", 200);

        assertEquals(1, player.getResources().size());
        assertEquals(2000, player.getResourceQuantity("Auramite"));
    }

    @Test
    @DisplayName("Retrait d'une ressource")
    void shouldRemoveResource() {
        player.addResource("Auramite", 1800);

        boolean removed = player.removeResource("Auramite", 500);

        assertTrue(removed);
        assertEquals(1300, player.getResourceQuantity("Auramite"));
    }

    @Test
    @DisplayName("Retrait complet d'une ressource supprime le stack")
    void shouldRemoveStackWhenQuantityReachesZero() {
        player.addResource("Auramite", 1800);

        player.removeResource("Auramite", 1800);

        assertEquals(0, player.getResources().size());
        assertEquals(0, player.getResourceQuantity("Auramite"));
    }

    @Test
    @DisplayName("Vérification de possession d'une ressource")
    void shouldCheckResourceAvailability() {
        player.addResource("Auramite", 1800);

        assertTrue(player.hasResource("Auramite", 1800));
        assertTrue(player.hasResource("Auramite", 1000));
        assertFalse(player.hasResource("Auramite", 2000));
        assertFalse(player.hasResource("Adamentium", 1));
    }

    @Test
    @DisplayName("Tentative de retrait d'une quantité supérieure échoue")
    void shouldFailToRemoveMoreThanAvailable() {
        player.addResource("Auramite", 1800);

        boolean removed = player.removeResource("Auramite", 2000);

        assertFalse(removed);
        assertEquals(1800, player.getResourceQuantity("Auramite"));
    }
}
