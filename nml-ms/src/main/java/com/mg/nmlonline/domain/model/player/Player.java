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

/**
 * Classe représentant un joueur avec son armée d'unités
 * Entité JPA fusionnée avec le modèle du domaine
 */
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

    // Stats du joueur (embedded)
    @Embedded
    private PlayerStats stats = new PlayerStats();

    // Bonuses du joueur (embedded)
    @Embedded
    private PlayerBonuses bonuses = new PlayerBonuses();

    // Personnage principal du joueur (leader unique)
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "character_id")
    private GameCharacter character;

    // Bâtiments du joueur (relation bidirectionnelle, côté inverse via mappedBy = "player" en cohérence avec @ManyToOne dans Building)
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<Building> buildings = new ArrayList<>();

    // Inventaire d'équipements du joueur
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<EquipmentStack> equipments = new ArrayList<>(); // Équipements possédés par le joueur

    // Inventaire de ressources du joueur
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<PlayerResource> resources = new ArrayList<>(); // Ressources possédées par le joueur (Or, Ivoire, etc.)

    public Player(String name) {
        this.name = name;
    }

    // === MÉTHODES NÉCESSITANT BOARD ===
    // Note: Les méthodes qui manipulent les unités dans les secteurs nécessitent désormais
    // une référence au Board pour accéder aux secteurs (single source of truth)
    // Ces méthodes devraient être déplacées vers un service ou recevoir Board en paramètre

    // === GESTION DES EQUIPMENT DU JOUEUR ===

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

    /**
     * Achète un véhicule pour le joueur et retourne l'entité créée.
     *
     * @param vehicleType type de véhicule à acheter
     * @return le véhicule créé, ou null si l'achat a échoué
     */
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

        // Chercher un stack existant avec le même équipement (par nom)
        EquipmentStack existingStack = findStackByName(equipment.getName());
        if (existingStack != null) {
            for (int i = 0; i < number; i++) {
                existingStack.increment();
            }
            return;
        }

        // Créer un nouveau stack
        EquipmentStack newStack = new EquipmentStack(equipment);
        newStack.setPlayer(this); // Important: définir la relation bidirectionnelle
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

    /**
     * Trouve un EquipmentStack par nom d'équipement.
     * Méthode helper privée pour éviter la duplication de code.
     */
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

    /**
     * Retourne la liste des équipements compatibles avec une unité donnée.
     * Un équipement est compatible si :
     * - Il correspond à une classe de l'unité
     * - Il est disponible dans l'inventaire du joueur
     * - L'unité n'a pas atteint la limite pour cette catégorie d'équipement
     *
     * @param unit L'unité pour laquelle vérifier la compatibilité
     * @return Liste des équipements compatibles disponibles
     */
    public List<Equipment> getCompatibleEquipments(Unit unit) {
        if (unit == null) {
            return new ArrayList<>();
        }

        return equipments.stream()
                .filter(stack -> stack.getAvailable() > 0) // Équipement disponible
                .map(EquipmentStack::getEquipment)
                .filter(unit::canEquip) // Compatible et limite non atteinte
                .toList();
    }

    /**
     * Retourne les équipements compatibles filtrés par catégorie.
     * Utile pour afficher uniquement les armes, ou uniquement les équipements défensifs.
     *
     * @param unit L'unité pour laquelle vérifier
     * @param category La catégorie d'équipement recherchée (FIREARM, MELEE, DEFENSIVE)
     * @return Liste des équipements compatibles de cette catégorie
     */
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

    /**
     * Remplace un équipement d'une unité par un nouveau.
     * Si l'unité possède déjà un équipement de la même catégorie et atteint la limite,
     * l'ancien équipement est retiré et rendu à l'inventaire du joueur.
     *
     * @param unit L'unité dont on veut changer l'équipement
     * @param oldEquipment L'équipement à retirer (peut être null si on veut juste équiper)
     * @param newEquipment Le nouvel équipement à ajouter
     * @return true si le remplacement a réussi
     */
    public boolean replaceEquipment(Unit unit, Equipment oldEquipment, Equipment newEquipment) {
        if (unit == null || newEquipment == null) {
            return false;
        }

        // Vérifier que le nouvel équipement est disponible
        if (isEquipmentUnavailable(newEquipment)) {
            logger.warn("Équipement non disponible : {}", newEquipment.getName());
            return false;
        }

        // Si un ancien équipement est spécifié, le retirer d'abord
        if (oldEquipment != null) {
            boolean removed = unit.removeEquipment(oldEquipment);
            if (removed) {
                // Rendre l'équipement à l'inventaire
                incrementEquipmentAvailability(oldEquipment);
                logger.info("Équipement retiré : {}", oldEquipment.getName());
            } else {
                logger.warn("Impossible de retirer l'équipement : {}", oldEquipment.getName());
                return false;
            }
        }

        // Équiper le nouvel équipement
        boolean equipped = unit.addEquipment(newEquipment);
        if (equipped) {
            decrementEquipmentAvailability(newEquipment);
            setTotalEquipmentValue();
            logger.info("Nouvel équipement ajouté : {}", newEquipment.getName());
            return true;
        } else {
            // Si l'équipement échoue, remettre l'ancien si on l'avait retiré
            if (oldEquipment != null) {
                unit.addEquipment(oldEquipment);
                decrementEquipmentAvailability(oldEquipment);
            }
            logger.warn("Impossible d'équiper : {}", newEquipment.getName());
            return false;
        }
    }

    /**
     * Remplace automatiquement un équipement de même catégorie.
     * Trouve automatiquement l'équipement de la même catégorie à remplacer.
     *
     * @param unit L'unité dont on veut changer l'équipement
     * @param newEquipment Le nouvel équipement à ajouter
     * @return true si le remplacement a réussi
     */
    public boolean replaceEquipmentByCategory(Unit unit, Equipment newEquipment) {
        if (unit == null || newEquipment == null) {
            return false;
        }

        EquipmentCategory category = newEquipment.getCategory();

        // Vérifier si l'unité a atteint la limite pour cette catégorie
        long currentCount = unit.countEquipmentsByCategory(category);
        int maxAllowed = switch (category) {
            case FIREARM -> unit.getType().getMaxFirearms();
            case MELEE -> unit.getType().getMaxMeleeWeapons();
            case DEFENSIVE -> unit.getType().getMaxDefensiveEquipment();
        };

        // Si la limite est atteinte, retirer le premier équipement de cette catégorie
        Equipment oldEquipment = null;
        if (currentCount >= maxAllowed) {
            List<Equipment> equipmentsOfCategory = unit.getEquipmentsByCategory(category);
            if (!equipmentsOfCategory.isEmpty()) {
                oldEquipment = equipmentsOfCategory.getFirst();
            }
        }

        return replaceEquipment(unit, oldEquipment, newEquipment);
    }

    /**
     * Incrémente la disponibilité d'un équipement dans l'inventaire.
     * Utilisé quand un équipement est retiré d'une unité.
     *
     * @param equipment L'équipement à rendre disponible
     */
    public void incrementEquipmentAvailability(Equipment equipment) {
        if (equipment == null) return;
        EquipmentStack stack = findStackByName(equipment.getName());
        if (stack != null) {
            stack.incrementAvailable();
            setTotalEquipmentValue();
            calculateTotalEconomyPower();
        }
    }

    // === GESTION DES RESSOURCES DU JOUEUR ===

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


    /**
     * Ajoute une ressource au joueur (sans baseValue - récupéré depuis Resource)
     */
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

    /**
     * Retire une ressource du joueur
     */
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

    /**
     * Vérifie si le joueur possède une quantité donnée d'une ressource
     */
    public boolean hasResource(String resourceName, int quantity) {
        PlayerResource resource = findResourceByName(resourceName);
        return resource != null && resource.hasQuantity(quantity);
    }

    /**
     * Retourne la quantité possédée d'une ressource
     */
    public int getResourceQuantity(String resourceName) {
        PlayerResource resource = findResourceByName(resourceName);
        return resource != null ? resource.getQuantity() : 0;
    }

    /**
     * Trouve une ressource par son nom
     */
    private PlayerResource findResourceByName(String resourceName) {
        if (resourceName == null) return null;
        return resources.stream()
                .filter(resource -> resourceName.equals(resource.getResourceName()))
                .findFirst()
                .orElse(null);
    }

    // === CALCULS ET STATISTIQUES ===

    public void calculateTotalEconomyPower() {
        double economyPower = stats.getTotalIncome()
                + stats.getTotalEquipmentValue()
                + stats.getMoney()
                + stats.getTotalVehiclesValue();
        stats.setTotalEconomyPower(economyPower);
    }

}
