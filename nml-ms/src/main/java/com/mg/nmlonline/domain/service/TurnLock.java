package com.mg.nmlonline.domain.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Verrou anti-concurrence des fins de tour, un par plateau. Partagé entre
 * {@link TurnService#advanceTurn()} (atomique) et {@link TurnResolutionOrchestrator}
 * (pas-à-pas) : un seul processus le tient à la fois par partie.
 */
@Component
public class TurnLock {

    private final Map<Long, Semaphore> locks = new ConcurrentHashMap<>();

    public boolean tryAcquire(Long boardId) {
        return lock(boardId).tryAcquire();
    }

    public void release(Long boardId) {
        lock(boardId).release();
    }

    public boolean isLocked(Long boardId) {
        return lock(boardId).availablePermits() == 0;
    }

    private Semaphore lock(Long boardId) {
        return locks.computeIfAbsent(boardId, id -> new Semaphore(1));
    }
}
