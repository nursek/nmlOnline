package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class GazetteEditorNotesDto {

    private List<EliminationNote> eliminations;
    private List<BattleNote> battles;
    private List<CaptureNote> captures;
    private List<AnnouncementNote> announcements;
    private List<PlayerNote> players;
    private OutcomeNote outcome;

    @Data
    public static class EliminationNote {
        private String victimName;
        private int eliminatedTurn;
        private String cause;
        private String killerName;
        private List<String> assists;
        private String characterName;
        private Integer battleSector;
    }

    @Data
    public static class BattleNote {
        private int sectorNumber;
        private boolean standoff;
        private String winnerName;
        private String attackerName;
        private String defenderName;
        private int attackerCasualties;
        private int defenderCasualties;
        private boolean attackerCharacterLost;
        private boolean defenderCharacterLost;
        private boolean headquartersCaptured;
        private List<String> log;
    }

    @Data
    public static class CaptureNote {
        private int sectorNumber;
        private String ownerName;
        private boolean onTheFly;
    }

    @Data
    public static class AnnouncementNote {
        private String type;
        private String actorName;
        private String targetName;
        private int turnCreated;
    }

    @Data
    public static class PlayerNote {
        private String name;
        private boolean alive;
        private int sectorCount;
    }

    @Data
    public static class OutcomeNote {
        private boolean finished;
        private String winnerName;
        private boolean posthumous;
        private boolean adjusted;
    }
}
