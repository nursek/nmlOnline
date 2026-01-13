package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.Unit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un ordre de déplacement donné par un joueur.
 * Contient toutes les informations nécessaires pour résoudre le mouvement :
 * origine, destination, unités impliquées, type de mouvement, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveOrder {
    /**
     * Le joueur qui donne l'ordre
     */
    private Player player;

    /**
     * ID du secteur de départ
     */
    private int fromSectorId;

    /**
     * ID du secteur de destination
     */
    private int toSectorId;

    /**
     * Liste des unités qui se déplacent
     */
    private List<Unit> units = new ArrayList<>();

    /**
     * Type de déplacement (INTERNAL, NEUTRAL, ENEMY, DOUBLE_MOVE)
     */
    private MoveType moveType;

    /**
     * Pour les DOUBLE_MOVE : ID du secteur intermédiaire traversé (optionnel)
     */
    private Integer intermediateSectorId;

    /**
     * Indique si le mouvement a été intercepté (pour les DOUBLE_MOVE)
     */
    private boolean intercepted = false;

    /**
     * Indique si le mouvement est instantané (déplacements internes ou doubles déplacements internes)
     */
    private boolean instant = false;

    /**
     * Constructeur simplifié pour les déplacements simples
     */
    public MoveOrder(Player player, int fromSectorId, int toSectorId, List<Unit> units, MoveType moveType) {
        this.player = player;
        this.fromSectorId = fromSectorId;
        this.toSectorId = toSectorId;
        this.units = units != null ? units : new ArrayList<>();
        this.moveType = moveType;
        this.instant = (moveType == MoveType.INTERNAL);
    }

    /**
     * Constructeur pour les doubles déplacements
     */
    public MoveOrder(Player player, int fromSectorId, int toSectorId, List<Unit> units,
                     MoveType moveType, Integer intermediateSectorId) {
        this.player = player;
        this.fromSectorId = fromSectorId;
        this.toSectorId = toSectorId;
        this.units = units != null ? units : new ArrayList<>();
        this.moveType = moveType;
        this.intermediateSectorId = intermediateSectorId;
        this.instant = false; // Sera déterminé lors de la résolution
    }

    /**
     * Vérifie si cet ordre est un double déplacement
     */
    public boolean isDoubleMove() {
        return moveType == MoveType.DOUBLE_MOVE && intermediateSectorId != null;
    }

    /**
     * Retourne une représentation textuelle de l'ordre
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MoveOrder{")
          .append("player=").append(player.getName())
          .append(", from=").append(fromSectorId)
          .append(", to=").append(toSectorId)
          .append(", units=").append(units.size())
          .append(", type=").append(moveType);

        if (intermediateSectorId != null) {
            sb.append(", via=").append(intermediateSectorId);
        }

        if (intercepted) {
            sb.append(", INTERCEPTED");
        }

        if (instant) {
            sb.append(", INSTANT");
        }

        sb.append("}");
        return sb.toString();
    }
}

