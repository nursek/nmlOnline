package com.mg.nmlonline.demo;

import com.mg.nmlonline.domain.model.battle.Battle;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.board.Resource;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.service.PlayerImportService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.*;

@Slf4j
public class PlayerDemo {

    public static void main(String[] args) {
        System.out.println("=== DÉMO CLASSE PLAYER ===\n");

        // Test complet : création des joueurs, board et simulation de bataille
        testCompleteGameSimulation();
    }

    /**
     * Test complet : création de 3 joueurs, intégration du board, liaison des secteurs et simulation de bataille.
     */
    private static void testCompleteGameSimulation() {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║  TEST: Simulation Complète de Jeu avec Board et Combat ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        PlayerImportService importService = new PlayerImportService();

        try {
            // === ÉTAPE 1: Chargement des 3 joueurs depuis les fichiers JSON ===
            System.out.println("═══ ÉTAPE 1: Chargement des joueurs ═══\n");

            URL p1Url = PlayerDemo.class.getClassLoader().getResource("players/player1.json");
            URL p2Url = PlayerDemo.class.getClassLoader().getResource("players/player2.json");
            URL p3Url = PlayerDemo.class.getClassLoader().getResource("players/player3.json");

            if (p1Url == null || p2Url == null || p3Url == null) {
                throw new NullPointerException("Un ou plusieurs fichiers JSON de joueurs sont introuvables");
            }

            Player player1 = importService.importPlayerFromJson(p1Url.getFile());
            Player player2 = importService.importPlayerFromJson(p2Url.getFile());
            Player player3 = importService.importPlayerFromJson(p3Url.getFile());

            System.out.println("✓ Joueur 1: " + player1.getName() + " - " + player1.getSectors().size() + " secteurs");
            System.out.println("✓ Joueur 2: " + player2.getName() + " - " + player2.getSectors().size() + " secteurs");
            System.out.println("✓ Joueur 3: " + player3.getName() + " - " + player3.getSectors().size() + " secteurs");

            // === ÉTAPE 2: Création du Board et ajout de tous les secteurs ===
            System.out.println("\n═══ ÉTAPE 2: Création du Board ═══\n");

            Board board = createAndPopulateBoard(player1, player2, player3);

            System.out.println("✓ Board créé avec " + board.getSectorCount() + " secteurs");
            System.out.println("  - Secteurs du joueur 1: " + board.getSectorsByOwner(1).size());
            System.out.println("  - Secteurs du joueur 2: " + board.getSectorsByOwner(2).size());
            System.out.println("  - Secteurs du joueur 3: " + board.getSectorsByOwner(3).size());
            System.out.println("  - Secteurs neutres: " + board.getNeutralSectors().size());

            // === ÉTAPE 3: Affichage des informations du Board ===
            System.out.println("\n═══ ÉTAPE 3: Informations du Board ═══\n");
            displayBoardInfo(board, player1, player2, player3);

            // === ÉTAPE 4: Vérification des conflits potentiels ===
            System.out.println("\n═══ ÉTAPE 4: Vérification des conflits ═══\n");
            checkPotentialConflicts(board);

            // === ÉTAPE 5: Simulation d'une bataille entre deux joueurs ===
            System.out.println("\n═══ ÉTAPE 5: Simulation de Bataille ═══\n");
            simulateBattleBetweenPlayers(player2, player3, board);

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'import des joueurs: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la simulation: " + e.getMessage(), e);
        }
    }

    /**
     * Crée le Board et y ajoute tous les secteurs des joueurs.
     */
    private static Board createAndPopulateBoard(Player player1, Player player2, Player player3) {
        Board board = new Board();

        // Récupérer tous les numéros de secteurs possédés par les joueurs
        Set<Integer> allSectorNumbers = new HashSet<>();
        for (Sector s : player1.getSectors()) allSectorNumbers.add(s.getNumber());
        for (Sector s : player2.getSectors()) allSectorNumbers.add(s.getNumber());
        for (Sector s : player3.getSectors()) allSectorNumbers.add(s.getNumber());

        // Trouver le numéro de secteur maximum pour créer la grille complète
        int maxSector = allSectorNumbers.stream().max(Integer::compareTo).orElse(16);

        // Créer tous les secteurs de 1 à maxSector
        for (int i = 1; i <= maxSector; i++) {
            board.addSector(new Sector(i, "Secteur n°" + i));
        }

        // Définir les voisins (grille 4x4 par exemple)
        setupNeighbors(board, maxSector);

        // Assigner des ressources à certains secteurs
        assignResources(board);

        // Lier les secteurs des joueurs au board
        linkPlayerSectorsToBoard(board, player1, 1, "#FF0000"); // Rouge
        linkPlayerSectorsToBoard(board, player2, 2, "#0000FF"); // Bleu
        linkPlayerSectorsToBoard(board, player3, 3, "#00FF00"); // Vert

        return board;
    }

    /**
     * Configure les voisins pour une grille (approximatif selon la taille).
     */
    private static void setupNeighbors(Board board, int maxSector) {
        // Configuration simple pour une grille carrée
        int gridSize = (int) Math.ceil(Math.sqrt(maxSector));

        for (int i = 1; i <= maxSector; i++) {
            Sector sector = board.getSector(i);
            if (sector == null) continue;

            int row = (i - 1) / gridSize;
            int col = (i - 1) % gridSize;

            // Voisin à droite
            if (col < gridSize - 1 && i + 1 <= maxSector) {
                sector.addNeighbor(i + 1);
            }
            // Voisin en bas
            if (row < gridSize - 1 && i + gridSize <= maxSector) {
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

    /**
     * Assigne des ressources à certains secteurs du board.
     */
    private static void assignResources(Board board) {
        if (board.hasSector(1)) board.getSector(1).setResource(new Resource("or", 2000.0));
        if (board.hasSector(5)) board.getSector(5).setResource(new Resource("joyaux", 1500.0));
        if (board.hasSector(9)) board.getSector(9).setResource(new Resource("cigares", 800.0));
        if (board.hasSector(13)) board.getSector(13).setResource(new Resource("uranium", 3000.0));
    }

    /**
     * Lie les secteurs d'un joueur au Board en assignant le propriétaire et la couleur.
     */
    private static void linkPlayerSectorsToBoard(Board board, Player player, int playerId, String color) {
        for (Sector playerSector : player.getSectors()) {
            Sector boardSector = board.getSector(playerSector.getNumber());
            if (boardSector != null) {
                // Assigner le propriétaire et la couleur
                board.assignOwner(playerSector.getNumber(), playerId, color);

                // Copier les données du secteur du joueur vers le board
                boardSector.setName(playerSector.getName());
                boardSector.setIncome(playerSector.getIncome());
                boardSector.setArmy(new ArrayList<>(playerSector.getArmy()));
                boardSector.setStats(playerSector.getStats());
            }
        }
    }

    /**
     * Affiche les informations détaillées du Board.
     */
    private static void displayBoardInfo(Board board, Player player1, Player player2, Player player3) {
        System.out.println("📊 Informations détaillées du Board:\n");

        // Afficher les secteurs par joueur
        displayPlayerSectors(board, player1, 1, "Rouge");
        displayPlayerSectors(board, player2, 2, "Bleu");
        displayPlayerSectors(board, player3, 3, "Vert");

        // Afficher les ressources
        System.out.println("\n💎 Ressources sur la carte:");
        for (Sector sector : board.getAllSectors()) {
            if (sector.getResource() != null) {
                System.out.printf("  • Secteur %d (%s): %s%n",
                    sector.getNumber(), sector.getName(), sector.getResource());
            }
        }
    }

    /**
     * Affiche les secteurs d'un joueur spécifique.
     */
    private static void displayPlayerSectors(Board board, Player player, int playerId, String colorName) {
        List<Sector> playerSectors = board.getSectorsByOwner(playerId);
        System.out.println("\n🎮 " + player.getName() + " (" + colorName + ") - " + playerSectors.size() + " secteurs:");

        for (Sector sector : playerSectors) {
            System.out.printf("  • Secteur %d: %s | Armée: %d unités | Revenus: %.0f$",
                sector.getNumber(), sector.getName(), sector.getArmySize(), sector.getIncome());

            if (sector.getResource() != null) {
                System.out.print(" | Ressource: " + sector.getResource().getType());
            }
            System.out.println();
        }
    }

    /**
     * Vérifie les conflits potentiels entre secteurs voisins.
     */
    private static void checkPotentialConflicts(Board board) {
        System.out.println("⚔️  Analyse des conflits potentiels:\n");

        int conflictCount = 0;
        for (Sector sector : board.getAllSectors()) {
            if (sector.isNeutral()) continue;

            for (int neighborNum : sector.getNeighbors()) {
                if (board.hasConflict(sector.getNumber(), neighborNum)) {
                    Sector neighbor = board.getSector(neighborNum);
                    System.out.printf("  ⚠️  Conflit détecté: Secteur %d (Joueur %d) ↔ Secteur %d (Joueur %d)%n",
                        sector.getNumber(), sector.getOwnerPlayerId(),
                        neighborNum, neighbor.getOwnerPlayerId());
                    conflictCount++;
                }
            }
        }

        if (conflictCount == 0) {
            System.out.println("  ✓ Aucun conflit détecté (pas de secteurs ennemis adjacents)");
        } else {
            System.out.println("\n  Total: " + conflictCount + " zones de conflit");
        }
    }

    /**
     * Simule une bataille entre deux joueurs sur un secteur spécifique.
     */
    private static void simulateBattleBetweenPlayers(Player attacker, Player defender, Board board) {
        System.out.println("⚔️  DÉBUT DE LA BATAILLE\n");
        System.out.println("  Attaquant: " + attacker.getName());
        System.out.println("  Défenseur: " + defender.getName() + "\n");

        // Trouver un secteur du défenseur avec une armée
        Sector defenderSector = defender.getSectorsWithArmy().stream()
            .findFirst()
            .orElse(null);

        if (defenderSector == null) {
            System.out.println("❌ Le défenseur n'a pas d'armée disponible pour le combat.");
            return;
        }

        // Trouver un secteur de l'attaquant avec une armée
        Sector attackerSector = attacker.getSectorsWithArmy().stream()
            .findFirst()
            .orElse(null);

        if (attackerSector == null) {
            System.out.println("❌ L'attaquant n'a pas d'armée disponible pour le combat.");
            return;
        }

        System.out.println("  📍 Secteur attaqué: " + defenderSector.getName() + " (n°" + defenderSector.getNumber() + ")");
        System.out.println("  📍 Secteur d'origine: " + attackerSector.getName() + " (n°" + attackerSector.getNumber() + ")");
        System.out.println("\n" + "=".repeat(60) + "\n");

        // Lancer la bataille
        Battle battle = new Battle();
        battle.classicCombatConfiguration(attacker, defender);

        System.out.println("\n" + "=".repeat(60));
        System.out.println("⚔️  FIN DE LA BATAILLE");
    }
}
