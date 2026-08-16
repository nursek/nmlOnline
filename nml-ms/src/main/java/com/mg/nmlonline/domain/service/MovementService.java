package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.DestinationConflict;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.domain.model.movement.TransitCombatResult;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de gestion des déplacements.
 *
 * <p>Les ordres de déplacement sont collectés pendant le tour via {@link #placeFootOrder} et
 * {@link #placeVehicleOrder} puis résolus simultanément
 * en fin de tour via {@link #resolveAllMovements}.</p>
 *
 * <h3>Règles de résolution :</h3>
 * <ul>
 *   <li>Croisement (A→B et B→A) : pas de combat, les entités se croisent.</li>
 *   <li>Arrivée contestée (B reste sur B, A→B) : combat entre A (attaquant) et B (défenseur).</li>
 *   <li>Transit véhicule : seul le véhicule combat (pas les passagers). Si détruit → passagers débarqués.</li>
 *   <li>Bâtiments : déplacement en secteur allié uniquement.</li>
 * </ul>
 */
@Service
@Transactional
public class MovementService {

    private final MovementOrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;

    public MovementService(MovementOrderRepository orderRepository,
                           VehicleRepository vehicleRepository) {
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // ==========================================
    // === CRÉATION DES ORDRES DE DÉPLACEMENT ===
    // ==========================================

    /**
     * Crée un ordre de déplacement à pied pour un groupe d'unités/personnages.
     *
     * <p>La route doit contenir au moins 2 secteurs. Les hops autorisés dépendent
     * de la classe de chaque entité : les unités légères (L) peuvent faire 2 hops,
     * les autres sont limitées à 1.</p>
     *
     * @param playerId  ID du joueur
     * @param turn      Tour courant
     * @param entityIds IDs des entités déplacées (toutes doivent supporter la longueur de la route)
     * @param route     Route complète — ex: {@code [1, 2]} ou {@code [1, 2, 3]} pour les LEGER
     * @param board     Plateau de jeu
     * @return L'ordre créé
     * @throws IllegalArgumentException si la validation échoue
     */
    public MovementOrder placeFootOrder(Long playerId, int turn, List<Long> entityIds,
                                        List<Integer> route, Board board) {
        if (route == null || route.size() < 2) {
            throw new IllegalArgumentException("La route doit contenir au moins 2 secteurs.");
        }

        int from = route.getFirst();
        int to   = route.getLast();
        validateBasicOrder(from, to, board);

        // Chaque paire consécutive de la route doit être adjacente
        if (board.isInvalidRoute(route)) {
            throw new IllegalArgumentException("La route n'est pas valide (secteurs non adjacents).");
        }

        // Vérification que le secteur source contient les entités du joueur
        Sector fromSector = board.getSector(from);
        validateEntitiesInSector(fromSector, entityIds, playerId);

        // Validation des hops : chaque entité doit supporter la longueur de la route
        int hops = route.size() - 1;
        validateFootHops(fromSector, entityIds, playerId, hops);

        MovementOrder order = MovementOrder.createFootOrder(playerId, turn, entityIds, route);
        return orderRepository.save(order);
    }

    /**
     * Crée un ordre de déplacement en véhicule.
     *
     * @param playerId  ID du joueur
     * @param turn      Tour courant
     * @param vehicleId ID du véhicule
     * @param route     Route complète (secteur départ inclus)
     * @param board     Plateau de jeu
     * @return L'ordre créé
     * @throws IllegalArgumentException si la validation échoue
     */
    public MovementOrder placeVehicleOrder(Long playerId, int turn, Long vehicleId,
                                           List<Integer> route, Board board) {
        if (route == null || route.size() < 2) {
            throw new IllegalArgumentException("La route doit contenir au moins 2 secteurs.");
        }

        int from = route.getFirst();
        int to = route.getLast();
        validateBasicOrder(from, to, board);

        // Validation de la route (chaque paire consécutive doit être voisine)
        if (board.isInvalidRoute(route)) {
            throw new IllegalArgumentException("La route n'est pas valide (secteurs non adjacents).");
        }

        // Vérifier le véhicule
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Véhicule introuvable : " + vehicleId));

        if (!vehicle.getPlayerId().equals(playerId)) {
            throw new SecurityException("Ce véhicule n'appartient pas au joueur.");
        }

        // Vérifier que le véhicule est effectivement dans le secteur de départ de la route
        Sector vehicleSector = vehicle.getSector();
        if (vehicleSector == null || vehicleSector.getNumber() != from) {
            throw new IllegalArgumentException(
                    "Le véhicule n'est pas dans le secteur de départ de la route (secteur " + from + ").");
        }
        if (vehicleSector.getBoard() != null && board.getId() != null
                && !vehicleSector.getBoard().getId().equals(board.getId())) {
            throw new IllegalArgumentException("Le véhicule n'appartient pas à ce plateau de jeu.");
        }

        if (vehicle.cantMove()) {
            throw new IllegalArgumentException("Le véhicule ne peut pas se déplacer (détruit ou sans pilote).");
        }

        // Vérifier la vitesse (nombre de hops = route.size() - 1)
        int hops = route.size() - 1;
        if (hops > vehicle.getSpeed()) {
            throw new IllegalArgumentException(
                    "Le véhicule ne peut parcourir que " + vehicle.getSpeed() + " secteur(s) par tour, "
                    + "mais la route en demande " + hops + ".");
        }

        MovementOrder order = MovementOrder.createVehicleOrder(playerId, turn, vehicleId, route);
        return orderRepository.save(order);
    }
    
    /**
     * Annule un ordre PENDING d'un joueur en levant une exception dédiée
     * selon la cause d'échec, pour mapper vers des codes HTTP distincts :
     * <ul>
     *   <li>{@link EntityNotFoundException} → 404 : ordre introuvable.</li>
     *   <li>{@link SecurityException} → 403 : l'ordre n'appartient pas à ce joueur.</li>
     *   <li>{@link IllegalStateException} → 409 : ordre déjà annulé/résolu (non PENDING).</li>
     * </ul>
     */
    public void cancelOrderOrThrow(Long playerId, Long orderId) {
        MovementOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ordre de déplacement #" + orderId + " introuvable."));
        if (!order.getPlayerId().equals(playerId)) {
            throw new SecurityException(
                    "L'ordre #" + orderId + " n'appartient pas au joueur " + playerId + ".");
        }
        if (order.isNotPending()) {
            throw new IllegalStateException(
                    "L'ordre #" + orderId + " n'est plus PENDING (statut=" + order.getStatus() + ").");
        }
        order.cancel();
        orderRepository.save(order);
    }

    /**
     * Retourne tous les ordres PENDING d'un joueur pour un tour.
     */
    public List<MovementOrder> getPlayerOrders(Long playerId, int turn) {
        return orderRepository.findByPlayerIdAndTurnAndStatus(playerId, turn, MovementStatus.PENDING);
    }

    // ========================================
    // === RÉSOLUTION EN FIN DE TOUR ===
    // ========================================

    /**
     * Résout tous les ordres de déplacement du tour de façon <strong>incrémentale</strong>.
     *
     * <p>L'algorithme avance d'un secteur à la fois (un « step »). À chaque step, toutes les
     * entités qui ont encore des secteurs à parcourir progressent simultanément d'un secteur.
     * Les combats sont détectés secteur par secteur à la fin de chaque step.
     * Les entités arrivées à un step précédent restent physiquement dans leur secteur et sont
     * donc « tangibles » pour tout arrivant au step suivant.</p>
     *
     * <h3>Règles appliquées à chaque step :</h3>
     * <ul>
     *   <li>Croisement (A→B et B→A pour le même step) : pas de combat entre A et B.</li>
     *   <li>Arrivée dans un secteur occupé : conflit signalé pour chaque paire attaquant/défenseur.</li>
     *   <li>Transit véhicule (secteur intermédiaire avec ennemis) : combat de transit signalé.</li>
     * </ul>
     *
     * @param turn  Tour à résoudre
     * @param board Plateau de jeu
     * @return Résultat de la résolution
     */
    public MovementResolutionResult resolveAllMovements(int turn, Board board) {
        ResolutionContext ctx = prepareResolution(turn, board);
        for (int step = 1; step <= ctx.maxSteps; step++) {
            resolveStep(board, step, ctx);
        }
        return finalizeResolution(board, ctx);
    }

    // ==================================================
    // === RÉSOLUTION PAS-À-PAS (admin, par hop) =========
    // ==================================================

    /**
     * État d'une résolution de mouvements, partagé entre les appels pas-à-pas
     * {@code prepare → resolveStep × N → finalize}. Ne contient que des valeurs
     * (IDs, numéros de secteurs, listes de conflits) — aucune référence d'entité
     * JPA — afin de survivre aux changements de transaction entre deux hops
     * admin. Les ordres/secteurs sont (re)chargés depuis la base à chaque hop.
     */
    public static class ResolutionContext {
        final int turn;
        final List<Long> validOrderIds = new ArrayList<>();
        final List<MovementOrder> activeOrders = new ArrayList<>();
        final Map<Long, Integer> currentPosition = new HashMap<>();
        final Set<Long> stoppedIds = new HashSet<>();
        final List<MovementOrder> blockedOrders = new ArrayList<>();
        final List<DestinationConflict> conflicts = new ArrayList<>();
        final List<TransitCombatResult> transitCombats = new ArrayList<>();
        final List<MovementOrder> resolvedOrders = new ArrayList<>();
        int maxSteps = 0;
        int currentStep = 0;
        boolean finalized = false;

        ResolutionContext(int turn) {
            this.turn = turn;
        }

        public int getTurn() { return turn; }
        public int getMaxSteps() { return maxSteps; }
        public int getCurrentStep() { return currentStep; }
        public boolean isFinalized() { return finalized; }
        public List<Long> getValidOrderIds() { return validOrderIds; }
        public List<MovementOrder> getActiveOrders() { return activeOrders; }
        public Set<Long> getStoppedIds() { return stoppedIds; }
        public Map<Long, Integer> getCurrentPosition() { return currentPosition; }
        public List<DestinationConflict> getConflicts() { return conflicts; }
        public List<TransitCombatResult> getTransitCombats() { return transitCombats; }
        public List<MovementOrder> getBlockedOrders() { return blockedOrders; }
        public List<MovementOrder> getResolvedOrders() { return resolvedOrders; }
    }

    /**
     * Phase 1 : valide les ordres PENDING du tour et initialise le contexte
     * (positions courantes, maxSteps). Les ordres invalides sont marqués BLOCKED
     * et persistés immédiatement.
     */
    public ResolutionContext prepareResolution(int turn, Board board) {
        ResolutionContext ctx = new ResolutionContext(turn);
        List<MovementOrder> pendingOrders = orderRepository.findPendingByTurn(turn);
        if (pendingOrders.isEmpty()) {
            return ctx;
        }

        List<MovementOrder> validOrders = new ArrayList<>();
        for (MovementOrder order : pendingOrders) {
            if (validateOrderForResolution(order, board)) {
                validOrders.add(order);
                ctx.validOrderIds.add(order.getId());
                ctx.currentPosition.put(order.getId(), order.getFromSectorNumber());
            } else {
                ctx.blockedOrders.add(order);
            }
        }
        ctx.activeOrders.addAll(validOrders);

        ctx.maxSteps = validOrders.stream()
                .mapToInt(o -> o.getRoute().size() - 1)
                .max().orElse(0);

        if (!ctx.blockedOrders.isEmpty()) {
            orderRepository.saveAll(ctx.blockedOrders);
        }
        return ctx;
    }

    /**
     * Recharge les ordres actifs du contexte depuis la base (rafraîchit
     * {@link ResolutionContext#getActiveOrders()} en excluant les ordres
     * stoppés). Utilisé par la résolution pas-à-pas entre deux hops : les
     * entités chargées au {@code prepare} deviennent détachées après la
     * commit du hop précédent, il faut donc les recharger dans la transaction
     * du hop courant.
     *
     * <p>N'est pas appelé par le chemin atomique {@link #resolveAllMovements}
     * (qui garde les mêmes entités gérées tout au long de la transaction).</p>
     */
    public void refreshActiveOrders(ResolutionContext ctx) {
        ctx.activeOrders.clear();
        if (ctx.validOrderIds.isEmpty()) {
            return;
        }
        List<Long> activeIds = ctx.validOrderIds.stream()
                .filter(id -> !ctx.stoppedIds.contains(id))
                .toList();
        if (!activeIds.isEmpty()) {
            ctx.activeOrders.addAll(orderRepository.findAllById(activeIds));
        }
    }

    /**
     * Exécute <strong>un seul</strong> hop (le {@code step} donné) : déplace
     * physiquement les entités d'un secteur, détecte les croisements et
     * enregistre les conflits de destination & combats de transit pour ce step.
     * Persiste les ordres bloqués en transit et mute le contexte (currentPosition,
     * stoppedIds). Retourne les conflits détectés sur ce hop pour que l'admin les
     * résolve un par un.
     *
     * <p>{@code board} et les ordres sont (re)chargés depuis la base — aucune
     * référence d'entité n'est conservée entre deux hops.</p>
     */
    public List<DestinationConflict> resolveStep(Board board, int step, ResolutionContext ctx) {
        List<DestinationConflict> stepConflicts = new ArrayList<>();
        if (ctx.activeOrders.isEmpty()) {
            ctx.currentStep = Math.max(ctx.currentStep, step);
            return stepConflicts;
        }

        // Ordres valides encore actifs (hors stoppedIds).
        // En mode atomique : liste chargée par prepareResolution (entités gérées).
        // En mode pas-à-pas : liste rafraîchie par l'orchestrateur à chaque hop.
        List<MovementOrder> validOrders = ctx.activeOrders.stream()
                .filter(o -> !ctx.stoppedIds.contains(o.getId()))
                .toList();

        // Calculer quelle destination chaque ordre vise à CE step
        Map<Integer, List<MovementOrder>> arrivalsPerSector = new LinkedHashMap<>();
        for (MovementOrder order : validOrders) {
            List<Integer> route = order.getRoute();
            if (route.size() > step) {
                int nextSector = route.get(step);
                arrivalsPerSector.computeIfAbsent(nextSector, k -> new ArrayList<>()).add(order);
            }
        }

        // Détecter les croisements à ce step (A→B et B→A : ils se croisent sans se combattre)
        Set<Long> crossingIds = detectStepCrossings(validOrders, ctx.stoppedIds, ctx.currentPosition, step);

        // Traiter chaque secteur vers lequel des entités arrivent
        for (Map.Entry<Integer, List<MovementOrder>> entry : arrivalsPerSector.entrySet()) {
            int targetNum = entry.getKey();
            Sector targetSector = board.getSector(targetNum);
            if (targetSector == null) {
                continue;
            }

            List<MovementOrder> arriving = entry.getValue();

            // Tous les arrivants, croiseurs inclus (ils peuvent combattre des défenseurs présents)
            Set<Long> arrivingPlayerIds = arriving.stream()
                    .map(MovementOrder::getPlayerId)
                    .collect(Collectors.toSet());

            // Arrivants non-croiseurs uniquement (pour les conflits entre arrivants)
            Set<Long> nonCrossingArrivingPlayerIds = arriving.stream()
                    .filter(o -> !crossingIds.contains(o.getId()))
                    .map(MovementOrder::getPlayerId)
                    .collect(Collectors.toSet());

            // Joueurs dont les unités quittent ce secteur en croisement avec un arrivant :
            // leur présence dans le secteur est transitoire, ils ne sont pas défenseurs
            Set<Long> leavingCrosserPlayerIds = validOrders.stream()
                    .filter(o -> crossingIds.contains(o.getId()) && !ctx.stoppedIds.contains(o.getId()))
                    .filter(o -> ctx.currentPosition.get(o.getId()).equals(targetNum))
                    .map(MovementOrder::getPlayerId)
                    .collect(Collectors.toSet());

            // Capturer les défenseurs AVANT de déplacer les arrivants
            // (inclut les unités stationnaires ET celles arrivées lors de steps précédents)
            Set<Long> defenderPlayerIds = targetSector.getCombatEntities().stream()
                    .map(CombatEntity::getPlayerId)
                    .filter(Objects::nonNull)
                    .filter(pid -> !arrivingPlayerIds.contains(pid) && !leavingCrosserPlayerIds.contains(pid))
                    .collect(Collectors.toSet());

            // Déplacer physiquement toutes les entités vers ce secteur
            for (MovementOrder order : arriving) {
                advanceOrder(order, ctx.currentPosition.get(order.getId()), targetSector, board);
                ctx.currentPosition.put(order.getId(), targetNum);
            }

            if (arrivingPlayerIds.isEmpty()) continue; // Aucun arrivant, pas de combat

            // Conflit arrivants (y compris croiseurs) vs défenseurs déjà en place
            for (Long attacker : arrivingPlayerIds) {
                for (Long defender : defenderPlayerIds) {
                    DestinationConflict conflict = new DestinationConflict(targetNum, attacker, defender);
                    stepConflicts.add(conflict);
                    ctx.conflicts.add(conflict);
                }
            }

            // Conflit entre arrivants non-croiseurs (les croiseurs A⇔B ne se combattent pas entre eux)
            List<Long> arrivingList = new ArrayList<>(nonCrossingArrivingPlayerIds);
            for (int i = 0; i < arrivingList.size(); i++) {
                for (int j = i + 1; j < arrivingList.size(); j++) {
                    DestinationConflict conflict = new DestinationConflict(
                            targetNum, arrivingList.get(i), arrivingList.get(j));
                    stepConflicts.add(conflict);
                    ctx.conflicts.add(conflict);
                }
            }

            // Combat de transit pour les véhicules passant par un secteur ennemi
            if (!defenderPlayerIds.isEmpty()) {
                for (MovementOrder order : arriving) {
                    if (!order.isVehicleMovement() || crossingIds.contains(order.getId())) continue;
                    List<Integer> route = order.getRoute();
                    if (step == route.size() - 1) continue; // Destination finale, pas un transit

                    Vehicle vehicle = vehicleRepository.findById(order.getVehicleId()).orElse(null);
                    if (vehicle == null) continue;

                    ctx.transitCombats.add(
                            new TransitCombatResult(targetNum, vehicle.getId(), vehicle.firesInTransit()));
                    // TODO : appeler CombatService ici pour résolution réelle du combat de transit
                    if (vehicle.isDestroyed()) {
                        for (CombatEntity occupant : vehicle.disembarkAll()) {
                            occupant.setSector(targetSector);
                            if (occupant instanceof Unit unit) {
                                targetSector.getArmy().add(unit);
                            } else if (occupant instanceof GameCharacter character) {
                                targetSector.getCharacters().add(character);
                            }
                        }
                        order.block("Véhicule détruit en transit au secteur " + targetNum);
                        orderRepository.save(order);
                        ctx.blockedOrders.add(order);
                        ctx.stoppedIds.add(order.getId());
                    }
                }
            }
        }

        ctx.currentStep = Math.max(ctx.currentStep, step);
        return stepConflicts;
    }

    /**
     * Phase finale : marque les ordres valides non stoppés comme RESOLVED et
     * persiste l'ensemble. Construit et retourne le compte-rendu global.
     */
    public MovementResolutionResult finalizeResolution(Board board, ResolutionContext ctx) {
        if (ctx.finalized) {
            return buildResult(ctx);
        }
        ctx.activeOrders.stream()
                .filter(o -> !ctx.stoppedIds.contains(o.getId()))
                .filter(o -> !o.isNotPending())
                .forEach(o -> {
                    o.resolve();
                    ctx.resolvedOrders.add(o);
                });
        if (!ctx.resolvedOrders.isEmpty()) {
            orderRepository.saveAll(ctx.resolvedOrders);
        }
        ctx.finalized = true;
        ctx.currentStep = ctx.maxSteps;
        return buildResult(ctx);
    }

    private MovementResolutionResult buildResult(ResolutionContext ctx) {
        MovementResolutionResult result = new MovementResolutionResult();
        ctx.resolvedOrders.forEach(result::addResolved);
        ctx.blockedOrders.forEach(result::addBlocked);
        ctx.transitCombats.forEach(result::addTransitCombat);
        ctx.conflicts.forEach(result::addConflict);
        return result;
    }

    // ============================
    // === MÉTHODES INTERNES ===
    // ============================

    /**
     * Détecte les croisements au step N : paires d'ordres ennemis qui échangent
     * exactement leurs positions (A va là où B était, B va là où A était).
     * Ces ordres se croisent sans se combattre entre eux.
     */
    private Set<Long> detectStepCrossings(List<MovementOrder> orders, Set<Long> stoppedIds,
                                          Map<Long, Integer> currentPositions, int step) {
        Set<Long> crossingIds = new HashSet<>();
        List<MovementOrder> active = orders.stream()
                .filter(o -> !stoppedIds.contains(o.getId()))
                .filter(o -> o.getRoute().size() > step)
                .toList();

        for (int i = 0; i < active.size(); i++) {
            for (int j = i + 1; j < active.size(); j++) {
                MovementOrder a = active.get(i);
                MovementOrder b = active.get(j);
                if (a.getPlayerId().equals(b.getPlayerId())) continue;

                int aNext = a.getRoute().get(step);
                int bNext = b.getRoute().get(step);
                int aCurr = currentPositions.get(a.getId());
                int bCurr = currentPositions.get(b.getId());

                // Croisement : A va là où B est, et B va là où A est
                if (aNext == bCurr && bNext == aCurr) {
                    crossingIds.add(a.getId());
                    crossingIds.add(b.getId());
                }
            }
        }
        return crossingIds;
    }

    /**
     * Avance physiquement les entités d'un ordre d'un secteur vers le secteur cible.
     * {@code fromSectorNum} est la position courante de l'ordre (peut être un secteur
     * intermédiaire pour un véhicule multi-hop).
     */
    private void advanceOrder(MovementOrder order, int fromSectorNum, Sector targetSector, Board board) {
        Sector fromSector = board.getSector(fromSectorNum);

        if (order.isVehicleMovement()) {
            Vehicle vehicle = vehicleRepository.findById(order.getVehicleId()).orElse(null);
            if (vehicle != null && !vehicle.isDestroyed()) {
                if (fromSector != null) fromSector.getVehicles().remove(vehicle);
                targetSector.getVehicles().add(vehicle);
                vehicle.setSector(targetSector);
            }
        } else if (fromSector != null) {
            List<Long> entityIds = order.getEntityIds();
            List<CombatEntity> toMove = fromSector.getCombatEntities().stream()
                    .filter(e -> entityIds.contains(e.getId()))
                    .toList();
            for (CombatEntity entity : toMove) {
                entity.setSector(targetSector);
                if (entity instanceof Unit unit) {
                    fromSector.getArmy().remove(unit);
                    targetSector.getArmy().add(unit);
                } else if (entity instanceof GameCharacter character) {
                    fromSector.getCharacters().remove(character);
                    targetSector.getCharacters().add(character);
                }
            }
        }

        if (fromSector != null) fromSector.recalculateMilitaryPower();
        targetSector.recalculateMilitaryPower();
    }

    /**
     * Validation commune aux ordres de déplacement.
     */
    private void validateBasicOrder(int from, int to, Board board) {
        if (board == null) throw new IllegalArgumentException("Le plateau de jeu est requis.");
        if (from == to) throw new IllegalArgumentException("Le secteur de départ et d'arrivée doivent être différents.");
        if (!board.hasSector(from)) throw new IllegalArgumentException("Secteur de départ inexistant : " + from);
        if (!board.hasSector(to)) throw new IllegalArgumentException("Secteur de destination inexistant : " + to);
    }

    /**
     * Vérifie que les entités spécifiées sont bien dans le secteur source.
     */
    private void validateEntitiesInSector(Sector sector, List<Long> entityIds, Long playerId) {
        if (sector == null) throw new IllegalArgumentException("Secteur source introuvable.");

        Set<Long> sectorEntityIds = sector.getCombatEntities().stream()
                .filter(e -> playerId.equals(e.getPlayerId()))
                .map(CombatEntity::getId)
                .collect(Collectors.toSet());

        for (Long entityId : entityIds) {
            if (!sectorEntityIds.contains(entityId)) {
                throw new IllegalArgumentException(
                        "L'entité " + entityId + " n'est pas dans le secteur " + sector.getNumber() + ".");
            }
        }
    }

    /**
     * Vérifie que la route ne dépasse pas la capacité de déplacement de chaque entité.
     * L'entité la plus lente contraint l'ensemble du groupe.
     */
    private void validateFootHops(Sector sector, List<Long> entityIds, Long playerId, int hops) {
        sector.getCombatEntities().stream()
                .filter(e -> playerId.equals(e.getPlayerId()) && entityIds.contains(e.getId()))
                .forEach(e -> {
                    int maxHops = (e instanceof Unit unit) ? unit.getMaxMovementHops() : 1;
                    if (hops > maxHops) {
                        throw new IllegalArgumentException(
                                "L'entité " + e.getId() + " ne peut parcourir que " + maxHops
                                + " secteur(s) par tour, mais la route en demande " + hops + ".");
                    }
                });
    }

    /**
     * Valide un ordre pour la phase de résolution (vérifications au moment de l'exécution).
     */
    private boolean validateOrderForResolution(MovementOrder order, Board board) {
        try {
            if (!board.hasSector(order.getFromSectorNumber()) || !board.hasSector(order.getToSectorNumber())) {
                order.block("Secteur inexistant.");
                return false;
            }

            if (order.isVehicleMovement()) {
                // Route toujours valide ?
                if (board.isInvalidRoute(order.getRoute())) {
                    order.block("Route invalide (secteurs non adjacents).");
                    return false;
                }
                // Véhicule encore opérationnel ?
                Vehicle vehicle = vehicleRepository.findById(order.getVehicleId()).orElse(null);
                if (vehicle == null || vehicle.cantMove()) {
                    order.block("Véhicule non opérationnel.");
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            order.block("Erreur de validation : " + e.getMessage());
            return false;
        }
    }
}
