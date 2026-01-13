package com.mg.nmlonline.demo;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.board.Resource;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.service.CombatService;
import com.mg.nmlonline.domain.service.PlayerImportService;
import com.mg.nmlonline.domain.service.PlayerStatsService;
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

        // Créer le service d'import
        PlayerImportService importService = new PlayerImportService();

        try {
            // === ÉTAPE 1 : Chargement des 3 joueurs depuis les fichiers JSON ===
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

            // Initialiser les IDs des joueurs (simuler la création en base de données)
            player1.setId(1L);
            player2.setId(2L);
            player3.setId(3L);

            // === ÉTAPE 2 : Création du Board et ajout de tous les secteurs ===
            System.out.println("\n═══ ÉTAPE 2: Création du Board ═══\n");

            Board board = createBoard();

            // Importer les secteurs depuis les fichiers JSON et les ajouter au Board
            importService.importSectorsToBoard(p1Url.getFile(), player1, board);
            importService.importSectorsToBoard(p2Url.getFile(), player2, board);
            importService.importSectorsToBoard(p3Url.getFile(), player3, board);

            // Définir les voisins et ressources
            setupNeighbors(board, 16);
            assignResources(board);

            // Assigner les couleurs aux joueurs
            assignColorsToPlayers(board, player1, 1L, "#FF0000");
            assignColorsToPlayers(board, player2, 2L, "#0000FF");
            assignColorsToPlayers(board, player3, 3L, "#00FF00");

            System.out.println("✓ Joueur 1: " + player1.getName() + " - " + player1.getOwnedSectorCount() + " secteurs");
            System.out.println("✓ Joueur 2: " + player2.getName() + " - " + player2.getOwnedSectorCount() + " secteurs");
            System.out.println("✓ Joueur 3: " + player3.getName() + " - " + player3.getOwnedSectorCount() + " secteurs");


            System.out.println("✓ Board créé avec " + board.getSectorCount() + " secteurs");
            System.out.println("  - Secteurs du joueur 1: " + board.getSectorsByOwner(1L).size());
            System.out.println("  - Secteurs du joueur 2: " + board.getSectorsByOwner(2L).size());
            System.out.println("  - Secteurs du joueur 3: " + board.getSectorsByOwner(3L).size());
            System.out.println("  - Secteurs neutres: " + board.getNeutralSectors().size());

            // === ÉTAPE 3: Affichage des informations du Board ===
            System.out.println("\n═══ ÉTAPE 3: Informations du Board ═══\n");
            displayBoardInfo(board, player1, player2, player3);

            // === ÉTAPE 4 : Vérification des conflits potentiels ===
            System.out.println("\n═══ ÉTAPE 4: Vérification des conflits ═══\n");
            checkPotentialConflicts(board);

            // === ÉTAPE 5 : Bataille Classique Rapide (Player1 vs Player2) ===
            System.out.println("\n═══ ÉTAPE 5: Bataille Classique Rapide ═══\n");
            quickClassicBattle(player1, player2, board);

            // === ÉTAPE 6 : Simulation d'une bataille avec CombatService ===
            System.out.println("\n═══ ÉTAPE 6: Simulation de Bataille (Player2 vs Player3) ═══\n");
            simulateBattleBetweenPlayers(player2, player3, board);

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'import des joueurs: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la simulation: " + e.getMessage(), e);
        }
    }

    /**
     * Crée un Board vide.
     */
    private static Board createBoard() {
        return new Board();
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
     * Assigne les couleurs aux secteurs déjà possédés par les joueurs.
     */
    private static void assignColorsToPlayers(Board board, Player player, Long playerId, String color) {
        // Mettre à jour la couleur des secteurs déjà assignés au joueur
        for (Long sectorId : player.getOwnedSectorIds()) {
            Sector boardSector = board.getSector(sectorId.intValue());
            if (boardSector != null && boardSector.isOwnedBy(playerId)) {
                boardSector.setColor(color);
            }
        }
    }

    /**
     * Affiche les informations détaillées du Board.
     */
    private static void displayBoardInfo(Board board, Player player1, Player player2, Player player3) {
        System.out.println("📊 Informations détaillées du Board:\n");

        // Afficher les secteurs par joueur
        displayPlayerSectors(board, player1, 1L, "Rouge");
        displayPlayerSectors(board, player2, 2L, "Bleu");
        displayPlayerSectors(board, player3, 3L, "Vert");

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
    private static void displayPlayerSectors(Board board, Player player, long playerId, String colorName) {
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
                        sector.getNumber(), sector.getOwnerId(),
                        neighborNum, neighbor.getOwnerId());
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
     * Méthode rapide pour lancer une bataille classique entre player1 et player2.
     * Utilise directement la classe Battle avec classicCombatConfiguration.
     */
    private static void quickClassicBattle(Player player1, Player player2, Board board) {
        System.out.println("\n═══ BATAILLE CLASSIQUE RAPIDE ═══\n");
        System.out.println("⚔️  " + player1.getName() + " VS " + player2.getName() + "\n");

        // Créer une nouvelle bataille
        com.mg.nmlonline.domain.model.battle.Battle battle =
            new com.mg.nmlonline.domain.model.battle.Battle();

        // Récupérer les unités des deux joueurs depuis leurs secteurs
        List<Unit> player1Units = new ArrayList<>();
        List<Unit> player2Units = new ArrayList<>();

        // Collecter toutes les unités des secteurs du joueur 1
        for (Sector sector : board.getSectorsByOwner(player1.getId())) {
            player1Units.addAll(sector.getUnits());
        }

        // Collecter toutes les unités des secteurs du joueur 2
        for (Sector sector : board.getSectorsByOwner(player2.getId())) {
            player2Units.addAll(sector.getUnits());
        }

        System.out.println("📊 Forces en présence:");
        System.out.println("  • " + player1.getName() + ": " + player1Units.size() + " unités");
        System.out.println("  • " + player2.getName() + ": " + player2Units.size() + " unités\n");

        // Lancer le combat classique (player1 = attaquant, player2 = défenseur)
        battle.classicCombatConfiguration(player1, player2, player1Units, player2Units);

        System.out.println("\n✓ Bataille terminée!");
    }

    /**
     * Simule une bataille entre deux joueurs sur un secteur spécifique.
     */
    private static void simulateBattleBetweenPlayers(Player attacker, Player defender, Board board) {
        // Créer les services nécessaires
        PlayerStatsService playerStatsService = new PlayerStatsService();
        CombatService combatService = new CombatService();

        // Le CombatService a besoin de PlayerStatsService, on doit le setter manuellement
        // (en production, ceci serait géré par Spring @Autowired)
        try {
            java.lang.reflect.Field field = CombatService.class.getDeclaredField("playerStatsService");
            field.setAccessible(true);
            field.set(combatService, playerStatsService);
        } catch (Exception e) {
            System.err.println("Erreur d'initialisation: " + e.getMessage());
        }

        // Lancer la bataille
        CombatService.BattleResult result = combatService.simulateBattle(attacker, defender, board);

        if (result.success() && result.winner() != null) {
            System.out.println("\n🏆 Vainqueur: " + result.winner().getName());
        } else if (!result.success()) {
            System.out.println("\n" + result.message());
        }
    }
}
