package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class GamePodiumDto {

    private boolean finished;
    private Integer finalTurn;
    private String finishedAt;
    private int totalPlayers;
    private int activePlayers;
    private boolean posthumous;
    private boolean championAdjusted;
    private Integer lastGazetteTurn;
    private PlaceDto champion;
    private List<PlaceDto> places;

    @Data
    public static class PlaceDto {
        private int place;
        private Long playerId;
        private String playerName;
        private String race;
        private String characterName;
        private Integer eliminatedTurn;
        private String eliminationCause;
        private boolean definitive;
        private Long killerPlayerId;
        private String killerName;
        private int kills;
        private int assists;
        private boolean alive;
        private List<String> victims;
        private List<String> assistedVictims;
    }
}
