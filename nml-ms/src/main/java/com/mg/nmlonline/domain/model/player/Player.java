package com.mg.nmlonline.domain.model.player;

import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Entity
@Table(name = "PLAYERS")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"equipments", "resources"})
public class Player {

    private static final Logger logger = LoggerFactory.getLogger(Player.class);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_seq")
    @SequenceGenerator(name = "player_seq", sequenceName = "players_id_seq", allocationSize = 50)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "user_id", unique = true)
    private Long userId;

    @Embedded
    private PlayerStats stats = new PlayerStats();

    @Embedded
    private PlayerBonuses bonuses = new PlayerBonuses();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "character_id")
    private GameCharacter character;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<Building> buildings = new ArrayList<>();

    // orphanRemoval=true + FK player_id NOT NULL : risque 500 si chemin de transfert ajouté (voir docs/jpa-pitfalls.md).
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<EquipmentStack> equipments = new ArrayList<>();

    // orphanRemoval=true + FK player_id NOT NULL : risque 500 si chemin de transfert ajouté (voir docs/jpa-pitfalls.md).
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<PlayerResource> resources = new ArrayList<>();

    public Player(String name) {
        this.name = name;
    }

    public Equipment getEquipmentByString(String name) {
        EquipmentStack stack = findStackByName(name);
        return stack != null ? stack.getEquipment() : null;
    }

    public boolean buyEquipment(Equipment equipment, int quantity) {
        if (equipment == null || quantity <=0){
            return false;
        }
        double totalCost = (double) equipment.getCost() * quantity;
        if (stats.getMoney() >= totalCost) {
            stats.setMoney(stats.getMoney() - totalCost);
            addEquipmentToStack(equipment, quantity);
            setTotalEquipmentValue();
            calculateTotalEconomyPower();
            return true;
        }
        return false;
    }

    public Vehicle buyVehicle(VehicleType vehicleType) {
        if (vehicleType == null) return null;
        int cost = vehicleType.getCost();
        if (stats.getMoney() >= cost) {
            stats.setMoney(stats.getMoney() - cost);
            stats.setTotalVehiclesValue(stats.getTotalVehiclesValue() + cost);
            calculateTotalEconomyPower();
            return new Vehicle(vehicleType, this.id);
        }
        return null;
    }

    public void addEquipmentToStack(Equipment equipment, int number) {
        if (equipment == null) return;

        EquipmentStack existingStack = findStackByName(equipment.getName());
        if (existingStack != null) {
            for (int i = 0; i < number; i++) {
                existingStack.increment();
            }
            return;
        }

        EquipmentStack newStack = new EquipmentStack(equipment);
        newStack.setPlayer(this); // côté inverse de la relation bidirectionnelle
        for (int i = 1; i < number; i++) {
            newStack.increment();
        }
        equipments.add(newStack);
    }

    public boolean isEquipmentUnavailable(Equipment equipment) {
        if (equipment == null) return true;
        return !isEquipmentAvailable(equipment.getName());
    }

    public boolean isEquipmentAvailable(String equipmentName) {
        EquipmentStack stack = findStackByName(equipmentName);
        return stack != null && stack.isAvailable();
    }

    public void decrementEquipmentAvailability(Equipment equipment) {
        if (equipment == null) return;
        decrementEquipmentAvailability(equipment.getName());
    }

    public void decrementEquipmentAvailability(String equipmentName) {
        EquipmentStack stack = findStackByName(equipmentName);
        if (stack != null) {
            stack.decrementAvailable();
            setTotalEquipmentValue();
            calculateTotalEconomyPower();
        }
    }

    private EquipmentStack findStackByName(String equipmentName) {
        if (equipmentName == null) return null;
        for (EquipmentStack stack : equipments) {
            Equipment stackEquip = stack.getEquipment();
            if (stackEquip != null && stackEquip.getName() != null &&
                stackEquip.getName().equals(equipmentName)) {
                return stack;
            }
        }
        return null;
    }

    public void removeEquipmentFromStack(Equipment equipment) {
        if (equipment == null) return;
        EquipmentStack stack = findStackByName(equipment.getName());
        if (stack != null) {
            if (stack.getQuantity() > 1) {
                stack.decrement();
            } else {
                equipments.remove(stack);
            }
        }
    }

    public void setTotalEquipmentValue() {
        double inventoryValue = equipments.stream()
                .mapToDouble(stack -> stack.getEquipment().getCost() * stack.getQuantity())
                .sum();
        stats.setTotalEquipmentValue(inventoryValue);
    }

    public List<Equipment> getCompatibleEquipments(Unit unit) {
        if (unit == null) {
            return new ArrayList<>();
        }

        return equipments.stream()
                .filter(stack -> stack.getAvailable() > 0)
                .map(EquipmentStack::getEquipment)
                .filter(unit::canEquip)
                .toList();
    }

    public List<Equipment> getCompatibleEquipmentsByCategory(Unit unit, EquipmentCategory category) {
        if (unit == null || category == null) {
            return new ArrayList<>();
        }

        return equipments.stream()
                .filter(stack -> stack.getAvailable() > 0)
                .map(EquipmentStack::getEquipment)
                .filter(eq -> eq.getCategory() == category)
                .filter(unit::canEquip)
                .toList();
    }

    public boolean replaceEquipment(Unit unit, Equipment oldEquipment, Equipment newEquipment) {
        if (unit == null || newEquipment == null) {
            return false;
        }

        if (isEquipmentUnavailable(newEquipment)) {
            logger.warn("Équipement non disponible : {}", newEquipment.getName());
            return false;
        }

        if (oldEquipment != null) {
            boolean removed = unit.removeEquipment(oldEquipment);
            if (removed) {
                incrementEquipmentAvailability(oldEquipment);
                logger.info("Équipement retiré : {}", oldEquipment.getName());
            } else {
                logger.warn("Impossible de retirer l'équipement : {}", oldEquipment.getName());
                return false;
            }
        }

        boolean equipped = unit.addEquipment(newEquipment);
        if (equipped) {
            decrementEquipmentAvailability(newEquipment);
            setTotalEquipmentValue();
            logger.info("Nouvel équipement ajouté : {}", newEquipment.getName());
            return true;
        } else {
            // Retour arrière : remettre l'ancien si on l'avait retiré
            if (oldEquipment != null) {
                unit.addEquipment(oldEquipment);
                decrementEquipmentAvailability(oldEquipment);
            }
            logger.warn("Impossible d'équiper : {}", newEquipment.getName());
            return false;
        }
    }

    public boolean replaceEquipmentByCategory(Unit unit, Equipment newEquipment) {
        if (unit == null || newEquipment == null) {
            return false;
        }

        EquipmentCategory category = newEquipment.getCategory();

        long currentCount = unit.countEquipmentsByCategory(category);
        int maxAllowed = switch (category) {
            case FIREARM -> unit.getType().getMaxFirearms();
            case MELEE -> unit.getType().getMaxMeleeWeapons();
            case DEFENSIVE -> unit.getType().getMaxDefensiveEquipment();
        };

        Equipment oldEquipment = null;
        if (currentCount >= maxAllowed) {
            List<Equipment> equipmentsOfCategory = unit.getEquipmentsByCategory(category);
            if (!equipmentsOfCategory.isEmpty()) {
                oldEquipment = equipmentsOfCategory.getFirst();
            }
        }

        return replaceEquipment(unit, oldEquipment, newEquipment);
    }

    public void incrementEquipmentAvailability(Equipment equipment) {
        if (equipment == null) return;
        EquipmentStack stack = findStackByName(equipment.getName());
        if (stack != null) {
            stack.incrementAvailable();
            setTotalEquipmentValue();
            calculateTotalEconomyPower();
        }
    }

    public void incrementMoney(double amount) {
        if (amount > 0) {
            stats.setMoney(stats.getMoney() + amount);
            calculateTotalEconomyPower();
        }
    }

    public void decrementMoney(double amount) {
        if (amount > 0 && stats.getMoney() >= amount) {
            stats.setMoney(stats.getMoney() - amount);
            calculateTotalEconomyPower();
        }
    }


    public void addResource(String resourceName, int quantity) {
        if (resourceName == null || quantity <= 0) return;

        PlayerResource existingResource = findResourceByName(resourceName);
        if (existingResource != null) {
            existingResource.addQuantity(quantity);
        } else {
            PlayerResource newResource = new PlayerResource(resourceName, quantity);
            newResource.setPlayer(this);
            resources.add(newResource);
        }
    }

    public boolean removeResource(String resourceName, int quantity) {
        if (resourceName == null || quantity <= 0) return false;

        PlayerResource resource = findResourceByName(resourceName);
        if (resource != null) {
            boolean removed = resource.removeQuantity(quantity);
            if (resource.getQuantity() == 0) {
                resources.remove(resource);
            }
            return removed;
        }
        return false;
    }

    public void removePlayerResourceEntity(PlayerResource resource) {
        resources.remove(resource);
    }

    public boolean hasResource(String resourceName, int quantity) {
        PlayerResource resource = findResourceByName(resourceName);
        return resource != null && resource.hasQuantity(quantity);
    }

    public int getResourceQuantity(String resourceName) {
        PlayerResource resource = findResourceByName(resourceName);
        return resource != null ? resource.getQuantity() : 0;
    }

    private PlayerResource findResourceByName(String resourceName) {
        if (resourceName == null) return null;
        return resources.stream()
                .filter(resource -> resourceName.equals(resource.getResourceName()))
                .findFirst()
                .orElse(null);
    }

    public void calculateTotalEconomyPower() {
        double economyPower = stats.getTotalIncome()
                + stats.getTotalEquipmentValue()
                + stats.getMoney()
                + stats.getTotalVehiclesValue();
        stats.setTotalEconomyPower(economyPower);
    }

}
