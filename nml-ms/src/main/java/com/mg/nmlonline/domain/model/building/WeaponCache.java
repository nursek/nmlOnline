package com.mg.nmlonline.domain.model.building;

import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Entity
@DiscriminatorValue("WEAPON_CACHE")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class WeaponCache extends Building {

    public static final int MAX_CAPACITY = 300;
    public static final int MOVE_COOLDOWN = 0;

    @Column(name = "max_capacity")
    private int maxCapacity = MAX_CAPACITY;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "weapon_cache_id")
    private List<EquipmentStack> storedEquipments = new ArrayList<>();

    public WeaponCache(Long playerId) {
        super(BuildingType.WEAPON_CACHE,
              BuildingType.WEAPON_CACHE.getBaseAttack(),
              BuildingType.WEAPON_CACHE.getBaseDefense());
        setPlayerId(playerId);
    }

    @Override
    public boolean canMove(int currentTurn) {
        if (isDestroyed()) {
            return false;
        }
        if (getLastMovedTurn() == null) {
            return true;
        }
        return currentTurn - getLastMovedTurn() >= MOVE_COOLDOWN;
    }

    @Override
    public int getMoveCooldown() {
        return MOVE_COOLDOWN;
    }

    public int getTotalStoredCount() {
        return storedEquipments.stream()
                .mapToInt(EquipmentStack::getQuantity)
                .sum();
    }

    //TODO lier une vérification lors de l'achat d'équipement pour un joueur. Si toutes ses caches sont pleines, il ne peut pas acheter d'équipement.
    public boolean hasCapacity(int quantity) {
        return getTotalStoredCount() + quantity <= maxCapacity;
    }

    public int getAvailableCapacity() {
        return maxCapacity - getTotalStoredCount();
    }

    public double getFillPercentage() {
        return (double) getTotalStoredCount() / maxCapacity * 100;
    }

    public List<EquipmentStack> transferAllEquipments() {
        List<EquipmentStack> transferred = new ArrayList<>(storedEquipments);
        storedEquipments.clear();
        return transferred;
    }

    public boolean discardEquipment(String equipmentName, int quantity) {
        Iterator<EquipmentStack> iterator = storedEquipments.iterator();
        while (iterator.hasNext()) {
            EquipmentStack stack = iterator.next();
            if (stack.getEquipment().getName().equals(equipmentName) && stack.getQuantity() >= quantity) {
                for (int i = 0; i < quantity; i++) {
                    stack.decrement();
                }
                if (stack.getQuantity() <= 0) {
                    iterator.remove();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCapture(Long capturingPlayerId, int currentTurn) {
        super.onCapture(capturingPlayerId, currentTurn);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (isDestroyed()) {
            sb.append("[DÉTRUIT] ");
        }

        sb.append("Cache d'armes (");
        buildStatsString(sb);
        sb.append(") - ");
        sb.append(getTotalStoredCount()).append("/").append(maxCapacity).append(" équipements");

        if (getFillPercentage() > 80) {
            sb.append(" [PRESQUE PLEIN]");
        }

        if (isCaptured()) {
            sb.append(" [Capturé]");
        }

        return sb.toString();
    }
}

