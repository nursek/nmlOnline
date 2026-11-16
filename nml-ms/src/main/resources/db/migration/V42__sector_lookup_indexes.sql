CREATE INDEX IF NOT EXISTS idx_combat_entities_board_sector ON combat_entities (board_id, sector_number);
CREATE INDEX IF NOT EXISTS idx_sectors_owner_id ON sectors (owner_id);
