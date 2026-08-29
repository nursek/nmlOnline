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
 * Ordres de déplacement collectés pendant le tour, résolus simultanément en fin
 * de tour par {@link #resolveAllMovements}.
 *
 * <p>Règles : croisement A→B/B→A sans combat ; arrivée en secteur occupé =
 * combat ; transit véhicule : seul le véhicule combat (passagers débarqués si
 * détruit) ; bâtiments : déplacement en secteur allié uniquement.</p>
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

    /** Route ≥ 2 secteurs ; les unités LEGER font 2 hops, les autres 1. */
    public MovementOrder placeFootOrder(Long playerId, int turn, List<Long> entityIds,
                                        List<Integer> route, Board board) {
        if (route == null || route.size() < 2) {
            throw new IllegalArgumentException("La route doit contenir au moins 2 secteurs.");
        }

        int from = route.getFirst();
        int to   = route.getLast();
        validateBasicOrder(from, to, board);

        if (board.isInvalidRoute(route)) {
            throw new IllegalArgumentException("La route n'est pas valide (secteurs non adjacents).");
        }

        Sector fromSector = board.getSector(from);
        validateEntitiesInSector(fromSector, entityIds, playerId);

        int hops = route.size() - 1;
        validateFootHops(fromSector, entityIds, playerId, hops);

        MovementOrder order = MovementOrder.createFootOrder(playerId, turn, entityIds, route);
        return orderRepository.save(order);
    }

    public MovementOrder placeVehicleOrder(Long playerId, int turn, Long vehicleId,
                                           List<Integer> route, Board board) {
        if (route == null || route.size() < 2) {
            throw new IllegalArgumentException("La route doit contenir au moins 2 secteurs.");
        }

        int from = route.getFirst();
        int to = route.getLast();
        validateBasicOrder(from, to, board);

        if (board.isInvalidRoute(route)) {
            throw new IllegalArgumentException("La route n'est pas valide (secteurs non adjacents).");
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Véhicule introuvable : " + vehicleId));

        if (!vehicle.getPlayerId().equals(playerId)) {
            throw new SecurityException("Ce véhicule n'appartient pas au joueur.");
        }

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

        int hops = route.size() - 1;
        if (hops > vehicle.getSpeed()) {
            throw new IllegalArgumentException(
                    "Le véhicule ne peut parcourir que " + vehicle.getSpeed() + " secteur(s) par tour, "
                    + "mais la route en demande " + hops + ".");
        }

        MovementOrder order = MovementOrder.createVehicleOrder(playerId, turn, vehicleId, route);
        return orderRepository.save(order);
    }
    
    /** Lève une exception par cause d'échec → HTTP distinct : 404 introuvable, 403 non possédé, 409 non PENDING. */
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

    public List<MovementOrder> getPlayerOrders(Long playerId, int turn) {
        return orderRepository.findByPlayerIdAndTurnAndStatus(playerId, turn, MovementStatus.PENDING);
    }

    /**
     * Résout tous les ordres du tour incrémentalement, un secteur (step) à la fois.
     * À chaque step, les entités encore en route avancent simultanément d'un secteur ;
     * les arrivées des steps précédents restent en place et sont tangibles pour les nouveaux
     * arrivants. Conflits et combats de transit détectés à la fin de chaque step.
     */
    public MovementResolutionResult resolveAllMovements(int turn, Board board) {
        ResolutionContext ctx = prepareResolution(turn, board);
        for (int step = 1; step <= ctx.maxSteps; step++) {
            resolveStep(board, step, ctx);
        }
        return finalizeResolution(board, ctx);
    }

    /**
     * État partagé entre les appels pas-à-pas {@code prepare → resolveStep × N → finalize}.
     * Ne contient que des valeurs (IDs, numéros de secteurs) — aucune entité JPA — afin de
     * survivre aux changements de transaction entre deux hops admin.
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

    /** Valide les ordres PENDING, initialise le contexte. Les ordres invalides sont BLOCKED et persistés. */
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
     * Recharge les ordres actifs depuis la base entre deux hops : les entités du
     * {@code prepare} sont détachées après la commit du hop précédent. Non appelé
     * par le chemin atomique {@link #resolveAllMovements}.
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
            // Un joueur peut annuler son ordre via cancelOrderOrThrow (hors TurnLock) pendant
            // la pause admin ; sans ce filtre l'ordre CANCELLED serait rechargé et déplacé.
            ctx.activeOrders.addAll(orderRepository.findAllById(activeIds).stream()
                    .filter(o -> !o.isNotPending())
                    .toList());
        }
    }

    /**
     * Exécute un seul hop : déplace les entités d'un secteur, détecte croisements,
     * enregistre les conflits de destination et combats de transit, persiste les
     * ordres bloqués en transit. Retourne les conflits du hop pour résolution admin.
     */
    public List<DestinationConflict> resolveStep(Board board, int step, ResolutionContext ctx) {
        List<DestinationConflict> stepConflicts = new ArrayList<>();
        if (ctx.activeOrders.isEmpty()) {
            ctx.currentStep = Math.max(ctx.currentStep, step);
            return stepConflicts;
        }

        // En mode atomique : entités gérées du prepare. En pas-à-pas : rafraîchies à chaque hop.
        List<MovementOrder> validOrders = ctx.activeOrders.stream()
                .filter(o -> !ctx.stoppedIds.contains(o.getId()))
                .toList();

        Map<Integer, List<MovementOrder>> arrivalsPerSector = new LinkedHashMap<>();
        for (MovementOrder order : validOrders) {
            List<Integer> route = order.getRoute();
            if (route.size() > step) {
                int nextSector = route.get(step);
                arrivalsPerSector.computeIfAbsent(nextSector, k -> new ArrayList<>()).add(order);
            }
        }

        Set<Long> crossingIds = detectStepCrossings(validOrders, ctx.stoppedIds, ctx.currentPosition, step);

        for (Map.Entry<Integer, List<MovementOrder>> entry : arrivalsPerSector.entrySet()) {
            int targetNum = entry.getKey();
            Sector targetSector = board.getSector(targetNum);
            if (targetSector == null) {
                continue;
            }

            List<MovementOrder> arriving = entry.getValue();

            // Croiseurs inclus : ils peuvent combattre les défenseurs présents.
            Set<Long> arrivingPlayerIds = arriving.stream()
                    .map(MovementOrder::getPlayerId)
                    .collect(Collectors.toSet());

            // Non-croiseurs uniquement : pour les conflits entre arrivants (A⇔B ne se combattent pas).
            Set<Long> nonCrossingArrivingPlayerIds = arriving.stream()
                    .filter(o -> !crossingIds.contains(o.getId()))
                    .map(MovementOrder::getPlayerId)
                    .collect(Collectors.toSet());

            // Unités quittant ce secteur en croisement : présence transitoire, ne sont pas défenseurs.
            Set<Long> leavingCrosserPlayerIds = validOrders.stream()
                    .filter(o -> crossingIds.contains(o.getId()) && !ctx.stoppedIds.contains(o.getId()))
                    .filter(o -> ctx.currentPosition.get(o.getId()).equals(targetNum))
                    .map(MovementOrder::getPlayerId)
                    .collect(Collectors.toSet());

            // Capturer les défenseurs AVANT de déplacer les arrivants (stationnaires + arrivés aux steps précédents).
            Set<Long> defenderPlayerIds = targetSector.getCombatEntities().stream()
                    .map(CombatEntity::getPlayerId)
                    .filter(Objects::nonNull)
                    .filter(pid -> !arrivingPlayerIds.contains(pid) && !leavingCrosserPlayerIds.contains(pid))
                    .collect(Collectors.toSet());

            for (MovementOrder order : arriving) {
                advanceOrder(order, ctx.currentPosition.get(order.getId()), targetSector, board);
                ctx.currentPosition.put(order.getId(), targetNum);
            }

            if (arrivingPlayerIds.isEmpty()) continue;

            // Conflit arrivants (croiseurs inclus) vs défenseurs en place.
            for (Long attacker : arrivingPlayerIds) {
                for (Long defender : defenderPlayerIds) {
                    DestinationConflict conflict = new DestinationConflict(targetNum, attacker, defender);
                    stepConflicts.add(conflict);
                    ctx.conflicts.add(conflict);
                }
            }

            // Conflits entre arrivants non-croiseurs (A⇔B ne se combattent pas).
            List<Long> arrivingList = new ArrayList<>(nonCrossingArrivingPlayerIds);
            for (int i = 0; i < arrivingList.size(); i++) {
                for (int j = i + 1; j < arrivingList.size(); j++) {
                    DestinationConflict conflict = new DestinationConflict(
                            targetNum, arrivingList.get(i), arrivingList.get(j));
                    stepConflicts.add(conflict);
                    ctx.conflicts.add(conflict);
                }
            }

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

    /** Marque les ordres non stoppés RESOLVED, persiste, et retourne le compte-rendu global. */
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

    /** Croisements au step N : paires ennemies qui échangent exactement leurs positions (se croisent sans combat). */
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

                if (aNext == bCurr && bNext == aCurr) {
                    crossingIds.add(a.getId());
                    crossingIds.add(b.getId());
                }
            }
        }
        return crossingIds;
    }

    /** {@code fromSectorNum} = position courante de l'ordre (secteur intermédiaire pour un véhicule multi-hop). */
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

    private void validateBasicOrder(int from, int to, Board board) {
        if (board == null) throw new IllegalArgumentException("Le plateau de jeu est requis.");
        if (from == to) throw new IllegalArgumentException("Le secteur de départ et d'arrivée doivent être différents.");
        if (!board.hasSector(from)) throw new IllegalArgumentException("Secteur de départ inexistant : " + from);
        if (!board.hasSector(to)) throw new IllegalArgumentException("Secteur de destination inexistant : " + to);
    }

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

    private boolean validateOrderForResolution(MovementOrder order, Board board) {
        try {
            if (!board.hasSector(order.getFromSectorNumber()) || !board.hasSector(order.getToSectorNumber())) {
                order.block("Secteur inexistant.");
                return false;
            }

            if (order.isVehicleMovement()) {
                if (board.isInvalidRoute(order.getRoute())) {
                    order.block("Route invalide (secteurs non adjacents).");
                    return false;
                }
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
