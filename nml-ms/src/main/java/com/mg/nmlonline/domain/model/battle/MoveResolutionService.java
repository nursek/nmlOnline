package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsable de la résolution des mouvements simultanés.
 * Gère les déplacements, les interceptions, les croisements et identifie les batailles.
 */
@Slf4j
public record MoveResolutionService(Board board) {

    /**
     * Méthode principale de résolution des mouvements.
     * Prend tous les ordres de déplacement et retourne les batailles à résoudre.
     *
     * @param orders Liste de tous les ordres de mouvement donnés par les joueurs
     * @return Liste des BattleSetup représentant les batailles à résoudre
     */
    public List<BattleSetup> resolveMoves(List<MoveOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            log.info("Aucun ordre de déplacement à résoudre");
            return new ArrayList<>();
        }

        log.info("\n========================================");
        log.info("DÉBUT DE LA RÉSOLUTION DES MOUVEMENTS");
        log.info("========================================");
        log.info("Nombre total d'ordres : {}", orders.size());

        // Afficher tous les ordres
        for (MoveOrder order : orders) {
            log.info("  - {}", order);
        }

        // Phase 1 : Validation des ordres
        List<MoveOrder> validOrders = validateOrders(orders);
        log.info("\nOrdres valides : {}/{}", validOrders.size(), orders.size());

        // Phase 2 : Déterminer les ordres instantanés
        categorizeOrders(validOrders);

        // Phase 3 : Exécuter les mouvements instantanés
        List<MoveOrder> instantOrders = validOrders.stream()
                .filter(MoveOrder::isInstant)
                .collect(Collectors.toList());
        log.info("\n--- PHASE 1 : Mouvements instantanés ---");
        log.info("Nombre de mouvements instantanés : {}", instantOrders.size());
        executeInstantMoves(instantOrders);

        // Phase 4 : Filtrer les croisements pour les ordres non instantanés
        List<MoveOrder> nonInstantOrders = validOrders.stream()
                .filter(o -> !o.isInstant())
                .collect(Collectors.toList());
        log.info("\n--- PHASE 2 : Détection des croisements ---");
        List<MoveOrder> nonCrossingOrders = filterCrossings(nonInstantOrders);
        log.info("Ordres après filtrage des croisements : {}", nonCrossingOrders.size());

        // Phase 5 : Vérifier les interceptions pour les doubles déplacements
        log.info("\n--- PHASE 3 : Vérification des interceptions ---");
        checkInterceptions(nonCrossingOrders);

        // Phase 6 : Exécuter les mouvements restants
        log.info("\n--- PHASE 4 : Exécution des mouvements ---");
        executeMoves(nonCrossingOrders);

        // Phase 7 : Identifier les batailles
        log.info("\n--- PHASE 5 : Identification des batailles ---");
        List<BattleSetup> battles = identifyBattles(nonCrossingOrders);
        log.info("Nombre de batailles identifiées : {}", battles.size());

        log.info("\n========================================");
        log.info("FIN DE LA RÉSOLUTION DES MOUVEMENTS");
        log.info("========================================\n");

        return battles;
    }

    /**
     * Valide les ordres de mouvement (vérification des secteurs, joueurs, unités)
     */
    private List<MoveOrder> validateOrders(List<MoveOrder> orders) {
        List<MoveOrder> validOrders = new ArrayList<>();

        for (MoveOrder order : orders) {
            if (order.getPlayer() == null) {
                log.warn("Ordre invalide : joueur null");
                continue;
            }
            if (order.getUnits() == null || order.getUnits().isEmpty()) {
                log.warn("Ordre invalide pour {} : aucune unité", order.getPlayer().getName());
                continue;
            }
            if (board.getSector(order.getFromSectorId()) == null) {
                log.warn("Ordre invalide pour {} : secteur source {} inexistant",
                        order.getPlayer().getName(), order.getFromSectorId());
                continue;
            }
            if (board.getSector(order.getToSectorId()) == null) {
                log.warn("Ordre invalide pour {} : secteur destination {} inexistant",
                        order.getPlayer().getName(), order.getToSectorId());
                continue;
            }
            if (order.isDoubleMove() && board.getSector(order.getIntermediateSectorId()) == null) {
                log.warn("Ordre invalide pour {} : secteur intermédiaire {} inexistant",
                        order.getPlayer().getName(), order.getIntermediateSectorId());
                continue;
            }

            validOrders.add(order);
        }

        return validOrders;
    }

    /**
     * Catégorise les ordres et détermine s'ils sont instantanés
     */
    private void categorizeOrders(List<MoveOrder> orders) {
        for (MoveOrder order : orders) {
            if (order.getMoveType() == MoveType.INTERNAL) {
                order.setInstant(true);
            } else if (order.getMoveType() == MoveType.DOUBLE_MOVE) {
                // Un double déplacement est instantané si tous les secteurs traversés appartiennent au joueur
                boolean allOwnedByPlayer = isOwnedByPlayer(order.getFromSectorId(), order.getPlayer())
                        && isOwnedByPlayer(order.getIntermediateSectorId(), order.getPlayer())
                        && isOwnedByPlayer(order.getToSectorId(), order.getPlayer());
                order.setInstant(allOwnedByPlayer);
            } else {
                order.setInstant(false);
            }
        }
    }

    /**
     * Vérifie si un secteur appartient à un joueur
     */
    private boolean isOwnedByPlayer(int sectorId, Player player) {
        Sector sector = board.getSector(sectorId);
        return sector != null && sector.getOwnerId() != null
                && sector.getOwnerId().equals(player.getId());
    }

    /**
     * Exécute les mouvements instantanés (déplacement immédiat des unités)
     */
    private void executeInstantMoves(List<MoveOrder> instantOrders) {
        for (MoveOrder order : instantOrders) {
            log.info("Mouvement instantané : {} déplace {} unités de {} vers {}",
                    order.getPlayer().getName(), order.getUnits().size(),
                    order.getFromSectorId(), order.getToSectorId());

            // Déplacer les unités
            moveUnits(order);
        }
    }

    /**
     * Filtre les ordres pour éliminer les croisements.
     * Si Joueur A va de X→Y et Joueur B va de Y→X, les deux ordres sont annulés.
     */
    private List<MoveOrder> filterCrossings(List<MoveOrder> orders) {
        List<MoveOrder> nonCrossingOrders = new ArrayList<>(orders);
        Set<MoveOrder> crossedOrders = new HashSet<>();

        for (int i = 0; i < orders.size(); i++) {
            MoveOrder order1 = orders.get(i);
            if (crossedOrders.contains(order1)) continue;

            for (int j = i + 1; j < orders.size(); j++) {
                MoveOrder order2 = orders.get(j);
                if (crossedOrders.contains(order2)) continue;

                // Vérifier si les ordres se croisent
                if (order1.getFromSectorId() == order2.getToSectorId()
                        && order1.getToSectorId() == order2.getFromSectorId()
                        && !order1.getPlayer().equals(order2.getPlayer())) {

                    log.info("CROISEMENT DÉTECTÉ : {} ({}->{}) et {} ({}->{}) se croisent",
                            order1.getPlayer().getName(), order1.getFromSectorId(), order1.getToSectorId(),
                            order2.getPlayer().getName(), order2.getFromSectorId(), order2.getToSectorId());

                    crossedOrders.add(order1);
                    crossedOrders.add(order2);

                    // Les unités restent sur place
                    break;
                }
            }
        }

        nonCrossingOrders.removeAll(crossedOrders);
        return nonCrossingOrders;
    }

    /**
     * Vérifie les interceptions pour les doubles déplacements.
     * Un double déplacement peut être intercepté si des ennemis sont présents sur le secteur intermédiaire.
     */
    private void checkInterceptions(List<MoveOrder> orders) {
        for (MoveOrder order : orders) {
            if (!order.isDoubleMove()) continue;

            Sector intermediateSector = board.getSector(order.getIntermediateSectorId());

            // Vérifier si des ennemis sont présents sur le secteur intermédiaire
            boolean hasEnemies = false;
            if (intermediateSector.getArmy() != null && !intermediateSector.getArmy().isEmpty()) {
                // Si le secteur intermédiaire appartient à un autre joueur ou a des unités ennemies
                if (intermediateSector.getOwnerId() != null
                        && !intermediateSector.getOwnerId().equals(order.getPlayer().getId())) {
                    hasEnemies = true;
                }
            }

            // Vérifier aussi si d'autres joueurs se déplacent vers le secteur intermédiaire
            for (MoveOrder otherOrder : orders) {
                if (otherOrder.equals(order)) continue;
                if (otherOrder.getToSectorId() == order.getIntermediateSectorId()
                        && !otherOrder.getPlayer().equals(order.getPlayer())) {
                    hasEnemies = true;
                    break;
                }
            }

            if (hasEnemies) {
                order.setIntercepted(true);
                log.info("INTERCEPTION : {} est intercepté au secteur {} lors de son déplacement {}->{}->{}",
                        order.getPlayer().getName(), order.getIntermediateSectorId(),
                        order.getFromSectorId(), order.getIntermediateSectorId(), order.getToSectorId());

                // Le mouvement s'arrête au secteur intermédiaire
                order.setToSectorId(order.getIntermediateSectorId());
            }
        }
    }

    /**
     * Exécute tous les mouvements non instantanés
     */
    private void executeMoves(List<MoveOrder> orders) {
        for (MoveOrder order : orders) {
            if (order.isIntercepted()) {
                log.info("Mouvement intercepté : {} déplace {} unités de {} vers {} (interception)",
                        order.getPlayer().getName(), order.getUnits().size(),
                        order.getFromSectorId(), order.getToSectorId());
            } else {
                log.info("Mouvement : {} déplace {} unités de {} vers {}",
                        order.getPlayer().getName(), order.getUnits().size(),
                        order.getFromSectorId(), order.getToSectorId());
            }

            moveUnits(order);
        }
    }

    /**
     * Déplace physiquement les unités d'un secteur à un autre
     */
    private void moveUnits(MoveOrder order) {
        Sector fromSector = board.getSector(order.getFromSectorId());
        Sector toSector = board.getSector(order.getToSectorId());

        // Retirer les unités du secteur source
        fromSector.getArmy().removeAll(order.getUnits());

        // Ajouter les unités au secteur destination
        if (toSector.getArmy() == null) {
            toSector.setArmy(new ArrayList<>());
        }
        toSector.getArmy().addAll(order.getUnits());
    }

    /**
     * Identifie les batailles en regroupant les ordres par secteur de destination
     * et en détectant les conflits entre joueurs ennemis.
     */
    private List<BattleSetup> identifyBattles(List<MoveOrder> orders) {
        // Regrouper les ordres par secteur de destination
        Map<Integer, List<MoveOrder>> ordersBySector = new HashMap<>();
        for (MoveOrder order : orders) {
            ordersBySector.computeIfAbsent(order.getToSectorId(), k -> new ArrayList<>()).add(order);
        }

        List<BattleSetup> battles = new ArrayList<>();

        // Pour chaque secteur, vérifier s'il y a des conflits
        for (Map.Entry<Integer, List<MoveOrder>> entry : ordersBySector.entrySet()) {
            int sectorId = entry.getKey();
            List<MoveOrder> sectorOrders = entry.getValue();

            Sector sector = board.getSector(sectorId);
            BattleSetup battleSetup = new BattleSetup(sectorId);
            battleSetup.setOriginalOwnerId(sector.getOwnerId());

            // Ajouter les unités déjà présentes dans le secteur (défenseurs potentiels)
            if (sector.getArmy() != null && !sector.getArmy().isEmpty()) {
                // Identifier le propriétaire des unités déjà présentes
                if (sector.getOwnerId() != null) {
                    // Trouver le joueur correspondant parmi les ordres
                    Player owner = sectorOrders.stream()
                            .map(MoveOrder::getPlayer)
                            .filter(p -> p.getId().equals(sector.getOwnerId()))
                            .findFirst()
                            .orElse(null);

                    if (owner != null) {
                        // Combiner unités présentes et unités qui arrivent pour le propriétaire
                        List<Unit> allUnits = new ArrayList<>(sector.getArmy());
                        battleSetup.addUnits(owner, allUnits);
                    }
                }
            }

            // Ajouter les unités de tous les joueurs qui arrivent
            for (MoveOrder order : sectorOrders) {
                // Si le joueur est déjà dans le battleSetup (propriétaire), combiner les unités
                if (battleSetup.getPlayerUnits().containsKey(order.getPlayer())) {
                    battleSetup.getPlayerUnits().get(order.getPlayer()).addAll(order.getUnits());
                } else {
                    battleSetup.addUnits(order.getPlayer(), new ArrayList<>(order.getUnits()));
                }
            }

            // Déterminer le type de bataille
            battleSetup.determineBattleType();

            // Ajouter à la liste si c'est une vraie bataille
            if (battleSetup.hasBattle()) {
                log.info("BATAILLE au secteur {} : {} joueurs impliqués", sectorId, battleSetup.getPlayerCount());
                for (Player player : battleSetup.getPlayers()) {
                    log.info("  - {} : {} unités", player.getName(),
                            battleSetup.getUnitsForPlayer(player).size());
                }
                battles.add(battleSetup);
            } else if (battleSetup.getPlayerCount() == 1) {
                log.info("OCCUPATION du secteur {} par {}", sectorId,
                        battleSetup.getPlayers().getFirst().getName());
            }
        }

        return battles;
    }

    /**
     * Méthode utilitaire pour créer un ordre de déplacement automatiquement typé
     */
    public static MoveOrder createMoveOrder(Player player, int from, int to, List<Unit> units, Board board) {
        Sector fromSector = board.getSector(from);
        Sector toSector = board.getSector(to);

        MoveType moveType;
        if (fromSector.isOwnedBy(player.getId()) && toSector.isOwnedBy(player.getId())) {
            moveType = MoveType.INTERNAL;
        } else if (toSector.getOwnerId() == null) {
            moveType = MoveType.NEUTRAL;
        } else {
            moveType = MoveType.ENEMY;
        }

        return new MoveOrder(player, from, to, units, moveType);
    }

    /**
     * Méthode utilitaire pour créer un double déplacement
     */
    public static MoveOrder createDoubleMoveOrder(Player player, int from, int via, int to,
                                                  List<Unit> units, Board board) {
        return new MoveOrder(player, from, to, units, MoveType.DOUBLE_MOVE, via);
    }
}

