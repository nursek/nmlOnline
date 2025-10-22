# Documentation de la Base de Données - NML Online

## Vue d'ensemble

La base de données est conçue pour gérer un système de jeu avec des joueurs, des équipements, des secteurs et des unités. Elle respecte la logique suivante :

**Un Player contient :**
- Un inventaire d'EquipementStack (qui contient un Equipment unique, sa quantité et sa disponibilité)
- Ses stats (argent, revenus, puissance, etc.)
- Ses secteurs qui contiennent ses unités
- Les équipements des unités décomptent la disponibilité de l'inventaire

## Architecture et Design Patterns

### Pattern Repository
Les repositories Spring Data JPA fournissent l'accès aux données avec des méthodes de recherche personnalisées.

### Pattern Mapper
Les mappers assurent la conversion entre les 3 couches :
- **Entity** (couche infrastructure/persistence)
- **Domain** (couche métier)
- **DTO** (couche API/présentation)

**Important:** Les mappers utilisent le `EquipmentRepository` pour rechercher les équipements existants par nom avant d'en créer de nouveaux. Cela évite les problèmes d'entités transitoires avec Hibernate.

## Structure des Tables

### 1. PLAYERS
Table principale des joueurs.

**Colonnes :**
- `id` : Identifiant unique (BIGINT, AUTO_INCREMENT)
- `name` : Nom du joueur (VARCHAR(255), UNIQUE)
- **Stats embarquées (PlayerStatsEmbeddable) :**
  - `money` : Argent en banque
  - `total_income` : Revenu quotidien total
  - `total_vehicles_value` : Valeur des véhicules
  - `total_equipment_value` : Valeur des équipements
  - `total_offensive_power` : Puissance offensive totale
  - `total_defensive_power` : Puissance défensive totale
  - `global_power` : Puissance globale
  - `total_economy_power` : Puissance économique
  - `total_atk`, `total_pdf`, `total_pdc`, `total_def`, `total_armor` : Stats de combat
- **Bonus embarqués (PlayerBonusesEmbeddable) :**
  - `attack_bonus_percent`, `defense_bonus_percent`, `pdf_bonus_percent`, `pdc_bonus_percent`, `armor_bonus_percent`, `evasion_bonus_percent`

**Relations :**
- One-to-Many vers `EQUIPMENT_STACKS` (inventaire)
- One-to-Many vers `SECTORS` (territoires contrôlés)

**Cascade :** `CascadeType.ALL` avec `orphanRemoval = true` sur les relations

---

### 2. EQUIPMENT
Catalogue des équipements disponibles dans le jeu.

**Colonnes :**
- `id` : Identifiant unique (BIGINT, AUTO_INCREMENT)
- `name` : Nom de l'équipement (VARCHAR(255), UNIQUE)
- `cost` : Coût d'achat (INT)
- `pdf_bonus` : Bonus de puissance de feu (DOUBLE)
- `pdc_bonus` : Bonus de puissance de choc (DOUBLE)
- `arm_bonus` : Bonus d'armure (DOUBLE)
- `evasion_bonus` : Bonus d'esquive (DOUBLE)
- `compatible_class` : Classe compatible unique (VARCHAR(50), nullable) - valeurs: LEGER, TIREUR, MASTODONTE, ELEMENTAIRE, PILOTE_DESTRUCTEUR, SNIPER
- `category` : Catégorie (ENUM : FIREARM, MELEE, DEFENSIVE)

**Note importante :** Un équipement a **une seule classe compatible**. Il n'y a plus de table séparée `EQUIPMENT_COMPATIBLE_CLASSES`. Cette simplification réduit la complexité et améliore les performances.

**Pré-chargement :** 59 équipements sont pré-chargés via `data.sql` au démarrage de l'application.

---

### 3. EQUIPMENT_STACKS
**Inventaire d'équipements du joueur** - Cette table est cruciale pour la gestion des équipements.

**Colonnes :**
- `id` : Identifiant unique (BIGINT, AUTO_INCREMENT)
- `player_id` : Référence vers PLAYERS (NOT NULL)
- `equipment_id` : Référence vers EQUIPMENT (NOT NULL)
- `quantity` : Quantité totale possédée
- `available` : Quantité disponible (non équipée sur des unités)

**Logique importante :**
- Quand un joueur achète un équipement, on incrémente `quantity` et `available`
- Quand une unité équipe cet équipement, on décrémente uniquement `available`
- Quand une unité déséquipe, on incrémente `available`
- La différence `quantity - available` = nombre d'équipements actuellement équipés sur des unités

**Relations :**
- Many-to-One vers `PLAYERS` (ON DELETE CASCADE)
- Many-to-One vers `EQUIPMENT` (ON DELETE CASCADE)

**Mappers :** Le `PlayerMapper` recherche d'abord l'équipement existant par nom (`equipmentRepository.findByName()`) avant de créer l'association. Cela évite les problèmes d'entités transitoires Hibernate.

---

### 4. SECTORS
Secteurs/territoires contrôlés par les joueurs, contenant des unités.

**Colonnes :**
- `id` : Identifiant unique (BIGINT, AUTO_INCREMENT)
- `player_id` : Référence vers PLAYERS (NOT NULL)
- `number` : Numéro du secteur (INT)
- `name` : Nom du secteur (VARCHAR(255))
- `income` : Revenu généré par le secteur (DOUBLE, défaut 2000.0)
- **Stats embarquées (SectorStatsEmbeddable) :**
  - `sector_total_atk`, `sector_total_pdf`, `sector_total_pdc`, `sector_total_def`, `sector_total_armor`
  - `sector_total_offensive`, `sector_total_defensive`, `sector_global_stats`

**Relations :**
- Many-to-One vers `PLAYERS` (ON DELETE CASCADE)
- One-to-Many vers `UNITS` (armée du secteur, CASCADE ALL)

---

### 5. UNITS
Unités militaires appartenant à un secteur.

**Colonnes :**
- `id` : Identifiant unique (BIGINT, AUTO_INCREMENT)
- `sector_id` : Référence vers SECTORS (NOT NULL)
- `name` : Nom de l'unité (VARCHAR(255))
- `number` : Numéro de l'unité (INT)
- `experience` : Points d'expérience (DOUBLE)
- `type` : Type d'unité (ENUM : LARBIN, VOYOU, MALFRAT, BRUTE, PERSONNAGE, VEHICULE)
- `is_injured` : État blessé (BOOLEAN)
- **Statistiques :**
  - `attack`, `defense`, `pdf`, `pdc`, `armor`, `evasion` (DOUBLE)

**Relations :**
- Many-to-One vers `SECTORS` (ON DELETE CASCADE)
- One-to-Many vers `UNIT_CLASSES` (spécialisations, CASCADE ALL)
- One-to-Many vers `UNIT_EQUIPMENTS` (équipements équipés, CASCADE ALL)

**Mappers :** Le `UnitMapper` recherche les équipements existants par nom avant de créer les associations `UnitEquipmentEntity`.

---

### 6. UNIT_CLASSES
Classes de spécialisation des unités (table de collection).

**Colonnes :**
- `unit_id` : Référence vers UNITS (NOT NULL)
- `unit_class` : Classe (ENUM : LEGER, TIREUR, MASTODONTE, ELEMENTAIRE, PILOTE_DESTRUCTEUR, SNIPER)

**Note :** Cette table permet à une unité d'avoir plusieurs classes de spécialisation. C'est une table `@ElementCollection` dans JPA.

---

### 7. UNIT_EQUIPMENTS
**Table de liaison entre les unités et leurs équipements équipés.**

**Colonnes :**
- `id` : Identifiant unique (BIGINT, AUTO_INCREMENT)
- `unit_id` : Référence vers UNITS (NOT NULL)
- `equipment_id` : Référence vers EQUIPMENT (NOT NULL)

**Logique importante :**
- Quand on ajoute un enregistrement ici, on doit décrémenter le champ `available` dans `EQUIPMENT_STACKS`
- Quand on supprime un enregistrement ici, on doit incrémenter le champ `available` dans `EQUIPMENT_STACKS`
- Cette table représente les équipements "en cours d'utilisation" par les unités

**Relations :**
- Many-to-One vers `UNITS` (ON DELETE CASCADE)
- Many-to-One vers `EQUIPMENT` (ON DELETE CASCADE)

---

## Flux de Données - Exemple d'Utilisation

### Exemple 1 : Achat d'équipement
1. Player achète 3x "Fusil d'assaut"
2. Le service récupère l'`EquipmentEntity` existant par son nom
3. Création/Mise à jour dans `EQUIPMENT_STACKS` :
   - `player_id` = ID du joueur
   - `equipment_id` = ID du Fusil d'assaut (référence à l'équipement existant)
   - `quantity` = 3
   - `available` = 3

### Exemple 2 : Équiper une unité
1. Une unité dans un secteur veut s'équiper d'un "Fusil d'assaut"
2. Vérifier dans `EQUIPMENT_STACKS` que `available` > 0
3. Le mapper recherche l'`EquipmentEntity` existant par nom
4. Créer un enregistrement dans `UNIT_EQUIPMENTS` :
   - `unit_id` = ID de l'unité
   - `equipment_id` = ID du Fusil d'assaut (référence à l'équipement existant)
5. Décrémenter `available` dans `EQUIPMENT_STACKS` (3 → 2)
6. Maintenant : `quantity` = 3, `available` = 2, donc 1 équipement est équipé

### Exemple 3 : Déséquiper une unité
1. Supprimer l'enregistrement dans `UNIT_EQUIPMENTS`
2. Incrémenter `available` dans `EQUIPMENT_STACKS` (2 → 3)
3. Maintenant : `quantity` = 3, `available` = 3, donc 0 équipement équipé

### Exemple 4 : Chargement d'un joueur depuis JSON
1. Le `PlayerMapper.toDomain(PlayerDto)` est appelé
2. Pour chaque équipement dans l'inventaire :
   - Le mapper appelle `equipmentRepository.findByName(equipmentName)`
   - Si trouvé → réutilise l'`EquipmentEntity` existant
   - Sinon → crée un nouveau (cas rare, seulement pour les nouveaux équipements)
3. Évite les warnings Hibernate "Unsaved transient entity"

---

## Énumérations Utilisées

### EquipmentCategory
- `FIREARM` : Armes à feu (fusils, pistolets, etc.)
- `MELEE` : Armes de corps à corps (couteaux, battes, etc.)
- `DEFENSIVE` : Équipements défensifs (armures, gilets, etc.)

### UnitClass
- `LEGER` : Unité légère - spécialisée en mobilité
- `TIREUR` : Spécialiste tir - bonus critiques (10% chance, x1.5 dégâts)
- `MASTODONTE` : Tank/Lourd - réduction de dégâts (25% sur PDF/PDC)
- `ELEMENTAIRE` : Spécialiste élémentaire
- `PILOTE_DESTRUCTEUR` : Pilote/Explosifs
- `SNIPER` : Tireur d'élite

### UnitType
- `LARBIN` : Niveau 1 (0-1 exp) - 10 ATK/DEF base
- `VOYOU` : Niveau 2 (2-4 exp) - 20 ATK/DEF base
- `MALFRAT` : Niveau 3 (5-7 exp) - 50 ATK/DEF base
- `BRUTE` : Niveau 4 (8+ exp) - 100 ATK/DEF base
- `PERSONNAGE` : Personnages spéciaux (non combattants)
- `VEHICULE` : Véhicules (à implémenter)

---

## Index de Performance

Les index suivants sont créés pour optimiser les requêtes :
- `idx_equipment_stacks_player` sur `EQUIPMENT_STACKS(player_id)`
- `idx_equipment_stacks_equipment` sur `EQUIPMENT_STACKS(equipment_id)`
- `idx_sectors_player` sur `SECTORS(player_id)`
- `idx_units_sector` sur `UNITS(sector_id)`
- `idx_unit_equipments_unit` sur `UNIT_EQUIPMENTS(unit_id)`
- `idx_unit_equipments_equipment` sur `UNIT_EQUIPMENTS(equipment_id)`

---

## Configuration JPA

Le projet utilise Hibernate avec `spring.jpa.hibernate.ddl-auto=update`, ce qui signifie que les tables seront créées/mises à jour automatiquement au démarrage de l'application.

**Fichier `application.properties` :**
```properties
spring.datasource.url=jdbc:h2:mem:nmlOnline
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.jpa.hibernate.ddl-auto=update
spring.sql.init.mode=always
spring.jpa.show-sql=true
```

**Mode d'initialisation :** `spring.sql.init.mode=always` charge automatiquement :
- `schema.sql` : Création des tables (si nécessaire)
- `data.sql` : Insertion des données initiales (59 équipements + 4 utilisateurs)

---

## Repositories Créés

Tous les repositories Spring Data JPA sont disponibles dans `infrastructure.repository` :

### 1. **PlayerRepository**
```java
Optional<PlayerEntity> findByName(String name);
```

### 2. **EquipmentRepository**
```java
Optional<EquipmentEntity> findByName(String name);
```
**Usage critique :** Utilisé par les mappers pour récupérer les équipements existants et éviter les doublons.

### 3. **EquipmentStackRepository**
```java
List<EquipmentStackEntity> findByPlayer(PlayerEntity player);
Optional<EquipmentStackEntity> findByPlayerAndEquipmentId(PlayerEntity player, Long equipmentId);
```

### 4. **SectorRepository**
```java
List<SectorEntity> findByPlayer(PlayerEntity player);
Optional<SectorEntity> findByPlayerAndNumber(PlayerEntity player, int number);
```

### 5. **UnitRepository**
```java
List<UnitEntity> findBySector(SectorEntity sector);
```

### 6. **UnitEquipmentRepository**
```java
List<UnitEquipmentEntity> findByUnit(UnitEntity unit);
void deleteByUnitAndEquipmentId(UnitEntity unit, Long equipmentId);
```

---

## Architecture des Mappers

### Hiérarchie et Dépendances

```
PlayerMapper
├─→ EquipmentMapper
├─→ SectorMapper
│   └─→ UnitMapper
│       └─→ EquipmentMapper
└─→ EquipmentRepository (injection directe)
```

### Pattern Anti-Corruption Layer

Les mappers servent de couche anti-corruption entre :
- La **couche domaine** (logique métier pure)
- La **couche infrastructure** (JPA/Hibernate)
- La **couche API** (DTOs JSON)

### Stratégie de Résolution des Équipements

**Problème résolu :** Éviter les entités transitoires Hibernate

**Solution :**
1. `PlayerMapper.equipmentStackToEntity()` :
   ```java
   EquipmentEntity equipmentEntity = equipmentRepository.findByName(stack.getEquipment().getName())
           .orElseGet(() -> equipmentMapper.toEntity(stack.getEquipment()));
   ```

2. `UnitMapper.toEntity()` :
   ```java
   var equipmentEntity = equipmentRepository.findByName(equipment.getName())
           .orElseGet(() -> equipmentMapper.toEntity(equipment));
   ```

**Avantages :**
- ✅ Réutilise les équipements existants
- ✅ Évite les doublons en base
- ✅ Élimine les warnings Hibernate
- ✅ Respecte les contraintes de clés étrangères

---

## Points Techniques Importants

### 1. Cascade et Orphan Removal

Toutes les relations parent-enfant utilisent `CascadeType.ALL` avec `orphanRemoval = true` :
- Suppression d'un **Player** → supprime ses **EquipmentStacks** et **Sectors**
- Suppression d'un **Sector** → supprime ses **Units**
- Suppression d'une **Unit** → supprime ses **UnitEquipments** et **UnitClasses**

### 2. Fetch Strategy

- **LAZY** (par défaut) : `@ManyToOne` vers Player, Sector
- **EAGER** : `@ManyToOne` vers Equipment (pour éviter les N+1 queries)

### 3. Embeddables vs Tables Séparées

**Choix de design :**
- `PlayerStats`, `PlayerBonuses`, `SectorStats` : **@Embeddable** (données intrinsèques à l'entité)
- `UnitClasses` : **@ElementCollection** (liste de valeurs simples)
- `EquipmentStacks`, `UnitEquipments` : **Tables séparées** (relations avec métadonnées)

### 4. Gestion des Transactions

Le service `PlayerService` utilise `@Transactional` pour garantir la cohérence :
```java
@Transactional
public Player create(Player player) {
    PlayerEntity entity = playerMapper.toEntity(player);
    PlayerEntity saved = playerRepository.save(entity);
    return playerMapper.toDomain(saved);
}
```

---

## Évolutions Futures

### 1. Gestion des Véhicules
- Table `VEHICLES` à créer
- Relation Many-to-One vers `PLAYERS`
- Intégration dans `PlayerStats.totalVehiclesValue`

### 2. Système de Combat
- Table `BATTLES` pour historiser les combats
- Application des bonus `PlayerBonuses` sur les unités
- Calcul des dégâts avec critiques (classe TIREUR) et réductions (classe MASTODONTE)

### 3. Optimisations
- Cache de second niveau Hibernate pour `EquipmentEntity`
- Projections DTO pour éviter de charger toutes les relations
- Pagination sur les requêtes `findAll()`

### 4. Audit et Historique
- Ajout de `@CreatedDate`, `@LastModifiedDate` sur les entités
- Table `AUDIT_LOG` pour tracer les actions importantes

---

## Diagramme ER Simplifié

```
PLAYERS (1) ────┐
                │
                ├──> (N) EQUIPMENT_STACKS ──> (1) EQUIPMENT
                │
                └──> (N) SECTORS ──> (N) UNITS ──> (N) UNIT_EQUIPMENTS ──> (1) EQUIPMENT
                                         │
                                         └──> (N) UNIT_CLASSES
```

**Légende :**
- (1) : Relation One
- (N) : Relation Many
- ──> : Clé étrangère avec CASCADE

---

## Commandes Utiles

### Accès Console H2
- URL : `http://localhost:8080/h2-console`
- JDBC URL : `jdbc:h2:mem:nmlOnline`
- Username : `sa`
- Password : (vide)

### Requêtes SQL Utiles

```sql
-- Voir tous les équipements d'un joueur
SELECT e.name, es.quantity, es.available
FROM EQUIPMENT_STACKS es
JOIN EQUIPMENT e ON es.equipment_id = e.id
WHERE es.player_id = 1;

-- Voir les équipements équipés par les unités d'un joueur
SELECT u.name as unit_name, e.name as equipment_name
FROM UNIT_EQUIPMENTS ue
JOIN UNITS u ON ue.unit_id = u.id
JOIN EQUIPMENT e ON ue.equipment_id = e.id
JOIN SECTORS s ON u.sector_id = s.id
WHERE s.player_id = 1;

-- Vérifier la cohérence quantity vs available
SELECT 
    e.name,
    es.quantity,
    es.available,
    (es.quantity - es.available) as equipped_count,
    COUNT(ue.id) as actual_equipped
FROM EQUIPMENT_STACKS es
JOIN EQUIPMENT e ON es.equipment_id = e.id
LEFT JOIN UNIT_EQUIPMENTS ue ON ue.equipment_id = e.id
WHERE es.player_id = 1
GROUP BY e.name, es.quantity, es.available;
```

---

## Date de Dernière Mise à Jour

**Version :** 1.1  
**Date :** 2025-10-22  
**Changements :**
- Simplification de `EQUIPMENT` : une seule classe compatible au lieu d'une table séparée
- Correction des mappers pour éviter les entités transitoires Hibernate
- Ajout de la stratégie de résolution des équipements dans les mappers
- Documentation des repositories et de leur usage dans les mappers
- Ajout des requêtes SQL utiles pour le debug
