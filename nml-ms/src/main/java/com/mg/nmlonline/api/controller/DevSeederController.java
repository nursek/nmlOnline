package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.domain.service.TurnResolutionScenarioSeeder;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints de seeding d'un scénario de test pour la résolution pas-à-pas.
 *
 * <p><strong>Dev uniquement</strong> : @{@link Profile "dev"} désactive
 * entièrement le bean en prod → les endpoints ne sont pas enregistrés (404
 * natif). Le frontend utilise le {@code GET} comme probe pour afficher ou non
 * le bouton « Seeder le scénario ».</p>
 */
@RestController
@RequestMapping("/api/admin/dev")
@Profile("dev")
@PreAuthorize("hasRole('ADMIN')")
public class DevSeederController {

    private final TurnResolutionScenarioSeeder scenarioSeeder;

    public DevSeederController(TurnResolutionScenarioSeeder scenarioSeeder) {
        this.scenarioSeeder = scenarioSeeder;
    }

    /**
     * Probe de disponibilité (cache UI en prod via 404).
     */
    @GetMapping("/seed-resolution-scenario")
    public Map<String, Object> getScenarioStatus() {
        return Map.of("available", scenarioSeeder.isAvailable());
    }

    /**
     * Prépare le scénario de test pas-à-pas (lurio → cegorach, 2 hops) pour le
     * tour courant. Re-jouable.
     */
    @PostMapping("/seed-resolution-scenario")
    public ResponseEntity<ScenarioSummaryDto> seedScenario() {
        return ResponseEntity.ok(scenarioSeeder.seedScenario());
    }
}