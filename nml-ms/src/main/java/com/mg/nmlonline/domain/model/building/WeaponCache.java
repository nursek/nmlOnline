package com.mg.nmlonline.domain.model.building;

import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Cache d'armes - Bâtiment de stockage des équipements.
 *
 * Caractéristiques :
 * - Stats : 100 Atk / 100 Def
 * - Capacité max : 300 équipements
 * - Déplaçable tous les 2 tours
 * - Si capturé, l'adversaire récupère tous les équipements stockés
 * - Possibilité de fonder d'autres caches (un par quartier max)
 * - Les équipements peuvent être jetés (action irréversible)
 */
@Entity
@DiscriminatorValue("WEAPON_CACHE")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class WeaponCache extends Building {

    // Constantes
    public static final int MAX_CAPACITY = 300;
    public static final int MOVE_COOLDOWN = 0; // Permet de déplacer tous les tours

    @Column(name = "max_capacity")
    private int maxCapacity = MAX_CAPACITY;

    /**
     * Équipements stockés dans cette cache.
     * Note : Les équipements sont liés via EquipmentStack pour gérer les quantités.
     */
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

    /**
     * Retourne le nombre total d'équipements stockés.
     */
    public int getTotalStoredCount() {
        return storedEquipments.stream()
                .mapToInt(EquipmentStack::getQuantity)
                .sum();
    }

    /**
     * Vérifie si la cache peut accepter de nouveaux équipements.
     *
     * @param quantity Nombre d'équipements à ajouter
     * @return true si l'espace est suffisant
     */
    //TODO lier une vérification lors de l'achat d'équipement pour un joueur. Si toutes ses caches sont pleines, il ne peut pas acheter d'équipement.
    public boolean hasCapacity(int quantity) {
        return getTotalStoredCount() + quantity <= maxCapacity;
    }

    /**
     * Retourne l'espace disponible dans la cache.
     */
    public int getAvailableCapacity() {
        return maxCapacity - getTotalStoredCount();
    }

    /**
     * Retourne le pourcentage de remplissage de la cache.
     */
    public double getFillPercentage() {
        return (double) getTotalStoredCount() / maxCapacity * 100;
    }

    /**
     * Transfère tous les équipements à un autre joueur (lors d'une capture).
     *
     * @return La liste des équipements transférés
     */
    public List<EquipmentStack> transferAllEquipments() {
        List<EquipmentStack> transferred = new ArrayList<>(storedEquipments);
        storedEquipments.clear();
        return transferred;
    }

    /**
     * Jette un équipement de la cache (action irréversible).
     *
     * @param equipmentName Nom de l'équipement à jeter
     * @param quantity Quantité à jeter
     * @return true si l'équipement a été jeté
     */
    public boolean discardEquipment(String equipmentName, int quantity) {
        for (EquipmentStack stack : storedEquipments) {
            if (stack.getEquipment().getName().equals(equipmentName)) {
                if (stack.getQuantity() >= quantity) {
                    for (int i = 0; i < quantity; i++) {
                        stack.decrement();
                    }
                    if (stack.getQuantity() <= 0) {
                        storedEquipments.remove(stack);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onCapture(Long capturingPlayerId, int currentTurn) {
        super.onCapture(capturingPlayerId, currentTurn);
        // Les équipements sont transférés au nouveau propriétaire
        // Cette logique sera gérée par le service.
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

