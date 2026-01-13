package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.Unit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Représente la configuration d'une bataille à un emplacement donné.
 * Regroupe tous les joueurs et leurs unités présents dans un secteur après résolution des mouvements.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BattleSetup {
    /**
     * ID du secteur où la bataille a lieu
     */
    private int sectorId;

    /**
     * Map associant chaque joueur à ses unités présentes dans le secteur
     * Clé : Player, Valeur : List<Unit>
     */
    private Map<Player, List<Unit>> playerUnits = new HashMap<>();

    /**
     * Type de bataille (ATTACK, DEFENSE, MULTI_PLAYER, etc.)
     */
    private BattleType battleType;

    /**
     * ID du joueur qui possédait initialement le secteur (peut être null si neutre).
     */
    private Long originalOwnerId;

    /**
     * Constructeur simplifié
     */
    public BattleSetup(int sectorId) {
        this.sectorId = sectorId;
        this.playerUnits = new HashMap<>();
    }

    /**
     * Ajoute des unités pour un joueur
     */
    public void addUnits(Player player, List<Unit> units) {
        if (player == null || units == null || units.isEmpty()) {
            return;
        }

        playerUnits.computeIfAbsent(player, k -> new ArrayList<>()).addAll(units);
    }

    /**
     * Retourne le nombre de joueurs distincts impliqués
     */
    public int getPlayerCount() {
        return playerUnits.size();
    }

    /**
     * Retourne la liste des joueurs impliqués
     */
    public List<Player> getPlayers() {
        return new ArrayList<>(playerUnits.keySet());
    }

    /**
     * Retourne les unités d'un joueur spécifique
     */
    public List<Unit> getUnitsForPlayer(Player player) {
        return playerUnits.getOrDefault(player, new ArrayList<>());
    }

    /**
     * Vérifie si une bataille doit avoir lieu (au moins 2 joueurs différents)
     */
    public boolean hasBattle() {
        return getPlayerCount() >= 2;
    }

    /**
     * Détermine automatiquement le type de bataille en fonction du contexte
     */
    public void determineBattleType() {
        if (getPlayerCount() == 0) {
            battleType = BattleType.NONE;
        } else if (getPlayerCount() == 1) {
            battleType = BattleType.OCCUPATION;
        } else if (getPlayerCount() == 2) {
            // Combat entre 2 joueurs
            List<Player> players = getPlayers();
            if (originalOwnerId != null && players.stream().anyMatch(p -> p.getId().equals(originalOwnerId))) {
                battleType = BattleType.DEFENSE;
            } else {
                battleType = BattleType.ATTACK;
            }
        } else {
            // Combat impliquant 3 joueurs ou plus
            battleType = BattleType.MULTI_PLAYER;
        }
    }

    /**
     * Retourne une représentation textuelle du setup
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BattleSetup{")
          .append("sector=").append(sectorId)
          .append(", type=").append(battleType)
          .append(", players=").append(getPlayerCount())
          .append(", details=[");

        for (Map.Entry<Player, List<Unit>> entry : playerUnits.entrySet()) {
            sb.append("\n    ")
              .append(entry.getKey().getName())
              .append(": ")
              .append(entry.getValue().size())
              .append(" units");
        }

        sb.append("\n]}");
        return sb.toString();
    }
}

