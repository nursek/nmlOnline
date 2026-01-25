package com.mg.nmlonline.infrastructure.loader;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.resource.Resource;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import com.mg.nmlonline.infrastructure.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Charge les données de base depuis les fichiers CSV au démarrage de l'application
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class CsvDataLoader implements CommandLineRunner {

    private final ResourceRepository resourceRepository;
    private final EquipmentRepository equipmentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        loadResources();
        loadEquipments();
        loadCompatibilities();
    }

    /**
     * Charge les ressources depuis resources.csv
     */
    private void loadResources() {
        if (resourceRepository.count() > 0) {
            log.info("Resources already loaded (count: {}), skipping", resourceRepository.count());
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(
                        getClass().getResourceAsStream("/resources.csv")), StandardCharsets.UTF_8))) {

            String header = reader.readLine(); // Skip header (name,baseValue)
            log.debug("Resources CSV header: {}", header);
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        Resource resource = new Resource(parts[0], Double.parseDouble(parts[1]));
                        resourceRepository.save(resource);
                        count++;
                        log.debug("Loaded resource: {}", resource);
                    }
                }
            }

            log.info("Successfully loaded {} resources from CSV", count);
        } catch (Exception e) {
            log.error("Failed to load resources from CSV", e);
        }
    }

    /**
     * Charge les équipements depuis equipments.csv
     */
    private void loadEquipments() {
        if (equipmentRepository.count() > 0) {
            log.info("Equipments already loaded (count: {}), skipping", equipmentRepository.count());
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(
                        getClass().getResourceAsStream("/equipments.csv")), StandardCharsets.UTF_8))) {

            String header = reader.readLine(); // Skip header
            log.debug("Equipments CSV header: {}", header);
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split(",");
                    if (parts.length >= 8) {
                        Equipment equipment = new Equipment(
                            parts[0],                                    // name
                            Integer.parseInt(parts[1]),                  // cost
                            Double.parseDouble(parts[2]),                // pdfBonus
                            Double.parseDouble(parts[3]),                // pdcBonus
                            Double.parseDouble(parts[4]),                // armBonus
                            Double.parseDouble(parts[5]),                // evasionBonus
                            new HashSet<>(),                             // compatibleClasses (chargé après)
                            EquipmentCategory.valueOf(parts[7])          // category
                        );

                        equipmentRepository.save(equipment);
                        count++;
                        log.debug("Loaded equipment: {}", equipment.getName());
                    }
                }
            }

            log.info("Successfully loaded {} equipments from CSV", count);

            // Log des équipements chargés pour vérification
            if (log.isDebugEnabled()) {
                equipmentRepository.findAll().forEach(eq ->
                    log.debug("Equipment in DB: {}", eq.getName()));
            }
        } catch (Exception e) {
            log.error("Failed to load equipments from CSV", e);
        }
    }

    /**
     * Charge les compatibilités depuis compatibility.csv
     */
    private void loadCompatibilities() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(
                        getClass().getResourceAsStream("/compatibility.csv")), StandardCharsets.UTF_8))) {

            String header = reader.readLine(); // Skip header (equipmentId,unitClass)
            log.debug("Compatibility CSV header: {}", header);
            String line;
            Map<Long, Set<UnitClass>> compatibilities = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        Long equipmentId = Long.parseLong(parts[0]);
                        UnitClass unitClass = UnitClass.valueOf(parts[1]);

                        compatibilities.computeIfAbsent(equipmentId, k -> new HashSet<>()).add(unitClass);
                    }
                }
            }

            // Appliquer les compatibilités aux équipements
            int count = 0;
            for (Map.Entry<Long, Set<UnitClass>> entry : compatibilities.entrySet()) {
                equipmentRepository.findById(entry.getKey()).ifPresent(equipment -> {
                    equipment.setCompatibleClasses(entry.getValue());
                    equipmentRepository.save(equipment);
                    log.debug("Set compatibilities for equipment {}: {}", equipment.getName(), entry.getValue());
                });
                count++;
            }

            log.info("Successfully loaded compatibilities for {} equipments from CSV", count);
        } catch (Exception e) {
            log.error("Failed to load compatibilities from CSV", e);
        }
    }
}
