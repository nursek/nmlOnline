package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.GazetteIssueDto;
import com.mg.nmlonline.domain.model.board.Board;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GazetteMastheadService")
class GazetteMastheadServiceTest {

    private final GazetteMastheadService service = new GazetteMastheadService();

    private static Board board() {
        Board board = new Board();
        board.setId(1L);
        board.setName("Carte Principale");
        return board;
    }

    @Test
    void buildsTheBrigadeMastheadWithPersistedValues() {
        Instant editionDate = Instant.parse("2026-11-13T20:14:00Z");

        GazetteIssueDto.MastheadDto masthead = service.generate(board(), 7, 3, editionDate);

        assertEquals("Brigade des Morts", masthead.getJournalName());
        assertEquals(7, masthead.getVolume());
        assertTrue(masthead.getFakeUrl().startsWith("www.brigade-des-morts.nml/vol-7/n-3/"));
        assertEquals("13 novembre 2026", masthead.getDateLabel());
        assertEquals("Carte Principale", masthead.getBoardName());
        assertEquals(editionDate.toString(), masthead.getEditionDate());
        assertNotNull(masthead.getWeather());
        assertNotNull(masthead.getWeather().getCondition());
        assertNotNull(masthead.getWeather().getIcon());
        assertTrue(masthead.getWeather().getTemperature() >= 4
                && masthead.getWeather().getTemperature() <= 42);
        assertTrue(masthead.getWeather().getPrecipitation() >= 0
                && masthead.getWeather().getPrecipitation() <= 30);
        assertTrue(masthead.getWeather().getHumidity() >= 10
                && masthead.getWeather().getHumidity() <= 95);
        assertTrue(masthead.getWeather().getWind() >= 0
                && masthead.getWeather().getWind() <= 80);
        assertTrue(List.of("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSO", "SO",
                "OSO", "O", "ONO", "NO", "NNO").contains(masthead.getWeather().getWindDirection()));
    }

    @Test
    void isDeterministicForTheSameBoardAndTurn() {
        Instant editionDate = Instant.parse("2026-11-13T20:14:00Z");

        GazetteIssueDto.MastheadDto first = service.generate(board(), 7, 3, editionDate);
        GazetteIssueDto.MastheadDto second = service.generate(board(), 7, 3, editionDate);

        assertEquals(first.getFakeUrl(), second.getFakeUrl());
        assertEquals(first.getWeather().getCondition(), second.getWeather().getCondition());
        assertEquals(first.getWeather().getTemperature(), second.getWeather().getTemperature());
    }
}
