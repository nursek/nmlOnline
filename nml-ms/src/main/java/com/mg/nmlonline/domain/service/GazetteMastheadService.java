package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.GazetteIssueDto;
import com.mg.nmlonline.domain.model.board.Board;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Service
public class GazetteMastheadService {

    public static final String JOURNAL_NAME = "Brigade des Morts";
    static final String FAKE_URL_BASE = "www.brigade-des-morts.nml";

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
    private static final List<String> SLUGS =
            List.of("le-front", "les-cendres", "la-une", "le-carnet", "l-edition");
    private static final List<String> DIRECTIONS = List.of("N", "NNE", "NE", "ENE", "E", "ESE",
            "SE", "SSE", "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO");
    private static final List<WeatherCondition> CONDITIONS = List.of(
            new WeatherCondition("Ciel de plomb", "cloud", 8, 18, 2, 8, 50, 75, 5, 20),
            new WeatherCondition("Brouillard de cendres", "foggy", 4, 14, 0, 3, 70, 95, 0, 10),
            new WeatherCondition("Averse acide", "water_drop", 12, 24, 12, 30, 75, 95, 15, 35),
            new WeatherCondition("Vent de sable", "air", 25, 42, 0, 1, 10, 30, 40, 80),
            new WeatherCondition("Nuit claire", "nights_stay", 5, 20, 0, 0, 30, 55, 0, 15),
            new WeatherCondition("Orage magnétique", "thunderstorm", 15, 30, 10, 25, 60, 90, 30, 70));

    public GazetteIssueDto.MastheadDto generate(Board board, int turn, int issueNumber,
                                                Instant editionDate) {
        Random random = new Random(board.getId() * 1_000_003L + turn);
        WeatherCondition condition = CONDITIONS.get(random.nextInt(CONDITIONS.size()));

        GazetteIssueDto.WeatherDto weather = new GazetteIssueDto.WeatherDto();
        weather.setCondition(condition.label());
        weather.setIcon(condition.icon());
        weather.setTemperature(between(random, condition.minTemperature(), condition.maxTemperature()));
        weather.setPrecipitation(between(random, condition.minPrecipitation(),
                condition.maxPrecipitation()));
        weather.setHumidity(between(random, condition.minHumidity(), condition.maxHumidity()));
        weather.setWind(between(random, condition.minWind(), condition.maxWind()));
        weather.setWindDirection(DIRECTIONS.get(random.nextInt(DIRECTIONS.size())));

        GazetteIssueDto.MastheadDto masthead = new GazetteIssueDto.MastheadDto();
        masthead.setJournalName(JOURNAL_NAME);
        masthead.setSubtitle("Organe des conflits de " + board.getName());
        masthead.setFakeUrl(FAKE_URL_BASE + "/vol-" + turn + "/n-" + issueNumber + "/"
                + SLUGS.get(random.nextInt(SLUGS.size())));
        masthead.setVolume(turn);
        masthead.setEditionDate(editionDate.toString());
        masthead.setDateLabel(DATE_FORMAT.withZone(ZONE).format(editionDate));
        masthead.setBoardName(board.getName());
        masthead.setWeather(weather);
        return masthead;
    }

    private static int between(Random random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private record WeatherCondition(String label, String icon, int minTemperature,
                                    int maxTemperature, int minPrecipitation,
                                    int maxPrecipitation, int minHumidity, int maxHumidity,
                                    int minWind, int maxWind) {
    }
}
