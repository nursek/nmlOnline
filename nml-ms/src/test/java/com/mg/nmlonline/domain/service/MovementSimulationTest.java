package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Simulation de la résolution incrémentale des déplacements.
 *
 * <h2>Carte de jeu</h2>
 * <pre>
 *   [1] ── [2] ── [3] ── [4]
 *   (A)    (B)           (C)
 * </pre>
 * <ul>
 *   <li>Secteur 1 : appartient à Joueur A. Contient unité A.</li>
 *   <li>Secteur 2 : appartient à Joueur B. Contient unité B (défenseur stationné).</li>
 *   <li>Secteur 3 : neutre, vide.</li>
 *   <li>Secteur 4 : appartient à Joueur C. Contient le VTT du Joueur C.</li>
 * </ul>
 *
 * <h2>Principe de la résolution incrémentale</h2>
 * <p>Le moteur avance d'un secteur à la fois (un « step »). Toutes les entités progressent
 * simultanément. Les conflits sont détectés secteur par secteur, à chaque step.</p>
 *
 * <p><strong>Propriété clé :</strong> une entité arrivée à un step N est physiquement présente
 * dans son secteur dès le step N+1 — elle est donc « tangible » pour tout arrivant au step suivant.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Simulation : résolution incrémentale multi-step")
class MovementSimulationTest {

    @Mock
    MovementOrderRepository orderRepository;

    @Mock
    VehicleRepository vehicleRepository;

    @InjectMocks
    MovementService service;

    // Joueurs
    private static final Long JOUEUR_A = 1L;
    private static final Long JOUEUR_B = 2L;
    private static final Long JOUEUR_C = 3L;

    // IDs
    private static final Long UNITE_A_ID  = 101L;
    private static final Long UNITE_B_ID  = 102L;
    private static final Long VEHICULE_C_ID = 201L;
    private static final Long PILOTE_C_ID   = 301L;

    private Board  board;
    private Sector secteur1, secteur2, secteur3, secteur4;
    private Unit   uniteA, uniteB;
    private Vehicle vehiculeC;

    @BeforeEach
    void setUp() {
        // --- Plateau ---
        board    = new Board();
        secteur1 = new Sector(1, "Zone A (départ A)");
        secteur2 = new Sector(2, "Objectif (B défend)");
        secteur3 = new Sector(3, "Zone intermédiaire");
        secteur4 = new Sector(4, "Zone C (départ C)");

        board.addSector(secteur1);
        board.addSector(secteur2);
        board.addSector(secteur3);
        board.addSector(secteur4);

        // Adjacences linéaires 1-2-3-4
        secteur1.addNeighbor(2); secteur2.addNeighbor(1);
        secteur2.addNeighbor(3); secteur3.addNeighbor(2);
        secteur3.addNeighbor(4); secteur4.addNeighbor(3);

        // Propriétés des secteurs
        secteur1.setOwnerId(JOUEUR_A);
        secteur2.setOwnerId(JOUEUR_B);
        secteur4.setOwnerId(JOUEUR_C);

        // --- Unité A en secteur 1 ---
        uniteA = new Unit(5.0, UnitClass.ELEMENTAIRE);
        uniteA.setId(UNITE_A_ID);
        uniteA.setPlayerId(JOUEUR_A);
        uniteA.setSector(secteur1);
        secteur1.getArmy().add(uniteA);

        // --- Unité B stationnaire en secteur 2 ---
        uniteB = new Unit(5.0, UnitClass.ELEMENTAIRE);
        uniteB.setId(UNITE_B_ID);
        uniteB.setPlayerId(JOUEUR_B);
        uniteB.setSector(secteur2);
        secteur2.getArmy().add(uniteB);

        // --- Véhicule C en secteur 4 ---
        vehiculeC = new Vehicle(VehicleType.VTT_LEGER, JOUEUR_C);
        vehiculeC.setId(VEHICULE_C_ID);
        vehiculeC.setSector(secteur4);
        // Pilote requis pour que cantMove() == false
        Unit pilote = new Unit(10.0, UnitClass.PILOTE_DESTRUCTEUR);
        pilote.setId(PILOTE_C_ID);
        pilote.setPlayerId(JOUEUR_C);
        vehiculeC.assignPilot(pilote);
        secteur4.getVehicles().add(vehiculeC);

        // Mock de persistance : saveAll est appelé en fin de resolveAllMovements
        when(orderRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Les entités arrivées au step 1 doivent être visibles comme défenseurs au step 2")
    void shouldDetectConflictsWithEntitiesArrivedInPreviousStep() {
        /*
         * === ÉTAT INITIAL ===
         *
         *   [1:A] ── [2:B] ── [3:vide] ── [4:C]
         *    (A)      (B)                  (C+VTT)
         *
         * Ordres soumis pour le tour 1 :
         *   - Joueur A : unité A se déplace à pied  1 → 2     (route [1, 2], 1 hop)
         *   - Joueur C : VTT se déplace             4 → 3 → 2 (route [4, 3, 2], 2 hops)
         *
         * Joueur B ne donne aucun ordre → unité B reste stationnaire en secteur 2.
         */

        // Joueur A : à pied, route [1, 2] (1 hop — ELEMENTAIRE, max 1 hop)
        MovementOrder ordreA = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(UNITE_A_ID), List.of(1, 2));
        ordreA.setId(1L);

        // Joueur C : VTT, route [4, 3, 2] (2 hops — vitesse VTT_LEGER = 2)
        MovementOrder ordreC = MovementOrder.createVehicleOrder(JOUEUR_C, 1, VEHICULE_C_ID, List.of(4, 3, 2));
        ordreC.setId(2L);

        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordreA, ordreC));
        when(vehicleRepository.findById(VEHICULE_C_ID)).thenReturn(Optional.of(vehiculeC));

        /*
         * === DÉROULEMENT STEP 1 ===
         *
         * Toutes les entités avancent d'un secteur simultanément :
         *   - Unité A : 1 → 2   (arrivée définitive, c'est son seul hop)
         *   - VTT C   : 4 → 3   (transit, pas encore à destination)
         *
         * Secteur 2 après step 1 :
         *   Arrivants  : Joueur A
         *   Défenseurs : Joueur B (stationné, jamais bougé)
         *   → CONFLIT A vs B détecté et enregistré.
         *
         * Secteur 3 après step 1 :
         *   Arrivants  : Joueur C (VTT)
         *   Défenseurs : aucun (secteur vide)
         *   → Pas de conflit.
         *
         * Positions en fin de step 1 :
         *   Unité A → secteur 2  (physiquement déplacée dans army du secteur 2)
         *   VTT C   → secteur 3  (physiquement déplacé dans le secteur 3)
         *   Unité B → secteur 2  (inchangée)
         */

        /*
         * === DÉROULEMENT STEP 2 ===
         *
         * Seul le VTT C a encore un hop à faire :
         *   - VTT C : 3 → 2
         *
         * Secteur 2 avant l'arrivée du VTT :
         *   Entités présentes : Unité A (arrivée au step 1) + Unité B (toujours stationnaire)
         *   Joueurs en place   : A et B
         *
         * Le moteur capture les défenseurs AVANT de déplacer le VTT.
         * Donc défenderPlayerIds = {A, B}.
         *
         * Après déplacement du VTT en secteur 2 :
         *   → CONFLIT C vs A  ← prouve que A (arrivé au step 1) est « tangible » au step 2
         *   → CONFLIT C vs B  ← B est toujours présent
         */

        // === Résolution ===
        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        // --- Conflit step 1 : A attaque B en secteur 2 ---
        assertTrue(
                resultat.getConflicts().stream().anyMatch(c ->
                        c.sectorNumber() == 2 &&
                        c.attackerPlayerId().equals(JOUEUR_A) &&
                        c.defenderPlayerId().equals(JOUEUR_B)),
                "Conflit step 1 attendu : Joueur A attaque Joueur B en secteur 2"
        );

        // --- Conflit step 2 : C vs A — prouve la tangibilité des entités arrivées au step précédent ---
        assertTrue(
                resultat.getConflicts().stream().anyMatch(c ->
                        c.sectorNumber() == 2 &&
                        c.attackerPlayerId().equals(JOUEUR_C) &&
                        c.defenderPlayerId().equals(JOUEUR_A)),
                "Conflit step 2 attendu : Joueur C affronte Joueur A (arrivé au step 1) en secteur 2"
        );

        // --- Conflit step 2 : C vs B — B, stationné, est toujours en secteur 2 ---
        assertTrue(
                resultat.getConflicts().stream().anyMatch(c ->
                        c.sectorNumber() == 2 &&
                        c.attackerPlayerId().equals(JOUEUR_C) &&
                        c.defenderPlayerId().equals(JOUEUR_B)),
                "Conflit step 2 attendu : Joueur C affronte Joueur B (défenseur originel) en secteur 2"
        );

        // --- Aucun ordre bloqué (pas de véhicule détruit dans ce scénario) ---
        assertTrue(resultat.getBlocked().isEmpty(), "Aucun ordre ne doit être bloqué");

        // --- Les deux ordres aboutissent ---
        assertEquals(2, resultat.getResolved().size(), "Les deux ordres doivent être résolus");

        /*
         * === POSITIONS FINALES ===
         *
         *   [1:A]  ── [2:A+B+C] ── [3:vide] ── [4:vide]
         *   vide        ↑↑↑
         *            tous en 2
         */

        // Unité A physiquement en secteur 2
        assertTrue(
                secteur2.getArmy().stream().anyMatch(u -> u.getId().equals(UNITE_A_ID)),
                "Unité A doit être en secteur 2 après résolution"
        );

        // VTT C physiquement en secteur 2 (a parcouru 2 secteurs)
        assertEquals(secteur2, vehiculeC.getSector(),
                "Véhicule C doit être en secteur 2 après 2 steps");

        // Secteur 1 vidé (unité A est partie)
        assertTrue(
                secteur1.getArmy().stream().noneMatch(u -> u.getId().equals(UNITE_A_ID)),
                "Unité A ne doit plus être en secteur 1"
        );
    }

    @Test
    @DisplayName("Secteur intermédiaire vide ne doit pas générer de conflit")
    void shouldNotGenerateConflictWhenTransitingThroughEmptySector() {
        /*
         * === ÉTAT INITIAL ===
         *
         *   [1:A] ── [2:B] ── [3:vide] ── [4:C]
         *                                 (C+VTT)
         *
         * Seul Joueur C donne un ordre :
         *   - VTT C : route [4, 3, 2] (2 hops)
         *
         * Joueur A et Joueur B ne bougent pas.
         *
         * === STEP 1 ===
         *   VTT C : 4 → 3 (secteur vide, aucun ennemi)
         *   → Aucun conflit, aucun combat de transit.
         *
         * === STEP 2 ===
         *   VTT C : 3 → 2
         *   Secteur 2 contient Joueur B (stationné).
         *   → CONFLIT C vs B enregistré.
         *   Le VTT est à destination (step 2 = route.size()-1), donc PAS de combat de transit
         *   (le combat de transit ne s'applique qu'aux secteurs intermédiaires, pas à l'arrivée).
         */

        // Joueur C traverse le secteur 3 (vide) avant d'arriver en 2
        MovementOrder ordreC = MovementOrder.createVehicleOrder(JOUEUR_C, 1, VEHICULE_C_ID, List.of(4, 3, 2));
        ordreC.setId(1L);

        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordreC));
        when(vehicleRepository.findById(VEHICULE_C_ID)).thenReturn(Optional.of(vehiculeC));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        // --- Aucun conflit en secteur 3 (vide au step 1) ---
        assertTrue(
                resultat.getConflicts().stream().noneMatch(c -> c.sectorNumber() == 3),
                "Aucun conflit ne doit être généré pour le secteur intermédiaire vide"
        );

        // --- Conflit en secteur 2 au step 2 (C arrive sur B stationné) ---
        assertTrue(
                resultat.getConflicts().stream().anyMatch(c ->
                        c.sectorNumber() == 2 &&
                        c.attackerPlayerId().equals(JOUEUR_C) &&
                        c.defenderPlayerId().equals(JOUEUR_B)),
                "Conflit attendu en secteur 2 : C vs B"
        );

        // --- Pas de combat de transit : secteur 3 était vide au step 1 ---
        assertTrue(resultat.getTransitCombats().isEmpty(),
                "Pas de combat de transit attendu (secteur intermédiaire vide)");
    }
}
