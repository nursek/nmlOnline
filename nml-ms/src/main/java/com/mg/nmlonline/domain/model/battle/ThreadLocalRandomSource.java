package com.mg.nmlonline.domain.model.battle;

import java.util.concurrent.ThreadLocalRandom;

public final class ThreadLocalRandomSource implements RandomSource {

    @Override
    public int rollPercent() {
        return ThreadLocalRandom.current().nextInt(1, 101);
    }
}
