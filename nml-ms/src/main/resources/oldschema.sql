-- Schema pour NML Online Database
-- Création des tables pour le système de jeu avec Players, Equipment, Sectors et Units

-- =============================================
-- Table PLAYERS : Contient les joueurs
-- =============================================
CREATE TABLE IF NOT EXISTS PLAYERS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,

    -- Stats du joueur (embedded PlayerStatsEmbeddable)
    money DOUBLE DEFAULT 0.0,
    total_income DOUBLE DEFAULT 0.0,
    total_vehicles_value DOUBLE DEFAULT 0.0,
    total_equipment_value DOUBLE DEFAULT 0.0,
    total_offensive_power DOUBLE DEFAULT 0.0,
    total_defensive_power DOUBLE DEFAULT 0.0,
    global_power DOUBLE DEFAULT 0.0,
    total_economy_power DOUBLE DEFAULT 0.0,
    total_atk DOUBLE DEFAULT 0.0,
    total_pdf DOUBLE DEFAULT 0.0,
    total_pdc DOUBLE DEFAULT 0.0,
    total_def DOUBLE DEFAULT 0.0,
    total_armor DOUBLE DEFAULT 0.0,

    -- Bonuses du joueur (embedded PlayerBonusesEmbeddable)
    attack_bonus_percent DOUBLE DEFAULT 0.0,
    defense_bonus_percent DOUBLE DEFAULT 0.0,
    pdf_bonus_percent DOUBLE DEFAULT 0.0,
    pdc_bonus_percent DOUBLE DEFAULT 0.0,
    armor_bonus_percent DOUBLE DEFAULT 0.0,
    evasion_bonus_percent DOUBLE DEFAULT 0.0
);


-- =============================================
-- Table EQUIPMENT : Catalogue des équipements
-- =============================================
CREATE TABLE IF NOT EXISTS EQUIPMENT (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    cost INT NOT NULL,
    pdf_bonus DOUBLE NOT NULL DEFAULT 0.0,
    pdc_bonus DOUBLE NOT NULL DEFAULT 0.0,
    arm_bonus DOUBLE NOT NULL DEFAULT 0.0,
    evasion_bonus DOUBLE NOT NULL DEFAULT 0.0,
    category VARCHAR(50) NOT NULL -- FIREARM, MELEE, DEFENSIVE
);

-- =============================================
-- Table EQUIPMENT_COMPATIBLE_CLASSES : Classes compatibles par équipement
-- =============================================
CREATE TABLE IF NOT EXISTS EQUIPMENT_COMPATIBLE_CLASSES (
    equipment_id BIGINT NOT NULL,
    unit_class VARCHAR(50) NOT NULL, -- LEGER, TIREUR, MASTODONTE, etc.
    PRIMARY KEY (equipment_id, unit_class),
    FOREIGN KEY (equipment_id) REFERENCES EQUIPMENT(id) ON DELETE CASCADE
);

-- =============================================
-- Table EQUIPMENT_STACKS : Inventaire d'équipements du joueur
-- =============================================
CREATE TABLE IF NOT EXISTS EQUIPMENT_STACKS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    available INT NOT NULL DEFAULT 1,
    FOREIGN KEY (player_id) REFERENCES PLAYERS(id) ON DELETE CASCADE,
    FOREIGN KEY (equipment_id) REFERENCES EQUIPMENT(id) ON DELETE CASCADE
);

-- =============================================
-- Table BOARDS : Représente les cartes de jeu
-- =============================================
CREATE TABLE IF NOT EXISTS BOARDS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- =============================================
-- Table SECTORS : Secteurs contrôlés par les joueurs ou neutres
-- =============================================
CREATE TABLE IF NOT EXISTS SECTORS (
    number INT NOT NULL,
    board_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    income DOUBLE NOT NULL DEFAULT 2000.0,

    -- Colonnes pour la carte
    owner_id BIGINT, -- null si secteur neutre
    color VARCHAR(7) NOT NULL DEFAULT '#ffffff',
    resource VARCHAR(50),

    -- Stats du secteur (embedded SectorStatsEmbeddable)
    sector_total_atk DOUBLE DEFAULT 0.0,
    sector_total_pdf DOUBLE DEFAULT 0.0,
    sector_total_pdc DOUBLE DEFAULT 0.0,
    sector_total_def DOUBLE DEFAULT 0.0,
    sector_total_armor DOUBLE DEFAULT 0.0,
    sector_total_offensive DOUBLE DEFAULT 0.0,
    sector_total_defensive DOUBLE DEFAULT 0.0,
    sector_global_stats DOUBLE DEFAULT 0.0,

    PRIMARY KEY (board_id, number),
    FOREIGN KEY (board_id) REFERENCES BOARDS(id) ON DELETE CASCADE,
    FOREIGN KEY (owner_id) REFERENCES PLAYERS(id) ON DELETE SET NULL
);

-- =============================================
-- Table SECTOR_NEIGHBORS : Relations de voisinage entre secteurs
-- =============================================
CREATE TABLE IF NOT EXISTS SECTOR_NEIGHBORS (
    board_id BIGINT NOT NULL,
    sector_number INT NOT NULL,
    neighbor_number INT NOT NULL,
    FOREIGN KEY (board_id, sector_number) REFERENCES SECTORS(board_id, number) ON DELETE CASCADE
);

-- =============================================
-- Table PLAYER_OWNED_SECTORS : Secteurs possédés par les joueurs
-- =============================================
CREATE TABLE IF NOT EXISTS PLAYER_OWNED_SECTORS (
    player_id BIGINT NOT NULL,
    sector_number INT NOT NULL,
    PRIMARY KEY (player_id, sector_number)
    -- Note: Pas de FK vers SECTORS car la clé primaire de SECTORS est composite (board_id, number)
    -- L'intégrité sera gérée au niveau applicatif
);

-- =============================================
-- Table UNITS : Unités dans les secteurs
-- =============================================
CREATE TABLE IF NOT EXISTS UNITS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    board_id BIGINT NOT NULL,
    sector_number INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    number INT NOT NULL DEFAULT 0,
    experience DOUBLE NOT NULL DEFAULT 0.0,
    type VARCHAR(50) NOT NULL, -- LARBIN, VOYOU, MALFRAT, BRUTE, etc.
    is_injured BOOLEAN NOT NULL DEFAULT FALSE,

    -- Statistiques
    attack DOUBLE NOT NULL,
    defense DOUBLE NOT NULL,
    pdf DOUBLE NOT NULL,
    pdc DOUBLE NOT NULL,
    armor DOUBLE NOT NULL,
    evasion DOUBLE NOT NULL,

    FOREIGN KEY (board_id, sector_number) REFERENCES SECTORS(board_id, number) ON DELETE CASCADE
);

-- =============================================
-- Table UNIT_CLASSES : Classes de spécialisation des unités
-- =============================================
CREATE TABLE IF NOT EXISTS UNIT_CLASSES (
    unit_id BIGINT NOT NULL,
    unit_class VARCHAR(50) NOT NULL, -- LEGER, TIREUR, MASTODONTE, etc.
    FOREIGN KEY (unit_id) REFERENCES UNITS(id) ON DELETE CASCADE
);

-- =============================================
-- Table UNIT_EQUIPMENTS : Équipements équipés par les unités
-- Ces équipements décomptent la disponibilité dans EQUIPMENT_STACKS
-- =============================================
CREATE TABLE IF NOT EXISTS UNIT_EQUIPMENTS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    FOREIGN KEY (unit_id) REFERENCES UNITS(id) ON DELETE CASCADE,
    FOREIGN KEY (equipment_id) REFERENCES EQUIPMENT(id) ON DELETE CASCADE
);

-- =============================================
-- Table CREDENTIALS : Informations d'authentification des joueurs
-- =============================================
CREATE TABLE IF NOT EXISTS CREDENTIALS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    refresh_token_hash VARCHAR(255),
    refresh_token_expiry BIGINT
);

-- =============================================
-- Index pour améliorer les performances
-- =============================================
CREATE INDEX IF NOT EXISTS idx_equipment_stacks_player ON EQUIPMENT_STACKS(player_id);
CREATE INDEX IF NOT EXISTS idx_equipment_stacks_equipment ON EQUIPMENT_STACKS(equipment_id);
CREATE INDEX IF NOT EXISTS idx_sectors_owner ON SECTORS(owner_id);
CREATE INDEX IF NOT EXISTS idx_units_sector ON UNITS(board_id, sector_number);
CREATE INDEX IF NOT EXISTS idx_unit_equipments_unit ON UNIT_EQUIPMENTS(unit_id);
CREATE INDEX IF NOT EXISTS idx_unit_equipments_equipment ON UNIT_EQUIPMENTS(equipment_id);
CREATE INDEX IF NOT EXISTS idx_sector_neighbors_sector ON SECTOR_NEIGHBORS(board_id, sector_number);
CREATE INDEX IF NOT EXISTS idx_player_owned_sectors_player ON PLAYER_OWNED_SECTORS(player_id);
CREATE INDEX IF NOT EXISTS idx_player_owned_sectors_sector ON PLAYER_OWNED_SECTORS(sector_number);
