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
 * Dev uniquement (@Profile "dev" → 404 en prod) ; le GET sert de probe de disponibilité pour l'UI admin.
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

    @GetMapping("/seed-resolution-scenario")
    public Map<String, Object> getScenarioStatus() {
        return Map.of("available", scenarioSeeder.isAvailable());
    }

    /**
     * Scénario hardcoded lurio→cegorach (2 hops), re-jouable.
     */
    @PostMapping("/seed-resolution-scenario")
    public ResponseEntity<ScenarioSummaryDto> seedScenario() {
        return ResponseEntity.ok(scenarioSeeder.seedScenario());
    }
}