package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class GazetteIssueDto {

    private int schemaVersion = 1;
    private int turn;
    private int issueNumber;
    private String publishedAt;
    private String updatedAt;
    private boolean finalEdition;
    private MastheadDto masthead;
    private List<ArticleDto> frontPage;
    private List<ArticleDto> briefs;

    @Data
    public static class MastheadDto {
        private String journalName;
        private String subtitle;
        private String fakeUrl;
        private int volume;
        private String editionDate;
        private String dateLabel;
        private String boardName;
        private WeatherDto weather;
    }

    @Data
    public static class WeatherDto {
        private String condition;
        private String icon;
        private int temperature;
        private int precipitation;
        private int humidity;
        private int wind;
        private String windDirection;
    }

    @Data
    public static class ArticleDto {
        private String kind;
        private String section;
        private String headline;
        private String quote;
        private String body;
        private String author;
        private String photoPlayerName;
        private String photoCaption;
    }
}
