package com.mg.nmlonline;

import com.mg.nmlonline.domain.model.battle.RandomSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class CombatTestRandomConfig {

    // Jet 100 : les scénarios d'intégration restent déterministes ; le critique est couvert par BattleTest.
    @Bean
    @Primary
    public RandomSource testRandomSource() {
        return () -> 100;
    }
}
