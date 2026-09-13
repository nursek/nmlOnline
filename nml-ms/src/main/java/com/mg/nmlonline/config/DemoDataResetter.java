package com.mg.nmlonline.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Component
@Order(0)
// H2 (mem, vierge à chaque boot) ne supporte pas le TRUNCATE multi-table : réservé à la prod persistante.
@Profile("prod")
@RequiredArgsConstructor
public class DemoDataResetter implements CommandLineRunner {

    private final EntityManager entityManager;
    private final DataSource dataSource;

    @Value("${app.import-demo-data:true}")
    private boolean importDemoData;

    @Override
    @Transactional
    public void run(String... args) {
        if (!importDemoData) {
            return;
        }
        if (isH2()) {
            truncateH2();
        } else {
            // CASCADE : purge secteurs/armées/ordres/actions ; credentials exclu, les comptes survivent au reset.
            entityManager.createNativeQuery(
                            "TRUNCATE TABLE battle_reports, boards, players, equipment, resource RESTART IDENTITY CASCADE")
                    .executeUpdate();
        }
    }

    private boolean isH2() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("h2");
        } catch (SQLException e) {
            throw new IllegalStateException("Type de base indétectable pour le reset des données démo", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void truncateH2() {
        // H2 ne supporte ni TRUNCATE multi-tables ni CASCADE : purge table par table, contraintes coupées, credentials épargnés.
        List<String> tables = entityManager.createNativeQuery(
                        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE' "
                                + "AND TABLE_NAME NOT IN ('CREDENTIALS', 'FLYWAY_SCHEMA_HISTORY')")
                .getResultList();
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        try {
            for (String table : tables) {
                entityManager.createNativeQuery("TRUNCATE TABLE " + table + " RESTART IDENTITY").executeUpdate();
            }
        } finally {
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
        }
    }
}
