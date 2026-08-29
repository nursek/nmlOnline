package com.mg.nmlonline.domain.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Verrou anti-concurrence pour les opérations de fin de tour. Partagé entre
 * {@link TurnService#advanceTurn()} (atomique) et {@link TurnResolutionOrchestrator}
 * (pas-à-pas) : un seul processus le tient à la fois dans la JVM.
 */
@Component
public class TurnLock {

    private final AtomicBoolean locked = new AtomicBoolean(false);

    public boolean tryAcquire() {
        return locked.compareAndSet(false, true);
    }

    public void release() {
        locked.set(false);
    }

    public boolean isLocked() {
        return locked.get();
    }
}