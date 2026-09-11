package com.mg.nmlonline.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
@Profile("!test")
@RequiredArgsConstructor
public class DemoDataResetter implements CommandLineRunner {

    private final EntityManager entityManager;

    @Value("${app.import-demo-data:true}")
    private boolean importDemoData;

    @Override
    @Transactional
    public void run(String... args) {
        if (!importDemoData) {
            return;
        }
        // CASCADE : purge secteurs/armées/ordres/actions ; credentials exclu, les comptes survivent au reset.
        entityManager.createNativeQuery(
                        "TRUNCATE TABLE boards, players, equipment, resource RESTART IDENTITY CASCADE")
                .executeUpdate();
    }
}
