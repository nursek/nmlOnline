package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.battle.RandomSource;
import com.mg.nmlonline.domain.model.battle.ThreadLocalRandomSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CombatRandomConfig {

    @Bean
    public RandomSource randomSource() {
        return new ThreadLocalRandomSource();
    }
}
