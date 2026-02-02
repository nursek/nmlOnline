# Implémentation du système de Move Order

## Vue d'ensemble

Le **Move Order** permet à un joueur de déplacer ses unités d'un quartier vers un autre (allié, neutre ou ennemi).

### Caractéristiques principales

1. **Résolution de tour** : Tous les Move Orders sont résolus en fin de tour, sauf les déplacements instantanés
2. **Déplacements instantanés** : Entre territoires du même joueur (exécutés immédiatement)
3. **Consommation de points** : Les points de mouvement sont consommés dès la création de l'ordre pour éviter les déplacements multiples accidentels
4. **Annulation** : Le joueur peut annuler ses ordres pendant le tour pour récupérer ses points de mouvement
5. **Interception** : Les déplacements non-instantanés peuvent être interceptés par des ennemis se déplaçant vers le même quartier

---

## 1. Modèle de données

### 1.1 Entité `MoveOrder`

```java
package com.mg.nmlonline.domain.model.order;

@Entity
@Table(name = "move_orders")
@Getter
@Setter
public class MoveOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID gameId;
    
    @Column(nullable = false)
    private UUID playerId;
    
    @Column(nullable = false)
    private UUID sourceDistrictId;
    
    @Column(nullable = false)
    private UUID targetDistrictId;
    
    @Column(nullable = false)
    private Integer turnNumber;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MoveType moveType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MoveOrderStatus status;
    
    @Column(nullable = false)
    private Integer distanceRequired;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "moveOrder")
    private List<MoveOrderUnit> units = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(
        name = "move_order_path",
        joinColumns = @JoinColumn(name = "move_order_id")
    )
    @OrderColumn(name = "step_order")
    @Column(name = "district_id")
    private List<UUID> pathDistrictIds = new ArrayList<>();
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "moveOrder")
    @OrderBy("stepNumber ASC")
    private List<MoveOrderStep> steps = new ArrayList<>();
}
```

### 1.2 Entité `MoveOrderUnit`

```java
package com.mg.nmlonline.domain.model.order;

@Entity
@Table(name = "move_order_units")
@Getter
@Setter
public class MoveOrderUnit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "move_order_id", nullable = false)
    private MoveOrder moveOrder;
    
    @Column(nullable = false)
    private UUID unitTypeId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private Boolean movementConsumed = true;
}
```

### 1.3 Entité `MoveOrderStep`

Représente chaque étape d'un déplacement multi-quartiers.

```java
package com.mg.nmlonline.domain.model.order;

@Entity
@Table(name = "move_order_steps")
@Getter
@Setter
public class MoveOrderStep {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "move_order_id", nullable = false)
    private MoveOrder moveOrder;
    
    @Column(nullable = false)
    private Integer stepNumber;
    
    @Column(nullable = false)
    private UUID fromDistrictId;
    
    @Column(nullable = false)
    private UUID toDistrictId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status;
    
    @Column(nullable = false)
    private Boolean canBeIntercepted;
    
    @Column(nullable = false, precision = 3, scale = 2)
    private Double defensiveModifier;
}
```

### 1.4 Entité `UnitMovementState`

Suit l'état des points de mouvement de chaque type d'unité par quartier et par tour.

```java
package com.mg.nmlonline.domain.model.order;

@Entity
@Table(
    name = "unit_movement_state",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"game_id", "player_id", "district_id", "unit_type_id", "turn_number"}
    )
)
@Getter
@Setter
public class UnitMovementState {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID gameId;
    
    @Column(nullable = false)
    private UUID playerId;
    
    @Column(nullable = false)
    private UUID districtId;
    
    @Column(nullable = false)
    private UUID unitTypeId;
    
    @Column(nullable = false)
    private Integer turnNumber;
    
    @Column(nullable = false)
    private Integer remainingMovementPoints;
    
    @Column(nullable = false)
    private Integer totalQuantity;
    
    @Column(nullable = false)
    private Integer availableToMove;
}
```

---

## 2. Enums

### 2.1 `MoveType`

```java
package com.mg.nmlonline.domain.model.order;

public enum MoveType {
    /**
     * Déplacement instantané entre territoires du même joueur.
     * Exécuté immédiatement, non interceptable.
     */
    INSTANT,
    
    /**
     * Déplacement standard (1 quartier) vers neutre/ennemi.
     * Résolu en fin de tour, interceptable.
     */
    STANDARD,
    
    /**
     * Déplacement longue portée (2+ quartiers).
     * Résolu en fin de tour, interceptable à chaque étape.
     */
    LONG_RANGE
}
```

### 2.2 `MoveOrderStatus`

```java
package com.mg.nmlonline.domain.model.order;

public enum MoveOrderStatus {
    /**
     * En attente de résolution en fin de tour.
     */
    PENDING,
    
    /**
     * Exécuté immédiatement (déplacement instantané).
     */
    EXECUTED,
    
    /**
     * Résolu en fin de tour avec succès.
     */
    RESOLVED,
    
    /**
     * Intercepté par un ennemi.
     */
    INTERCEPTED,
    
    /**
     * Annulé par le joueur.
     */
    CANCELLED
}
```

### 2.3 `StepStatus`

```java
package com.mg.nmlonline.domain.model.order;

public enum StepStatus {
    /**
     * Étape en attente de résolution.
     */
    PENDING,
    
    /**
     * Unités en transit sur ce quartier (vulnérables).
     */
    IN_TRANSIT,
    
    /**
     * Étape terminée avec succès.
     */
    COMPLETED,
    
    /**
     * Intercepté à cette étape.
     */
    INTERCEPTED
}
```

---

## 3. Modifications de `UnitClass`

Ajouter les méthodes de gestion du mouvement à l'enum existant.

```java
package com.mg.nmlonline.domain.model.unit;

import lombok.Getter;

@Getter
public enum UnitClass {
    LEGER("L") {
        @Override
        public int getMovementPoints() {
            return 2;
        }

        @Override
        public int getMaxMovementRange() {
            return 2;
        }

        @Override
        public double getDefensiveModifierAtStep(int stepNumber, int totalSteps) {
            // Si déplacement de 2 quartiers ET qu'on est sur la 1ère étape
            // ALORS défense réduite à 50%
            return (totalSteps == 2 && stepNumber == 1) ? 0.5 : 1.0;
        }
    },

    ELEMENTAIRE("E") {},

    TIREUR("T") {
        @Override
        public double getCriticalChance() {
            return 0.10;
        }

        @Override
        public double getCriticalMultiplier() {
            return 1.5;
        }
    },

    MASTODONTE("M") {
        @Override
        public double getDamageReduction(String damageType) {
            return switch (damageType) {
                case "PDF", "PDC" -> 0.25;
                default -> 0.0;
            };
        }
    },

    PILOTE_DESTRUCTEUR("P") {},

    SNIPER("S") {};

    private final String code;

    UnitClass(String code) {
        this.code = code;
    }

    // Méthodes par défaut (surchargées par certaines classes d'unité)
    
    /**
     * Points de mouvement disponibles par tour.
     * @return nombre de points (1 par défaut, 2 pour LEGER)
     */
    public int getMovementPoints() {
        return 1;
    }

    /**
     * Distance maximale de déplacement en un tour.
     * @return nombre de quartiers (1 par défaut, 2 pour LEGER, 3-5 pour véhicules futurs)
     */
    public int getMaxMovementRange() {
        return 1;
    }

    /**
     * Modificateur défensif selon l'étape du déplacement.
     * @param stepNumber numéro de l'étape actuelle (1-based)
     * @param totalSteps nombre total d'étapes du déplacement
     * @return multiplicateur des stats défensives (1.0 = 100%, 0.5 = 50%)
     */
    public double getDefensiveModifierAtStep(int stepNumber, int totalSteps) {
        return 1.0;
    }

    public double getDamageReduction(String damageType) {
        return 0;
    }

    public double getCriticalChance() {
        return 0.0;
    }

    public double getCriticalMultiplier() {
        return 1.0;
    }
}
```

**Note** : Pour les véhicules futurs, on ajoutera simplement de nouvelles entrées dans l'enum avec leurs propres valeurs de `getMovementPoints()` et `getMaxMovementRange()`.

---

## 4. Services à créer

### 4.1 `MoveOrderService`

Service principal pour la gestion des Move Orders.

```java
package com.mg.nmlonline.domain.service;

@Service
@Transactional
public class MoveOrderService {
    
    /**
     * Crée un Move Order et consomme immédiatement les points de mouvement.
     * Si le mouvement est INSTANT, l'exécute immédiatement.
     * 
     * @param dto données du Move Order
     * @return l'ordre créé
     * @throws IllegalArgumentException si validation échoue
     */
    MoveOrder createMoveOrder(CreateMoveOrderDto dto);
    
    /**
     * Annule un ordre et restaure les points de mouvement.
     * Seulement possible pour les ordres PENDING.
     * 
     * @param orderId ID de l'ordre à annuler
     * @param playerId ID du joueur (vérification de propriété)
     */
    void cancelMoveOrder(UUID orderId, UUID playerId);
    
    /**
     * Annule tous les ordres en attente d'un joueur pour le tour actuel.
     * Restaure tous les points de mouvement.
     * 
     * @param playerId ID du joueur
     * @param gameId ID de la partie
     * @param turnNumber numéro du tour
     */
    void cancelAllPendingOrders(UUID playerId, UUID gameId, Integer turnNumber);
    
    /**
     * Exécute immédiatement tous les Move Orders de type INSTANT.
     * 
     * @param gameId ID de la partie
     * @param turnNumber numéro du tour
     */
    void executeInstantMoves(UUID gameId, Integer turnNumber);
    
    /**
     * Récupère tous les Move Orders d'un joueur pour le tour actuel.
     * 
     * @param playerId ID du joueur
     * @param gameId ID de la partie
     * @param turnNumber numéro du tour
     * @return liste des ordres
     */
    List<MoveOrder> getPlayerOrders(UUID playerId, UUID gameId, Integer turnNumber);
}
```

### 4.2 `MovementValidator`

Service de validation des mouvements.

```java
package com.mg.nmlonline.domain.service;

@Service
public class MovementValidator {
    
    /**
     * Valide qu'un Move Order est légal.
     * 
     * @param dto données du Move Order
     * @return résultat de validation avec erreurs éventuelles
     */
    ValidationResult validateMoveOrder(CreateMoveOrderDto dto);
    
    /**
     * Vérifie que les unités ont assez de points de mouvement.
     * 
     * @param units liste des unités à déplacer
     * @param distance distance à parcourir
     * @param districtId quartier de départ
     * @param playerId ID du joueur
     * @param turnNumber numéro du tour
     * @return true si tous les points sont disponibles
     */
    boolean hasEnoughMovementPoints(
        List<UnitQuantityDto> units,
        int distance,
        UUID districtId,
        UUID playerId,
        Integer turnNumber
    );
    
    /**
     * Vérifie que deux quartiers sont adjacents.
     * 
     * @param sourceId ID du quartier source
     * @param targetId ID du quartier cible
     * @return true si adjacents
     */
    boolean areDistrictsAdjacent(UUID sourceId, UUID targetId);
    
    /**
     * Calcule le type de mouvement (INSTANT, STANDARD, LONG_RANGE).
     * 
     * @param sourceId quartier de départ
     * @param targetId quartier d'arrivée
     * @param playerId ID du joueur
     * @param path chemin complet
     * @return type de mouvement
     */
    MoveType calculateMoveType(
        UUID sourceId,
        UUID targetId,
        UUID playerId,
        List<UUID> path
    );
    
    /**
     * Vérifie que les unités sont disponibles dans le quartier.
     * 
     * @param districtId ID du quartier
     * @param units unités requises avec quantités
     * @param playerId ID du joueur
     * @param turnNumber numéro du tour (pour exclure unités déjà déplacées)
     * @return true si toutes les unités sont disponibles
     */
    boolean areUnitsAvailable(
        UUID districtId,
        List<UnitQuantityDto> units,
        UUID playerId,
        Integer turnNumber
    );
    
    /**
     * Vérifie que toutes les unités peuvent parcourir la distance.
     * 
     * @param units liste des unités
     * @param distance distance totale
     * @return true si toutes les unités ont la portée suffisante
     */
    boolean canAllUnitsReachDistance(List<UUID> unitTypeIds, int distance);
}
```

### 4.3 `PathfindingService`

Service de calcul de chemins.

```java
package com.mg.nmlonline.domain.service;

@Service
public class PathfindingService {
    
    /**
     * Trouve le chemin le plus court entre deux quartiers.
     * Utilise Dijkstra ou A*.
     * 
     * @param sourceId quartier de départ
     * @param targetId quartier d'arrivée
     * @param maxDistance distance maximale autorisée
     * @return liste ordonnée des IDs de quartiers (source inclus)
     * @throws PathNotFoundException si aucun chemin trouvé
     */
    List<UUID> findShortestPath(UUID sourceId, UUID targetId, int maxDistance);
    
    /**
     * Vérifie qu'un chemin est valide (tous les quartiers sont adjacents).
     * 
     * @param path liste des quartiers
     * @return true si le chemin est valide
     */
    boolean isPathValid(List<UUID> path);
    
    /**
     * Calcule la distance entre deux quartiers.
     * 
     * @param sourceId quartier de départ
     * @param targetId quartier d'arrivée
     * @return nombre de quartiers à traverser
     */
    int calculateDistance(UUID sourceId, UUID targetId);
}
```

### 4.4 `MoveResolutionService`

Service de résolution des Move Orders en fin de tour.

```java
package com.mg.nmlonline.domain.service;

@Service
@Transactional
public class MoveResolutionService {
    
    /**
     * Résout tous les Move Orders en attente pour un tour.
     * Appelé en fin de tour.
     * 
     * @param gameId ID de la partie
     * @param turnNumber numéro du tour
     */
    void resolveTurnMoves(UUID gameId, Integer turnNumber);
    
    /**
     * Détecte les interceptions (ordres vers le même quartier).
     * 
     * @param moves liste des Move Orders à résoudre
     * @return liste des interceptions détectées
     */
    List<MoveInterception> detectInterceptions(List<MoveOrder> moves);
    
    /**
     * Applique un déplacement effectif (met à jour les unités sur les quartiers).
     * 
     * @param order le Move Order à appliquer
     */
    void applyMove(MoveOrder order);
    
    /**
     * Gère une interception (déclenche un combat).
     * 
     * @param interception détails de l'interception
     */
    void handleInterception(MoveInterception interception);
    
    /**
     * Réinitialise tous les points de mouvement pour le prochain tour.
     * 
     * @param gameId ID de la partie
     * @param turnNumber numéro du tour
     */
    void resetMovementPoints(UUID gameId, Integer turnNumber);
    
    /**
     * Résout les mouvements étape par étape (pour les LONG_RANGE).
     * 
     * @param moves ordres à résoudre
     */
    void resolveMovesStepByStep(List<MoveOrder> moves);
}
```

### 4.5 `UnitMovementStateService`

Service de gestion de l'état des points de mouvement.

```java
package com.mg.nmlonline.domain.service;

@Service
@Transactional
public class UnitMovementStateService {
    
    /**
     * Initialise l'état des mouvements pour un tour.
     * Crée les UnitMovementState pour toutes les unités.
     * 
     * @param gameId ID de la partie
     * @param turnNumber numéro du tour
     */
    void initializeTurnMovementState(UUID gameId, Integer turnNumber);
    
    /**
     * Consomme des points de mouvement pour des unités.
     * 
     * @param districtId quartier concerné
     * @param units unités avec quantités
     * @param pointsToConsume nombre de points à consommer
     * @param playerId ID du joueur
     * @param turnNumber numéro du tour
     */
    void consumeMovementPoints(
        UUID districtId,
        List<UnitQuantityDto> units,
        int pointsToConsume,
        UUID playerId,
        Integer turnNumber
    );
    
    /**
     * Restaure des points de mouvement (après annulation).
     * 
     * @param moveOrder ordre annulé
     */
    void restoreMovementPoints(MoveOrder moveOrder);
    
    /**
     * Récupère les points de mouvement restants pour une unité.
     * 
     * @param districtId quartier
     * @param unitTypeId type d'unité
     * @param playerId ID du joueur
     * @param turnNumber numéro du tour
     * @return points restants
     */
    int getRemainingMovementPoints(
        UUID districtId,
        UUID unitTypeId,
        UUID playerId,
        Integer turnNumber
    );
    
    /**
     * Réinitialise tous les points de mouvement pour le tour suivant.
     * 
     * @param gameId ID de la partie
     * @param turnNumber numéro du tour
     */
    void resetAllMovementPoints(UUID gameId, Integer turnNumber);
}
```

---

## 5. DTOs

### 5.1 `CreateMoveOrderDto`

```java
package com.mg.nmlonline.api.dto;

@Data
public class CreateMoveOrderDto {
    @NotNull
    private UUID gameId;
    
    @NotNull
    private UUID playerId;
    
    @NotNull
    private UUID sourceDistrictId;
    
    @NotNull
    private UUID targetDistrictId;
    
    @NotEmpty
    private List<UnitQuantityDto> units;
}
```

### 5.2 `UnitQuantityDto`

```java
package com.mg.nmlonline.api.dto;

@Data
public class UnitQuantityDto {
    @NotNull
    private UUID unitTypeId;
    
    @NotNull
    @Min(1)
    private Integer quantity;
}
```

### 5.3 `MoveOrderResponseDto`

```java
package com.mg.nmlonline.api.dto;

@Data
public class MoveOrderResponseDto {
    private UUID id;
    private UUID sourceDistrictId;
    private UUID targetDistrictId;
    private MoveType moveType;
    private MoveOrderStatus status;
    private Integer distanceRequired;
    private List<UUID> path;
    private List<UnitQuantityDto> units;
    private List<MoveOrderStepDto> steps;
    private LocalDateTime createdAt;
}
```

### 5.4 `MoveOrderStepDto`

```java
package com.mg.nmlonline.api.dto;

@Data
public class MoveOrderStepDto {
    private Integer stepNumber;
    private UUID fromDistrictId;
    private UUID toDistrictId;
    private StepStatus status;
    private Boolean canBeIntercepted;
    private Double defensiveModifier;
}
```

### 5.5 `ValidationResult`

```java
package com.mg.nmlonline.domain.model;

@Data
@AllArgsConstructor
public class ValidationResult {
    private boolean valid;
    private List<String> errors;
    
    public static ValidationResult valid() {
        return new ValidationResult(true, Collections.emptyList());
    }
    
    public static ValidationResult invalid(String... errors) {
        return new ValidationResult(false, Arrays.asList(errors));
    }
}
```

---

## 6. Repositories

### 6.1 `MoveOrderRepository`

```java
package com.mg.nmlonline.infrastructure.repository;

@Repository
public interface MoveOrderRepository extends JpaRepository<MoveOrder, UUID> {
    
    List<MoveOrder> findByGameIdAndTurnNumberAndStatus(
        UUID gameId,
        Integer turnNumber,
        MoveOrderStatus status
    );
    
    List<MoveOrder> findByPlayerIdAndGameIdAndTurnNumber(
        UUID playerId,
        UUID gameId,
        Integer turnNumber
    );
    
    List<MoveOrder> findByGameIdAndTurnNumberAndStatusIn(
        UUID gameId,
        Integer turnNumber,
        List<MoveOrderStatus> statuses
    );
    
    @Query("SELECT m FROM MoveOrder m WHERE m.gameId = :gameId " +
           "AND m.turnNumber = :turnNumber " +
           "AND m.status = 'PENDING' " +
           "AND m.moveType = 'INSTANT'")
    List<MoveOrder> findInstantMovesForTurn(
        @Param("gameId") UUID gameId,
        @Param("turnNumber") Integer turnNumber
    );
}
```

### 6.2 `UnitMovementStateRepository`

```java
package com.mg.nmlonline.infrastructure.repository;

@Repository
public interface UnitMovementStateRepository extends JpaRepository<UnitMovementState, UUID> {
    
    Optional<UnitMovementState> findByGameIdAndPlayerIdAndDistrictIdAndUnitTypeIdAndTurnNumber(
        UUID gameId,
        UUID playerId,
        UUID districtId,
        UUID unitTypeId,
        Integer turnNumber
    );
    
    List<UnitMovementState> findByGameIdAndTurnNumber(UUID gameId, Integer turnNumber);
    
    void deleteByGameIdAndTurnNumber(UUID gameId, Integer turnNumber);
}
```

---

## 7. SQL Schema

### 7.1 Tables principales

```sql
-- Table des Move Orders
CREATE TABLE move_orders (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL,
    player_id UUID NOT NULL,
    source_district_id UUID NOT NULL,
    target_district_id UUID NOT NULL,
    turn_number INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    move_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    distance_required INTEGER NOT NULL,
    CONSTRAINT fk_move_order_game FOREIGN KEY (game_id) REFERENCES games(id),
    CONSTRAINT fk_move_order_player FOREIGN KEY (player_id) REFERENCES players(id)
);

CREATE INDEX idx_move_orders_game_turn ON move_orders(game_id, turn_number);
CREATE INDEX idx_move_orders_player ON move_orders(player_id, game_id, turn_number);
CREATE INDEX idx_move_orders_status ON move_orders(status, game_id, turn_number);

-- Table des unités dans les Move Orders
CREATE TABLE move_order_units (
    id UUID PRIMARY KEY,
    move_order_id UUID NOT NULL,
    unit_type_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    movement_consumed BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_move_order_unit_order FOREIGN KEY (move_order_id) REFERENCES move_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_move_order_unit_type FOREIGN KEY (unit_type_id) REFERENCES unit_types(id)
);

CREATE INDEX idx_move_order_units_order ON move_order_units(move_order_id);

-- Table des étapes de déplacement
CREATE TABLE move_order_steps (
    id UUID PRIMARY KEY,
    move_order_id UUID NOT NULL,
    step_number INTEGER NOT NULL,
    from_district_id UUID NOT NULL,
    to_district_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    can_be_intercepted BOOLEAN NOT NULL,
    defensive_modifier DECIMAL(3,2) NOT NULL,
    CONSTRAINT fk_move_order_step_order FOREIGN KEY (move_order_id) REFERENCES move_orders(id) ON DELETE CASCADE,
    CONSTRAINT uq_move_order_step UNIQUE (move_order_id, step_number)
);

CREATE INDEX idx_move_order_steps_order ON move_order_steps(move_order_id);

-- Table du chemin de déplacement
CREATE TABLE move_order_path (
    move_order_id UUID NOT NULL,
    district_id UUID NOT NULL,
    step_order INTEGER NOT NULL,
    PRIMARY KEY (move_order_id, step_order),
    CONSTRAINT fk_move_order_path_order FOREIGN KEY (move_order_id) REFERENCES move_orders(id) ON DELETE CASCADE
);

-- Table de l'état des mouvements des unités
CREATE TABLE unit_movement_state (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL,
    player_id UUID NOT NULL,
    district_id UUID NOT NULL,
    unit_type_id UUID NOT NULL,
    turn_number INTEGER NOT NULL,
    remaining_movement_points INTEGER NOT NULL,
    total_quantity INTEGER NOT NULL,
    available_to_move INTEGER NOT NULL,
    CONSTRAINT fk_unit_movement_game FOREIGN KEY (game_id) REFERENCES games(id),
    CONSTRAINT fk_unit_movement_player FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT fk_unit_movement_unit_type FOREIGN KEY (unit_type_id) REFERENCES unit_types(id),
    CONSTRAINT uq_unit_movement_state UNIQUE (game_id, player_id, district_id, unit_type_id, turn_number)
);

CREATE INDEX idx_unit_movement_state_game_turn ON unit_movement_state(game_id, turn_number);
CREATE INDEX idx_unit_movement_state_player ON unit_movement_state(player_id, district_id, turn_number);
```

---

## 8. Workflow de résolution

### 8.1 Création d'un Move Order (pendant le tour)

1. **Validation** :
   - Vérifier que le joueur possède les unités dans le quartier source
   - Calculer le chemin et la distance
   - Vérifier que les unités ont assez de points de mouvement restants
   - Vérifier que les unités peuvent parcourir la distance

2. **Création de l'ordre** :
   - Déterminer le `MoveType` (INSTANT si tous les quartiers appartiennent au joueur)
   - Créer les `MoveOrderStep` pour chaque étape
   - Calculer les `defensiveModifier` pour chaque étape

3. **Consommation des points** :
   - Mettre à jour `UnitMovementState` pour chaque type d'unité
   - Décrémenter `remainingMovementPoints`
   - Décrémenter `availableToMove`

4. **Exécution immédiate si INSTANT** :
   - Déplacer les unités immédiatement
   - Statut → `EXECUTED`

5. **Sinon** :
   - Statut → `PENDING`

### 8.2 Annulation d'un Move Order

1. Vérifier que l'ordre est en statut `PENDING`
2. Restaurer les points de mouvement dans `UnitMovementState`
3. Changer le statut → `CANCELLED`
4. Supprimer l'ordre (optionnel, ou garder pour historique)

### 8.3 Résolution en fin de tour

1. **Récupération des ordres** :
   - Tous les `MoveOrder` avec statut `PENDING` pour le tour

2. **Tri par timestamp** :
   - Ordre de création pour résolution des conflits

3. **Détection des interceptions** :
   - Grouper les ordres par quartier cible
   - Si plusieurs joueurs différents → interception
   - Identifier l'étape d'interception

4. **Résolution étape par étape** :
   - Pour chaque étape du déplacement le plus long :
     - Vérifier les interceptions à cette étape
     - Si interception → déclencher combat, arrêter le mouvement
     - Sinon → continuer

5. **Application des mouvements** :
   - Déplacer les unités des ordres non interceptés
   - Mettre à jour les quartiers
   - Statuts → `RESOLVED` ou `INTERCEPTED`

6. **Réinitialisation** :
   - Supprimer tous les `UnitMovementState` du tour
   - Recréer pour le tour suivant avec points pleins

---

## 9. Règles métier détaillées

### 9.1 Calcul du type de mouvement

```
SI tous les quartiers du chemin appartiennent au joueur
  ALORS MoveType = INSTANT
SINON SI distance == 1
  ALORS MoveType = STANDARD
SINON
  ALORS MoveType = LONG_RANGE
```

### 9.2 Vérification des points de mouvement

Pour chaque type d'unité à déplacer :
```
pointsNécessaires = distance
pointsDisponibles = UnitMovementState.remainingMovementPoints
quantitéDisponible = UnitMovementState.availableToMove

SI pointsDisponibles < pointsNécessaires
  ALORS ERREUR "Pas assez de points de mouvement"
  
SI quantitéDisponible < quantitéDemandée
  ALORS ERREUR "Pas assez d'unités disponibles"
```

### 9.3 Calcul des modificateurs défensifs

Pour chaque `MoveOrderStep` :
```java
for (int i = 0; i < steps.size(); i++) {
    int stepNumber = i + 1;
    int totalSteps = steps.size();
    
    for (UnitType unit : units) {
        double modifier = unit.getUnitClass().getDefensiveModifierAtStep(stepNumber, totalSteps);
        step.setDefensiveModifier(modifier);
    }
}
```

**Exemple** : Unités LEGER de A → B → C
- Étape 1 (A → B) : `getDefensiveModifierAtStep(1, 2)` → 0.5 (défense réduite)
- Étape 2 (B → C) : `getDefensiveModifierAtStep(2, 2)` → 1.0 (défense normale)

### 9.4 Interceptions

Une interception se produit quand :
1. Deux joueurs différents ont des ordres vers le même quartier
2. Les ordres se rencontrent à la même étape de résolution
3. Au moins un des ordres est interceptable (`canBeIntercepted = true`)

Les déplacements INSTANT ne peuvent jamais être interceptés.

---

## 10. Extensions futures

### 10.1 Véhicules et personnages

Ajouter de nouvelles entrées dans `UnitClass` :

```java
VEHICULE_LEGER("VL") {
    @Override
    public int getMovementPoints() {
        return 3;
    }

    @Override
    public int getMaxMovementRange() {
        return 3;
    }
},

VEHICULE_LOURD("VH") {
    @Override
    public int getMovementPoints() {
        return 5;
    }

    @Override
    public int getMaxMovementRange() {
        return 5;
    }
    
    @Override
    public double getDefensiveModifierAtStep(int stepNumber, int totalSteps) {
        // Pas de pénalité défensive pour les véhicules lourds
        return 1.0;
    }
},

PERSONNAGE_RAPIDE("PR") {
    @Override
    public int getMovementPoints() {
        return 4;
    }

    @Override
    public int getMaxMovementRange() {
        return 4;
    }
}
```

### 10.2 Restrictions de terrain

Ajouter un enum `MovementRestriction` :

```java
public enum MovementRestriction {
    TERRAIN_MOUNTAIN,
    TERRAIN_WATER,
    REQUIRES_ROAD,
    WEATHER_SENSITIVE,
    NIGHT_ONLY
}
```

Et dans `Movable` :
```java
Set<MovementRestriction> getMovementRestrictions();
```

Le `PathfindingService` devra prendre en compte ces restrictions lors du calcul des chemins.

### 10.3 Coûts variables par terrain

Ajouter dans `District` :
```java
private TerrainType terrainType;
```

Et dans `PathfindingService` :
```java
int calculatePathCost(List<UUID> path) {
    int cost = 0;
    for (UUID districtId : path) {
        District district = districtRepository.findById(districtId);
        cost += district.getTerrainType().getMovementCost();
    }
    return cost;
}
```

---

## 11. Tests à implémenter

### 11.1 Tests unitaires

- `UnitClass.getMovementPoints()` pour chaque type
- `UnitClass.getDefensiveModifierAtStep()` pour LEGER
- `MovementValidator.validateMoveOrder()` avec divers cas
- `PathfindingService.findShortestPath()` sur différents graphes
- `MoveResolutionService.detectInterceptions()`

### 11.2 Tests d'intégration

- Création d'un Move Order INSTANT et vérification de l'exécution immédiate
- Création d'un Move Order PENDING et résolution en fin de tour
- Annulation d'un Move Order et restauration des points
- Interception de deux joueurs sur le même quartier
- Déplacement LONG_RANGE avec étapes multiples
- Consommation et réinitialisation des points de mouvement

### 11.3 Tests de scénarios

- Joueur A déplace A→B→C, Joueur B déplace D→B → interception à B
- Joueur A déplace A→B (instant), B→C (standard) dans le même tour
- Tentative de redéplacer une unité déjà déplacée → erreur
- Unité LEGER avec défense réduite interceptée sur quartier intermédiaire
- Annulation de tous les ordres et recréation

---

## 12. API REST

### 12.1 Endpoints

```
POST   /api/games/{gameId}/moves
GET    /api/games/{gameId}/moves?turnNumber={turn}&playerId={player}
DELETE /api/games/{gameId}/moves/{moveId}
DELETE /api/games/{gameId}/moves?playerId={player}&turnNumber={turn}
POST   /api/games/{gameId}/turns/{turnNumber}/resolve-moves
POST   /api/games/{gameId}/turns/{turnNumber}/execute-instant-moves
```

### 12.2 Contrôleur

```java
@RestController
@RequestMapping("/api/games/{gameId}/moves")
public class MoveOrderController {
    
    @PostMapping
    public ResponseEntity<MoveOrderResponseDto> createMoveOrder(
        @PathVariable UUID gameId,
        @RequestBody CreateMoveOrderDto dto
    ) {
        // Créer le Move Order
    }
    
    @GetMapping
    public ResponseEntity<List<MoveOrderResponseDto>> getPlayerMoves(
        @PathVariable UUID gameId,
        @RequestParam UUID playerId,
        @RequestParam Integer turnNumber
    ) {
        // Récupérer les ordres du joueur
    }
    
    @DeleteMapping("/{moveId}")
    public ResponseEntity<Void> cancelMoveOrder(
        @PathVariable UUID gameId,
        @PathVariable UUID moveId,
        @RequestParam UUID playerId
    ) {
        // Annuler un ordre
    }
    
    @DeleteMapping
    public ResponseEntity<Void> cancelAllPlayerMoves(
        @PathVariable UUID gameId,
        @RequestParam UUID playerId,
        @RequestParam Integer turnNumber
    ) {
        // Annuler tous les ordres du joueur
    }
}
```

---

## 13. Questions ouvertes

### À clarifier avec le code existant

1. **Structure du Board** : Comment sont stockés les quartiers et leur adjacence ?
2. **Structure des unités** : Comment sont stockées les unités par quartier ?
3. **Système de tours** : Existe-t-il déjà une entité `Game` ou `Turn` ?
4. **Combat** : Comment déclencher un combat lors d'une interception ?
5. **Événements** : Faut-il un système d'événements pour notifier les joueurs ?

### Optimisations possibles

1. **Cache** : Mettre en cache les chemins calculés
2. **Batch processing** : Résoudre les mouvements par lots
3. **Index SQL** : Ajouter des index pour les requêtes fréquentes
4. **WebSocket** : Notifier en temps réel les joueurs des interceptions

---

## 14. Ordre d'implémentation suggéré

1. **Phase 1 : Fondations**
   - Créer les enums (`MoveType`, `MoveOrderStatus`, `StepStatus`)
   - Modifier `UnitClass` avec les méthodes de mouvement
   - Créer les entités JPA (`MoveOrder`, `MoveOrderUnit`, `MoveOrderStep`, `UnitMovementState`)
   - Créer les repositories

2. **Phase 2 : Validation**
   - Implémenter `MovementValidator`
   - Implémenter `PathfindingService`
   - Tests unitaires de validation

3. **Phase 3 : Gestion des ordres**
   - Implémenter `UnitMovementStateService`
   - Implémenter `MoveOrderService` (création, annulation)
   - Tests d'intégration création/annulation

4. **Phase 4 : Résolution**
   - Implémenter `MoveResolutionService`
   - Tests d'interception
   - Tests de résolution complète

5. **Phase 5 : API**
   - Créer les DTOs
   - Créer le contrôleur REST
   - Tests E2E

6. **Phase 6 : Polish**
   - Gestion d'erreurs
   - Logging
   - Documentation Swagger
   - Tests de performance

---

## Résumé

Le système de Move Order permet une gestion complète et flexible des déplacements d'unités avec :

- ✅ Déplacements instantanés entre territoires alliés
- ✅ Déplacements standard et longue portée avec résolution en fin de tour
- ✅ Consommation de points de mouvement pour éviter les déplacements multiples
- ✅ Système d'annulation pour récupérer les points
- ✅ Détection et gestion des interceptions
- ✅ Support des modificateurs défensifs (unités LEGER)
- ✅ Extensibilité pour véhicules et personnages futurs
- ✅ Pathfinding automatique avec validation

Le système est conçu pour être extensible et maintenable, en suivant les principes de Clean Architecture et Domain-Driven Design.
