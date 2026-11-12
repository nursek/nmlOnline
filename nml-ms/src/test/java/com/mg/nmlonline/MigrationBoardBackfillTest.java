package com.mg.nmlonline;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Migrations V36/V37/V38 — backfill d'une base mono-partie existante")
class MigrationBoardBackfillTest {

    @Test
    @DisplayName("les lignes existantes rejoignent l'unique plateau, les unicités deviennent par partie")
    void backfillsExistingRowsOntoTheSingleBoard() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder().start()) {
            String url = postgres.getJdbcUrl("postgres", "postgres");
            migrateTo(url, "31");

            long boardId;
            try (Connection connection = DriverManager.getConnection(url, "postgres", "");
                 Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO public.boards (name) VALUES ('Ancienne partie')");
                boardId = singleLong(statement, "SELECT id FROM public.boards");
                statement.execute("INSERT INTO public.players (name) VALUES ('AncienJoueur')");
                statement.execute("INSERT INTO public.game_statistics (stat_key, total) VALUES ('UNIT_LARBIN', 3)");
            }

            migrateTo(url, "34");
            long unitId;
            try (Connection connection = DriverManager.getConnection(url, "postgres", "");
                 Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO public.sectors (board_id, number, color, income, name)"
                        + " VALUES (" + boardId + ", 1, '#ffffff', 0, 'Secteur 1')");
                statement.execute("INSERT INTO public.combat_entities (id, entity_type, armor, attack, defense,"
                        + " evasion, is_destroyed, pdc, pdf, board_id, sector_number, player_id, number)"
                        + " VALUES (nextval('public.combat_entities_id_seq'), 'UNIT', 0, 0, 0, 0, false, 0, 0,"
                        + " " + boardId + ", 1, (SELECT id FROM public.players), 1)");
                unitId = singleLong(statement, "SELECT id FROM public.combat_entities");
                statement.execute("INSERT INTO public.sniper_orders (id, player_id, turn, unit_id,"
                        + " from_sector, target_sector, status, submitted_at)"
                        + " VALUES (nextval('public.sniper_orders_id_seq'), (SELECT id FROM public.players),"
                        + " 3, " + unitId + ", 1, 2, 'PENDING', now())");
            }

            migrateTo(url, "35");
            try (Connection connection = DriverManager.getConnection(url, "postgres", "");
                 Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO public.turn_journals (turn, payload, created_at)"
                        + " VALUES (3, '{}', now())");
            }

            migrateTo(url, "latest");

            try (Connection connection = DriverManager.getConnection(url, "postgres", "");
                 Statement statement = connection.createStatement()) {
                assertEquals(boardId, singleLong(statement, "SELECT board_id FROM public.players"),
                        "le joueur existant doit être rattaché au plateau existant");
                assertEquals(boardId, singleLong(statement, "SELECT board_id FROM public.game_statistics"),
                        "les compteurs existants doivent être rattachés au plateau existant");
                assertEquals(boardId, singleLong(statement, "SELECT board_id FROM public.sniper_orders"),
                        "les ordres sniper existants rejoignent le plateau de leur unité");
                assertEquals(boardId, singleLong(statement, "SELECT board_id FROM public.turn_journals"),
                        "les journaux existants rejoignent le plateau existant");

                statement.execute("INSERT INTO public.players (name, board_id) VALUES ('Homonyme', " + boardId + ")");
                statement.execute("INSERT INTO public.boards (name) VALUES ('Seconde partie')");
                long secondBoardId = singleLong(statement, "SELECT max(id) FROM public.boards");
                statement.execute("INSERT INTO public.players (name, board_id) VALUES ('Homonyme', "
                        + secondBoardId + ")");
            }
        }
    }

    private static long singleLong(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static void migrateTo(String url, String target) {
        Flyway.configure()
                .dataSource(url, "postgres", "")
                .target(target.equals("latest") ? MigrationVersion.LATEST : MigrationVersion.fromVersion(target))
                .load()
                .migrate();
    }
}
