package com.mg.nmlonline.api.dto;

import lombok.Data;

/**
 * Compte-rendu d'une bataille résolue sur un secteur (résolution pas-à-pas
 * admin). Porte les compteurs de pertes et blessés par camp, ainsi que le
 * vainqueur si {@link com.mg.nmlonline.domain.model.battle.Battle} en a fixé un.
 *
 * <p>La même structure est utilisée pour le détail d'une bataille venant d'être
 * résolue (retour de {@code resolve-battle}) et pour l'historique des
 * {@code resolvedConflicts} de l'état de session.</p>
 */
@Data
public class ResolvedBattleDto {
    private int sectorNumber;
    private Long attackerPlayerId;
    private String attackerName;
    private Long defenderPlayerId;
    private String defenderName;
    private boolean success;
    private String message;
    private Long winnerId;
    private String winnerName;
    private int attackerCasualties;
    private int defenderCasualties;
    private int attackerInjured;
    private int defenderInjured;
}