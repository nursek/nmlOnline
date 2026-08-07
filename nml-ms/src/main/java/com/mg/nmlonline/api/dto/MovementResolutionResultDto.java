package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

/**
 * Compte-rendu de résolution des mouvements d'un tour, exposé à l'admin :
 * ordres résolus/bloqués, conflits potentiels détectés et combats de transit.
 *
 * <p>Une variante « aperçu » (dry-run) est construite par
 * {@link com.mg.nmlonline.domain.service.MovementAdminService#previewMovements(int)}
 * sans persister les effets : la transaction est rollback après
 * calcul du résultat, les ordres restent PENDING et les entités non déplacées.
 */
@Data
public class MovementResolutionResultDto {
    private int turn;
    private List<AdminMovementOrderDto> resolved;
    private List<AdminMovementOrderDto> blocked;
    private List<DestinationConflictDto> conflicts;
    private List<TransitCombatResultDto> transitCombats;
    private boolean hasConflicts;
    private boolean hasTransitCombats;
}