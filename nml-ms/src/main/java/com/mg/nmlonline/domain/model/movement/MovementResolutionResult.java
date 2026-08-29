package com.mg.nmlonline.domain.model.movement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MovementResolutionResult {

    private final List<MovementOrder> resolved = new ArrayList<>();
    private final List<MovementOrder> blocked = new ArrayList<>();
    private final List<TransitCombatResult> transitCombats = new ArrayList<>();
    private final List<DestinationConflict> conflicts = new ArrayList<>();

    public void addResolved(MovementOrder order) { resolved.add(order); }
    public void addBlocked(MovementOrder order) { blocked.add(order); }
    public void addTransitCombat(TransitCombatResult combatResult) { transitCombats.add(combatResult); }
    public void addConflict(DestinationConflict conflict) { conflicts.add(conflict); }

    public List<MovementOrder> getResolved() { return Collections.unmodifiableList(resolved); }
    public List<MovementOrder> getBlocked() { return Collections.unmodifiableList(blocked); }
    public List<TransitCombatResult> getTransitCombats() { return Collections.unmodifiableList(transitCombats); }
    public List<DestinationConflict> getConflicts() { return Collections.unmodifiableList(conflicts); }

    public boolean hasConflicts() { return !conflicts.isEmpty(); }
    public boolean hasTransitCombats() { return !transitCombats.isEmpty(); }

    @Override
    public String toString() {
        return String.format("MovementResolution{resolved=%d, blocked=%d, transitCombats=%d, conflicts=%d}",
                resolved.size(), blocked.size(), transitCombats.size(), conflicts.size());
    }
}
