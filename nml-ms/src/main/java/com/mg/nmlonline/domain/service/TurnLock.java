package com.mg.nmlonline.domain.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Verrou global anti-concurrence pour les opérations de fin de tour.
 *
 * <p>Partagé entre {@link TurnService#advanceTurn()} (résolution atomique +
 * incrément) et {@link TurnResolutionOrchestrator} (résolution pas-à-pas par
 * hop) : un seul de ces processus peut tenir le verrou à la fois dans la JVM.
 *
 * <p>ponytail: ceiling = JVM unique, garde in-process ; le verrou est relâché
 * dans {@code finally} avant le commit transactionnel, donc une fenêtre
 * (infime) reste ouverte entre deux threads en rafale. Perte du verrou si la
 * JVM plante. Upgrade path = lock pessimiste JPA sur {@code Board} ou
 * DistributedLock si multi-instance.</p>
 */
@Component
public class TurnLock {

    private final AtomicBoolean locked = new AtomicBoolean(false);

    /** Tente d'acquérir le verrou. Retourne {@code true} si réussi. */
    public boolean tryAcquire() {
        return locked.compareAndSet(false, true);
    }

    /** Relâche le verrou (idempotent). */
    public void release() {
        locked.set(false);
    }

    public boolean isLocked() {
        return locked.get();
    }
}