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
import java.util.function.Function;

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
     * Lit un CSV du classpath (header ignoré) et mappe chaque ligne non vide.
     * Les lignes pour lesquelles le mapper retourne null sont ignorées.
     */
    private <T> List<T> load(String path, Function<String[], T> mapper) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(
                        getClass().getResourceAsStream(path)), StandardCharsets.UTF_8))) {

            String header = reader.readLine(); // Skip header
            log.debug("CSV header ({}): {}", path, header);

            List<T> result = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    T item = mapper.apply(line.split(","));
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to load data from CSV {}", path, e);
            return List.of();
        }
    }

    /**
     * Charge les ressources depuis resources.csv
     */
    private void loadResources() {
        if (resourceRepository.count() > 0) {
            log.info("Resources already loaded (count: {}), skipping", resourceRepository.count());
            return;
        }

        List<Resource> resources = load("/resources.csv", parts ->
                parts.length >= 2 ? new Resource(parts[0], Double.parseDouble(parts[1])) : null);
        resources.forEach(resourceRepository::save);

        log.info("Successfully loaded {} resources from CSV", resources.size());
    }

    /**
     * Charge les équipements depuis equipments.csv
     */
    private void loadEquipments() {
        if (equipmentRepository.count() > 0) {
            log.info("Equipments already loaded (count: {}), skipping", equipmentRepository.count());
            return;
        }

        List<Equipment> equipments = load("/equipments.csv", parts ->
                parts.length >= 8 ? new Equipment(
                        parts[0],                                    // name
                        Integer.parseInt(parts[1]),                  // cost
                        Double.parseDouble(parts[2]),                // pdfBonus
                        Double.parseDouble(parts[3]),                // pdcBonus
                        Double.parseDouble(parts[4]),                // armBonus
                        Double.parseDouble(parts[5]),                // evasionBonus
                        new HashSet<>(),                             // compatibleClasses (chargé après)
                        EquipmentCategory.valueOf(parts[7])          // category
                ) : null);
        equipments.forEach(equipmentRepository::save);

        log.info("Successfully loaded {} equipments from CSV", equipments.size());

        // Log des équipements chargés pour vérification
        if (log.isDebugEnabled()) {
            equipmentRepository.findAll().forEach(eq ->
                log.debug("Equipment in DB: {}", eq.getName()));
        }
    }

    /**
     * Charge les compatibilités depuis compatibility.csv
     */
    private void loadCompatibilities() {
        List<Map.Entry<Long, UnitClass>> entries = load("/compatibility.csv", parts ->
                parts.length >= 2
                        ? Map.entry(Long.parseLong(parts[0]), UnitClass.valueOf(parts[1]))
                        : null);

        Map<Long, Set<UnitClass>> compatibilities = new HashMap<>();
        for (Map.Entry<Long, UnitClass> entry : entries) {
            compatibilities.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).add(entry.getValue());
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
    }
}
