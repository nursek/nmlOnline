package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.ExchangeScenarioSummaryDto;
import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.domain.service.ExchangeScenarioSeeder;
import com.mg.nmlonline.domain.service.TurnResolutionScenarioSeeder;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Dev uniquement ; le GET sert de probe de disponibilité pour l'UI admin. */
@RestController
@RequestMapping("/api/admin/dev")
@Profile("dev")
@PreAuthorize("hasRole('ADMIN')")
public class DevSeederController {

    private final TurnResolutionScenarioSeeder scenarioSeeder;
    private final ExchangeScenarioSeeder exchangeScenarioSeeder;

    public DevSeederController(TurnResolutionScenarioSeeder scenarioSeeder,
                               ExchangeScenarioSeeder exchangeScenarioSeeder) {
        this.scenarioSeeder = scenarioSeeder;
        this.exchangeScenarioSeeder = exchangeScenarioSeeder;
    }

    @GetMapping("/seed-resolution-scenario")
    public Map<String, Object> getScenarioStatus() {
        return Map.of("available", scenarioSeeder.isAvailable());
    }

    /** Scénario hardcoded lurio→cegorach (2 hops), re-jouable. */
    @PostMapping("/seed-resolution-scenario")
    public ResponseEntity<ScenarioSummaryDto> seedScenario() {
        return ResponseEntity.ok(scenarioSeeder.seedScenario());
    }

    /** Impasse mexicaine : cegorach en 32, imotekh (43) et lurio (41) arrivent au même hop. */
    @PostMapping("/seed-standoff-scenario")
    public ResponseEntity<ScenarioSummaryDto> seedStandoffScenario() {
        return ResponseEntity.ok(scenarioSeeder.seedStandoffScenario());
    }

    /** Une offre en attente (lurio→cegorach) et un échange accepté (imotekh→nursek), re-jouable. */
    @PostMapping("/seed-exchange-scenario")
    public ResponseEntity<ExchangeScenarioSummaryDto> seedExchangeScenario() {
        return ResponseEntity.ok(exchangeScenarioSeeder.seedExchangeScenario());
    }
}