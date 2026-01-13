package com.mg.nmlonline.domain.model.battle;

/**
 * Énumération représentant les différents types de déplacements possibles dans le jeu.
 */
public enum MoveType {
    /**
     * Déplacement interne au territoire du joueur (secteurs alliés adjacents).
     * Ce type de déplacement est instantané.
     */
    INTERNAL,

    /**
     * Déplacement vers un secteur neutre (non possédé).
     * Ce type de déplacement s'effectue à la fin du tour.
     */
    NEUTRAL,

    /**
     * Déplacement vers un secteur ennemi (possédé par un autre joueur).
     * Ce type de déplacement s'effectue à la fin du tour et peut déclencher un combat.
     */
    ENEMY,

    /**
     * Double déplacement traversant deux secteurs.
     * Ce type de déplacement peut être intercepté sur le secteur intermédiaire.
     * Les doubles déplacements internes (territoire allié) sont instantanés,
     * sinon ils s'effectuent à la fin du tour.
     */
    DOUBLE_MOVE
}

