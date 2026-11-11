package com.mg.nmlonline.infrastructure.loader;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EmbeddedPostgresTest
@DisplayName("CsvDataLoader — modificateurs de visée sniper")
class EquipmentAimMalusLoaderTest {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Test
    @DisplayName("les deux fusils réduisent le malus de visée, le gilet l'augmente pour l'adversaire")
    void shouldLoadSniperAimMalus() {
        Equipment shredder = equipmentRepository.findByName("Particle Shredder").orElseThrow();
        assertEquals(10, shredder.getAimMalusReduction());
        assertEquals(0, shredder.getEnemyAimMalus());

        Equipment exterminator = equipmentRepository.findByName("Enmitic Exterminator").orElseThrow();
        assertEquals(15, exterminator.getAimMalusReduction());

        Equipment gilet = equipmentRepository.findByName("Gilet de camouflage optique").orElseThrow();
        assertEquals(0, gilet.getAimMalusReduction());
        assertEquals(15, gilet.getEnemyAimMalus());
    }
}
