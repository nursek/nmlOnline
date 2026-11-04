package com.mg.nmlonline.domain.model.vehicle;

import com.mg.nmlonline.domain.model.player.PlayerRace;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public enum VehicleType {

    TOURELLE("Véhicule à tourelle", 25, 40, 1300, 2, 1, 0, false, false, 4,
            Map.of(PlayerRace.ORKS, "Motoch'nill'",
                    PlayerRace.NECRONS, "Barge d'annihilation")),

    VTT_LEGER("VTT léger", 0, 50, 4000, 2, 10, 0, false, false, 5,
            Map.of(PlayerRace.ORKS, "Rukkatrukk Squigbuggy",
                    PlayerRace.NECRONS, "Arche fantôme")),

    VTT_BLINDE("VTT blindé", 100, 150, 6500, 2, 10, 0, true, false, 7,
            Map.of(PlayerRace.ORKS, "Chariot de guerre",
                    PlayerRace.NECRONS, "Monolithe")),

    TANK("Tank de combat", 125, 250, 7500, 1, 0, 50, false, false, 8,
            Map.of(PlayerRace.ORKS, "Morkanaut",
                    PlayerRace.NECRONS, "Barge de commandement")),

    HELICOPTERE("Hélicoptère de combat", 250, 125, 9000, 2, 5, 0, true, true, 8,
            Map.of(PlayerRace.ORKS, "Kopters d'la Mort",
                    PlayerRace.NECRONS, "Moissonneuse funèbre")),

    AVION_TRANSPORT("Avion de transport", 0, 1000, 15000, 4, 50, 0, false, true, 12,
            Map.of(PlayerRace.ORKS, "Dakkajet",
                    PlayerRace.NECRONS, "Moissonneuse nocturne"));

    private final String displayName;
    private final double basePdf;
    private final double baseDefense;
    private final int cost;
    private final int speed;           // Nombre max de secteurs par tour
    private final int capacity;        // Nombre de passagers (hors pilote)
    private final int resistance;      // % de résistance aux dégâts (ex: Tank = 50)
    private final boolean firesInTransit;
    private final boolean isAerial;
    private final int availableFromTurn;
    private final Map<PlayerRace, String> raceNames;

    public boolean isAvailableAt(int turn) {
        return turn >= availableFromTurn;
    }

    /** Repli sur le nom générique tant qu'une race n'est pas traduite. */
    public String displayNameFor(PlayerRace race) {
        return race == null ? displayName : raceNames.getOrDefault(race, displayName);
    }
}