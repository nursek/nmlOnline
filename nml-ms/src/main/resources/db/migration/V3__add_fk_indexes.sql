-- Index sur les colonnes FK non indexées (Postgres n'indexe pas les FK automatiquement).
-- Cible les requêtes dérivées les plus fréquentes : findByPlayerId, collection loads,
-- findByTurnAndStatus, collection loads d'ordres (route/entityIds).
CREATE INDEX IF NOT EXISTS idx_combat_entities_player_id    ON combat_entities (player_id);
CREATE INDEX IF NOT EXISTS idx_combat_entities_vehicle_id   ON combat_entities (vehicle_id);
CREATE INDEX IF NOT EXISTS idx_equipment_stacks_player_id   ON equipment_stacks (player_id);
CREATE INDEX IF NOT EXISTS idx_equipment_stacks_weapon_cache_id ON equipment_stacks (weapon_cache_id);
CREATE INDEX IF NOT EXISTS idx_player_resources_player_id  ON player_resources (player_id);
CREATE INDEX IF NOT EXISTS idx_player_resources_bank_id     ON player_resources (bank_id);
CREATE INDEX IF NOT EXISTS idx_unit_equipments_unit_id       ON unit_equipments (unit_id);
CREATE INDEX IF NOT EXISTS idx_unit_classes_unit_id          ON unit_classes (unit_id);
CREATE INDEX IF NOT EXISTS idx_movement_orders_turn_status  ON movement_orders (turn, status);
CREATE INDEX IF NOT EXISTS idx_movement_orders_player_id     ON movement_orders (player_id);
CREATE INDEX IF NOT EXISTS idx_movement_order_route_order_id    ON movement_order_route (order_id);
CREATE INDEX IF NOT EXISTS idx_movement_order_entities_order_id ON movement_order_entities (order_id);
