package com.mg.nmlonline;

import com.mg.nmlonline.config.PlayerStartupImporter;
import com.mg.nmlonline.config.TestDataInitializer;
import com.mg.nmlonline.domain.service.TurnService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Remet la base dans l'état d'un démarrage de contexte (fixtures démo + comptes de test) sans
 * reconstruire le contexte Spring : les {@code @DirtiesContext} coûtaient ~8 s par test.
 */
public abstract class DemoDatabaseReset {

    private static final Set<String> CATALOG_TABLES = Set.of(
            "flyway_schema_history", "equipment", "equipment_compatible_classes",
            "equipment_descriptions", "equipment_translations", "resource", "unit_classes");

    private static final Map<Class<?>, Set<Long>> CATALOG_EQUIPMENT_IDS = new ConcurrentHashMap<>();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PlayerStartupImporter startupImporter;

    @Autowired
    private TestDataInitializer testDataInitializer;

    @Autowired
    private TurnService turnService;

    @BeforeEach
    protected void resetDemoData() {
        // REQUIRES_NEW : un test @Transactional doit voir les entités réimportées, pas son cache de session.
        TransactionTemplate reset = new TransactionTemplate(transactionManager);
        reset.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        reset.executeWithoutResult(status -> {
            truncateMutableTables();
            deleteEquipmentCreatedByTests();
            startupImporter.run(null);
            testDataInitializer.init();
        });
        entityManager.clear();
        turnService.invalidateTurnCache();
    }

    private void deleteEquipmentCreatedByTests() {
        Set<Long> catalog = CATALOG_EQUIPMENT_IDS.computeIfAbsent(getClass(),
                key -> Set.copyOf(jdbc.queryForList("SELECT id FROM equipment", Long.class)));
        List<Long> ids = List.copyOf(catalog);
        if (ids.isEmpty()) {
            return;
        }
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(", "));
        jdbc.update("DELETE FROM equipment_compatible_classes WHERE equipment_id NOT IN ("
                + placeholders + ")", ids.toArray());
        jdbc.update("DELETE FROM equipment WHERE id NOT IN (" + placeholders + ")", ids.toArray());
    }

    private void truncateMutableTables() {
        List<String> tables = jdbc.queryForList(
                        "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class)
                .stream().filter(table -> !CATALOG_TABLES.contains(table)).toList();
        if (tables.isEmpty()) {
            throw new IllegalStateException("Aucune table à réinitialiser");
        }
        jdbc.execute("TRUNCATE TABLE " + tables.stream()
                .map(table -> "public." + table)
                .collect(Collectors.joining(", ")) + " CASCADE");
    }
}
