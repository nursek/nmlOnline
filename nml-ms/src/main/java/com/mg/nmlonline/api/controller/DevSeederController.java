package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.CombatScenarioDto;
import com.mg.nmlonline.api.dto.ExchangeScenarioSummaryDto;
import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.domain.service.DemoTurnsSeeder;
import com.mg.nmlonline.domain.service.ExchangeScenarioSeeder;
import com.mg.nmlonline.domain.service.FeatureScenarioSeeder;
import com.mg.nmlonline.domain.service.TurnResolutionScenarioSeeder;
import com.mg.nmlonline.domain.service.TurnService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Dev uniquement ; le GET sert de probe de disponibilité pour l'UI admin. */
@RestController
@RequestMapping("/api/admin/dev")
@Profile("dev")
@PreAuthorize("hasRole('ADMIN')")
public class DevSeederController {

    private final TurnResolutionScenarioSeeder scenarioSeeder;
    private final ExchangeScenarioSeeder exchangeScenarioSeeder;
    private final FeatureScenarioSeeder featureScenarioSeeder;
    private final DemoTurnsSeeder demoTurnsSeeder;
    private final TurnService turnService;

    public DevSeederController(TurnResolutionScenarioSeeder scenarioSeeder,
                               ExchangeScenarioSeeder exchangeScenarioSeeder,
                               FeatureScenarioSeeder featureScenarioSeeder,
                               DemoTurnsSeeder demoTurnsSeeder,
                               TurnService turnService) {
        this.scenarioSeeder = scenarioSeeder;
        this.exchangeScenarioSeeder = exchangeScenarioSeeder;
        this.featureScenarioSeeder = featureScenarioSeeder;
        this.demoTurnsSeeder = demoTurnsSeeder;
        this.turnService = turnService;
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

    /** Catalogue des scénarios de combat rejouables (arènes neutres dédiées). */
    @GetMapping("/combat-scenarios")
    public List<CombatScenarioDto> listCombatScenarios() {
        return scenarioSeeder.listCombatScenarios();
    }

    /** Sème un scénario de combat par code : arène purgée puis peuplée, un hop à résoudre. */
    @PostMapping("/seed-combat-scenario/{code}")
    public ResponseEntity<ScenarioSummaryDto> seedCombatScenario(@PathVariable("code") String code) {
        return ResponseEntity.ok(scenarioSeeder.seedCombatScenario(code));
    }

    @PostMapping("/seed-exchange-scenario")
    public ResponseEntity<ExchangeScenarioSummaryDto> seedExchangeScenario() {
        return ResponseEntity.ok(exchangeScenarioSeeder.seedExchangeScenario());
    }

    @PostMapping("/seed-alliance-scenario")
    public ResponseEntity<ScenarioSummaryDto> seedAllianceScenario() {
        return ResponseEntity.ok(featureScenarioSeeder.seedAllianceScenario());
    }

    @PostMapping("/seed-resource-sale-scenario")
    public ResponseEntity<ScenarioSummaryDto> seedResourceSaleScenario() {
        return ResponseEntity.ok(featureScenarioSeeder.seedResourceSaleScenario());
    }

    /** Navigation manuelle du compteur de tour, sans résolution des mouvements. */
    @PostMapping("/turn/{turn}")
    public Map<String, Object> navigateToTurn(@PathVariable("turn") int turn) {
        return Map.of("currentTurn", turnService.setCurrentTurn(turn));
    }

    /** Dev-only, jouée une seule fois : refuse une partie déjà entamée. */
    @PostMapping("/play-demo-turns")
    public ResponseEntity<ScenarioSummaryDto> playDemoTurns() {
        return ResponseEntity.ok(demoTurnsSeeder.playDemoTurns());
    }
}
